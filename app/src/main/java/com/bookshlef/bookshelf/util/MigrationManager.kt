package com.bookshlef.bookshelf.util

import android.content.Context
import android.util.Log
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.db.WishlistEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object MigrationManager {
    private const val PREF_NAME = "migration_prefs"
    private const val KEY_MIGRATED = "is_migrated_to_cloud"

    suspend fun migrateDataToCloud(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            Log.d("Migration", "Data already migrated")
            return true
        }

        return withContext(Dispatchers.IO) {
            try {
                val db = AppDb.get(context)
                val firestore = FirebaseFirestore.getInstance()
                val batch = firestore.batch()

                // 1. Migrate Library Books
                val books = db.bookDao().getAllNow() 
                if (books.isNotEmpty()) {
                    val booksRef = firestore.collection("users").document(userId).collection("books")
                    books.forEach { book ->
                        val docRef = booksRef.document(book.isbn)
                        batch.set(docRef, book)
                    }
                }

                // 2. Migrate Wishlist
                val wishlist = db.wishlistDao().getAllNow()
                if (wishlist.isNotEmpty()) {
                    val wishlistRef = firestore.collection("users").document(userId).collection("wishlist")
                    wishlist.forEach { item ->
                        val docRef = wishlistRef.document(item.isbn)
                        batch.set(docRef, item)
                    }
                }

                // 3. Migrate Scan History
                val history = db.scanHistoryDao().getAllNow()
                if (history.isNotEmpty()) {
                    val historyRef = firestore.collection("users").document(userId).collection("history")
                    history.forEach { item ->
                        val docRef = historyRef.document(item.isbn)
                        batch.set(docRef, item)
                    }
                }

                // Commit the batch
                batch.commit().await()

                // Mark as migrated
                prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
                Log.d("Migration", "Migration successful")
                true
            } catch (e: Exception) {
                Log.e("Migration", "Migration failed", e)
                false
            }
        }
    }
}
