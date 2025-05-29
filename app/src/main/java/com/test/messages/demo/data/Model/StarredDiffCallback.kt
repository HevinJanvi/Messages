package com.test.messages.demo.data.Model

import androidx.recyclerview.widget.DiffUtil
import com.test.messages.demo.data.Database.Starred.StarredMessage

class StarredDiffCallback(
    private val oldList: List<StarredMessage>,
    private val newList: List<StarredMessage>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        return oldList[oldItemPosition] == newList[newItemPosition]
        return oldList[oldItemPosition].thread_id == newList[newItemPosition].thread_id

    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return old == new || (
                old.body == new.body &&
                        old.timestamp == new.timestamp &&
                        old.sender == new.sender &&
                        old.profile_image == new.profile_image
                )
    }
}