package com.test.messages.demo.ui.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem

class ContactAdapter(
    private var contacts: List<ContactItem>,
    private val onContactSelected: (ContactItem) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    fun submitList(newContacts: List<ContactItem>) {
        this.contacts = newContacts
        notifyDataSetChanged()  // Notify the adapter that the data has changed
    }
    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactName: TextView = itemView.findViewById(R.id.contactName)

        fun bind(contact: ContactItem) {
            contactName.text = contact.name

//            if (contact.cid == -1) {
//                holder.iconImageView.setImageResource(R.drawable.ic_user) // Replace with your user icon
//            } else {
//                hol.der.iconImageView.setImageResource(R.drawable.ic_contact) // Regular contact icon
//            }

            itemView.setOnClickListener {
                onContactSelected(contact)
            }
        }
    }
}