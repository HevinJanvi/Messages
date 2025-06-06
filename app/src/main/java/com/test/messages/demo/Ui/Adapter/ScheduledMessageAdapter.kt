package com.test.messages.demo.Ui.Adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants
import com.test.messages.demo.Utils.TimeUtils.formatTimestamp
import com.test.messages.demo.Utils.TimeUtils.getInitials
import com.test.messages.demo.Utils.TimeUtils.getRandomColor

class ScheduledMessageAdapter(private val onItemClick: (ScheduledMessage) -> Unit) :
    ListAdapter<ScheduledMessage, ScheduledMessageAdapter.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ScheduledMessage>() {
            override fun areItemsTheSame(
                oldItem: ScheduledMessage,
                newItem: ScheduledMessage
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ScheduledMessage,
                newItem: ScheduledMessage
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
        holder.itemView.setOnClickListener { onItemClick(message) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipientView: TextView = itemView.findViewById(R.id.recipient)
        private val messageView: TextView = itemView.findViewById(R.id.message)
        private val timeView: TextView = itemView.findViewById(R.id.time)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        fun bind(message: ScheduledMessage) {
            recipientView.text = message.recipientName
            messageView.text = message.message
            timeView.text = formatTimestamp(itemView.context,message.scheduledTime)

            if (message.recipientNumber.contains(Constants.GROUP_SEPARATOR)) {
                icUser.visibility = View.VISIBLE
                initialsTextView.visibility = View.GONE
                icUser.setImageResource(R.drawable.ic_group)
            } else {
                val firstChar = message.recipientName.trim().firstOrNull()
                val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()
                if (message.profileUrl != null && message.profileUrl.isNotEmpty() || startsWithSpecialChar) {
                    icUser.visibility = View.VISIBLE
                    initialsTextView.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(message.profileUrl)
                        .placeholder(R.drawable.ic_user)
                        .into(icUser)
                } else {
                    icUser.visibility = View.GONE
                    initialsTextView.visibility = View.VISIBLE
                    initialsTextView.text = getInitials(message.recipientName)
                    profileContainer.backgroundTintList =
                        ColorStateList.valueOf(getRandomColor(message.recipientName))
                }
            }

        }
    }
}
