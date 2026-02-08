package com.bookshlef.bookshelf.utils

import com.bookshlef.bookshelf.db.BookDao
import com.bookshlef.bookshelf.db.WishlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LegacyCloudSyncHelper {

    suspend fun syncLocalToCloud(
        bookDao: BookDao,
        wishlistDao: WishlistDao
    ) {
        withContext(Dispatchers.IO) {

            // Sync Library
            val books = bookDao.getAllOnce()
            books.forEach { book ->
                FirebaseSyncHelper.addBook(book)
            }

            // Sync Wishlist
            val wishlist = wishlistDao.getAllOnce()
            wishlist.forEach { entry ->
                FirebaseSyncHelper.addWishlist(entry)
            }
        }
    }
}
