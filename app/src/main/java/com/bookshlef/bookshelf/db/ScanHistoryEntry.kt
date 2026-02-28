package com.bookshlef.bookshelf.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntry(
    @PrimaryKey val isbn: String = "",
    val title: String = "",
    val authors: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val scannedAt: Long = 0L
)
