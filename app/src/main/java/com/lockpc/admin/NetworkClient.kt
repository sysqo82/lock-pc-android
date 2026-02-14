package com.lockpc.admin

import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Request

object NetworkClient {
    private val cookieJar = SessionCookieJar()

    private val okHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val authInterceptor = Interceptor { chain ->
            val reqBuilder: Request.Builder = chain.request().newBuilder()
            try {
                val prefs = SecurePrefs.get(MainApplication.appContext())
                val token = prefs.getString(SecurePrefs.KEY_JWT, null)
                if (!token.isNullOrEmpty()) {
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }
                    // Add bypass header to avoid tunnel password pages when using proxies/tunnels
                    reqBuilder.addHeader("bypass-tunnel-reminder", "true")
            } catch (_: Exception) {
            }
            chain.proceed(reqBuilder.build())
        }
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    // Return the Cookie header string for the provided base URL if cookies are present.
    // This is used to supply cookies to non-HTTP clients (e.g. Socket.IO handshake).
    fun getCookieHeader(url: String): String? {
        return try {
            val parsed = url.toHttpUrlOrNull()
            parsed?.host?.let { cookieJar.getCookieHeaderForHost(it) }
        } catch (_: Exception) {
            null
        }
    }
}
