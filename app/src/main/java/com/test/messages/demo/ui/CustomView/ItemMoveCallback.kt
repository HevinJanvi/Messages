package com.test.messages.demo.ui.CustomView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ItemMoveCallback(private val adapter: ItemTouchHelperContract) : ItemTouchHelper.Callback() {

    interface ItemTouchHelperContract {
        fun onItemMove(fromPosition: Int, toPosition: Int)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder.adapterPosition == 0) {
            makeMovementFlags(0, 0)
        } else {
            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (viewHolder.adapterPosition == 0 || target.adapterPosition == 0) {
            return false
        }
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}


}
