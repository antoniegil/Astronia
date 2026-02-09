package com.antoniegil.astronia.util

import android.os.Build
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object NetworkUtils {

    private val android7TrustManager by lazy {
        @Suppress("CustomX509TrustManager")
        object : X509TrustManager {
            @Suppress("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            @Suppress("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    private val android7SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(android7TrustManager), SecureRandom())
        }
    }

    private var isAndroid7SSLSetup = false

    fun setupAndroid7SSL() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N && !isAndroid7SSLSetup) {
            HttpsURLConnection.setDefaultSSLSocketFactory(android7SSLContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            isAndroid7SSLSetup = true
        }
    }

    fun createHttpClient(
        connectTimeoutMs: Long,
        readTimeoutMs: Long,
        trustAllCerts: Boolean = false
    ): OkHttpClient {
        val builder =
            OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)

        if (trustAllCerts && Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            builder.sslSocketFactory(android7SSLContext.socketFactory, android7TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    fun convertToHttps(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true)) {
            url.replaceFirst("http://", "https://", ignoreCase = true)
        } else url
    }

    fun isPermanentHttpError(code: Int): Boolean = code in listOf(400, 403, 404, 410, 451)

    fun isRetriableHttpError(code: Int): Boolean = code in listOf(500, 502, 503, 504)
}
