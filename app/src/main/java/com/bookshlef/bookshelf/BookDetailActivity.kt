package com.bookshlef.bookshelf

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.bookshlef.bookshelf.databinding.ActivityBookDetailBinding
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.WishlistEntry
import com.bookshlef.bookshelf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ISBN = "extra_isbn"
        const val EXTRA_FROM_SCAN = "extra_from_scan"
    }

    private lateinit var b: ActivityBookDetailBinding
    private val dao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }

    private var currentIsbn: String? = null
    private var isReadNow: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // whether this screen was opened right after scanning
        val fromScan = intent.getBooleanExtra(EXTRA_FROM_SCAN, false)

        super.onCreate(savedInstanceState)
        b = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val isbn = intent.getStringExtra(EXTRA_ISBN)
        currentIsbn = isbn
        if (isbn.isNullOrBlank()) {
            Toast.makeText(this, "Missing ISBN", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            // Load from Library
            val book = withContext(Dispatchers.IO) { dao.findByIsbn(isbn) }
            if (book == null) {
                Toast.makeText(this@BookDetailActivity, "Book not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Fill text UI
            b.titleText.text = book.title.ifBlank { "—" }
            b.authorsText.text = book.authors.ifBlank { "—" }
            b.isbnText.text = "ISBN: ${book.isbn}"
            b.descText.text = book.description.ifBlank { "—" }

            // ✅ Cover image (with placeholder if missing)
            b.coverImage.visibility = View.VISIBLE
            if (book.coverUrl.isNotBlank()) {
                b.coverImage.load(book.coverUrl) {
                    placeholder(R.drawable.ic_book_placeholder)
                    error(R.drawable.ic_book_placeholder)
                }
            } else {
                b.coverImage.load(R.drawable.ic_book_placeholder)
            }

            // ✅ Add to Wishlist: show only if came from scan AND not already in wishlist
            if (fromScan) {
                val alreadyInWishlist = withContext(Dispatchers.IO) { wishlistDao.findByIsbn(book.isbn) != null }
                b.addToWishlistBtn.visibility = if (alreadyInWishlist) View.GONE else View.VISIBLE
                b.addToWishlistBtn.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        wishlistDao.upsert(
                            WishlistEntry(
                                isbn = book.isbn,
                                title = book.title,
                                authors = book.authors,
                                description = book.description,
                                coverUrl = book.coverUrl
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@BookDetailActivity, "Added to wishlist ✔", Toast.LENGTH_SHORT).show()
                            b.addToWishlistBtn.visibility = View.GONE
                        }
                    }
                }
            } else {
                b.addToWishlistBtn.visibility = View.GONE
            }

            // Read/Unread tag + toggle
            isReadNow = book.isRead
            renderReadStatus(isReadNow)

            b.toggleReadBtn.setOnClickListener {
                val newValue = !isReadNow
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.setRead(book.isbn, newValue)
                    withContext(Dispatchers.Main) {
                        isReadNow = newValue
                        renderReadStatus(isReadNow)
                        Toast.makeText(
                            this@BookDetailActivity,
                            if (newValue) "Marked as Read" else "Marked as Unread",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Delete from Library
            b.deleteBtn.setOnClickListener {
                AlertDialog.Builder(this@BookDetailActivity)
                    .setTitle("Delete book?")
                    .setMessage("Remove “${book.title.ifBlank { "Untitled" }}” from your library?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            dao.delete(book)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@BookDetailActivity, "Deleted", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun renderReadStatus(isRead: Boolean) {
        // Tag text + color
        b.readTag.text = if (isRead) "Read" else "Unread"
        val color = if (isRead) android.R.color.holo_green_dark else android.R.color.holo_blue_dark
        b.readTag.background.setTint(ContextCompat.getColor(this, color))

        // Toggle button label
        b.toggleReadBtn.text = if (isRead) "Mark as Unread" else "Mark as Read"
    }
}
