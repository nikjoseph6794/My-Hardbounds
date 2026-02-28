package com.bookshlef.bookshelf.data

import android.content.Context
import android.util.Log
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.db.ScanHistoryEntry
import com.bookshlef.bookshelf.db.WishlistEntry
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object BookRepository {
    private var db: AppDb? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSyncing = false

    fun init(context: Context) {
        if (db == null) {
            db = AppDb.get(context)
        }
    }

    private fun getUid(): String? = auth.currentUser?.uid

    fun startSync() {
        if (isSyncing) return
        val uid = getUid() ?: return
        isSyncing = true
        
        Log.d("BookRepository", "Starting sync for user: $uid")

        // Sync Books
        firestore.collection("users").document(uid).collection("books")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("BookRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val dao = db?.bookDao() ?: return@launch
                    snapshots?.documentChanges?.forEach { change ->
                        val book = change.document.toObject(Book::class.java)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                dao.upsert(book)
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteByIsbn(book.isbn)
                            }
                        }
                    }
                }
            }

        // Sync Wishlist
        firestore.collection("users").document(uid).collection("wishlist")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                scope.launch {
                    val dao = db?.wishlistDao() ?: return@launch
                    snapshots?.documentChanges?.forEach { change ->
                        val item = change.document.toObject(WishlistEntry::class.java)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                dao.upsert(item)
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteByIsbn(item.isbn)
                            }
                        }
                    }
                }
            }

        // Sync Scan History
        firestore.collection("users").document(uid).collection("history")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                scope.launch {
                    val dao = db?.scanHistoryDao() ?: return@launch
                    snapshots?.documentChanges?.forEach { change ->
                        val item = change.document.toObject(ScanHistoryEntry::class.java)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                dao.upsert(item)
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteByIsbn(item.isbn)
                            }
                        }
                    }
                }
            }
    }

    suspend fun addBook(book: Book) {
        db?.bookDao()?.upsert(book)
        val uid = getUid() ?: return
        try {
            firestore.collection("users").document(uid).collection("books")
                .document(book.isbn).set(book).await()
        } catch (e: Exception) {
            Log.e("BookRepository", "Error uploading book", e)
        }
    }

    suspend fun removeBook(book: Book) {
        db?.bookDao()?.delete(book)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("books")
            .document(book.isbn).delete()
    }

    suspend fun addToWishlist(entry: WishlistEntry) {
        db?.wishlistDao()?.upsert(entry)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("wishlist")
            .document(entry.isbn).set(entry)
    }

    suspend fun removeFromWishlist(entry: WishlistEntry) {
        removeFromWishlistByIsbn(entry.isbn)
    }

    suspend fun removeBookByIsbn(isbn: String) {
        db?.bookDao()?.deleteByIsbn(isbn)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("books")
            .document(isbn).delete()
    }

    suspend fun removeFromWishlistByIsbn(isbn: String) {
        db?.wishlistDao()?.deleteByIsbn(isbn)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("wishlist")
            .document(isbn).delete()
    }

    suspend fun addScanHistory(entry: ScanHistoryEntry) {
        db?.scanHistoryDao()?.upsert(entry)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("history")
            .document(entry.isbn).set(entry)
    }

    suspend fun removeScanHistoryByIsbn(isbn: String) {
        db?.scanHistoryDao()?.deleteByIsbn(isbn)
        val uid = getUid() ?: return
        firestore.collection("users").document(uid).collection("history")
            .document(isbn).delete()
    }

    suspend fun updateReadStatus(isbn: String, isRead: Boolean) {
        db?.bookDao()?.setRead(isbn, isRead)
        val uid = getUid() ?: return
        try {
            firestore.collection("users").document(uid).collection("books")
                .document(isbn).update("isRead", isRead).await()
        } catch (e: Exception) {
            Log.e("BookRepository", "Error updating read status", e)
        }
    }
}
