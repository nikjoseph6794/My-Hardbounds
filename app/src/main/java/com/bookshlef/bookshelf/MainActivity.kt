package com.bookshlef.bookshelf

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookshlef.bookshelf.databinding.ActivityMainBinding
import com.bookshlef.bookshelf.data.RetrofitClient
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.Book
import com.bookshlef.bookshelf.ui.BookAdapter
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val uiScope = MainScope()

    // keep last fetched details for "Add to Library"
    private var lastIsbn: String? = null
    private var lastTitle: String? = null
    private var lastAuthors: List<String>? = null
    private var lastDescription: String? = null

    private val db by lazy { AppDb.get(this) }
    private val dao by lazy { db.bookDao() }
    private val adapter by lazy { BookAdapter { } }

    /** Launcher to request CAMERA permission at runtime */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRealScan()
        else {
            // Inform the user; you could show a Snackbar/Toast instead
            b.titleText.text = "Camera permission denied"
            b.authorText.text = ""
            b.descText.text = "Enable camera permission to scan ISBN barcodes."
        }
    }

    /** Launcher to receive ZXing scan result */
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
                uiScope.launch {
                    b.progress.visibility = android.view.View.VISIBLE
                    fetchAndShow(isbn)
                    b.progress.visibility = android.view.View.GONE
                    b.addBtn.isEnabled = !lastTitle.isNullOrBlank()
                }
            } else {
                b.titleText.text = "No barcode read"
                b.authorText.text = ""
                b.descText.text = "Try again with better lighting and framing."
            }
        }
        b.scanBtn.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Library list
        b.libraryList.layoutManager = LinearLayoutManager(this)
        b.libraryList.adapter = adapter

        // Observe DB
        lifecycleScope.launch {
            dao.getAll().collect { books -> adapter.submitList(books) }
        }

        // Scan button -> request permission then open ZXing scanner
        b.scanBtn.setOnClickListener {
            b.scanBtn.isEnabled = false
            b.addBtn.isEnabled = false
            b.isbnText.text = "ISBN: —"
            b.titleText.text = "—"
            b.authorText.text = "—"
            b.descText.text = "—"
            // Ask for CAMERA permission if needed
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Add to Library
        b.addBtn.setOnClickListener {
            val isbn = lastIsbn ?: return@setOnClickListener
            val title = lastTitle ?: return@setOnClickListener
            val authors = lastAuthors?.joinToString().orEmpty()
            val desc = lastDescription.orEmpty()

            lifecycleScope.launch(Dispatchers.IO) {
                dao.upsert(
                    Book(
                        isbn = isbn,
                        title = title,
                        authors = authors,
                        description = desc,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /** Opens ZXing's scanning Activity for 1D barcodes (EAN-13/UPC), typical for books */
    private fun startRealScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES) // EAN_13 etc.
        integrator.setPrompt("Align the book barcode inside the frame")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)

        val intent = integrator.createScanIntent()
        scanLauncher.launch(intent)
    }

    private suspend fun fetchAndShow(isbn: String) {
        b.isbnText.text = "ISBN: $isbn"
        try {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.api.searchByIsbn("isbn:$isbn")
            }
            val info = resp.items?.firstOrNull()?.volumeInfo

            val title = info?.title ?: "—"
            val authors = info?.authors ?: emptyList()
            val description = info?.description ?: "—"

            // cache for Add button
            lastIsbn = isbn
            lastTitle = if (title == "—") null else title
            lastAuthors = authors
            lastDescription = if (description == "—") null else description

            // UI
            b.titleText.text = title
            b.authorText.text = if (authors.isEmpty()) "—" else authors.joinToString()
            b.descText.text = description

        } catch (e: Exception) {
            lastIsbn = null; lastTitle = null; lastAuthors = null; lastDescription = null
            b.titleText.text = "Couldn’t fetch details"
            b.authorText.text = ""
            b.descText.text = e.message ?: "Unknown error"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
    }
}
