package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        Member::class,
        Meeting::class,
        OfficialDocument::class,
        Notice::class,
        Donation::class,
        Expense::class,
        ChatMessage::class
    ],
    version = 6,
    exportSchema = false
)
abstract class TTSDatabase : RoomDatabase() {
    abstract fun ttsDao(): TTSDao

    companion object {
        @Volatile
        private var INSTANCE: TTSDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TTSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TTSDatabase::class.java,
                    "tts_committee_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
