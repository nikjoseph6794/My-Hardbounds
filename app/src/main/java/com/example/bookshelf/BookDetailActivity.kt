package com.example.bookshelf

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bookshelf.databinding.ActivityBookDetailBinding
import com.example.bookshelf.db.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import coil.load
import com.example.bookshelf.db.WishlistEntry


class BookDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ISBN = "extra_isbn"
        const val EXTRA_FROM_SCAN = "extra_from_scan"
    }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }


    private lateinit var b: ActivityBookDetailBinding
    private val dao by lazy { AppDb.get(this).bookDao() }
    private var currentIsbn: String? = null
    private var isReadNow: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val fromScan = intent.getBooleanExtra(EXTRA_FROM_SCAN, false)

        super.onCreate(savedInstanceState)
        b = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val isbn = intent.getStringExtra(EXTRA_ISBN)
        currentIsbn = isbn
        if (isbn.isNullOrBlank()) { Toast.makeText(this, "Missing ISBN", Toast.LENGTH_SHORT).show(); finish(); return }

        // Load book
        lifecycleScope.launch {
            val book = withContext(Dispatchers.IO) { dao.findByIsbn(isbn) }


            if (book == null) { Toast.makeText(this@BookDetailActivity, "Book not found", Toast.LENGTH_SHORT).show(); finish(); return@launch }

            b.addToWishlistBtn.visibility = if (fromScan) View.VISIBLE else View.GONE

            b.addToWishlistBtn.setOnClickListener {
                val entry = WishlistEntry(
                    isbn = book.isbn,
                    title = book.title,
                    authors = book.authors,
                    description = book.description,
                    coverUrl = book.coverUrl
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    wishlistDao.upsert(entry)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BookDetailActivity, "Added to wishlist ✔", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            b.coverImage.apply {
                if (book.coverUrl.isNotBlank()) {
                    visibility = View.VISIBLE
                    load(book.coverUrl)
                } else {
                    // hide if no image saved
                    setImageDrawable(null)
                    visibility = View.INVISIBLE
                }
            }
            // Fill UI
            b.titleText.text = if (book.title.isBlank()) "—" else book.title
            b.authorsText.text = if (book.authors.isBlank()) "—" else book.authors
            b.isbnText.text = "ISBN: ${book.isbn}"
            b.descText.text = if (book.description.isBlank()) "—" else book.description
// ✅ Load cover image if present
            if (book.coverUrl.isNotBlank()) {
                b.coverImage.visibility = View.VISIBLE
                b.coverImage.load(book.coverUrl)
            } else {
                b.coverImage.setImageDrawable(null)
                b.coverImage.visibility = View.INVISIBLE
            }

            // NEW: status & toggle
            isReadNow = book.isRead
            renderReadStatus(isReadNow)

            b.toggleReadBtn.setOnClickListener {
                val newValue = !isReadNow
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.setRead(book.isbn, newValue)
                    withContext(Dispatchers.Main) {
                        isReadNow = newValue
                        renderReadStatus(isReadNow)
                        Toast.makeText(this@BookDetailActivity, if (newValue) "Marked as Read" else "Marked as Unread", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Existing delete button code stays the same…
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
