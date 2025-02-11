package com.test.messages.demo.data

import androidx.recyclerview.widget.DiffUtil

class MessageDiffCallback(
    private val oldList: List<MessageItem>,
    private val newList: List<MessageItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare unique identifiers (e.g., threadId, or any unique field)
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare the contents of the item
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}