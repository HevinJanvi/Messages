package com.test.messages.demo.ui.Adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
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
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
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
import com.test.messages.demo.Util.CustomLinkMovementMethod
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.extractOtp
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.repository.MessageRepository
import com.test.messages.demo.ui.Dialogs.ExternalLinkDialog
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


class ConversationBinAdapter(
    private val context: Context,
    private val isContactSaved: Boolean,
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<ConversationItem, ConversationBinAdapter.ViewHolder>(
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
    var starredMessageIds: Set<Long> = emptySet()

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
        val headerText: TextView? = view.findViewById(R.id.headerText)
        var fontSize: Float = getFontSizeFromPreferences()

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
                messageBody.setOnClickListener {
                    if (message.isHeader) return@setOnClickListener
                    // selection logic
                }


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
                    messageBody.text = highlightText(message.body, searchQuery!!, itemView.context)
                } else {
                    messageBody.text = message.body
                }
//                messageDate.text =
//                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.date))
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val timeFormat = if (is24Hour) "HH:mm" else "hh:mm a"
                val sdf = SimpleDateFormat(timeFormat, Locale.getDefault())
                messageDate.text = sdf.format(Date(message.date))
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
                            messageStatus.text =
                                itemView.context.getString(R.string.failed_to_send_tap)
                            messageStatus.visibility = View.VISIBLE
                            messageStatus.setTextColor(Color.RED)
                            messageStatus.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_fail,
                                0,
                                0,
                                0
                            )


                            messageStatus.setOnClickListener {
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
                        toggleSelection(message, 1)
                    } else {
                        toggleTimeVisibility(message)
                    }
                }
                messageBody.setOnClickListener {
                    if (isMultiSelectionEnabled) {
                        toggleSelection(message, 1)
                    } else {
                        toggleTimeVisibility(message)
                    }
                }

                formatMessageWithLinks(
                    messageBody,
                    message,
                    message = message.body
                )

            }
        }


        private fun formatMessageWithLinks(
            textView: TextView,
            conversationItem: ConversationItem,
            message: String
        ) {
            val isSelected = selectedItems.contains(conversationItem)
            val spannableString = SpannableString(message)

            val emailPattern = Pattern.compile(
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
            )

            val mapPattern = Pattern.compile(
                "(https?:\\/\\/)?(www\\.)?(google\\.com\\/maps\\/place\\/[^\\s]+|goo\\.gl\\/maps\\/[^\\s]+)"
            )

            val urlPattern = Pattern.compile(
                "(https?:\\/\\/)?(www\\.)?[a-zA-Z0-9\\-._~:/?#@!$&'()*+,;=]+\\.[a-zA-Z]{2,}(/[a-zA-Z0-9\\-._~:/?#@!$&'()*+,;=]*)?(\\?[a-zA-Z0-9\\-._~:/?#@!$&'()*+,;=%]*)?(\\&[a-zA-Z0-9\\-._~:/?#@!$&'()*+,;=%]*)*(#[a-zA-Z0-9\\-._~:/?#@!$&'()*+,;=]*)?"
            )

            val phonePattern = Pattern.compile(
                "\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{2,4}?\\)?[-.\\s]?[0-9]{2,4}[-.\\s]?[0-9]{3,10}"
            )

            val otpPattern = Pattern.compile(
                "(?i)((otp|code|verification|password).*?\\b(\\d{4,8})\\b|\\b(\\d{4,8})\\b.*?(otp|code|verification|password))"
            )


            fun applySpan(
                pattern: Pattern,
                msgType: String,
                msg: String,
                onClick: (String) -> Unit,
            ) {

                val matcher = pattern.matcher(message)
                while (matcher.find()) {
                    val matchedText = matcher.group()
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onClick(matchedText)
                          /*  if (!isContactSaved && conversationItem.isIncoming()) {

                                ExternalLinkDialog(widget.context, matchedText).show()
                            } else {
                                onClick(matchedText)
                            }*/
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)

                            val colorRes = if (isSelected) {
                                R.color.white
                            } else {
                                R.color.textcolor
                            }
                            ds.color = ContextCompat.getColor(itemView.context, colorRes)
                            ds.isUnderlineText = true
                            val typeface = ResourcesCompat.getFont(
                                itemView.context,
                                R.font.product_sans_medium
                            )
                            ds.typeface = typeface
                        }

                    }, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            applySpan(
                emailPattern,
                "Email",
                context.getString(R.string.email_type),
            ) { email ->
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                context.startActivity(intent)
            }

            applySpan(
                mapPattern,
                "Map",
                context.getString(R.string.map_type),
            ) { address ->
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                context.startActivity(intent)
            }

            applySpan(
                urlPattern,
                "Website",
                context.getString(R.string.web_type),
            ) { url ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(if (url.startsWith("http")) url else "http://$url")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            applySpan(
                phonePattern,
                "Phone Number", context.getString(R.string.phone_type),
            ) { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            val otpMatcher = otpPattern.matcher(message)
            while (otpMatcher.find()) {
                val otpCode = otpMatcher.group(3) ?: otpMatcher.group(4)
                val startIndex =
                    if (otpMatcher.group(3) != null) otpMatcher.start(3) else otpMatcher.start(4)
                val endIndex =
                    if (otpMatcher.group(3) != null) otpMatcher.end(3) else otpMatcher.end(4)

                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        otpCode?.let {
                            copyToClipboard(it)
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        val colorRes = if (isSelected) {
                            R.color.white
                        } else {
                            R.color.textcolor
                        }
                        ds.color = ContextCompat.getColor(itemView.context, colorRes)

                        ds.isUnderlineText = false
                        val typeface = ResourcesCompat.getFont(
                            itemView.context,
                            R.font.product_sans_medium
                        )
                        ds.typeface = typeface
                    }


                }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            textView.text = spannableString

            textView.movementMethod = CustomLinkMovementMethod {
                enableMultiSelection(conversationItem)
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
        toggleSelection(message, 2)
    }

    fun getPositionOfMessage(messageItem: ConversationItem): Int {
        return currentList.indexOf(messageItem)
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


    fun setStarredMessages(starredIds: Set<Long>) {
        starredMessageIds = starredIds
        notifyDataSetChanged()
    }

    private fun toggleSelection(message: ConversationItem, i: Int) {
        Log.d("TAG", "toggleSelection: $i")
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
        return selectedItems.filterNot { it.isHeader }
    }


    private fun highlightText(
        fullText: String,
        searchText: String,
        context: Context
    ): SpannableString {
        val spannable = SpannableString(fullText)
        if (searchText.isNotEmpty()) {
            val start = fullText.lowercase(Locale.getDefault())
                .indexOf(searchText.lowercase(Locale.getDefault()))
            if (start >= 0) {
                val end = start + searchText.length
                val highlightColor = ContextCompat.getColor(context, R.color.yellow)
                val textColor = ContextCompat.getColor(context, R.color.serach_highlight_color)
                spannable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(textColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannable
    }


}
