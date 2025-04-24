package com.test.messages.demo.ui.Adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.Model.MessageDiffCallback
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.Util.TimeUtils

class BlockedMessagesAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<BlockedMessagesAdapter.ViewHolder>() {

    var onBlockItemClickListener: ((MessageItem) -> Unit)? = null
    private var messages: MutableList<MessageItem> = mutableListOf()
    val selectedMessages = mutableSetOf<MessageItem>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
        val blueDot: ImageView = itemView.findViewById(R.id.blueDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_block_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.senderName.text = message.sender
        holder.messageBody.text = message.body
        holder.date.text = TimeUtils.formatTimestamp(holder.itemView.context,message.timestamp)

        if (!message.isRead) {
            holder.senderName.setTypeface(null, Typeface.BOLD)
            holder.blueDot.visibility = View.VISIBLE
            holder.messageBody.setTextColor(holder.itemView.resources.getColor(R.color.textcolor))
        } else {
            holder.senderName.setTypeface(null, Typeface.NORMAL)
            holder.blueDot.visibility = View.GONE
            holder.messageBody.setTextColor(holder.itemView.resources.getColor(R.color.gray_txtcolor))
        }

        if (selectedMessages.contains(message)) {
            holder.icSelect.visibility = View.VISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        } else {
            holder.icSelect.visibility = View.GONE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        }

        holder.itemView.setOnClickListener {
            if (selectedMessages.isNotEmpty()) {
                toggleSelection(message, holder)
            } else {
                onBlockItemClickListener?.invoke(message)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(message, holder)
            true
        }
    }

    override fun getItemCount() = messages.size

    fun getSelectedThreadIds(): List<Long> {
        return selectedMessages.map { it.threadId }
    }

    fun isAllSelected(): Boolean {
        return messages.isNotEmpty() && selectedMessages.size == messages.size
    }
    fun removeItems(threadIds: List<Long>) {
        val updatedList = messages.toMutableList()
        updatedList.removeAll { it.threadId in threadIds }
        submitList(updatedList)
    }

    fun submitList(newMessages: List<MessageItem>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }


    fun selectAll(select: Boolean) {
        if (select) {
            selectedMessages.clear()
            selectedMessages.addAll(messages)
        } else {
            selectedMessages.clear()
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedMessages.size)
    }

    fun clearSelection() {
        selectedMessages.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedMessages.size)
    }


    fun getAllMessages(): List<MessageItem> = messages


    private fun toggleSelection(message: MessageItem, holder:ViewHolder) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message)
            holder.icSelect.visibility = View.GONE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        } else {
            selectedMessages.add(message)
            holder.icSelect.visibility = View.VISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.gray_txtcolor))
        }

        onSelectionChanged(selectedMessages.size)
        notifyDataSetChanged()
    }
}
