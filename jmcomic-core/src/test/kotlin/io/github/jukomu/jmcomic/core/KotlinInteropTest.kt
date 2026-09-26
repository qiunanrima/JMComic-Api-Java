package io.github.jukomu.jmcomic.core

import io.github.jukomu.jmcomic.api.model.JmImage
import io.github.jukomu.jmcomic.api.result.JmResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinInteropTest {

    @Test
    fun testKotlinDslInitAndTokenStore() {
        val customStore = object : JmTokenStore {
            var saved: JmSession? = null
            override fun loadSession(): JmSession? = saved
            override fun saveSession(session: JmSession?) {
                saved = session
            }
        }

        // Test Kotlin DSL init
        val client = Jm.init(tokenStore = customStore) {
            retryTimes(2)
        }

        assertNotNull(client)
        assertSame(client, Jm.client)
        assertSame(customStore, Jm.tokenStore)

        // Verify session persistence mechanism
        val session = JmSession(username = "test_user", userId = "123456", cookies = listOf("session=abc"))
        Jm.restoreSession(session)

        assertEquals("test_user", customStore.saved?.username)
        assertEquals("123456", customStore.saved?.userId)
        assertTrue(Jm.isLoggedIn)
        assertEquals("test_user", Jm.client.username)
    }

    @Test
    fun testFileJmTokenStore(@TempDir tempDir: File) {
        val sessionFile = File(tempDir, "jm_session.json")
        val store = FileJmTokenStore(sessionFile)

        assertNull(store.loadSession())

        val session = JmSession(username = "file_user", userId = "999", cookies = listOf("token=xyz"))
        store.saveSession(session)

        val loaded = store.loadSession()
        assertNotNull(loaded)
        assertEquals("file_user", loaded?.username)
        assertEquals("999", loaded?.userId)
        assertEquals(listOf("token=xyz"), loaded?.cookies)

        // Clear session
        store.saveSession(null)
        assertNull(store.loadSession())
    }

    @Test
    fun testJmResultKotlinExtensions() {
        val success = JmResult.success("hello")
        assertEquals("hello", success.getOrNull())
        assertNull(success.exceptionOrNull())

        val transformed = success.map { it.uppercase() }
        assertEquals("HELLO", transformed.getOrNull())

        val folded = success.fold(
            { "success: $it" },
            { "error: ${it.message}" }
        )
        assertEquals("success: hello", folded)

        val failure = JmResult.failure<String>(RuntimeException("boom"))
        assertTrue(failure.isFailure)
        assertEquals("default", failure.getOrDefault("default"))
    }

    @Test
    fun testImageExtensions() {
        // Scramble check: photoId >= 220980 is scrambled
        // photoId, scrambleId, filename, url, queryParams, sortOrder
        val unscrambled = JmImage("100000", "220980", "00001.jpg", "https://cdn.example.com/1.jpg", null, 1)
        assertFalse(unscrambled.isScrambled())

        val scrambled = JmImage("300000", "220980", "00001.jpg", "https://cdn.example.com/1.jpg", null, 1)
        assertTrue(scrambled.isScrambled())

        // Page URL extension
        assertEquals("https://cdn.example.com/1.jpg", scrambled.toPageUrl())
    }
}
