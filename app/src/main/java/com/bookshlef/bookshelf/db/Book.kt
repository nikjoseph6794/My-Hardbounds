package com.bookshlef.bookshelf.db


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    var isbn: String = "",

    var title: String = "",
    var authors: String = "",
    var description: String = "",
    var coverUrl: String = "",

    var isRead: Boolean = false,
    var addedAt: Long = 0L
)
