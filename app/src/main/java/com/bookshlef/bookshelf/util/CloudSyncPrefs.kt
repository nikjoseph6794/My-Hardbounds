package com.bookshlef.bookshelf.utils

import android.content.Context

object CloudSyncPrefs {

    private const val PREF_NAME = "cloud_sync_prefs"
    private const val KEY_ENABLED = "cloud_sync_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun markEnabled(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }
}
