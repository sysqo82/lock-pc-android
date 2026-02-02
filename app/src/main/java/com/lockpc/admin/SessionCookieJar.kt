package com.lockpc.admin

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private val cookieStore: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val existing = cookieStore.getOrPut(key) { mutableListOf() }
        existing.removeAll { c -> cookies.any { it.name == c.name } }
        existing.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    // Return cookie header string for a given host (e.g. "example.com").
    // Example output: "session=abcd; other=val"
    fun getCookieHeaderForHost(host: String): String? {
        val list = cookieStore[host] ?: return null
        if (list.isEmpty()) return null
        return list.joinToString(separator = "; ") { cookie -> "${cookie.name}=${cookie.value}" }
    }
}
