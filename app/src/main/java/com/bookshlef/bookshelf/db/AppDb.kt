package com.bookshlef.bookshelf.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Book::class,
        WishlistEntry::class,
        ScanHistoryEntry::class
    ],
    version = 4,   // ✅ MUST MATCH OLD VERSION
    exportSchema = false
)

abstract class AppDb : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "bookshelf.db"
                )
                    // ❌ DO NOT use fallbackToDestructiveMigration for migration builds
                    .build()
                    .also { INSTANCE = it }
            }




    }
}
