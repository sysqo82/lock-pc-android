package com.lockpc.admin

import org.junit.Assert.assertTrue
import org.junit.Test

class ApiConfigTest {

    @Test
    fun baseUrl_isValidHttpsUrlEndingWithSlash() {
        val url = ApiConfig.BASE_URL
        assertTrue("Base URL should start with https://", url.startsWith("https://"))
        assertTrue("Base URL should end with a trailing slash", url.endsWith("/"))
    }
}
