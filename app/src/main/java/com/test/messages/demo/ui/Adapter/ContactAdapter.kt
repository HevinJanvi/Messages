package com.test.messages.demo.ui.Adapter

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.test.messages.demo.AlphabticScroll.RecyclerViewFastScroller
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.ui.Utils.TimeUtils
import kotlin.text.isLetter

class ContactAdapter(
    private var contacts: List<ContactItem>,
    private val onContactSelected: (ContactItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), RecyclerViewFastScroller.BubbleTextGetter {

    private var sectionPositions = mutableListOf<Int>()
    private var sections = mutableListOf<String>()

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_CONTACT = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_contact, parent, false)
                ContactViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_HEADER -> {
                val sectionIndex = sectionPositions.indexOf(position)
                if (sectionIndex in sections.indices) {
                    val section = sections[sectionIndex]
                    (holder as SectionHeaderViewHolder).bind(section)
                }
            }

            VIEW_TYPE_CONTACT -> {
                val adjustedPosition = getContactIndex(position)
                if (adjustedPosition in contacts.indices) {
                    val contact = contacts[adjustedPosition]
                    (holder as ContactViewHolder).bind(contact)
                } else {
                    Log.e(
                        "ContactAdapter",
                        "Invalid contact position: $adjustedPosition for position: $position"
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return contacts.size + sections.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (sectionPositions.contains(position)) VIEW_TYPE_HEADER else VIEW_TYPE_CONTACT
    }


    fun submitList(newContacts: List<ContactItem>) {
        val specialCharContacts = newContacts.filter { contact ->
            val firstChar = contact.name?.firstOrNull()?.uppercase() ?: ""
            firstChar.isNotEmpty() && !firstChar[0].isLetter()
        }
        val alphabeticContacts = newContacts.filter { contact ->
            val firstChar = contact.name?.firstOrNull()?.uppercase() ?: ""
            firstChar.isNotEmpty() && firstChar[0].isLetter()
        }.sortedBy { it.name?.firstOrNull()?.uppercase() }
        contacts = specialCharContacts + alphabeticContacts
        sections.clear()
        sectionPositions.clear()
        var currentPos = 0
        var indexedContacts = mutableListOf<ContactItem>()
        val specialCharRegex = Regex("[^A-Za-z]")

        contacts.forEach { contact ->
            val firstChar = contact.name?.firstOrNull()?.uppercase() ?: "#"
            var section = when {
                firstChar.isNotEmpty() && firstChar[0].isLetter() -> {
                    firstChar
                }

                else -> {
                    "#"
                }
            }
            if (specialCharRegex.matches(firstChar)) {
                section = "#"
            }
            if (!sections.contains(section)) {
                sections.add(section)
                sectionPositions.add(currentPos)
                currentPos++
            }
            indexedContacts.add(contact)
            currentPos++
        }
        contacts = indexedContacts
        notifyDataSetChanged()
    }

    private fun getContactIndex(position: Int): Int {
        var offset = 0
        for (sectionIndex in sectionPositions) {
            if (position <= sectionIndex) break
            offset++
        }
        return position - offset
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

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)

        fun bind(section: String) {
            sectionTitle.text = section
        }
    }

    /*override fun getSections(): Array<String> {
        return sections.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return sectionPositions.getOrNull(sectionIndex) ?: 0
    }

    override fun getSectionForPosition(position: Int): Int {
        return sectionPositions.indexOfFirst { it >= position }
    }*/

    override fun getTextToShowInBubble(pos: Int): String {
        if (contacts.size > pos) {
            var section =
                if (contacts.get(pos).name!!.isNotEmpty() && contacts.get(pos).name!!.startsWithAtoZ()) contacts.get(
                    pos
                ).name!!.get(0).toUpperCase()
                    .toString() else "#"
            return section
        } else {
            return "#"
        }
    }

    fun String.startsWithAtoZ(): Boolean {
        return this.isNotEmpty() && this[0].isLetter()
    }
}