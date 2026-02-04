package com.bookshlef.bookshelf.db

import androidx.room.Dao
import kotlinx.coroutines.flow.Flow
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query



@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getAll(): Flow<List<ScanHistoryEntry>>

    @Query("SELECT * FROM scan_history WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): ScanHistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ScanHistoryEntry)

    @Delete
    suspend fun delete(entry: ScanHistoryEntry)

    @Query("DELETE FROM scan_history WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
