package com.berlin.nbatv.data

// com.berlin.nbatv.data.AppDatabase.kt (创建新文件)

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoItem::class], version = 1, exportSchema = false) // 初始版本为1
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_database" // 数据库文件名
                )
                    // .fallbackToDestructiveMigration() // 如果发生 schema 更改且没有提供迁移路径，则销毁并重建数据库 (开发阶段方便，生产环境需谨慎)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}