package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao

@Database(entities = [DeletedMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recycleBinDao(): RecycleBinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recycle_bin_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
