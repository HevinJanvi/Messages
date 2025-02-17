package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem

class ConversationContactAdapter(
private var contacts: List<ContactItem>,
private val onContactSelected: (ContactItem) -> Unit
) : RecyclerView.Adapter<ConversationContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
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

        fun bind(contact: ContactItem) {
            // Skip the header item (ContactItem with cid = -1)
//            if (contact.cid != -1) {
                contactName.text = contact.name
                itemView.setOnClickListener { onContactSelected(contact) }
//            }
        }
    }

    fun submitList(newContacts: List<ContactItem>) {
        // Filter out header items (those with cid = -1)
        this.contacts = newContacts
        notifyDataSetChanged()
    }
}
