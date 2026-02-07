package com.bookshlef.bookshelf.db

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "wishlist")
data class WishlistEntry(
    @PrimaryKey
    var isbn: String = "",

    var title: String = "",
    var authors: String = "",
    var description: String = "",
    var coverUrl: String = "",

    var addedAt: Long = 0L
)

