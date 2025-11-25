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
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.db.WishlistEntry
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
                // 1) Try Google Books
                val gb = withContext(Dispatchers.IO) {
                    RetrofitClient.api.searchByIsbn("isbn:$isbn")
                }

                // Helper to run the common DB checks + button state
                suspend fun applyDbState(title: String) {
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
                            val canAdd = title.isNotBlank()
                            b.addBtn.isEnabled = canAdd
                            b.addWishlistBtn.visibility = if (canAdd) View.VISIBLE else View.GONE
                            b.addWishlistBtn.isEnabled = canAdd
                        }
                    }
                }

                if (!gb.items.isNullOrEmpty()) {
                    // 2) Google Books HIT
                    val info = gb.items.firstOrNull()?.volumeInfo
                    val title = info?.title ?: ""
                    val authors = info?.authors?.joinToString().orEmpty()
                    val description = info?.description.orEmpty()

                    // ✅ Prefer HTTPS for Google Books covers
                    currentCoverUrl = preferHttps(info?.imageLinks?.thumbnail)
                        .ifBlank { preferHttps(info?.imageLinks?.smallThumbnail) }


                    // cache
                    currentTitle = title
                    currentAuthors = authors
                    currentDesc = description

                    // UI
                    b.titleText.text = title.ifBlank { "—" }
                    b.authorText.text = authors.ifBlank { "—" }
                    b.descText.text = description.ifBlank { "—" }

                    // Buttons state
                    applyDbState(title)
                } else {
                    // 3) Google Books MISS → try Open Library as fallback
                    val ol = withContext(Dispatchers.IO) {
                        RetrofitClient.openLibrary.searchByIsbn(isbn)
                    }
                    val doc = ol.docs.firstOrNull()


                    if (doc == null) {
                        // 4) Open Library MISS too → show "No book found"
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
                        return@launch
                    }

                    // 5) Map minimal fields from Open Library
                    val title = doc.title.orEmpty()
                    val authors = doc.author_name?.joinToString().orEmpty()
                    val description = "" // OL does not send description here

                   // ✅ Choose an OL cover URL
                    currentCoverUrl = when {
                        doc.cover_i != null ->
                            com.bookshlef.bookshelf.util.openLibraryCoverForId(doc.cover_i!!)
                        else ->
                            com.bookshlef.bookshelf.util.openLibraryCoverForIsbn(isbn)
                    }


                    // cache
                    currentTitle = title
                    currentAuthors = authors
                    currentDesc = description

                    // UI
                    b.titleText.text = title.ifBlank { "—" }
                    b.authorText.text = authors.ifBlank { "—" }
                    b.descText.text = "—"

                    // Buttons state
                    applyDbState(title)
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
