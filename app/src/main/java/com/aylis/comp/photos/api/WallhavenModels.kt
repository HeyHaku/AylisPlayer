package com.aylis.comp.photos.api

import com.squareup.moshi.Json

data class WallhavenPhoto(
    val id: String,
    val url: String,
    val path: String,
    val ratio: String?,
    val thumbs: WallhavenThumbs,
    val source: String? = null,
    val views: Int = 0,
    val favorites: Int = 0
)

data class WallhavenThumbs(
    val large: String,
    val original: String,
    val small: String
)

data class WallhavenSearchResponse(
    val data: List<WallhavenPhoto>
)

data class WallhavenPhotoDetailsResponse(
    val data: WallhavenPhotoDetails
)

data class WallhavenPhotoDetails(
    val id: String,
    val url: String,
    val path: String,
    val uploader: Uploader?,
    val tags: List<Tag>,
    val views: Int = 0,
    val favorites: Int = 0
)

data class Uploader(
    val username: String,
    val avatar: AvatarUrls? = null
)

data class AvatarUrls(
    @Json(name = "200px") val px200: String?,
    @Json(name = "128px") val px128: String?,
    @Json(name = "32px") val px32: String?,
    @Json(name = "20px") val px20: String?
)

data class Tag(
    val name: String
)
