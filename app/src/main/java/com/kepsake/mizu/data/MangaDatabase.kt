package com.kepsake.mizu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaFileDao
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.models.MangaPageDao

@Database(
    entities = [MangaFile::class, MangaPage::class],
    version = 1,
    exportSchema = false
)
abstract class MangaDatabase : RoomDatabase() {
    abstract fun mangaFileDao(): MangaFileDao
    abstract fun mangaPageDao(): MangaPageDao

    companion object {
        @Volatile
        private var INSTANCE: MangaDatabase? = null

        fun getDatabase(context: Context): MangaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MangaDatabase::class.java,
                    "manga_database"
                ).fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
