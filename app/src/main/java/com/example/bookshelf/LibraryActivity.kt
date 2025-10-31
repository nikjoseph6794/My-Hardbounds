package com.example.bookshelf

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookshelf.databinding.ActivityLibraryBinding
import com.example.bookshelf.db.AppDb
import com.example.bookshelf.db.Book
import com.example.bookshelf.ui.BookAdapter
import com.example.bookshelf.utils.BackupHelper
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope


class LibraryActivity : AppCompatActivity() {
    private lateinit var b: ActivityLibraryBinding
    private val dao by lazy { AppDb.get(this).bookDao() }

    private val adapter by lazy {
        BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(BookDetailActivity.EXTRA_ISBN, book.isbn)
            )
        }
    }

    // --- NEW: in-memory state for filtering ---
    private var latestList: List<Book> = emptyList()
    private var currentQuery: String = ""
    private var showRead: Boolean = false  // false = Unread (default), true = Read

    // For creating a new backup JSON file
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                BackupHelper.exportToUri(this@LibraryActivity, uri)
                Toast.makeText(this@LibraryActivity, "✅ Backup saved successfully!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@LibraryActivity, "Backup canceled", Toast.LENGTH_SHORT).show()
        }
    }

    // For restoring from an existing backup file
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val count = BackupHelper.importFromUri(this@LibraryActivity, uri)
                Toast.makeText(this@LibraryActivity, "✅ Restored $count books!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@LibraryActivity, "Restore canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(b.root)

        // RecyclerView setup
        b.libraryList.layoutManager = LinearLayoutManager(this)
        b.libraryList.adapter = adapter

        // Backup/Restore click listeners
        b.backupBtn.setOnClickListener { exportLauncher.launch("MyHardboundsBackup.json") }
        b.restoreBtn.setOnClickListener { importLauncher.launch(arrayOf("application/json", "text/json")) }

        // Observe full library; we keep latest list and filter in memory
        lifecycleScope.launch {
            dao.getAll().collect { list ->
                latestList = list
                updateCounts(list)
                applyFilters()
            }
        }

        // --- NEW: Read/Unread toggle ---
        b.readToggle.setOnCheckedChangeListener { _, isChecked ->
            showRead = isChecked
            applyFilters()
        }

        // Real-time search with debounce (now filters in-memory)
        var searchJob: Job? = null
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim().orEmpty()
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(250)
                    applyFilters()
                }
            }
        })

        // Hide keyboard when scrolling list
        b.libraryList.setOnTouchListener { _, _ ->
            currentFocus?.clearFocus()
            false
        }
    }

    private fun updateCounts(all: List<Book>) {
        val total = all.size
        val read = all.count { it.isRead }
        val unread = total - read
        b.countText.text = "Total: $total • Read: $read • Unread: $unread"
    }

    private fun applyFilters() {
        var list = latestList

        // 1) Read/Unread
        list = if (showRead) list.filter { it.isRead } else list.filter { !it.isRead }

        // 2) Search (title or authors)
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.authors.lowercase().contains(q)
            }
        }

        adapter.submitList(list)
        b.emptyHint.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    // (Optional) Dialog search you already had — unchanged
    private fun openSearchDialog() {
        val input = EditText(this).apply {
            hint = "Enter book title or author"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Search Library")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val q = input.text.toString().trim()
                if (q.isEmpty()) {
                    Toast.makeText(this, "Type a title or author", Toast.LENGTH_SHORT).show()
                } else {
                    searchAndRoute(q)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchAndRoute(query: String) {
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) { dao.search(query) }
            when {
                results.isEmpty() -> { /* no-op or toast */ }
                results.size == 1 -> {
                    val only = results.first()
                    startActivity(
                        Intent(this@LibraryActivity, BookDetailActivity::class.java)
                            .putExtra(BookDetailActivity.EXTRA_ISBN, only.isbn)
                    )
                }
                else -> {
                    startActivity(
                        Intent(this@LibraryActivity, SearchResultsActivity::class.java)
                            .putExtra(SearchResultsActivity.EXTRA_QUERY, query)
                    )
                }
            }
        }
    }
}
