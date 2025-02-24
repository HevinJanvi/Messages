package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.Log
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
import com.test.messages.demo.data.MessageDiffCallback
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.ui.Utils.TimeUtils.formatTimestamp
import com.test.messages.demo.ui.Utils.TimeUtils.getInitials
import com.test.messages.demo.ui.Utils.TimeUtils.getRandomColor


class MessageAdapter(private val onSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    var onItemClickListener: ((MessageItem) -> Unit)? = null
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

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.senderName.text = message.sender
        holder.messageBody.text = message.body
        holder.date.text = formatTimestamp(message.timestamp)
        if (message.profileImageUrl != null && message.profileImageUrl.isNotEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.profileImageUrl)
                .placeholder(R.drawable.ic_user)
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
                onItemClickListener?.invoke(message)
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
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        }
        onSelectionChanged(selectedMessages.size)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedMessages.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedMessages.size)
    }

    override fun getItemCount(): Int = messages.size
    fun getAllMessages(): List<MessageItem> = messages

    fun submitList(newMessages: List<MessageItem>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }
}
