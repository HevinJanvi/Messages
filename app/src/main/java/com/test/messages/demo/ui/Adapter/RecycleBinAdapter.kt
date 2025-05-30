package com.test.messages.demo.ui.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.makeramen.roundedimageview.RoundedImageView
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.GROUP_NAME_KEY
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.ViewUtils.copyToClipboard
import com.test.messages.demo.Util.ViewUtils.extractOtp
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage

class RecycleBinAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecycleBinAdapter.ViewHolder>() {

    private var messages: MutableList<DeletedMessage> = mutableListOf()
    val selectedMessages = mutableSetOf<DeletedMessage>()
    private var isMultiSelectionMode = false
    var onBinItemClick: ((DeletedMessage) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val messageBody: TextView = itemView.findViewById(R.id.messageContent)
        val date: TextView = itemView.findViewById(R.id.date)
        val icUser: RoundedImageView = itemView.findViewById(R.id.icUser)
        val profileContainer: RelativeLayout = itemView.findViewById(R.id.profileContainer)
        val initialsTextView: TextView = itemView.findViewById(R.id.initialsTextView)
        val otpTextView: TextView = itemView.findViewById(R.id.otpTextView)
        val icSelect: ImageView = itemView.findViewById(R.id.icSelect)

        val container: ConstraintLayout = itemView.findViewById(R.id.itemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deleted_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]

        val context = holder.itemView.context
        val sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val savedGroupName = sharedPreferences.getString("${GROUP_NAME_KEY}${message.threadId}", null)
        val finalName = if (!savedGroupName.isNullOrEmpty()) {
            savedGroupName
        } else {

            val addressList = message.address.split(GROUP_SEPARATOR)
            val nameList = addressList.map { number ->
                val name = getContactName(context, number.trim())
                if (!name.isNullOrEmpty() && name != number) name else number
            }
            nameList.joinToString(GROUP_SEPARATOR)
        }
        message.address = finalName

        holder.senderName.text = finalName
        holder.messageBody.text = message.body
            holder.date.text = TimeUtils.formatTimestamp(holder.itemView.context,message.date)


        if (selectedMessages.find { it.threadId == message.threadId } != null) {
            holder.icSelect.visibility = View.VISIBLE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        } else {
            holder.icSelect.visibility = View.GONE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        }

        if (message.isGroupChat) {
            holder.icUser.visibility = View.VISIBLE
            holder.initialsTextView.visibility = View.GONE

            Glide.with(holder.itemView.context)
                .load(R.drawable.ic_group)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(50))))
                .into(holder.icUser)
        }else {
            val firstChar = message.address.trim().firstOrNull()
            val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()

            if (startsWithSpecialChar || message.profileImageUrl != null && message.profileImageUrl.isNotEmpty()) {
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

        val otp = message.body.extractOtp()
        if (!otp.isNullOrEmpty()) {
            holder.otpTextView.text = holder.itemView.context.getString(R.string.copy_otp)
            holder.otpTextView.visibility = View.VISIBLE

            holder.otpTextView.setOnClickListener {
                copyToClipboard(holder.itemView.context, otp)
                holder.otpTextView.animate()
                    .alpha(0.5f)
                    .setDuration(100)
                    .withEndAction {
                        holder.otpTextView.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        } else {
            holder.otpTextView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (selectedMessages.isNotEmpty()) {
                toggleSelection(message, holder)
            } else {
                onBinItemClick?.invoke(message)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectionMode) {
                isMultiSelectionMode = true
            }
            toggleSelection(message, holder)
            true
        }
    }

    private fun toggleSelection(message: DeletedMessage, holder: ViewHolder) {
        if (selectedMessages.find { it.threadId == message.threadId } != null) {
            selectedMessages.removeIf { it.threadId == message.threadId }
            holder.icSelect.visibility = View.INVISIBLE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.transparant))
        } else {
            selectedMessages.add(message)
            holder.icSelect.visibility = View.VISIBLE
            holder.container.setBackgroundColor(holder.itemView.context.getColor(R.color.select_bg))
        }
        onSelectionChanged(selectedMessages.size)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedMessages.clear()
        isMultiSelectionMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun selectAll() {
        if (messages.isNotEmpty()) {
            isMultiSelectionMode = true
            selectedMessages.clear()
            selectedMessages.addAll(messages)
            onSelectionChanged(selectedMessages.size)
            notifyDataSetChanged()
        }
    }

    fun isAllSelected(): Boolean {
        return selectedMessages.size == messages.size && messages.isNotEmpty()
    }

    fun unselectAll() {
        selectedMessages.clear()
        isMultiSelectionMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun submitList(newMessages: List<DeletedMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = messages.size

    fun getContactName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

}
