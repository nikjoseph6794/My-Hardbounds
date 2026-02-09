package com.bookshlef.bookshelf.util

import android.content.Context

object MigrationPrefs {

    private const val PREF = "migration_prefs"
    private const val KEY_DONE = "firebase_migration_done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DONE, true)
            .apply()
    }
}
