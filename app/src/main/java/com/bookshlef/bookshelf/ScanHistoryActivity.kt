package com.bookshlef.bookshelf

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookshlef.bookshelf.databinding.ActivityScanHistoryBinding
import com.bookshlef.bookshelf.db.*
import com.bookshlef.bookshelf.ui.ScanHistoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanHistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityScanHistoryBinding

    private val historyDao by lazy { AppDb.get(this).scanHistoryDao() }
    private val bookDao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityScanHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        val adapter = ScanHistoryAdapter(
            onAddLibrary = { entry ->
                lifecycleScope.launch(Dispatchers.IO) {
                    bookDao.upsert(
                        Book(
                            isbn = entry.isbn,
                            title = entry.title,
                            authors = entry.authors,
                            description = entry.description,
                            coverUrl = entry.coverUrl,
                            isRead = false,
                            addedAt = System.currentTimeMillis()
                        )
                    )
                    historyDao.deleteByIsbn(entry.isbn)
                }
            },
            onAddWishlist = { entry ->
                lifecycleScope.launch(Dispatchers.IO) {
                    wishlistDao.upsert(
                        WishlistEntry(
                            isbn = entry.isbn,
                            title = entry.title,
                            authors = entry.authors,
                            description = entry.description,
                            coverUrl = entry.coverUrl
                        )
                    )
                    historyDao.deleteByIsbn(entry.isbn)
                }
            },
            onDelete = { entry ->
                lifecycleScope.launch(Dispatchers.IO) {
                    historyDao.delete(entry)
                }
            }
        )

        b.historyList.layoutManager = LinearLayoutManager(this)
        b.historyList.adapter = adapter

        // ✅ Collect + safety filter (prevents duplicates showing)
        lifecycleScope.launch {
            historyDao.getAll().collect { historyList ->

                val filtered = withContext(Dispatchers.IO) {
                    historyList.filter { entry ->
                        bookDao.findByIsbn(entry.isbn) == null &&
                                wishlistDao.findByIsbn(entry.isbn) == null
                    }
                }

                adapter.submitList(filtered)
            }
        }
    }
}
