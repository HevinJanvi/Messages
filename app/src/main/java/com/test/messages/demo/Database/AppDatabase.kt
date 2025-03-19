package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.test.messages.demo.Database.Archived.ArchivedConversation
import com.test.messages.demo.Database.Archived.ArchivedDao
import com.test.messages.demo.Database.Block.BlockConversation
import com.test.messages.demo.Database.Block.BlockDao
import com.test.messages.demo.Database.Converters
import com.test.messages.demo.Database.Pin.PinDao
import com.test.messages.demo.Database.Pin.PinMessage
import com.test.messages.demo.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.Database.Scheduled.ScheduledMessageDao
import com.test.messages.demo.Database.Starred.StarredMessage
import com.test.messages.demo.Database.Starred.StarredMessageDao
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao

@Database(entities = [DeletedMessage::class, ArchivedConversation::class, BlockConversation::class,
    PinMessage::class, StarredMessage::class,ScheduledMessage::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun archivedDao(): ArchivedDao
    abstract fun blockDao(): BlockDao
    abstract fun pinDao(): PinDao
    abstract fun starredMessageDao(): StarredMessageDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "messages_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
