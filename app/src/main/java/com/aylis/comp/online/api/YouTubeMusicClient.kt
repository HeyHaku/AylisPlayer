package com.aylis.comp.online.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeMusicClient {

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("search")
    suspend fun searchTracks(
        @Query("key") apiKey: String?,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("browse")
    suspend fun browse(
        @Query("key") apiKey: String?,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Query("continuation") continuation: String? = null,
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("next")
    suspend fun next(
        @Query("key") apiKey: String?,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Query("continuation") continuation: String? = null,
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("like/like")
    suspend fun like(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("like/removelike")
    suspend fun removeLike(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("like/dislike")
    suspend fun dislike(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("playlist/create")
    suspend fun createPlaylist(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("browse/edit_playlist")
    suspend fun editPlaylist(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody

    @Headers(
        "Content-Type: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )
    @POST("account/account_menu")
    suspend fun getAccountMenu(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): okhttp3.ResponseBody
}
