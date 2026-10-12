package com.minetallest.mino.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Dexie `mino-db` v4 → SQLite v4 (spec §7.1). */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        FolderEntity::class,
        ScheduledEntity::class,
        TrashEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MinoDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun folderDao(): FolderDao
    abstract fun scheduledDao(): ScheduledDao
    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile
        private var instance: MinoDatabase? = null

        fun get(context: Context): MinoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MinoDatabase::class.java,
                    "mino-db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
