package com.example.bookshelf

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookshelf.databinding.ActivityLibraryBinding
import com.example.bookshelf.db.AppDb
import com.example.bookshelf.ui.BookAdapter
import com.example.bookshelf.utils.BackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : AppCompatActivity() {
    private lateinit var b: ActivityLibraryBinding
    private val dao by lazy { AppDb.get(this).bookDao() }

    private val adapter by lazy {
        // Tap on a book in the list still opens details
        BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(BookDetailActivity.EXTRA_ISBN, book.isbn)
            )
        }
    }
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

        // ✅ Set button click listeners OUTSIDE of collect { ... }
        b.backupBtn.setOnClickListener {
            // optional: quick debug toast
            // Toast.makeText(this, "Backup clicked", Toast.LENGTH_SHORT).show()
            exportLauncher.launch("MyHardboundsBackup.json")
        }
        b.restoreBtn.setOnClickListener {
            // Toast.makeText(this, "Restore clicked", Toast.LENGTH_SHORT).show()
            importLauncher.launch(arrayOf("application/json", "text/json"))
        }

        // Observe library (this suspends forever; keep it in its own coroutine)
        lifecycleScope.launch {
            dao.getAll().collect { list ->
                adapter.submitList(list)
            }
        }

        // Real-time search with debounce
        var searchJob: Job? = null
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchJob?.cancel()

                if (query.isEmpty()) {
                    // show full list via the Flow above
                    b.emptyHint.visibility = View.GONE
                    return
                }

                searchJob = lifecycleScope.launch {
                    delay(250)
                    val results = withContext(Dispatchers.IO) { dao.search(query) }
                    withContext(Dispatchers.Main) {
                        adapter.submitList(results)
                        b.emptyHint.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        })

        // Hide keyboard when scrolling list
        b.libraryList.setOnTouchListener { _, _ ->
            currentFocus?.clearFocus()
            false
        }
    }


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
                results.isEmpty() -> {
                    // same as before
                }
                results.size == 1 -> {
                    val only = results.first()
                    startActivity(
                        Intent(this@LibraryActivity, BookDetailActivity::class.java)
                            .putExtra(BookDetailActivity.EXTRA_ISBN, only.isbn)
                    )
                }
                else -> {
                    // NEW: go to SearchResultsActivity instead of dialog
                    startActivity(
                        Intent(this@LibraryActivity, SearchResultsActivity::class.java)
                            .putExtra(SearchResultsActivity.EXTRA_QUERY, query)
                    )
                }
            }
        }
    }
}
