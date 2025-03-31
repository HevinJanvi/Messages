package com.test.messages.demo.ui.Adapter


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.test.messages.demo.Util.ViewUtils.extractOtp

class MessageAdapter(private val onSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    var onItemClickListener: ((MessageItem) -> Unit)? = null
    private var messages: MutableList<MessageItem> = mutableListOf()

    val selectedMessages = mutableSetOf<MessageItem>()
    private var draftMessages: MutableMap<Long, Pair<String, Long>> = mutableMapOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val otpTextView: TextView = itemView.findViewById(R.id.otpTextView)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
        val blueDot: ImageView = itemView.findViewById(R.id.blueDot)
        val icPin: ImageView = itemView.findViewById(R.id.icPin)
        val icMute: ImageView = itemView.findViewById(R.id.icMute)

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
        val firstChar = message.sender.trim().firstOrNull()
        val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()
        if (startsWithSpecialChar || message.profileImageUrl != null && message.profileImageUrl.isNotEmpty()) {
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


        if (draftMessages.containsKey(message.threadId)) {
            val (draftText, _) = draftMessages[message.threadId]!!

            val draftLabel = holder.itemView.context.getString(R.string.draft) + " "
            val draftTextSpannable = SpannableStringBuilder(draftLabel).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)),
                    0, draftLabel.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(draftText)
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(holder.itemView.context, R.color.gray_txtcolor)),
                    draftLabel.length, draftLabel.length + draftText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            holder.messageBody.text = draftTextSpannable

        } else {
            holder.messageBody.text = message.body
            holder.messageBody.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.textcolor))
        }


        val otp = message.body.extractOtp()
        if (!otp.isNullOrEmpty()) {
            holder.otpTextView.text = holder.itemView.context.getString(R.string.copy_code)
            holder.otpTextView.visibility = View.VISIBLE


            holder.otpTextView.setOnClickListener {
                copyToClipboard(holder.itemView.context, otp)
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

        if (message.isPinned) {
            holder.icPin.visibility = View.VISIBLE
        } else {
            holder.icPin.visibility = View.GONE
        }

        if (message.isMuted) {
            holder.icMute.visibility = View.VISIBLE
        } else {
            holder.icMute.visibility = View.GONE
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

    fun removeItems(ids: List<Long>) {
        messages.removeAll { it.threadId in ids }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = messages.size
    fun getAllMessages(): List<MessageItem> = messages

    fun submitList(newMessages: List<MessageItem>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", text)
        clipboard.setPrimaryClip(clip)
    }

    fun updateDrafts(drafts: Map<Long, Pair<String, Long>>) {
        this.draftMessages.clear()
        this.draftMessages.putAll(drafts)
        notifyDataSetChanged()  // Refresh UI
    }

    fun getPositionForThreadId(threadId: Long): Int {
        return messages.indexOfFirst { it.threadId == threadId }
    }

    fun updateThreadStatus(threadId: Long, isRead: Boolean) {
        val index = messages.indexOfFirst { it.threadId == threadId }
        if (index != -1) {
            messages[index].isRead = isRead
        }
    }

}


/*class MessageAdapter(private val onSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    var onItemClickListener: ((MessageItem) -> Unit)? = null
    private val messages: MutableList<MessageItem> = mutableListOf()
    val selectedMessages = mutableSetOf<MessageItem>()
    private var draftMessages: MutableMap<Long, Pair<String, Long>> = mutableMapOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val otpTextView: TextView = itemView.findViewById(R.id.otpTextView)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
        val blueDot: ImageView = itemView.findViewById(R.id.blueDot)
        val icPin: ImageView = itemView.findViewById(R.id.icPin)
        val icMute: ImageView = itemView.findViewById(R.id.icMute)
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

        // Profile image or initials setup
        if (!message.profileImageUrl.isNullOrEmpty()) {
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

        holder.blueDot.visibility = if (!message.isRead) View.VISIBLE else View.GONE
        holder.senderName.setTypeface(null, if (!message.isRead) Typeface.BOLD else Typeface.NORMAL)
        holder.messageBody.setTextColor(
            holder.itemView.resources.getColor(
                if (!message.isRead) R.color.textcolor else R.color.gray_txtcolor
            )
        )

        if (draftMessages.containsKey(message.threadId)) {
            val (draftText, _) = draftMessages[message.threadId]!!
            holder.messageBody.text = SpannableStringBuilder("Draft: ").apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)),
                    0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(draftText)
            }
        }

        holder.otpTextView.visibility = if (message.body.extractOtp().isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.icPin.visibility = if (message.isPinned) View.VISIBLE else View.GONE
        holder.icMute.visibility = if (message.isMuted) View.VISIBLE else View.GONE

        holder.icSelect.visibility = if (selectedMessages.contains(message)) View.VISIBLE else View.GONE
        holder.itemContainer.setBackgroundColor(
            holder.itemView.context.getColor(
                if (selectedMessages.contains(message)) R.color.select_bg else R.color.transparant
            )
        )

        holder.itemView.setOnClickListener {
            if (selectedMessages.isNotEmpty()) toggleSelection(message, holder) else onItemClickListener?.invoke(message)
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(message, holder)
            true
        }
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<MessageItem>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun toggleSelection(message: MessageItem, holder: ViewHolder) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message)
        } else {
            selectedMessages.add(message)
        }
        onSelectionChanged(selectedMessages.size)
        notifyItemChanged(messages.indexOf(message))
    }

    fun clearSelection() {
        selectedMessages.clear()
        notifyDataSetChanged()
    }

    fun removeItems(ids: List<Long>) {
        messages.removeAll { it.threadId in ids }
        notifyDataSetChanged()
    }

    fun updateDrafts(drafts: Map<Long, Pair<String, Long>>) {
        this.draftMessages.clear()
        this.draftMessages.putAll(drafts)
        notifyDataSetChanged()
    }

    fun getAllMessages(): List<MessageItem> = messages

    fun getPositionForThreadId(threadId: Long): Int {
        return messages.indexOfFirst { it.threadId == threadId }
    }

    fun updateThreadStatus(threadId: Long, isRead: Boolean) {
        val index = messages.indexOfFirst { it.threadId == threadId }
        if (index != -1) {
            messages[index].isRead = isRead
        }
    }

}*/