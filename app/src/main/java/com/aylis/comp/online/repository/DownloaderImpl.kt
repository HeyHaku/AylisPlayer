package com.aylis.comp.online.repository

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody.Companion.toRequestBody

class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {
    
    private val client: OkHttpClient = builder.build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        var requestBody: okhttp3.RequestBody? = null
        if (dataToSend != null) {
            requestBody = dataToSend.toRequestBody(null)
        }

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)

        headers.forEach { (key, list) ->
            list.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        val okHttpRequest = requestBuilder.build()
        val response = client.newCall(okHttpRequest).execute()
        
        val responseBody = response.body?.string()
        val latestUrl = response.request.url.toString()

        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers.values(name)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            latestUrl
        )
    }

    companion object {
        private var instance: DownloaderImpl? = null

        fun init(builder: OkHttpClient.Builder?): DownloaderImpl {
            val actualBuilder = builder ?: OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                
            actualBuilder.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                val cookies = com.aylis.comp.online.managers.AuthManager.getCookies()
                if (cookies.isNotEmpty() && original.url.host.contains("youtube.com")) {
                    requestBuilder.header("Cookie", cookies)
                }
                chain.proceed(requestBuilder.build())
            }

            val newInstance = DownloaderImpl(actualBuilder)
            instance = newInstance
            return newInstance
        }

        fun getInstance(): DownloaderImpl {
            return instance ?: init(null)
        }
    }
}
