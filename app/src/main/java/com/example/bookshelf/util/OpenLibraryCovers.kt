package com.example.bookshelf.util

/**
 * Open Library covers API
 * Docs: https://covers.openlibrary.org
 *
 * You can fetch by ISBN or by cover ID (cover_i from search docs).
 * Sizes: S (small), M (medium), L (large)
 * Format: jpg or png (jpg is default and safe)
 */

fun openLibraryCoverForIsbn(isbn: String, size: Char = 'M', format: String = "jpg"): String {
    // Example: https://covers.openlibrary.org/b/isbn/9780143126560-M.jpg
    return "https://covers.openlibrary.org/b/isbn/${isbn}-${size}.${format}"
}

fun openLibraryCoverForId(coverId: Int, size: Char = 'M', format: String = "jpg"): String {
    // Example: https://covers.openlibrary.org/b/id/8231856-M.jpg
    return "https://covers.openlibrary.org/b/id/${coverId}-${size}.${format}"
}

/** Optional: normalizes non-https links you may get elsewhere */
fun preferHttps(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return url.replace("http://", "https://")
}
