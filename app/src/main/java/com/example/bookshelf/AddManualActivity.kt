package com.example.bookshelf

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bookshelf.data.RetrofitClient
import com.example.bookshelf.databinding.ActivityAddManualBinding
import com.example.bookshelf.db.AppDb
import com.example.bookshelf.db.Book
import com.example.bookshelf.db.WishlistEntry
import com.example.bookshelf.util.openLibraryCoverForId
import com.example.bookshelf.util.openLibraryCoverForIsbn
import com.example.bookshelf.util.preferHttps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddManualActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddManualBinding
    private val dao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }

    // Used for the author search results list
    private data class Candidate(
        val title: String,
        val authors: String,
        val isbn: String,        // may be blank
        val description: String, // may be blank
        val coverUrl: String     // may be blank
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddManualBinding.inflate(layoutInflater)
        setContentView(b.root)

        // optional: focus the author field
        // b.authorsInput.requestFocus()

        // Buttons
        b.fetchBtn.setOnClickListener { onFetchClicked() }            // hidden in UI (kept for future)
        b.saveBtn.setOnClickListener { onSaveToLibraryClicked() }     // Save to Library
        b.saveWishlistBtn?.setOnClickListener { onSaveToWishlistClicked() } // NEW: Save to Wishlist
        b.cancelBtn.setOnClickListener { finish() }

        // Search by author
        b.fetchByAuthorBtn?.setOnClickListener { onFetchByAuthorClicked() }
    }

    // --- ISBN fetch (Google Books → Open Library) ---
    private fun onFetchClicked() {
        val raw = b.isbnInput.text.toString().trim()
        val isbn = normalizeIsbn(raw)
        if (!isValidIsbn(isbn)) {
            b.fetchStatus.text = "Please enter a valid ISBN (10 or 13 digits)"
            return
        }

        b.fetchStatus.text = "Checking sources…"

        lifecycleScope.launch {
            try {
                // 1) Google Books (no lang filter)
                val gb = withContext(Dispatchers.IO) {
                    RetrofitClient.api.searchByIsbn("isbn:$isbn")
                }

                if (!gb.items.isNullOrEmpty()) {
                    val info = gb.items.first().volumeInfo
                    val title = info?.title ?: ""
                    val authors = info?.authors?.joinToString().orEmpty()
                    val desc = info?.description.orEmpty()
                    val cover = preferHttps(info?.imageLinks?.thumbnail)
                        .ifBlank { preferHttps(info?.imageLinks?.smallThumbnail) }

                    b.titleInput.setText(title)
                    b.authorsInput.setText(authors)
                    b.descInput.setText(desc)
                    b.fetchStatus.text = "Found via Google Books"
                    b.saveBtn.tag = cover
                    return@launch
                }

                // 2) Open Library fallback
                val ol = withContext(Dispatchers.IO) {
                    RetrofitClient.openLibrary.searchByIsbn(isbn)
                }
                val doc = ol.docs.firstOrNull()
                if (doc != null) {
                    val title = doc.title.orEmpty()
                    val authors = doc.author_name?.joinToString().orEmpty()
                    val cover = when {
                        doc.cover_i != null -> openLibraryCoverForId(doc.cover_i!!)
                        else -> openLibraryCoverForIsbn(isbn)
                    }

                    b.titleInput.setText(title)
                    b.authorsInput.setText(authors)
                    b.fetchStatus.text = "Found via Open Library"
                    b.saveBtn.tag = cover
                } else {
                    b.fetchStatus.text = "No details found — you can enter Title/Author and save."
                    b.saveBtn.tag = "" // no cover
                }

            } catch (e: Exception) {
                b.fetchStatus.text = "Error fetching details: ${e.message ?: "Unknown"}"
                b.saveBtn.tag = ""
            }
        }
    }

    // --- Author search flow (list & select → prefill) ---
    private fun onFetchByAuthorClicked() {
        val authorQuery = b.authorsInput.text.toString().trim()
        if (authorQuery.isEmpty()) {
            b.fetchStatus.text = "Type an author name first"
            return
        }

        b.fetchStatus.text = "Searching by author…"

        lifecycleScope.launch {
            val candidates = mutableListOf<Candidate>()
            try {
                // 1) Google Books by author (no industryIdentifiers usage)
                val gb = withContext(Dispatchers.IO) {
                    RetrofitClient.api.searchByIsbn("inauthor:$authorQuery")
                }
                gb.items?.forEach { item ->
                    val info = item.volumeInfo
                    val title = info?.title.orEmpty()
                    val authors = info?.authors?.joinToString().orEmpty()
                    val desc = info?.description.orEmpty()
                    val isbn = "" // not extracting from identifiers in your model
                    val cover = preferHttps(info?.imageLinks?.thumbnail)
                        .ifBlank { preferHttps(info?.imageLinks?.smallThumbnail) }

                    if (title.isNotBlank()) {
                        candidates += Candidate(title, authors, isbn, desc, cover)
                    }
                }

                // 2) Open Library fallback by author (if still empty)
                if (candidates.isEmpty()) {
                    val ol = withContext(Dispatchers.IO) {
                        RetrofitClient.openLibrary.searchByAuthor(authorQuery)
                    }
                    ol.docs.take(20).forEach { doc ->
                        val title = doc.title.orEmpty()
                        val authors = doc.author_name?.joinToString().orEmpty()
                        val firstIsbn = (doc.isbn?.firstOrNull() ?: "")
                            .replace("-", "").replace(" ", "")
                        val cover = when {
                            doc.cover_i != null -> openLibraryCoverForId(doc.cover_i!!)
                            firstIsbn.isNotEmpty() -> openLibraryCoverForIsbn(firstIsbn)
                            else -> ""
                        }
                        if (title.isNotBlank()) {
                            candidates += Candidate(title, authors, firstIsbn, "", cover)
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    b.fetchStatus.text = "No books found for author: $authorQuery"
                    return@launch
                }

                showCandidatePicker(candidates)

            } catch (e: Exception) {
                b.fetchStatus.text = "Error searching author: ${e.message ?: "Unknown"}"
            }
        }
    }

    private fun showCandidatePicker(candidates: List<Candidate>) {
        val lines = candidates.map {
            buildString {
                append(it.title.ifBlank { "Untitled" })
                if (it.authors.isNotBlank()) append(" — ${it.authors}")
                if (it.isbn.isNotBlank()) append("  [${it.isbn}]")
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lines)

        AlertDialog.Builder(this)
            .setTitle("Select a book")
            .setAdapter(adapter) { dialog, which ->
                val c = candidates[which]
                b.titleInput.setText(c.title)
                b.authorsInput.setText(c.authors)
                if (c.isbn.isNotBlank()) b.isbnInput.setText(c.isbn)
                b.descInput.setText(c.description)
                b.saveBtn.tag = c.coverUrl
                b.fetchStatus.text = "Selected: ${c.title}"
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Save to LIBRARY (existing) ---
    private fun onSaveToLibraryClicked() {
        val raw = b.isbnInput.text.toString().trim()
        val isbn = normalizeIsbn(raw)
        val title = b.titleInput.text.toString().trim()
        val authors = b.authorsInput.text.toString().trim()
        val desc = b.descInput.text.toString().trim()
        val cover = (b.saveBtn.tag as? String).orEmpty()

        if (!isValidIsbn(isbn)) {
            Toast.makeText(this, "Enter a valid ISBN", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty() && authors.isEmpty()) {
            Toast.makeText(this, "Enter at least Title or Author", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val exists = dao.findByIsbn(isbn) != null
            if (exists) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddManualActivity, "Already in your library", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // If present in wishlist, remove (optional)
            wishlistDao.deleteByIsbn(isbn)

            dao.upsert(
                Book(
                    isbn = isbn,
                    title = title,
                    authors = authors,
                    description = desc,
                    coverUrl = cover,
                    isRead = false,
                    addedAt = System.currentTimeMillis()
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddManualActivity, "Saved to library ✔", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // --- NEW: Save to WISHLIST ---
    private fun onSaveToWishlistClicked() {
        val raw = b.isbnInput.text.toString().trim()
        val isbn = normalizeIsbn(raw)
        val title = b.titleInput.text.toString().trim()
        val authors = b.authorsInput.text.toString().trim()
        val desc = b.descInput.text.toString().trim()
        val cover = (b.saveBtn.tag as? String).orEmpty()

        // Wishlist uses isbn as PK → require valid ISBN
        if (!isValidIsbn(isbn)) {
            Toast.makeText(this, "Enter a valid ISBN to add to wishlist", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty() && authors.isEmpty()) {
            Toast.makeText(this, "Enter at least Title or Author", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // If already in library → don't add to wishlist
            val inLibrary = dao.findByIsbn(isbn) != null
            if (inLibrary) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddManualActivity, "Already in library", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val inWishlist = wishlistDao.findByIsbn(isbn) != null
            if (inWishlist) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddManualActivity, "Already in wishlist", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            wishlistDao.upsert(
                WishlistEntry(
                    isbn = isbn,
                    title = title,
                    authors = authors,
                    description = desc,
                    coverUrl = cover
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddManualActivity, "Added to wishlist ♡", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // --- Utils ---
    private fun normalizeIsbn(raw: String) =
        raw.replace("-", "").replace(" ", "")

    private fun isValidIsbn(s: String): Boolean =
        s.length == 10 || s.length == 13
}
