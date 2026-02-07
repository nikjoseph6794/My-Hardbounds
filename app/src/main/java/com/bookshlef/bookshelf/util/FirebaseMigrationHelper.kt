package com.bookshlef.bookshelf.utils

import com.bookshlef.bookshelf.db.BookDao
import com.bookshlef.bookshelf.db.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirebaseMigrationHelper {

    suspend fun migrateIfNeeded(
        context: android.content.Context,
        bookDao: BookDao,
        wishlistDao: WishlistDao
    ) {
        if (MigrationPrefs.isMigrated(context)) return

        withContext(Dispatchers.IO) {
            // 🔹 Migrate Library
            val books = bookDao.getAllOnce()
            books.forEach { book ->
                FirebaseSyncHelper.addBook(book)
            }

            // 🔹 Migrate Wishlist
            val wishlist = wishlistDao.getAllOnce()
            wishlist.forEach { entry ->
                FirebaseSyncHelper.addWishlist(entry)
            }
        }

        // ✅ Mark migration complete
        MigrationPrefs.setMigrated(context)
    }
}
