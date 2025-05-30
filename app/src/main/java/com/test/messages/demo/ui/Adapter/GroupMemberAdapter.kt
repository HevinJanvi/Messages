package com.test.messages.demo.ui.Adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.test.messages.demo.R
import com.test.messages.demo.Util.ActivityFinishEvent
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.send.getThreadId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class GroupMemberAdapter(
    private val members: List<String>,
    private val context: Activity
) : RecyclerView.Adapter<GroupMemberAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icUser: ImageView = view.findViewById(R.id.icUser)
        val initialsTextView: TextView = view.findViewById(R.id.initialsTextView)
        val profileContainer: RelativeLayout = view.findViewById(R.id.profileContainer)
        val nameText: TextView = view.findViewById(R.id.nameText)
        val callButton: ImageView = view.findViewById(R.id.callButton)
        val messageButton: ImageView = view.findViewById(R.id.messageButton)
        val divider: View = view.findViewById(R.id.divider)
    }

    private val contactInfoCache = mutableMapOf<String, ContactInfo?>()

    data class ContactInfo(val name: String, val photoUri: Uri?)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = members[position]

        val cachedInfo = contactInfoCache[number]
        if (cachedInfo != null) {
            bindContactInfo(holder, cachedInfo, number)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val contactInfo = getContactInfo(context, number)
                contactInfoCache[number] = contactInfo

                withContext(Dispatchers.Main) {
                    bindContactInfo(holder, contactInfo, number)
                }
            }
        }


        holder.callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        }

        holder.messageButton.setOnClickListener {
            val intent = Intent(context, ConversationActivity::class.java).apply {
                val threadId = context.getThreadId(setOf(number))
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(NUMBER, number)
                putExtra(NAME, holder.nameText.text)
                putExtra(Constants.ISGROUP, true)
            }
            context.startActivity(intent)
            context.finish()
            EventBus.getDefault().post(ActivityFinishEvent(true))
        }


        if (position == members.size - 1) {
            holder.divider.visibility = View.GONE
        } else {
            holder.divider.visibility = View.VISIBLE
        }
    }

    private fun bindContactInfo(holder: ViewHolder, contactInfo: ContactInfo?, phoneNumber: String) {
        val name = contactInfo?.name ?: phoneNumber
        val photoUri = contactInfo?.photoUri

        holder.nameText.text = name

        val firstChar = name.trim().firstOrNull()
        val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()

        if (startsWithSpecialChar || (photoUri != null)) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE

            Glide.with(holder.itemView.context)
                .load(photoUri)
                .placeholder(R.drawable.ic_user)
                .circleCrop()
                .into(holder.icUser)
        } else {
            holder.icUser.visibility = View.GONE
            holder.initialsTextView.visibility = View.VISIBLE
            holder.initialsTextView.text = TimeUtils.getInitials(name)
            holder.profileContainer.backgroundTintList =
                ColorStateList.valueOf(TimeUtils.getRandomColor(name))
        }
    }

    private fun getContactInfo(context: Context, phoneNumber: String): ContactInfo? {
        val resolver = context.contentResolver

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )
        val cursor = resolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUriString =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                val photoUri = photoUriString?.let { Uri.parse(it) }
                return ContactInfo(name, photoUri)
            }
        }

        return null
    }

    override fun getItemCount(): Int = members.size

    fun clearCacheAndReload() {
        contactInfoCache.clear()
        notifyDataSetChanged()
    }
}
