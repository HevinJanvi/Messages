package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class DeletedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: Long,

    @ColumnInfo(name = "thread_id")
    val threadId: Long,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "type")
    val type: Int,

    @ColumnInfo(name = "read")
    val read: Boolean,

    @ColumnInfo(name = "subscription_id")
    val subscriptionId: Int,

    val deletedTime: Long = System.currentTimeMillis()
)
