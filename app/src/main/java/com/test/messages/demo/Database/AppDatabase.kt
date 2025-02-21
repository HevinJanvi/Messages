package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.test.messages.demo.Database.Archived.ArchivedConversation
import com.test.messages.demo.Database.Archived.ArchivedDao
import com.test.messages.demo.Database.Converters
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao

@Database(entities = [DeletedMessage::class, ArchivedConversation::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun archivedDao(): ArchivedDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "messages_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
