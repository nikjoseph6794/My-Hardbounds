package com.bookshlef.bookshelf

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bookshlef.bookshelf.databinding.ActivityHomeBinding
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

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


    }
}
