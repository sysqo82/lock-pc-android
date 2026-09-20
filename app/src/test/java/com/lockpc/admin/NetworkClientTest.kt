package com.lockpc.admin

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkClientTest {

    @Test
    fun getCookieHeader_returnsNullForInvalidUrl() {
        val result = NetworkClient.getCookieHeader("not a valid url")
        assertNull(result)
    }

    @Test
    fun getCookieHeader_returnsNullWhenNoCookiesForHost() {
        val result = NetworkClient.getCookieHeader("https://nonexistent-host-12345.com/path")
        assertNull(result)
    }

    @Test
    fun retrofit_isNotNullAndConfigured() {
        assertNotNull(NetworkClient.retrofit)
        assertNotNull(NetworkClient.create(ApiService::class.java))
    }
}
