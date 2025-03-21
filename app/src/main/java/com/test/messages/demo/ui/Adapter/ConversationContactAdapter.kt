package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.ui.Utils.TimeUtils

class ConversationContactAdapter(
    private var contacts: List<ContactItem>,
    private val onContactSelected: (ContactItem) -> Unit
) : RecyclerView.Adapter<ConversationContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_conversation, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactName: TextView = itemView.findViewById(R.id.contactName)
        private val contactNumber: TextView = itemView.findViewById(R.id.contactNumber)
        private val icUser: ImageView = itemView.findViewById(R.id.icUser)
        private val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        private val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)

        fun bind(contact: ContactItem) {
            contactName.text = contact.name
            contactNumber.text = contact.phoneNumber
            val firstChar = contact.name!!.trim().firstOrNull()
            val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()
            if (startsWithSpecialChar || contact.profileImageUrl != null && contact.profileImageUrl.isNotEmpty()) {
                icUser.visibility = View.VISIBLE
                initialsTextView.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(contact.profileImageUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(icUser)
            } else {
                icUser.visibility = View.GONE
                initialsTextView.visibility = View.VISIBLE
                initialsTextView.text = TimeUtils.getInitials(contact.name!!)
                profileContainer.backgroundTintList =
                    ColorStateList.valueOf(TimeUtils.getRandomColor(contact.name))
            }

            itemView.setOnClickListener { onContactSelected(contact) }
        }
    }

    fun submitList(newContacts: List<ContactItem>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }
}
