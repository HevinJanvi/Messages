package com.test.messages.demo.data.Model

import androidx.recyclerview.widget.DiffUtil

class MessageDiffCallback(
    private val oldList: List<MessageItem>,
    private val newList: List<MessageItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        return oldList[oldItemPosition] == newList[newItemPosition]
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId

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