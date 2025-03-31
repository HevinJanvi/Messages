package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
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
        holder.senderName.text = message.sender
        holder.messageBody.text = message.body
        holder.date.text = SimpleDateFormat(
            "dd/MM/yyyy hh:mm a",
            Locale.getDefault()
        ).format(Date(message.timestamp))

        val isSelected = selectedMessages.contains(message)
        holder.icSelect.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.container.setBackgroundColor(
            holder.itemView.context.getColor(if (isSelected) R.color.select_bg else R.color.transparant)
        )

        holder.itemView.setOnClickListener {
            if (isMultiSelectionMode) {
                toggleSelection(message, holder)
            } else {
//                onItemClick(message) // Open ConversationActivity
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
}
