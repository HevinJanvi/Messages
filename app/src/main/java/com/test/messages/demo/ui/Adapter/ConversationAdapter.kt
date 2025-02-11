package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.databinding.ListItemConversationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter : ListAdapter<ConversationItem, ConversationAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ConversationItem>() {
        override fun areItemsTheSame(oldItem: ConversationItem, newItem: ConversationItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ConversationItem, newItem: ConversationItem) =
            oldItem == newItem
    }
) {
    companion object {
        private const val VIEW_TYPE_INCOMING = 1
        private const val VIEW_TYPE_OUTGOING = 2
    }


    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isIncoming()) VIEW_TYPE_INCOMING else VIEW_TYPE_OUTGOING
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageBody: TextView = view.findViewById(R.id.messageBody)
        val messageDate: TextView = view.findViewById(R.id.messageDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_INCOMING) {
            R.layout.item_message_incoming
        } else {
            R.layout.item_message_outgoing
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        holder.messageBody.text = message.body
        holder.messageDate.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(message.date))
    }
}
