package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseDifferAdapter
import com.test.messages.demo.R
import com.test.messages.demo.data.Message
import com.test.messages.demo.data.MessageDiffCallback


class MessageAdapter1 :
    BaseDifferAdapter<Message, MessageAdapter1.MessageViewHolder>(MessageDiffCallback()) {

    private val messages = mutableListOf<Message>()

    // Method to get the last message's number or position
    private fun getMessageNumber(position: Int): Int {
        return position + 1  // This will give a number starting from 1
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, item: Message?) {
        if (item != null) {
            // Get the number for the message (for example, position + 1 for 1-based indexing)
            val messageNumber = getMessageNumber(position)
            holder.bind(item, messageNumber)
        } else {
            Log.d("MessageAdapter", "Item at position $position is null.")
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }


    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: Message, messageNumber: Int) {
            itemView.findViewById<TextView>(R.id.senderName).text = message.sender
            itemView.findViewById<TextView>(R.id.messageContent).text = message.content
        }
    }


}