package com.example.bookshelf

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bookshelf.databinding.ActivityScanBinding
import com.example.bookshelf.data.RetrofitClient
import com.example.bookshelf.db.AppDb
import com.example.bookshelf.db.Book
import com.example.bookshelf.db.WishlistEntry
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*

class ScanActivity : AppCompatActivity() {
    private lateinit var b: ActivityScanBinding
    private val uiScope = MainScope()
    private val dao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }

    private var currentIsbn: String? = null
    private var currentTitle: String? = null
    private var currentAuthors: String? = null
    private var currentDesc: String? = null
    private var currentCoverUrl: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScan()
        else b.saveStatus.text = "Camera permission denied. Enable it to scan."
    }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val res = IntentIntegrator.parseActivityResult(
                IntentIntegrator.REQUEST_CODE, result.resultCode, data
            )
            val raw = res?.contents
            if (!raw.isNullOrBlank()) {
                val isbn = raw.replace("-", "").replace(" ", "")
                fetchAndDisplay(isbn)
            } else {
                goBackHome()
            }
        } else {
            goBackHome()
        }
    }

    private fun goBackHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityScanBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Scan again button
        b.scanAgainBtn.setOnClickListener { requestPermissionAndScan() }

        // Add to Wishlist button
        b.addWishlistBtn.setOnClickListener {
            val isbn = currentIsbn ?: return@setOnClickListener

            uiScope.launch(Dispatchers.IO) {
                wishlistDao.upsert(
                    WishlistEntry(
                        isbn = isbn,
                        title = currentTitle.orEmpty(),
                        authors = currentAuthors.orEmpty(),
                        description = currentDesc.orEmpty(),
                        coverUrl = currentCoverUrl.orEmpty()
                    )
                )
                withContext(Dispatchers.Main) {
                    b.saveStatus.text = "Added to wishlist ✔"
                    b.addWishlistBtn.visibility = View.GONE
                }
            }
        }

        // Add to Library button
        b.addBtn.setOnClickListener {
            val isbn = currentIsbn ?: return@setOnClickListener

            uiScope.launch(Dispatchers.IO) {
                dao.upsert(
                    Book(
                        isbn = isbn,
                        title = currentTitle.orEmpty(),
                        authors = currentAuthors.orEmpty(),
                        description = currentDesc.orEmpty(),
                        addedAt = System.currentTimeMillis(),
                        isRead = false,
                        coverUrl = currentCoverUrl.orEmpty()
                    )
                )

                // ✅ Auto-remove from wishlist if existed
                wishlistDao.deleteByIsbn(isbn)

                withContext(Dispatchers.Main) {
                    b.saveStatus.text = "Saved to library ✔"
                    b.addBtn.isEnabled = false
                    b.addWishlistBtn.visibility = View.GONE
                }
            }
        }

        requestPermissionAndScan()
    }

    private fun preferHttps(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url.replace("http://", "https://")
    }

    private fun requestPermissionAndScan() {
        b.progress.visibility = View.GONE
        b.addBtn.isEnabled = false
        b.saveStatus.text = ""
        b.titleText.text = "—"
        b.authorText.text = "—"
        b.descText.text = "—"
        b.isbnText.text = "ISBN: —"

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
        integrator.setPrompt("Align the book barcode")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)

        val intent = integrator.createScanIntent()
        scanLauncher.launch(intent)
    }

    private fun fetchAndDisplay(isbn: String) {
        currentIsbn = isbn
        b.isbnText.text = "ISBN: $isbn"
        b.progress.visibility = View.VISIBLE
        b.addBtn.isEnabled = false
        b.saveStatus.text = ""

        uiScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.searchByIsbn("isbn:$isbn")
                }

                // ✅ No results from API → show message, only allow scanning again
                if (resp.items.isNullOrEmpty()) {
                    currentTitle = null
                    currentAuthors = null
                    currentDesc = null
                    currentCoverUrl = null

                    b.titleText.text = "—"
                    b.authorText.text = "—"
                    b.descText.text = "—"

                    b.saveStatus.text = "No book found for the scan"
                    b.addBtn.isEnabled = false
                    b.addWishlistBtn.visibility = View.GONE
                    // keep Scan Again visible; nothing else to do
                    return@launch
                }

                val info = resp.items.firstOrNull()?.volumeInfo
                val title = info?.title ?: ""
                val authors = info?.authors?.joinToString().orEmpty()
                val description = info?.description.orEmpty()

                // Best cover pick (may be empty if API has none)
                val cover = preferHttps(info?.imageLinks?.thumbnail)
                    .ifBlank { preferHttps(info?.imageLinks?.smallThumbnail) }
                currentCoverUrl = cover

                // cache
                currentTitle = title
                currentAuthors = authors
                currentDesc = description

                // show UI info
                b.titleText.text = title.ifBlank { "—" }
                b.authorText.text = authors.ifBlank { "—" }
                b.descText.text = description.ifBlank { "—" }

                // DB Check
                val inLibrary = withContext(Dispatchers.IO) { dao.findByIsbn(isbn) != null }
                val inWishlist = withContext(Dispatchers.IO) { wishlistDao.findByIsbn(isbn) != null }

                when {
                    inLibrary -> {
                        b.saveStatus.text = "Already in your library"
                        b.addBtn.isEnabled = false
                        b.addWishlistBtn.visibility = View.GONE
                    }
                    inWishlist -> {
                        b.saveStatus.text = "Already in wishlist"
                        b.addBtn.isEnabled = title.isNotBlank()
                        b.addWishlistBtn.visibility = View.GONE
                    }
                    else -> {
                        b.saveStatus.text = ""
                        b.addBtn.isEnabled = title.isNotBlank()
                        b.addWishlistBtn.visibility = if (title.isNotBlank()) View.VISIBLE else View.GONE
                        b.addWishlistBtn.isEnabled = title.isNotBlank()
                    }
                }

            } catch (e: Exception) {
                b.saveStatus.text = "Error: ${e.message ?: "Unknown error"}"
                b.addBtn.isEnabled = false
                b.addWishlistBtn.visibility = View.GONE
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
    }
}
