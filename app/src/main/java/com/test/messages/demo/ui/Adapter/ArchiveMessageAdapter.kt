package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.R
import com.test.messages.demo.data.Model.MessageDiffCallback
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.Util.TimeUtils.formatTimestamp
import com.test.messages.demo.Util.TimeUtils.getInitials
import com.test.messages.demo.Util.TimeUtils.getRandomColor


class ArchiveMessageAdapter(private val onArchiveSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<ArchiveMessageAdapter.ViewHolder>() {

    var onArchiveItemClickListener: ((MessageItem) -> Unit)? = null
    private var messages: MutableList<MessageItem> = mutableListOf()

    val selectedMessages = mutableSetOf<MessageItem>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
        val blueDot: ImageView = itemView.findViewById(R.id.blueDot)
        val icPin: ImageView = itemView.findViewById(R.id.icPin)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.senderName.text = message.sender
        holder.messageBody.text = message.body
        holder.date.text = formatTimestamp(holder.itemView.context,message.timestamp)
        val firstChar = message.sender.trim().firstOrNull()
        val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()
        if (startsWithSpecialChar|| message.profileImageUrl != null && message.profileImageUrl.isNotEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.profileImageUrl)
                .placeholder(R.drawable.ic_user)
                .dontAnimate()
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = getInitials(message.sender)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(getRandomColor(message.sender))
        }

        if (!message.isRead) {
            holder.senderName.setTypeface(null, Typeface.BOLD)
            holder.blueDot.visibility = View.VISIBLE
            holder.messageBody.setTextColor(holder.itemView.resources.getColor(R.color.textcolor))
        } else {
            holder.senderName.setTypeface(null, Typeface.NORMAL)
            holder.blueDot.visibility = View.GONE
            holder.messageBody.setTextColor(holder.itemView.resources.getColor(R.color.gray_txtcolor))
        }

        if (message.isPinned) {
            holder.icPin.visibility = View.VISIBLE
        } else {
            holder.icPin.visibility = View.GONE
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
                onArchiveItemClickListener?.invoke(message)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(message, holder)
            true
        }
    }

    private fun toggleSelection(message: MessageItem, holder: ViewHolder) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message)
            holder.icSelect.visibility = View.GONE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        } else {
            selectedMessages.add(message)
            holder.icSelect.visibility = View.VISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.gray_txtcolor))
        }

        onArchiveSelectionChanged(selectedMessages.size)
        notifyDataSetChanged()
    }

    fun getSelectedThreadIds(): List<Long> {
        return selectedMessages.map { it.threadId }
    }

    fun selectAll(select: Boolean) {
        if (select) {
            selectedMessages.clear()
            selectedMessages.addAll(messages)
        } else {
            selectedMessages.clear()
        }
        notifyDataSetChanged()
        onArchiveSelectionChanged(selectedMessages.size)
    }

    fun clearSelection() {
        selectedMessages.clear()
        notifyDataSetChanged()
        onArchiveSelectionChanged(selectedMessages.size)
    }

    fun isAllSelected(): Boolean {
        return messages.isNotEmpty() && selectedMessages.size == messages.size
    }
    override fun getItemCount(): Int = messages.size

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

   /* fun updateReadStatus(threadIds: List<Long>) {
        val newList = messages.map { message ->
            if (threadIds.contains(message.threadId)) message.copy(isRead = true) else message
        }
        submitList(newList)
    }*/

    fun updateReadStatus(threadIds: List<Long>) {
        val updatedItems = messages.toMutableList()
        var hasChanges = false

        threadIds.forEachIndexed { _, threadId ->
            val index = updatedItems.indexOfFirst { it.threadId == threadId }
            if (index != -1) {
                val item = updatedItems[index]
                updatedItems[index] = item.copy(isRead = !item.isRead)
                notifyItemChanged(index)
                hasChanges = true
            }
        }

        if (hasChanges) {
            submitList(updatedItems)
        }
    }

    fun updateUnreadStatus(threadIds: List<Long>) {
        val newList = messages.map { message ->
            if (threadIds.contains(message.threadId)) message.copy(isRead = false) else message
        }
        submitList(newList)
    }

}
