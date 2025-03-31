package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.ui.AlphabticScroll.ItemMoveCallback
import com.test.messages.demo.R
import java.util.Collections

class EditCategoryAdapter(
    private var categories: MutableList<String>,
    private val onItemMoved: (List<String>) -> Unit
) :
    RecyclerView.Adapter<EditCategoryAdapter.CategoryViewHolder>(),
    ItemMoveCallback.ItemTouchHelperContract {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.categoryText)
        val view: View = itemView.findViewById(R.id.view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.textView.text = category
        holder.view.visibility = if (position == categories.size - 1) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = categories.size


    fun updateCategories(newCategories: List<String>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged() // Refresh RecyclerView
    }


    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(categories, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
        onItemMoved(categories)
    }
}
