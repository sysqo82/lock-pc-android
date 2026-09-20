package com.lockpc.admin

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionCookieJarTest {

    private lateinit var cookieJar: SessionCookieJar
    private val sampleUrl = "https://example.com/api".toHttpUrl()

    @Before
    fun setUp() {
        cookieJar = SessionCookieJar()
    }

    @Test
    fun loadForRequest_returnsEmptyListWhenNoCookiesSaved() {
        val cookies = cookieJar.loadForRequest(sampleUrl)
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun saveFromResponse_storesCookiesAndLoadsForRequest() {
        val cookie1 = Cookie.Builder()
            .domain("example.com")
            .name("session")
            .value("abc123")
            .build()

        cookieJar.saveFromResponse(sampleUrl, listOf(cookie1))

        val loaded = cookieJar.loadForRequest(sampleUrl)
        assertEquals(1, loaded.size)
        assertEquals("session", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun saveFromResponse_replacesCookieWithSameName() {
        val cookie1 = Cookie.Builder()
            .domain("example.com")
            .name("session")
            .value("old_val")
            .build()
        val cookie2 = Cookie.Builder()
            .domain("example.com")
            .name("session")
            .value("new_val")
            .build()

        cookieJar.saveFromResponse(sampleUrl, listOf(cookie1))
        cookieJar.saveFromResponse(sampleUrl, listOf(cookie2))

        val loaded = cookieJar.loadForRequest(sampleUrl)
        assertEquals(1, loaded.size)
        assertEquals("new_val", loaded[0].value)
    }

    @Test
    fun getCookieHeaderForHost_returnsNullWhenNoCookies() {
        assertNull(cookieJar.getCookieHeaderForHost("example.com"))
    }

    @Test
    fun getCookieHeaderForHost_returnsFormattedHeaderString() {
        val c1 = Cookie.Builder().domain("example.com").name("token").value("xyz").build()
        val c2 = Cookie.Builder().domain("example.com").name("user").value("admin").build()

        cookieJar.saveFromResponse(sampleUrl, listOf(c1, c2))

        val header = cookieJar.getCookieHeaderForHost("example.com")
        assertEquals("token=xyz; user=admin", header)
    }
}
