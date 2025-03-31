package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.R
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.Util.TimeUtils
import java.util.Locale

class SearchMessageAdapter(
    private var messages: List<MessageItem>,
    private var query: String = "",
    private val onItemClick: (MessageItem, String) -> Unit
) : RecyclerView.Adapter<SearchMessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    fun updateList(newMessages: List<MessageItem>, newQuery: String) {
        messages = newMessages
        query = newQuery
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.sender.text = message.sender
        holder.message.text = highlightText(message.body, query, holder.itemView.context)
        holder.date.text = TimeUtils.formatTimestamp(message.timestamp)
        holder.itemView.setOnClickListener {
            onItemClick(message, query)
        }

        if (message.profileImageUrl != null && message.profileImageUrl.isNotEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.profileImageUrl)
                .placeholder(R.drawable.ic_user)
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = TimeUtils.getInitials(message.sender)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(TimeUtils.getRandomColor(message.sender))
        }

    }

    override fun getItemCount(): Int = messages.size

    private fun highlightText(
        fullText: String,
        searchText: String,
        context: Context
    ): SpannableString {
        val spannable = SpannableString(fullText)
        if (searchText.isNotEmpty()) {
            val start = fullText.lowercase(Locale.getDefault())
                .indexOf(searchText.lowercase(Locale.getDefault()))
            if (start >= 0) {
                val end = start + searchText.length
                val highlightColor = ContextCompat.getColor(context, R.color.yellow)
                val textColor = ContextCompat.getColor(context, R.color.textcolor)
                spannable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(textColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannable
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: TextView = itemView.findViewById(R.id.sender)
        val message: TextView = itemView.findViewById(R.id.message)
        val date: TextView = itemView.findViewById(R.id.date)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
    }
}
