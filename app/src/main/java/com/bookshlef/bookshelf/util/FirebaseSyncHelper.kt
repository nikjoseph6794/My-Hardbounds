package com.bookshlef.bookshelf.util

import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.db.WishlistEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseSyncHelper {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 🔐 Ensure user is signed in (anonymous)
    private suspend fun ensureUser(): String {
        val current = auth.currentUser
        if (current != null) return current.uid

        val result = auth.signInAnonymously().await()
        return result.user!!.uid
    }

    // 📚 Sync full library (used during migration)
    suspend fun syncLibrary(books: List<Book>) {
        val uid = ensureUser()
        val batch = db.batch()

        books.forEach { book ->
            val ref = db.collection("users")
                .document(uid)
                .collection("library")
                .document(book.isbn)

            batch.set(ref, book)
        }

        batch.commit().await()
    }

    // 📝 Sync full wishlist
    suspend fun syncWishlist(wishlist: List<WishlistEntry>) {
        val uid = ensureUser()
        val batch = db.batch()

        wishlist.forEach { w ->
            val ref = db.collection("users")
                .document(uid)
                .collection("wishlist")
                .document(w.isbn)

            batch.set(ref, w)
        }

        batch.commit().await()
    }

    // ➕ Add single book
    suspend fun addBook(book: Book) {
        val uid = ensureUser()
        db.collection("users")
            .document(uid)
            .collection("library")
            .document(book.isbn)
            .set(book)
            .await()
    }

    // ❌ Remove book
    suspend fun removeBook(isbn: String) {
        val uid = ensureUser()
        db.collection("users")
            .document(uid)
            .collection("library")
            .document(isbn)
            .delete()
            .await()
    }

    // ➕ Add wishlist item
    suspend fun addWishlist(entry: WishlistEntry) {
        val uid = ensureUser()
        db.collection("users")
            .document(uid)
            .collection("wishlist")
            .document(entry.isbn)
            .set(entry)
            .await()
    }

    // ❌ Remove wishlist item
    suspend fun removeWishlist(isbn: String) {
        val uid = ensureUser()
        db.collection("users")
            .document(uid)
            .collection("wishlist")
            .document(isbn)
            .delete()
            .await()
    }
}
