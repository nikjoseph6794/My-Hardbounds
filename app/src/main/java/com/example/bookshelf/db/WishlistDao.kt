package com.example.bookshelf.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WishlistEntry)

    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WishlistEntry>>

    @Query("SELECT * FROM wishlist WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): WishlistEntry?

    @Delete
    suspend fun delete(entry: WishlistEntry)

    @Query("DELETE FROM wishlist WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
