package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.extractOtp


class ArchiveMessageAdapter(private val onArchiveSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<ArchiveMessageAdapter.ViewHolder>() {

    var onArchiveItemClickListener: ((MessageItem) -> Unit)? = null
    var messages: MutableList<MessageItem> = mutableListOf()

    val selectedMessages = mutableSetOf<MessageItem>()
    private var draftMessages: MutableMap<Long, Pair<String, Long>> = mutableMapOf()

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
        val otpTextView: TextView = itemView.findViewById(R.id.otpTextView)

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

        if (selectedMessages.find { it.threadId == message.threadId } != null) {
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
        if (draftMessages.containsKey(message.threadId)) {
            val (draftText, _) = draftMessages[message.threadId]!!
            val draftLabel = holder.itemView.context.getString(R.string.draft) + " "
            val draftTextSpannable = SpannableStringBuilder(draftLabel).apply {
                setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.colorPrimary
                        )
                    ),
                    0, draftLabel.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(draftText)
                setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.gray_txtcolor
                        )
                    ),
                    draftLabel.length, draftLabel.length + draftText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            holder.messageBody.text = draftTextSpannable

        } else {
            holder.messageBody.text = message.body
        }

        val otp = message.body.extractOtp()
        if (!otp.isNullOrEmpty()) {
            holder.otpTextView.text = holder.itemView.context.getString(R.string.copy_otp)
            holder.otpTextView.visibility = View.VISIBLE
            holder.otpTextView.setOnClickListener {
                ViewUtils.copyToClipboard(holder.itemView.context, otp)
                holder.otpTextView.animate()
                    .alpha(0.5f)
                    .setDuration(100)
                    .withEndAction {
                        holder.otpTextView.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        } else {
            holder.otpTextView.visibility = View.GONE
        }
    }

    private fun toggleSelection(message: MessageItem, holder: ViewHolder) {
        if (selectedMessages.find { it.threadId == message.threadId } != null) {
            selectedMessages.removeIf { it.threadId == message.threadId }
            holder.icSelect.visibility = View.INVISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        } else {
            selectedMessages.add(message)
            holder.icSelect.visibility = View.VISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
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


    fun updateDrafts(drafts: Map<Long, Pair<String, Long>>) {
        this.draftMessages.clear()
        this.draftMessages.putAll(drafts)
        notifyDataSetChanged()
    }

}
