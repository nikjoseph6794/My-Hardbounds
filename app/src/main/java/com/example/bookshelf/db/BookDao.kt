package com.example.bookshelf.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book)

    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun getAll(): Flow<List<Book>>

    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    suspend fun getAllNow(): List<Book>   // ✅ added for backup export

    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): Book?

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)

    @Query("""
        SELECT * FROM books
        WHERE title   LIKE '%' || :q || '%' COLLATE NOCASE
           OR authors LIKE '%' || :q || '%' COLLATE NOCASE
        ORDER BY addedAt DESC
    """)
    suspend fun search(q: String): List<Book>

    @Query("UPDATE books SET isRead = :read WHERE isbn = :isbn")
    suspend fun setRead(isbn: String, read: Boolean)

    @Query("SELECT * FROM books WHERE isRead = 1 ORDER BY addedAt DESC")
    fun getRead(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isRead = 0 ORDER BY addedAt DESC")
    fun getUnread(): Flow<List<Book>>
}
