package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.Util.TimeUtils
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecycleBinAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecycleBinAdapter.ViewHolder>() {

    private var messages: MutableList<DeletedMessage> = mutableListOf()
    val selectedMessages = mutableSetOf<DeletedMessage>()
    private var isMultiSelectionMode = false
    var onBinItemClick: ((DeletedMessage) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)

        val container: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deleted_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val contactName = getContactName(holder.itemView.context, message.address)
        holder.senderName.text = contactName ?: message.address
//        holder.senderName.text = message.address
        holder.messageBody.text = message.body
        holder.date.text = TimeUtils.formatTimestamp(message.date)

        val isSelected = selectedMessages.contains(message)
        holder.icSelect.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.container.setBackgroundColor(
            holder.itemView.context.getColor(if (isSelected) R.color.select_bg else R.color.transparant)
        )

        holder.itemView.setOnClickListener {
            if (isMultiSelectionMode) {
                toggleSelection(message, holder)
            } else {
                onBinItemClick?.invoke(message)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectionMode) {
                isMultiSelectionMode = true
            }
            toggleSelection(message, holder)
            true
        }
    }

    private fun toggleSelection(message: DeletedMessage, holder: ViewHolder) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message)
        } else {
            selectedMessages.add(message)
        }

        updateSelectionUI(holder, message)
        onSelectionChanged(selectedMessages.size)
        if (selectedMessages.isEmpty()) {
            isMultiSelectionMode = false
            notifyDataSetChanged()
        }
    }

    private fun updateSelectionUI(holder: ViewHolder, message: DeletedMessage) {
        if (selectedMessages.contains(message)) {
            holder.icSelect.visibility = View.VISIBLE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        } else {
            holder.icSelect.visibility = View.GONE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        }
    }

    fun clearSelection() {
        isMultiSelectionMode = false
        selectedMessages.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun selectAll() {
        if (messages.isNotEmpty()) {
            isMultiSelectionMode = true
            selectedMessages.clear()
            selectedMessages.addAll(messages)
            onSelectionChanged(selectedMessages.size)
            notifyDataSetChanged()
        }
    }

    fun isAllSelected(): Boolean {
        return selectedMessages.size == messages.size && messages.isNotEmpty()
    }

    fun unselectAll() {
        selectedMessages.clear()
        isMultiSelectionMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun submitList(newMessages: List<DeletedMessage>) {
        messages = newMessages.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = messages.size

    private fun getContactName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return null
    }


}
