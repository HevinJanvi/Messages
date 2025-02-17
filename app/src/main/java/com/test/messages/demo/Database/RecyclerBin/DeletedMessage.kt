package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class DeletedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Optional: Use this as the unique identifier if needed

    @ColumnInfo(name = "thread_id")
    val threadId: Long,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "number")
    val number: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean,

    @ColumnInfo(name = "reciptid")
    val reciptid: Int, // This can be renamed if necessary

    @ColumnInfo(name = "reciptids")
    val reciptids: String, // This can be renamed if necessary

    @ColumnInfo(name = "profile_image_url")
    val profileImageUrl: String
)
