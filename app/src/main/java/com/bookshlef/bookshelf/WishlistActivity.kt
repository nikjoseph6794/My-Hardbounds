package com.bookshlef.bookshelf

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookshlef.bookshelf.databinding.ActivityWishlistBinding
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.ui.WishlistAdapter
import kotlinx.coroutines.launch

class WishlistActivity : AppCompatActivity() {
    private lateinit var b: ActivityWishlistBinding
    private val dao by lazy { AppDb.get(this).wishlistDao() }
    private val adapter by lazy {
        WishlistAdapter { item ->
            // long-press handler: confirm & delete
            AlertDialog.Builder(this)
                .setTitle("Remove from wishlist?")
                .setMessage("Remove “${item.title.ifBlank { item.isbn }}” from your wishlist?")
                .setPositiveButton("Remove") { _, _ ->
                    lifecycleScope.launch {
                        AppDb.get(this@WishlistActivity).wishlistDao().deleteByIsbn(item.isbn)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityWishlistBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.wishlistList.layoutManager = LinearLayoutManager(this)
        b.wishlistList.adapter = adapter

        lifecycleScope.launch {
            dao.getAll().collect { adapter.submitList(it) }
        }
    }
}
