package com.aylis.comp.photos.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApi {
    @GET("api/v1/search")
    suspend fun searchPhotos(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("categories") categories: String = "111", // General(1), Anime(1), People(1)
        @Query("purity") purity: String = "100", // SFW only
        @Query("sorting") sorting: String = "random"
    ): WallhavenSearchResponse

    @GET("api/v1/w/{id}")
    suspend fun getPhotoDetails(
        @retrofit2.http.Path("id") id: String
    ): WallhavenPhotoDetailsResponse
}
