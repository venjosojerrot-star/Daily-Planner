package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ClosetItem
import com.example.data.model.ClosetOutfit
import com.example.data.model.FocusSession
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.data.model.WalletTransaction
import com.example.data.model.WalletDebt
import com.example.data.model.WalletBudget
import com.example.data.model.WalletGoal
import com.example.data.model.WalletAccount
import com.example.data.model.PublicSpeakingTalk
import com.example.data.model.DailyVerseChatMessage

@Database(
    entities = [
        PlannerEvent::class, 
        PlannerTask::class, 
        FocusSession::class, 
        WalletTransaction::class,
        WalletDebt::class,
        WalletBudget::class,
        WalletGoal::class,
        WalletAccount::class,
        ClosetItem::class,
        ClosetOutfit::class,
        DailyVerseChatMessage::class,
        PublicSpeakingTalk::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun walletDao(): WalletDao
    abstract fun walletExtraDao(): WalletExtraDao
    abstract fun closetDao(): ClosetDao
    abstract fun dailyVerseChatDao(): DailyVerseChatDao
    abstract fun publicSpeakingTalkDao(): PublicSpeakingTalkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "planner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
