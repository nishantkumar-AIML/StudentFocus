package com.ai_assistant.studentfocus.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ai_assistant.studentfocus.models.*

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        StudyHistoryEntity::class,
        NoteEntity::class,
        ExamEntity::class,
        AppSettingEntity::class,
        AttendanceEntity::class,
        CgpaCourseEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        HabitEntity::class,
        ExpenseEntity::class,
        ChatMessageEntity::class,
        RoutineActivityEntity::class,
        AppUsageEntity::class,
        AppActivitySessionEntity::class,
        AppCategoryOverrideEntity::class,
        UserFeedbackEntity::class,
        PersonalBehaviorProfileEntity::class,
        TimeTableSlotEntity::class
    ],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_activity_sessions_date` ON `app_activity_sessions` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_activity_sessions_startTime` ON `app_activity_sessions` (`startTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_activity_sessions_packageName` ON `app_activity_sessions` (`packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_usage_history_date` ON `app_usage_history` (`date`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_focus_db"
                )
                .addMigrations(MIGRATION_19_20)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
