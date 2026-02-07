package com.bookshlef.bookshelf

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookshlef.bookshelf.databinding.ActivityLibraryBinding
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.ui.BookAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : AppCompatActivity() {

    private lateinit var b: ActivityLibraryBinding

    private val bookDao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }

    private val adapter by lazy {
        BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(BookDetailActivity.EXTRA_ISBN, book.isbn)
            )
        }
    }

    // Local file pickers (kept in case you want them later)
//    private val exportLauncher = registerForActivityResult(
//        ActivityResultContracts.CreateDocument("application/json")
//    ) {}
//
//    private val importLauncher = registerForActivityResult(
//        ActivityResultContracts.OpenDocument()
//    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(b.root)

        // RecyclerView
        b.libraryList.layoutManager = LinearLayoutManager(this)
        b.libraryList.adapter = adapter

        // Observe library
        lifecycleScope.launch {
            bookDao.getAll().collect { list ->
                adapter.submitList(list)
            }
        }

        // 🔍 Real-time search with debounce
        var searchJob: Job? = null
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchJob?.cancel()

                if (query.isEmpty()) {
                    b.emptyHint.visibility = View.GONE
                    return
                }

                searchJob = lifecycleScope.launch {
                    delay(250)
                    val results = withContext(Dispatchers.IO) {
                        bookDao.search(query)
                    }
                    adapter.submitList(results)
                    b.emptyHint.visibility =
                        if (results.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        })

        // Hide keyboard on scroll
        b.libraryList.setOnTouchListener { _, _ ->
            currentFocus?.clearFocus()
            false
        }
    }
}
