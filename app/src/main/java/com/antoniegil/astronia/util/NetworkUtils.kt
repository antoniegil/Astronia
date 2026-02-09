package com.antoniegil.astronia.util

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkUtils {
    
    fun createHttpClient(
        connectTimeoutMs: Long,
        readTimeoutMs: Long,
        trustAllCerts: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
        
        if (trustAllCerts && Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            @Suppress("CustomX509TrustManager")
            val trustManagers = arrayOf<TrustManager>(object : X509TrustManager {
                @Suppress("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @Suppress("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustManagers, SecureRandom())
            }
            
            builder.sslSocketFactory(sslContext.socketFactory, trustManagers[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        
        return builder.build()
    }
    
    suspend fun testUrlWithHttpsUpgrade(
        url: String,
        timeoutMs: Long = 3000
    ): String = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://", ignoreCase = true)) {
            return@withContext url
        }
        
        val httpsUrl = url.replaceFirst("http://", "https://", ignoreCase = true)
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            
            val request = Request.Builder()
                .url(httpsUrl)
                .head()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) httpsUrl else url
            }
        } catch (e: Exception) {
            url
        }
    }
    
    fun convertToHttps(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true)) {
            url.replaceFirst("http://", "https://", ignoreCase = true)
        } else url
    }
    
    fun convertToHttp(url: String): String {
        return if (url.startsWith("https://", ignoreCase = true)) {
            url.replaceFirst("https://", "http://", ignoreCase = true)
        } else url
    }
    
    fun isPermanentHttpError(code: Int): Boolean = code in listOf(400, 403, 404, 410, 451)
    
    fun isRetriableHttpError(code: Int): Boolean = code in listOf(500, 502, 503, 504)
}
