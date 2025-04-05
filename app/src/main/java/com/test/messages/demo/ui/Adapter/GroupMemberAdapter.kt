package com.test.messages.demo.ui.Adapter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.ui.Activity.ConversationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupMemberAdapter(
    private val members: List<String>,
    private val context: Context
) : RecyclerView.Adapter<GroupMemberAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val nameText: TextView = view.findViewById(R.id.nameText)
        val callButton: ImageView = view.findViewById(R.id.callButton)
        val messageButton: ImageView = view.findViewById(R.id.messageButton)
        val divider: View = view.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = members[position]

        CoroutineScope(Dispatchers.IO).launch {
            val contactName = getContactNameOrNumber(context, number)
            withContext(Dispatchers.Main) {
                holder.nameText.text = contactName
            }
        }

        holder.callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        }

        holder.messageButton.setOnClickListener {
            val intent = Intent(context, ConversationActivity::class.java).apply {
                putExtra(NUMBER, number)
            }
            context.startActivity(intent)
        }

        if (position == members.size - 1) {
            holder.divider.visibility = View.GONE
        } else {
            holder.divider.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = members.size

    private fun getContactNameOrNumber(context: Context, phoneNumber: String): String {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = resolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return phoneNumber
    }
}
