package com.example.bookshelf.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntry(
    @PrimaryKey val isbn: String,
    val title: String,
    val authors: String,
    val description: String,
    val coverUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
