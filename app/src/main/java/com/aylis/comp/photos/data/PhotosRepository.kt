package com.aylis.comp.photos.data

import com.aylis.comp.photos.api.WallhavenApi
import com.aylis.comp.photos.api.WallhavenPhoto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object PhotosRepository {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://wallhaven.cc/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(WallhavenApi::class.java)

    suspend fun getFeed(page: Int = 1, categories: String = "111"): List<WallhavenPhoto> {
        return try {
            // Empty query with 'random' sorting returns a random feed
            api.searchPhotos(query = "", page = page, categories = categories).data
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchPhotos(query: String, page: Int = 1, categories: String = "111"): List<WallhavenPhoto> {
        return try {
            api.searchPhotos(query = query, page = page, categories = categories).data
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPhotoDetails(id: String): com.aylis.comp.photos.api.WallhavenPhotoDetails? {
        return try {
            api.getPhotoDetails(id).data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
