package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.R
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.data.Model.ConversationItem
import java.util.Locale

class SearchMessageAdapter(
    private var messages: List<ConversationItem>,
    private var query: String = "",
    private var matchCountMap: Map<Long, Int> = emptyMap(),
    private val onItemClick: (ConversationItem, String) -> Unit
) : RecyclerView.Adapter<SearchMessageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        return messages[position].id
    }

    fun updateList(newMessages: List<ConversationItem>, newQuery: String, newCountMap: Map<Long, Int>) {
        Log.d("TAG", "updateList:search 1 ")
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages
        query = newQuery
        matchCountMap = newCountMap
        diffResult.dispatchUpdatesTo(this)
        Log.d("TAG", "updateList:search 2 ")

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.sender.text = highlightText(message.address, query, holder.itemView.context)
        holder.message.text = highlightText(message.body, query, holder.itemView.context)
        holder.date.text = TimeUtils.formatTimestamp(message.date)

        holder.itemView.setOnClickListener {
            onItemClick(message, query)
        }
//        val count = matchCountMap[message.threadId] ?: 1
//        holder.matchCount.text = "$count match${if (count > 1) "es" else ""} found"

        val matchCount = matchCountMap[message.threadId] ?: 0
        holder.matchCount.text = "${matchCount}" +""+ holder.itemView.context.getString(R.string.matches_found)

        if (!message.profileImageUrl.isNullOrEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.profileImageUrl)
                .placeholder(R.drawable.ic_user)
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = TimeUtils.getInitials(message.address)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(TimeUtils.getRandomColor(message.address))
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
                val textColor = ContextCompat.getColor(context, R.color.serach_highlight_color)
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
        val matchCount: TextView = itemView.findViewById(R.id.matchCount)
    }
}

class MessageDiffCallback(
    private val oldList: List<ConversationItem>,
    private val newList: List<ConversationItem>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos].id == newList[newPos].id
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos]
    }
}

/*
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
        holder.sender.text = highlightText(message.sender, query, holder.itemView.context)
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
                val textColor = ContextCompat.getColor(context, R.color.serach_highlight_color)
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
*/
