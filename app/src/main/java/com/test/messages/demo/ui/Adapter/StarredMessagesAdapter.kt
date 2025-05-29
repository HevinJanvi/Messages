package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
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
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.extractOtp
import com.test.messages.demo.data.Database.Starred.StarredMessage
import com.test.messages.demo.data.Model.StarredDiffCallback


class StarredMessagesAdapter :
    RecyclerView.Adapter<StarredMessagesAdapter.ViewHolder>() {

    var onItemClickListener: ((StarredMessage) -> Unit)? = null
    private var messages: MutableList<StarredMessage> = mutableListOf()

    val selectedMessages = mutableSetOf<StarredMessage>()
    private var lastStarredMessages: Map<Long, String> = emptyMap()


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
        val otpTextView: TextView = itemView.findViewById(R.id.otpTextView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_starred, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.senderName.text = message.sender
        val lastStarredMessage = lastStarredMessages[message.thread_id] ?: message.body
        holder.messageBody.text = lastStarredMessage ?: message.body
        holder.date.text = formatTimestamp(holder.itemView.context,message.timestamp)
        if (message.profile_image != null && message.profile_image.isNotEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.profile_image)
                .placeholder(R.drawable.ic_user)
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = getInitials(message.sender)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(getRandomColor(message.sender))
        }

        if (selectedMessages.contains(message)) {
            holder.icSelect.visibility = View.VISIBLE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        } else {
            holder.icSelect.visibility = View.GONE
            holder.itemContainer.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(message)
        }
        val displayedBody = lastStarredMessages[message.thread_id] ?: message.body
        val otp = displayedBody.extractOtp()
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

    fun getAllMessages(): List<StarredMessage> {
        return messages.toList()
    }

    override fun getItemCount(): Int = messages.size
    fun submitList(newMessages: List<StarredMessage>) {
        val diffCallback = StarredDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }
}
