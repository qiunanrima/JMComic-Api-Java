package io.github.jukomu.jmcomic.core

import java.io.File

/**
 * JMComic 会话数据实体，存储登录用户名、用户ID及 Cookie 列表。
 */
data class JmSession(
    val username: String? = null,
    val userId: String? = null,
    val cookies: List<String> = emptyList()
) {
    val isValid: Boolean get() = !username.isNullOrBlank()
}

/**
 * JMComic 会话持久化接口，设计对齐 picapi 的 `PicaTokenStore`。
 * 应用可自行实现（如 Android SharedPreferences / DataStore）并传递给 [Jm.init]。
 */
interface JmTokenStore {
    /**
     * 读取已持久化的会话信息，若没有或已失效则返回 null。
     */
    fun loadSession(): JmSession?

    /**
     * 持久化保存会话信息；传 null 表示清除持久化状态。
     */
    fun saveSession(session: JmSession?)
}

/**
 * 默认内存实现：仅保存在内存中，进程重启即丢失。
 */
object MemoryJmTokenStore : JmTokenStore {
    @Volatile
    private var session: JmSession? = null

    override fun loadSession(): JmSession? = session

    override fun saveSession(session: JmSession?) {
        this.session = session
    }
}

/**
 * 基于文件的会话存储，适合桌面/服务端 JVM 环境。
 */
class FileJmTokenStore(private val file: File) : JmTokenStore {

    private val gson = com.google.gson.Gson()

    override fun loadSession(): JmSession? = try {
        if (file.exists()) {
            val json = file.readText().trim()
            if (json.isNotBlank()) gson.fromJson(json, JmSession::class.java) else null
        } else null
    } catch (_: Exception) {
        null
    }

    override fun saveSession(session: JmSession?) {
        try {
            if (session == null || !session.isValid) {
                if (file.exists()) file.delete()
            } else {
                file.parentFile?.mkdirs()
                file.writeText(gson.toJson(session))
            }
        } catch (_: Exception) {
            // 忽略写入失败，保证主业务不中断
        }
    }
}
