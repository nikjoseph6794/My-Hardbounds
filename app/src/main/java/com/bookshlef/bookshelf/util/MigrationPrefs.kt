package com.bookshlef.bookshelf.utils

import android.content.Context

object MigrationPrefs {

    private const val PREF_NAME = "migration_prefs"
    private const val KEY_MIGRATED = "firebase_migrated"

    fun isMigrated(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MIGRATED, false)
    }

    fun setMigrated(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }
}
