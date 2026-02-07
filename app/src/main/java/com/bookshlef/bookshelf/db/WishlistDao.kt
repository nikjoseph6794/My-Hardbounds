package com.bookshlef.bookshelf.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WishlistEntry>>

    @Query("SELECT * FROM wishlist")
    suspend fun getAllOnce(): List<WishlistEntry>

    @Query("SELECT * FROM wishlist WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): WishlistEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WishlistEntry)

    @Query("DELETE FROM wishlist WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)

    @Delete
    suspend fun delete(entry: WishlistEntry)
}
