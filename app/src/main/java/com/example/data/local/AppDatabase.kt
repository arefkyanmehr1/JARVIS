package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.ApiKeyDao
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.RuleDao
import com.example.data.local.dao.SmsDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RuleEntity
import com.example.data.local.entity.SmsMessageEntity

@Database(
    entities = [
        SmsMessageEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        ApiKeyEntity::class,
        RuleEntity::class,
        MemoryEntity::class,
        ActivityLogEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun chatDao(): ChatDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun ruleDao(): RuleDao
    abstract fun memoryDao(): MemoryDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_sms_ai.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
