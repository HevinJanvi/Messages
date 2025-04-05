package com.test.messages.demo.ui.Adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.provider.Telephony
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.ui.Dialogs.ExternalLinkDialog
import com.test.messages.demo.Util.ViewUtils.extractOtp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<ConversationItem, ConversationAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ConversationItem>() {
        override fun areItemsTheSame(oldItem: ConversationItem, newItem: ConversationItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ConversationItem, newItem: ConversationItem) =
            oldItem == newItem
    }
) {
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_INCOMING = 1
        private const val VIEW_TYPE_OUTGOING = 2
    }

    val selectedItems = mutableSetOf<ConversationItem>()
    var isMultiSelectionEnabled = false
    private val expandedMessages = mutableSetOf<Long>()
    private var lastMessagePosition: Int? = null
    private var searchQuery: String? = null

    interface OnMessageRetryListener {
        fun onRetry(message: ConversationItem)
    }

    var retryListener: OnMessageRetryListener? = null

    fun setOnRetryListener(listener: OnMessageRetryListener) {
        this.retryListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            getItem(position).isHeader -> VIEW_TYPE_HEADER
            getItem(position).isIncoming() -> VIEW_TYPE_INCOMING
            else -> VIEW_TYPE_OUTGOING
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageBody: TextView = view.findViewById(R.id.messageBody)
        val messageDate: TextView = view.findViewById(R.id.messageDate)
        val otptext: TextView = view.findViewById(R.id.otptext)
        val messageStatus: TextView = view.findViewById(R.id.messageStatus)
        val starIcon: ImageView = view.findViewById(R.id.starIcon)
        private val headerText: TextView? = view.findViewById(R.id.headerText)
        private var fontSize: Float = getFontSizeFromPreferences()

        private fun getFontSizeFromPreferences(): Float {
            return when (ViewUtils.getFontSize(itemView.context)) {
                CommanConstants.ACTION_SMALL -> 13f
                CommanConstants.ACTION_NORMAL -> 15f
                CommanConstants.ACTION_LARGE -> 17f
                CommanConstants.ACTION_EXTRALARGE -> 20f
                else -> 15f
            }
        }

        fun bind(message: ConversationItem, isLastMessage: Boolean) {



            if (message.isHeader) {
                headerText?.visibility = View.VISIBLE
                headerText?.text = message.body
            } else {

                messageBody.textSize = fontSize
                otptext.textSize = fontSize

                val blinkAnimation = AlphaAnimation(0.3f, 1.0f).apply {
                    duration = 500
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                messageStatus.clearAnimation()


                headerText?.visibility = View.GONE
                messageBody.visibility = View.VISIBLE

                if (!searchQuery.isNullOrEmpty()) {
                    messageBody.text = highlightText(message.body, searchQuery!!,itemView.context)
                } else {
                    messageBody.text = message.body
                }
                messageDate.text =
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.date))

                val shouldShowTime = isLastMessage || expandedMessages.contains(message.id)
                messageDate.visibility = if (shouldShowTime) View.VISIBLE else View.GONE

                if (!message.isIncoming() && (isLastMessage || message.type == Telephony.Sms.MESSAGE_TYPE_FAILED)) {
                    when (message.type) {
                        Telephony.Sms.MESSAGE_TYPE_SENT -> {
                            messageStatus.text = itemView.context.getString(R.string.delivered)
                            messageStatus.visibility = View.VISIBLE
                            messageStatus.setTextColor(Color.GRAY)
                            messageStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        }

                        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> {
                            messageStatus.text = itemView.context.getString(R.string.sending)
                            messageStatus.visibility = View.VISIBLE
                            messageStatus.setTextColor(Color.GRAY)
                            messageStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        }

                        Telephony.Sms.MESSAGE_TYPE_FAILED -> {
                            messageStatus.text = itemView.context.getString(R.string.failed_to_send_tap)
                            messageStatus.visibility = View.VISIBLE
                            messageStatus.setTextColor(Color.RED)
                            messageStatus.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_fail,
                                0,
                                0,
                                0
                            )



                            messageStatus.setOnClickListener {
                                // 🔄 Show sending UI with animation
                                messageStatus.text = itemView.context.getString(R.string.sending)
                                messageStatus.setTextColor(Color.GRAY)
                                messageStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                                messageStatus.startAnimation(blinkAnimation)

                                retryListener?.onRetry(message)
                            }
                        }

                        else -> {
                            messageStatus.visibility = View.GONE
                        }
                    }
                } else {
                    messageStatus.visibility = View.GONE
                }

                val isSelected = selectedItems.contains(message)
                val otpCode = message.body?.extractOtp()
                if (otpCode != null) {
                    otptext.visibility = View.VISIBLE
                    otptext.text = itemView.context.getString(R.string.copy, otpCode)
                    otptext.setOnClickListener {
                        copyToClipboard(otpCode)
                    }
                } else {
                    otptext.visibility = View.GONE
                }

                if (isSelected) {
                    messageBody.setBackgroundResource(R.drawable.bg_message_selected)
                    messageBody.setTextColor(Color.WHITE)
                } else {
                    messageBody.setTextColor(itemView.context.resources.getColor(R.color.textcolor))
                    if (message.isIncoming()) {
                        messageBody.setBackgroundResource(R.drawable.bg_message_incoming)
                    } else {
                        messageBody.setBackgroundResource(R.drawable.bg_message_outgoing)
                    }
                }

                val isStarred = starredMessageIds.contains(message.id)
                if (isStarred) {
                    starIcon.visibility = View.VISIBLE
                } else {
                    starIcon.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    if (isMultiSelectionEnabled) {
                        toggleSelection(message)
                    } else {
                        toggleTimeVisibility(message)
                    }
                }

                itemView.setOnLongClickListener {
                    enableMultiSelection(message)
                    true
                }

                val link = message.extractLink()
                if (link != null) {
                    val spannable = SpannableString(message.body)
                    val matcher = Patterns.WEB_URL.matcher(message.body)

                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                ExternalLinkDialog(itemView.context, link).show()
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.color =
                                    ContextCompat.getColor(itemView.context, R.color.textcolor)
                                ds.isUnderlineText = true
                                val typeface = ResourcesCompat.getFont(
                                    itemView.context,
                                    R.font.product_sans_medium
                                )
                                ds.typeface = typeface
                            }
                        }
                        spannable.setSpan(
                            clickableSpan,
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        spannable.setSpan(
                            UnderlineSpan(),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    messageBody.text = spannable
                    messageBody.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }

        private fun copyToClipboard(otp: String) {
            val clipboard =
                itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OTP Code", otp)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                itemView.context,
                itemView.context.getString(R.string.otp_copied), Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_HEADER -> R.layout.item_date_header
            VIEW_TYPE_INCOMING -> R.layout.item_message_incoming
            else -> R.layout.item_message_outgoing
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)

        val newLastMessagePosition = currentList.lastIndex
        val isLastMessage = position == newLastMessagePosition
        holder.bind(message, isLastMessage)
        holder.itemView.post {
            if (lastMessagePosition != newLastMessagePosition) {
                lastMessagePosition?.let { notifyItemChanged(it) }
                notifyItemChanged(newLastMessagePosition)
                lastMessagePosition = newLastMessagePosition

            }
        }
    }


    private fun enableMultiSelection(message: ConversationItem) {
        isMultiSelectionEnabled = true
        toggleSelection(message)
    }

    fun getPositionOfMessage(messageItem: ConversationItem): Int {
        return currentList.indexOf(messageItem)
    }

    fun removeMessageWithAnimation(position: Int) {
        val updatedList = currentList.toMutableList()
        updatedList.removeAt(position)
        submitList(updatedList) {
            notifyItemRemoved(position)
        }
    }

    private fun toggleTimeVisibility(message: ConversationItem) {
        val position = currentList.indexOf(message)

        if (expandedMessages.contains(message.id)) {
            expandedMessages.remove(message.id)
        } else {
            expandedMessages.add(message.id)
        }
        notifyItemChanged(position)
    }

         var starredMessageIds: Set<Long> = emptySet()

    fun setStarredMessages(starredIds: Set<Long>) {
        starredMessageIds = starredIds
        notifyDataSetChanged()
    }

    private fun toggleSelection(message: ConversationItem) {
        if (selectedItems.contains(message)) {
            selectedItems.remove(message)
        } else {
            selectedItems.add(message)
        }

        if (selectedItems.isEmpty()) {
            isMultiSelectionEnabled = false
        }

        onSelectionChanged(selectedItems.size)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        isMultiSelectionEnabled = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<ConversationItem> {
        return selectedItems.toList()
    }

    fun setSearchQuery(query: String?) {
        searchQuery = query
        notifyDataSetChanged()
    }

    private fun highlightText(fullText: String, searchText: String, context: Context): SpannableString {
        val spannable = SpannableString(fullText)
        if (searchText.isNotEmpty()) {
            val start = fullText.lowercase(Locale.getDefault()).indexOf(searchText.lowercase(Locale.getDefault()))
            if (start >= 0) {
                val end = start + searchText.length
                val highlightColor = ContextCompat.getColor(context, R.color.yellow)
                val textColor = ContextCompat.getColor(context, R.color.textcolor)
                spannable.setSpan(BackgroundColorSpan(highlightColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }

}
