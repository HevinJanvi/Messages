package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
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
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.Util.TimeUtils

class SearchContactAdapter(
    private var contacts: List<ContactItem>,
    private var query: String,
    private val onItemClick: (ContactItem) -> Unit

) : RecyclerView.Adapter<SearchContactAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        return contacts[position].cid!!.toLong()
    }

    fun updateList(newContacts: List<ContactItem>, newQuery: String) {
        val diffCallback = ContactDiffCallback(contacts, newContacts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        contacts = newContacts
        this.query = newQuery
        diffResult.dispatchUpdatesTo(this)
    }

    private fun highlightText(context: Context, text: String, query: String): SpannableString {
        val spannableString = SpannableString(text)
        var startIndex = text.indexOf(query, ignoreCase = true)

        while (startIndex != -1) {
            val endIndex = startIndex + query.length

            val highlightColor = ContextCompat.getColor(context, R.color.yellow)
            val textColor = ContextCompat.getColor(context, R.color.serach_highlight_color)

            spannableString.setSpan(
                ForegroundColorSpan(textColor),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                BackgroundColorSpan(highlightColor),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            startIndex = text.indexOf(
                query,
                startIndex + 1,
                ignoreCase = true
            )
        }

        return spannableString
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.number.text = contact.phoneNumber
        holder.itemView.setOnClickListener { onItemClick(contact) }

        holder.name.text = if (query.isNotEmpty()) highlightText(holder.itemView.context,contact.name ?: "", query) else contact.name
        holder.number.text = if (query.isNotEmpty()) highlightText(holder.itemView.context,contact.phoneNumber, query) else contact.phoneNumber


        if (contact.profileImageUrl != null && contact.profileImageUrl.isNotEmpty()) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(contact.profileImageUrl)
                .placeholder(R.drawable.ic_user)
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = TimeUtils.getInitials(contact.name!!)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(TimeUtils.getRandomColor(contact.name))

        }
    }

    override fun getItemCount(): Int = contacts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.contactName)
        val number: TextView = itemView.findViewById(R.id.contactNumber)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
    }

    class ContactDiffCallback(
        private val oldList: List<ContactItem>,
        private val newList: List<ContactItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos].cid == newList[newPos].cid

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }
}
