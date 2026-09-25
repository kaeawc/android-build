package dev.jasonpearson.android.data.photography

import kotlinx.serialization.Serializable

@Serializable
data class GalleryPhoto(
    val thumbUrl: String,
    val fullUrl: String,
    val width: Int?,
    val height: Int?,
    val takenOn: String?,
)
