package com.bookshlef.bookshelf.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val isbn: String,
    val title: String,
    val authors: String,
    val description: String,
    val addedAt: Long,
    val isRead: Boolean = false,
    val coverUrl: String = ""            // ← NEW
)
