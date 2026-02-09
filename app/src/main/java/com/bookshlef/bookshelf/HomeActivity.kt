package com.bookshlef.bookshelf

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bookshlef.bookshelf.databinding.ActivityHomeBinding
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.util.FirebaseSyncHelper
import com.bookshlef.bookshelf.util.MigrationPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var b: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        runMigrationIfNeeded()

        b.libraryBtn.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        b.scanBtn.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        b.wishlistBtn.setOnClickListener {
            startActivity(Intent(this, WishlistActivity::class.java))
        }

        b.addManualBtn.setOnClickListener {
            startActivity(Intent(this, AddManualActivity::class.java))
        }

        b.historyBtn.setOnClickListener {
            startActivity(Intent(this, ScanHistoryActivity::class.java))
        }
    }

    private fun runMigrationIfNeeded() {
        if (MigrationPrefs.isDone(this)) return

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDb.get(this@HomeActivity)

            val books = db.bookDao().getAllNow()
            val wishlist = db.wishlistDao().getAllNow()

            // ✅ THIS IS WHERE IT BELONGS
            if (books.isNotEmpty()) {
                FirebaseSyncHelper.syncLibrary(books)
            }

            if (wishlist.isNotEmpty()) {
                FirebaseSyncHelper.syncWishlist(wishlist)
            }

            MigrationPrefs.markDone(this@HomeActivity)
        }
    }
}

