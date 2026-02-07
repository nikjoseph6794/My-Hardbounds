package com.bookshlef.bookshelf.utils

import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.db.WishlistEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseSyncHelper {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private suspend fun userId(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user!!
        return user.uid
    }

    /* ---------- LIBRARY ---------- */

    suspend fun addBook(book: Book) {
        val uid = userId()
        db.collection("users")
            .document(uid)
            .collection("library")
            .document(book.isbn)
            .set(book)
            .await()
    }

    suspend fun removeBook(isbn: String) {
        val uid = userId()
        db.collection("users")
            .document(uid)
            .collection("library")
            .document(isbn)
            .delete()
            .await()
    }

    /* ---------- WISHLIST ---------- */

    suspend fun addWishlist(entry: WishlistEntry) {
        val uid = userId()
        db.collection("users")
            .document(uid)
            .collection("wishlist")
            .document(entry.isbn)
            .set(entry)
            .await()
    }

    suspend fun removeWishlist(isbn: String) {
        val uid = userId()
        db.collection("users")
            .document(uid)
            .collection("wishlist")
            .document(isbn)
            .delete()
            .await()
    }
}
