package io.github.jukomu.jmcomic.core.client

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/**
 * 客户端初始化状态机。
 *
 * 封装客户端初始化的全部状态流转，职责包括：
 * - 幂等启动：并发或重复触发初始化只会执行一次；
 * - 失败可恢复：初始化最终失败后允许重新初始化，避免瞬时网络故障导致实例永久不可用；
 * - 可观察：通过 [future] 等待初始化完成或失败；
 * - 关闭安全：[beginClose] 后不再允许（重）初始化，等待方会立即收到关闭异常。
 *
 * 本类只负责状态与 Future 的管理，不感知线程与网络细节。
 */
class JmClientInitializer {

    enum class InitState {
        /** 尚未启动过初始化 */
        NOT_STARTED,

        /** 初始化进行中 */
        RUNNING,

        /** 初始化成功 */
        READY,

        /** 初始化最终失败（允许重新初始化） */
        FAILED,

        /** 客户端已关闭，禁止再初始化 */
        CLOSED
    }

    private val lock = Any()
    private var state: InitState = InitState.NOT_STARTED
    private var future: CompletableFuture<Void> = newFuture()

    /** 新初始化 Future 创建时的回调（用于挂接"取消时自动关闭客户端"等监听逻辑）。 */
    var onFutureCreated: FutureCreatedListener? = null
        set(value) {
            synchronized(lock) {
                field = value
                value?.onCreated(future)
            }
        }

    private fun newFuture(): CompletableFuture<Void> = CompletableFuture()

    /** 当前初始化状态 */
    fun state(): InitState = synchronized(lock) { state }

    /** 当前初始化 Future（重新初始化后返回新的 Future） */
    fun future(): CompletableFuture<Void> = synchronized(lock) { future }

    /** 是否已完成初始化 */
    fun isReady(): Boolean = state() == InitState.READY

    /** 是否正在初始化 */
    fun isRunning(): Boolean = state() == InitState.RUNNING

    /**
     * 尝试开始（或重启）初始化。
     * 仅当状态为 [InitState.NOT_STARTED] / [InitState.FAILED] 时生效；
     * 否则返回 null，调用方应直接使用 [future]。
     *
     * @param restart true 表示失败后的重新初始化（会创建新的 Future，旧 Future 保留给历史等待方）
     * @return 本次初始化对应的 Future；不可启动时返回 null
     */
    fun tryBegin(restart: Boolean): CompletableFuture<Void>? = synchronized(lock) {
        when (state) {
            InitState.NOT_STARTED -> {
                state = InitState.RUNNING
                future
            }

            InitState.FAILED -> {
                if (restart) {
                    future = newFuture()
                    onFutureCreated?.onCreated(future)
                }
                state = InitState.RUNNING
                future
            }

            else -> null
        }
    }

    /**
     * 初始化成功：RUNNING -> READY 并完成 Future。
     * 若状态已非 RUNNING（例如已关闭）则返回 false，交由关闭流程收尾。
     */
    fun markReady(): Boolean = synchronized(lock) {
        if (state != InitState.RUNNING) return false
        state = InitState.READY
        future.complete(null)
        true
    }

    /**
     * 初始化最终失败：RUNNING -> FAILED。
     * 若状态已非 RUNNING 返回 false（交由关闭流程收尾）。
     */
    fun markFailed(): Boolean = synchronized(lock) {
        if (state != InitState.RUNNING) return false
        state = InitState.FAILED
        true
    }

    /** 以异常完成初始化 Future（供失败流程在资源清理完成后调用）。 */
    fun completeFailure(exception: Throwable) {
        synchronized(lock) {
            future.completeExceptionally(exception)
        }
    }

    /**
     * 校验初始化是否仍然有效，否则抛出 [CancellationException]。
     * 在初始化任务体内逐步调用，保证客户端关闭后能尽快中断初始化流程。
     */
    fun ensureActive() {
        synchronized(lock) {
            if (state != InitState.RUNNING || Thread.currentThread().isInterrupted()) {
                throw CancellationException("Client initialization is no longer active.")
            }
        }
    }

    /** 进入关闭流程。返回 false 表示客户端已处于关闭状态。 */
    fun beginClose(): Boolean = synchronized(lock) {
        if (state == InitState.CLOSED) return false
        state = InitState.CLOSED
        true
    }

    /** 关闭流程收尾：确保等待方收到关闭异常（若 Future 尚未完成）。 */
    fun completeAsClosed(exception: Throwable) {
        synchronized(lock) {
            future.completeExceptionally(exception)
        }
    }
}

/**
 * 新初始化 Future 创建时的监听器（Java/Kotlin 双端友好）。
 */
fun interface FutureCreatedListener {
    fun onCreated(future: CompletableFuture<Void>)
}
