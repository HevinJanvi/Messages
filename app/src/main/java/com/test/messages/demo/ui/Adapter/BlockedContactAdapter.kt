package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.MessageDiffCallback
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.databinding.ItemMessageBinding

class BlockedContactAdapter(
    private val onItemClick: (MessageItem) -> Unit,
    private val onSelectionChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<BlockedContactAdapter.ViewHolder>() {

    val messages = mutableListOf<MessageItem>()
    var selectedItems = mutableSetOf<MessageItem>()
    var isMultiSelectMode = false

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNumber: TextView = itemView.findViewById(R.id.senderName)
        val txtMessage: TextView = itemView.findViewById(R.id.messageContent)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.txtNumber.text = message.sender
        holder.txtMessage.text = message.body

        if (selectedItems.contains(message)) {
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        } else {
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        }

        holder.itemView.setOnLongClickListener {
            isMultiSelectMode = true
            toggleSelection(message)
            true
        }

        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) {
                toggleSelection(message)
            } else {
                onItemClick(message)
            }
        }
    }

    override fun getItemCount() = messages.size

    fun updateList(newMessages: List<MessageItem>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    fun clearSelection() {
        selectedItems.clear()
        isMultiSelectMode = false
        notifyDataSetChanged()
    }

    fun getAllMessages(): List<MessageItem> = messages


    private fun toggleSelection(messageItem: MessageItem) {
        if (selectedItems.contains(messageItem)) {
            selectedItems.remove(messageItem)
        } else {
            selectedItems.add(messageItem)
        }
        if (selectedItems.isEmpty()) {
            isMultiSelectMode = false
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedItems.isNotEmpty())
        (onSelectionChanged as? (Boolean, Boolean) -> Unit)?.invoke(
            selectedItems.isNotEmpty(),
            selectedItems.size == messages.size
        )

    }
}
