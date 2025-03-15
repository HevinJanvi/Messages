package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.ui.Utils.TimeUtils

class SearchContactAdapter(
    private var contacts: List<ContactItem>,
    private val onItemClick: (ContactItem) -> Unit
) : RecyclerView.Adapter<SearchContactAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    fun updateList(newContacts: List<ContactItem>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.number.text = contact.phoneNumber
        holder.itemView.setOnClickListener { onItemClick(contact) }

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
}
