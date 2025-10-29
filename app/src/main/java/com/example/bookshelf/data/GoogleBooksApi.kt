package com.example.bookshelf.data

import retrofit2.http.GET
import retrofit2.http.Query

data class VolumeResponse(val items: List<VolumeItem>?)
data class VolumeItem(val volumeInfo: VolumeInfo?)
data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)
data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?          // ← NEW
)

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchByIsbn(@Query("q") q: String): VolumeResponse
}
