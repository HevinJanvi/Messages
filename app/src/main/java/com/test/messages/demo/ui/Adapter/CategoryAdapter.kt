package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R

class CategoryAdapter(private val context: Context,
                      private var categories: List<String>,
                      private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedCategory: String = context.getString(R.string.inbox)

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.categoryText)
        val txtLy: ConstraintLayout = itemView.findViewById(R.id.txtLy)
        val underlineView: View = itemView.findViewById(R.id.underlineView)
    }

    fun updateCategories(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.textView.text = category
        val selectedColor = ContextCompat.getColor(holder.itemView.context, R.color.textcolor)
        val defaultColor = ContextCompat.getColor(holder.itemView.context, R.color.gray_txtcolor)
        val selectedFont = ResourcesCompat.getFont(holder.itemView.context, R.font.product_sans_medium)
        val defaultFont = ResourcesCompat.getFont(holder.itemView.context, R.font.product_sans_regular)

        holder.textView.setTextColor(if (category == selectedCategory) selectedColor else defaultColor)
        holder.textView.typeface = if (category == selectedCategory) selectedFont else defaultFont


        if (category == selectedCategory) {
            holder.textView.post {
                holder.underlineView.layoutParams.width = holder.textView.width / 2
                holder.underlineView.requestLayout()
            }
        } else {
            holder.underlineView.layoutParams.width = 0
            holder.underlineView.requestLayout()
        }

        holder.txtLy.setOnClickListener {
            selectedCategory = category
            onCategorySelected(category)
            holder.textView.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(350)
                .withEndAction {
                    holder.textView.animate().scaleX(1f).scaleY(1f).setDuration(350).start()
                }
                .start()
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = categories.size
}
