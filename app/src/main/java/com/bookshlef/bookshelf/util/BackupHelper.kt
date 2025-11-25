package com.bookshlef.bookshelf.utils

import android.content.Context
import android.net.Uri
import com.bookshlef.bookshelf.db.AppDb
import com.bookshlef.bookshelf.db.Book
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupHelper {

    // Create a backup file
    suspend fun exportToUri(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val dao = AppDb.get(context).bookDao()
            val books = dao.getAllNow()
            val json = Gson().toJson(books)
            context.contentResolver.openOutputStream(uri)?.use {
                OutputStreamWriter(it).use { writer ->
                    writer.write(json)
                }
            }
        }
    }

    // Restore from a backup file
    suspend fun importFromUri(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val dao = AppDb.get(context).bookDao()
        val listType = object : TypeToken<List<Book>>() {}.type

        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: return@withContext 0

        val books: List<Book> = try {
            Gson().fromJson(json, listType)
        } catch (e: Exception) {
            emptyList()
        }

        for (b in books) {
            dao.upsert(b)
        }
        books.size
    }
}
