package com.bookshlef.bookshelf

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bookshlef.bookshelf.databinding.ActivityScanBinding
import com.bookshlef.bookshelf.data.RetrofitClient
import com.bookshlef.bookshelf.db.*
import com.bookshlef.bookshelf.util.FirebaseSyncHelper
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*

class ScanActivity : AppCompatActivity() {

    private lateinit var b: ActivityScanBinding
    private val uiScope = MainScope()

    private val dao by lazy { AppDb.get(this).bookDao() }
    private val wishlistDao by lazy { AppDb.get(this).wishlistDao() }
    private val historyDao by lazy { AppDb.get(this).scanHistoryDao() }   // ✅ NEW

    private var currentIsbn: String? = null
    private var currentTitle: String? = null
    private var currentAuthors: String? = null
    private var currentDesc: String? = null
    private var currentCoverUrl: String? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startScan()
            else b.saveStatus.text = "Camera permission denied"
        }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val res = IntentIntegrator.parseActivityResult(
                    IntentIntegrator.REQUEST_CODE, result.resultCode, data
                )
                val raw = res?.contents
                if (!raw.isNullOrBlank()) {
                    fetchAndDisplay(raw.replace("-", "").replace(" ", ""))
                } else goBackHome()
            } else goBackHome()
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

        b.scanAgainBtn.setOnClickListener { requestPermissionAndScan() }

        // ➕ Add to Wishlist
        b.addWishlistBtn.setOnClickListener {
            val isbn = currentIsbn ?: return@setOnClickListener

            uiScope.launch(Dispatchers.IO) {
                val entry = WishlistEntry(
                    isbn = isbn,
                    title = currentTitle.orEmpty(),
                    authors = currentAuthors.orEmpty(),
                    description = currentDesc.orEmpty(),
                    coverUrl = currentCoverUrl.orEmpty()
                )

                // 1️⃣ Save locally
                wishlistDao.upsert(entry)

                // 2️⃣ Sync to cloud
                FirebaseSyncHelper.addWishlist(entry)

                withContext(Dispatchers.Main) {
                    b.saveStatus.text = "Added to wishlist ✔"
                    b.addWishlistBtn.visibility = View.GONE
                }
            }

        }

        // ➕ Add to Library
        b.addBtn.setOnClickListener {
            val isbn = currentIsbn ?: return@setOnClickListener

            uiScope.launch(Dispatchers.IO) {
                val book = Book(
                    isbn = isbn,
                    title = currentTitle.orEmpty(),
                    authors = currentAuthors.orEmpty(),
                    description = currentDesc.orEmpty(),
                    coverUrl = currentCoverUrl.orEmpty(),
                    isRead = false,
                    addedAt = System.currentTimeMillis()
                )

                // 1️⃣ Save locally
                dao.upsert(book)

                // 2️⃣ Remove from wishlist locally
                wishlistDao.deleteByIsbn(isbn)
                FirebaseSyncHelper.removeWishlist(isbn)

                // 3️⃣ Sync to cloud
                FirebaseSyncHelper.addBook(book)
                FirebaseSyncHelper.removeWishlist(isbn)

                withContext(Dispatchers.Main) {
                    b.saveStatus.text = "Saved to library ✔"
                    b.addBtn.isEnabled = false
                    b.addWishlistBtn.visibility = View.GONE
                }
            }

        }

        requestPermissionAndScan()
    }

    private fun requestPermissionAndScan() {
        resetUI()
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun resetUI() {
        b.progress.visibility = View.GONE
        b.addBtn.isEnabled = false
        b.addWishlistBtn.visibility = View.GONE
        b.saveStatus.text = ""
        b.titleText.text = "—"
        b.authorText.text = "—"
        b.descText.text = "—"
        b.isbnText.text = "ISBN: —"
    }

    private fun startScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
        integrator.setPrompt("Align the book barcode")
        scanLauncher.launch(integrator.createScanIntent())
    }

    private fun preferHttps(url: String?) =
        url?.replace("http://", "https://") ?: ""

    private fun fetchAndDisplay(isbn: String) {

        currentIsbn = isbn
        b.isbnText.text = "ISBN: $isbn"
        b.progress.visibility = View.VISIBLE

        uiScope.launch {
            try {
                val gb = withContext(Dispatchers.IO) {
                    RetrofitClient.api.searchByIsbn("isbn:$isbn")
                }

                if (!gb.items.isNullOrEmpty()) {
                    val info = gb.items.first().volumeInfo

                    val title = info?.title ?: ""
                    val authors = info?.authors?.joinToString().orEmpty()
                    val desc = info?.description.orEmpty()
                    val cover = preferHttps(info?.imageLinks?.thumbnail)
                        .ifBlank { preferHttps(info?.imageLinks?.smallThumbnail) }

                    applyData(isbn, title, authors, desc, cover)
                    saveToHistory(isbn, title, authors, desc, cover)

                } else {
                    val ol = withContext(Dispatchers.IO) {
                        RetrofitClient.openLibrary.searchByIsbn(isbn)
                    }

                    val doc = ol.docs.firstOrNull()
                    if (doc == null) {
                        showNoBookFound()
                        return@launch
                    }

                    val title = doc.title.orEmpty()
                    val authors = doc.author_name?.joinToString().orEmpty()
                    val cover = if (doc.cover_i != null)
                        com.bookshlef.bookshelf.util.openLibraryCoverForId(doc.cover_i!!)
                    else
                        com.bookshlef.bookshelf.util.openLibraryCoverForIsbn(isbn)

                    applyData(isbn, title, authors, "", cover)
                    saveToHistory(isbn, title, authors, "", cover)
                }

                applyButtonState(isbn)

            } catch (e: Exception) {
                b.saveStatus.text = "Error: ${e.message}"
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }
    private suspend fun saveToHistory(
        isbn: String,
        title: String,
        authors: String,
        desc: String,
        cover: String
    ) {
        val inLibrary = dao.findByIsbn(isbn) != null
        val inWishlist = wishlistDao.findByIsbn(isbn) != null

        // ✅ Only save if not already tracked elsewhere
        if (!inLibrary && !inWishlist) {
            historyDao.upsert(
                ScanHistoryEntry(
                    isbn = isbn,
                    title = title,
                    authors = authors,
                    description = desc,
                    coverUrl = cover,
                    scannedAt = System.currentTimeMillis()
                )
            )
        }
    }


    private fun applyData(
        isbn: String,
        title: String,
        authors: String,
        desc: String,
        cover: String
    ) {
        currentTitle = title
        currentAuthors = authors
        currentDesc = desc
        currentCoverUrl = cover

        b.titleText.text = title.ifBlank { "—" }
        b.authorText.text = authors.ifBlank { "—" }
        b.descText.text = desc.ifBlank { "—" }
    }

    private suspend fun applyButtonState(isbn: String) {
        val inLibrary = dao.findByIsbn(isbn) != null
        val inWishlist = wishlistDao.findByIsbn(isbn) != null

        withContext(Dispatchers.Main) {
            when {
                inLibrary -> {
                    b.saveStatus.text = "Already in your library"
                    b.addBtn.isEnabled = false
                }
                inWishlist -> {
                    b.saveStatus.text = "Already in wishlist"
                    b.addBtn.isEnabled = true
                }
                else -> {
                    b.addBtn.isEnabled = true
                    b.addWishlistBtn.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showNoBookFound() {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
    }
}
