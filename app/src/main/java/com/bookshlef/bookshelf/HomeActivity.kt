package com.bookshlef.bookshelf

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bookshlef.bookshelf.databinding.ActivityHomeBinding
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.utils.CloudSyncPrefs
import com.bookshlef.bookshelf.utils.FirebaseMigrationHelper
import com.bookshlef.bookshelf.utils.LegacyCloudSyncHelper
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            FirebaseMigrationHelper.migrateIfNeeded(
                context = this@HomeActivity,
                bookDao = AppDb.get(this@HomeActivity).bookDao(),
                wishlistDao = AppDb.get(this@HomeActivity).wishlistDao()
            )
        }

        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        if (!CloudSyncPrefs.isEnabled(this)) {
            // Show a one-time prompt
            AlertDialog.Builder(this)
                .setTitle("Enable Cloud Sync")
                .setMessage(
                    "Sync your existing library to the cloud so it’s safe and available on other devices."
                )
                .setPositiveButton("Enable") { _, _ ->
                    lifecycleScope.launch {
                        LegacyCloudSyncHelper.syncLocalToCloud(
                            bookDao = AppDb.get(this@HomeActivity).bookDao(),
                            wishlistDao = AppDb.get(this@HomeActivity).wishlistDao()
                        )
                        CloudSyncPrefs.markEnabled(this@HomeActivity)
                        Toast.makeText(
                            this@HomeActivity,
                            "Cloud sync enabled ✔",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNegativeButton("Later", null)
                .show()
        }

        b.scanBtn.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
        b.libraryBtn.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
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
}
