package com.test.messages.demo.data.Model

import androidx.recyclerview.widget.DiffUtil

class MessageDiffCallback(
    private val oldList: List<MessageItem>,
    private val newList: List<MessageItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare unique identifiers (e.g., threadId, or any unique field)
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId &&
                oldList[oldItemPosition].isPinned == newList[newItemPosition].isPinned
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return old == new || (
                old.body == new.body &&
                        old.timestamp == new.timestamp &&
                        old.isRead == new.isRead &&
                        old.isPinned == new.isPinned &&
                        old.isMuted == new.isMuted &&
                        old.sender == new.sender
                )
    }
}