package com.bookshlef.bookshelf.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface OpenLibraryApi {
    // ✅ Correct method name: searchByIsbn (capital I)
    @GET("search.json")
    suspend fun searchByIsbn(@Query("isbn") isbn: String): OpenLibrarySearch

    // Optional: edition details by ISBN (may return more metadata)
    @GET("isbn/{isbn}.json")
    suspend fun editionByIsbn(@Path("isbn") isbn: String): OpenLibraryEdition?

    @GET("search.json")
    suspend fun searchByAuthor(@Query("author") author: String): OpenLibrarySearch

}

// ---- Models ----
data class OpenLibrarySearch(
    @SerializedName("numFound") val numFound: Int? = 0,
    @SerializedName("docs") val docs: List<OpenLibraryDoc> = emptyList()
)

data class OpenLibraryDoc(
    @SerializedName("title") val title: String? = null,
    @SerializedName("author_name") val author_name: List<String>? = null,
    @SerializedName("isbn") val isbn: List<String>? = null,
    @SerializedName("cover_i") val cover_i: Int? = null,
    @SerializedName("first_publish_year") val first_publish_year: Int? = null
)

data class OpenLibraryEdition(
    @SerializedName("title") val title: String? = null,
    @SerializedName("publishers") val publishers: List<String>? = null,
    @SerializedName("number_of_pages") val number_of_pages: Int? = null
)
