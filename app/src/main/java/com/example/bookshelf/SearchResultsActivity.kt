package com.example.bookshelf

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookshelf.databinding.ActivitySearchResultsBinding
import com.example.bookshelf.db.AppDb
import com.example.bookshelf.ui.BookAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchResultsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var b: ActivitySearchResultsBinding
    private val dao by lazy { AppDb.get(this).bookDao() }

    // Tap takes you to BookDetail
    private val adapter by lazy {
        BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(BookDetailActivity.EXTRA_ISBN, book.isbn)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(b.root)

        val query = intent.getStringExtra(EXTRA_QUERY).orEmpty()

        b.title.text = "Search Results"
        b.subtitle.text = "for \"$query\""

        b.resultsList.layoutManager = LinearLayoutManager(this)
        b.resultsList.adapter = adapter

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) { dao.search(query) }
            adapter.submitList(results)
        }
    }

    override fun onResume() {
        super.onResume()

        val query = intent.getStringExtra(EXTRA_QUERY).orEmpty()

        lifecycleScope.launch(Dispatchers.IO) {
            val results = dao.search(query)
            withContext(Dispatchers.Main) {
                adapter.submitList(results)
            }
        }
    }

}
