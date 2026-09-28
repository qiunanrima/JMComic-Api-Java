package io.github.jukomu.jmcomic.core

import io.github.jukomu.jmcomic.api.model.*
import io.github.jukomu.jmcomic.api.result.JmResult
import io.github.jukomu.jmcomic.core.client.JmAsyncClient
import io.github.jukomu.jmcomic.core.client.JmComicClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import java.util.concurrent.CompletableFuture

/**
 * 全局单例管理器，跨页面或组件共享同一个 [JmComicClient] 与登录会话。
 * 对齐 picapi 的 `Pica` 设计风格，Java 与 Kotlin 均可极简调用。
 *
 * ### Kotlin 使用示例
 * ```kotlin
 * // 启动时初始化
 * Jm.init {
 *     retryTimes(3)
 * }
 *
 * // 登录
 * val loginRes = Jm.login("username", "password")
 *
 * // 业务页面直接调用单例
 * val detail = Jm.client.getComicDetail("12345")
 * val pages = Jm.client.getComicPages("67890")
 *
 * // 异步非阻塞调用
 * Jm.async.getComicDetail("12345").thenAccept { res ->
 *     res.onSuccess { comic -> println(comic.title) }
 * }
 * ```
 *
 * ### Java 使用示例
 * ```java
 * Jm.init(JmConfiguration.defaultConfig());
 * JmResult<JmAlbum> result = Jm.getComicDetail("12345");
 * result.onSuccess(album -> System.out.println(album.title()));
 * ```
 */
object Jm {

    @Volatile
    private var defaultClient: JmComicClient? = null

    @Volatile
    @JvmStatic
    var tokenStore: JmTokenStore = MemoryJmTokenStore

    /** 是否已完成初始化 */
    @JvmStatic
    val isInitialized: Boolean
        get() = defaultClient != null

    /** 是否当前已处于登录状态 */
    @JvmStatic
    val isLoggedIn: Boolean
        get() = isInitialized && (defaultClient?.isLoggedIn == true)

    /** 获取全局客户端实例（未初始化抛 IllegalStateException） */
    @JvmStatic
    val client: JmComicClient
        get() = defaultClient ?: error("Jm is not initialized. Please call Jm.init(...) first.")

    /** 获取全局客户端实例（兼容 Java Jm.client() 命名） */
    @JvmStatic
    fun client(): JmComicClient = client

    /** 获取全局异步客户端实例 */
    @JvmStatic
    val async: JmAsyncClient
        get() = client.async()

    /** 获取全局异步客户端实例（兼容 Java Jm.async() 命名） */
    @JvmStatic
    fun async(): JmAsyncClient = async

    /**
     * 初始化全局客户端。自动恢复 [tokenStore] 中的历史会话。
     *
     * @param config 客户端配置
     * @param tokenStore 会话持久化存储
     * @return 全局 JmComicClient 实例
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        config: JmConfiguration = JmConfiguration.defaultConfig(),
        tokenStore: JmTokenStore = MemoryJmTokenStore
    ): JmComicClient {
        synchronized(this) {
            this.tokenStore = tokenStore
            val newClient = JmComicClient.create(config)
            defaultClient = newClient

            val saved = tokenStore.loadSession()
            if (saved != null && saved.isValid) {
                newClient.restoreSession(saved)
            }
            return newClient
        }
    }

    /**
     * Kotlin DSL: 使用配置块快速初始化单例。
     */
    inline fun init(
        tokenStore: JmTokenStore = MemoryJmTokenStore,
        block: JmConfiguration.Builder.() -> Unit
    ): JmComicClient {
        val builder = JmConfiguration.builder()
        builder.block()
        return init(builder.build(), tokenStore)
    }

    /**
     * 异步初始化全局客户端：返回的 [CompletableFuture] 仅在客户端完整初始化后成功完成，
     * 初始化失败（含自动重试后仍失败）时以
     * [io.github.jukomu.jmcomic.api.exception.JmClientInitializationException] 异常完成。
     *
     * ### Kotlin 使用示例
     * ```kotlin
     * Jm.initAsync { retryTimes(3) }.thenAccept { client ->
     *     println("客户端就绪: ${client.isInitialized}")
     * }
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun initAsync(
        config: JmConfiguration = JmConfiguration.defaultConfig(),
        tokenStore: JmTokenStore = MemoryJmTokenStore
    ): CompletableFuture<JmComicClient> {
        val client = init(config, tokenStore)
        return client.initializationFuture().thenApply { client }
    }

    /**
     * 设置自定义或现有的客户端为全局单例。
     */
    @JvmStatic
    fun setClient(client: JmComicClient?) {
        synchronized(this) {
            defaultClient = client
        }
    }

    /**
     * 恢复登录会话状态与 Cookies。
     */
    @JvmStatic
    fun restoreSession(session: JmSession?) {
        if (session != null) {
            client.restoreSession(session)
        }
        tokenStore.saveSession(session)
    }

    // =========================================================================
    // 全局单例直接代理常用方法 (Global Shortcut Methods)
    // =========================================================================

    /** 登录 */
    @JvmStatic
    fun login(username: String, password: String): JmResult<JmUserInfo> {
        val res = client.login(username, password)
        if (res.isSuccess) {
            tokenStore.saveSession(client.extractSession())
        }
        return res
    }

    /** 登出 */
    @JvmStatic
    fun logout(): JmResult<Void> {
        val res = client.logout()
        tokenStore.saveSession(null)
        return res
    }

    /** 获取漫画/本子详情 */
    @JvmStatic
    fun getComicDetail(comicId: String): JmResult<JmAlbum> = client.getComicDetail(comicId)

    /** 获取漫画章节列表 */
    @JvmStatic
    fun getComicEpisodes(comicId: String): JmResult<List<JmPhotoMeta>> = client.getComicEpisodes(comicId)

    /** 获取章节详情 */
    @JvmStatic
    fun getComicEpisode(episodeId: String): JmResult<JmPhoto> = client.getComicEpisode(episodeId)

    /** 获取章节图片列表 */
    @JvmStatic
    fun getComicPages(episodeId: String): JmResult<List<JmImage>> = client.getComicPages(episodeId)

    /** 搜索漫画 */
    @JvmStatic
    fun searchComics(keyword: String): JmResult<JmSearchPage> = client.searchComics(keyword)

    /** 搜索漫画（带页码） */
    @JvmStatic
    fun searchComics(keyword: String, page: Int): JmResult<JmSearchPage> = client.searchComics(keyword, page)

    /** 搜索漫画（高级查询） */
    @JvmStatic
    fun searchComics(query: SearchQuery): JmResult<JmSearchPage> = client.searchComics(query)

    /** 收藏漫画 */
    @JvmStatic
    fun favouriteComic(comicId: String): JmResult<Void> = client.favouriteComic(comicId)

    /** 点赞漫画 */
    @JvmStatic
    fun likeComic(comicId: String): JmResult<Void> = client.likeComic(comicId)

    /** 获取收藏夹 */
    @JvmStatic
    fun getFavouriteComics(page: Int): JmResult<JmFavoritePage> = client.getFavouriteComics(page)

    /** 每日签到 */
    @JvmStatic
    fun punchIn(): JmResult<Void> = client.punchIn()

    /** 获取当前用户信息 */
    @JvmStatic
    fun getUserProfile(): JmResult<JmUserProfile> = client.getUserProfile()
}
