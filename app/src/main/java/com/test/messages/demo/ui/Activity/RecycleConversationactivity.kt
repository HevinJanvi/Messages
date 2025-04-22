//package com.test.messages.demo.ui.Activity
//
//import android.content.ContentValues
//import android.graphics.Color
//import android.net.Uri
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.provider.Telephony
//import android.util.Log
//import android.view.ActionMode
//import android.view.LayoutInflater
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.test.messages.demo.R
//import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
//import com.test.messages.demo.Util.CommanConstants.ISDELETED
//import com.test.messages.demo.Util.CommanConstants.NAME
//import com.test.messages.demo.data.Model.ConversationItem
//import com.test.messages.demo.databinding.ActivityRecycleConversationBinding
//import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
//import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class RecycleConversationactivity : BaseActivity() {
//
//
//    private lateinit var binding: ActivityRecycleConversationBinding
//    private lateinit var adapter: RecycleConversationAdapter
//    private var isDeletedThread: Boolean = false
//    private var threadId: Long = -1L
//    private lateinit var name: String
//    private lateinit var db: RecycleBinDao
//    private val selectedMessages = mutableSetOf<ConversationItem>()
//    private var isMultiSelectEnabled = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityRecycleConversationBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        db = AppDatabase.getDatabase(this).recycleBinDao()
//
//        adapter = RecycleConversationAdapter()
//        binding.recycleConversationView.layoutManager = LinearLayoutManager(this)
//        binding.recycleConversationView.adapter = adapter
//        adapter.onSelectionChanged = { count ->
//            if (count == 0) {
//                disableMultiSelection()
//            } else {
//                binding.countText.text = "$count" + " " + getString(R.string.selected)
//            }
//        }
//
//        isDeletedThread = intent.getBooleanExtra(ISDELETED, false)
//        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
//        name = intent.getStringExtra(NAME) ?: ""
//        Log.d("TAG", "onCreate:namee "+name)
//        binding.address.text = name
//        loadDeletedMessages(threadId)
//        binding.icClose.setOnClickListener {
//            disableMultiSelection()
//        }
//
//        binding.btnDelete.setOnClickListener {
//            deleteSelectedMessages()
//            disableMultiSelection()
//        }
//
//        binding.btnRestore.setOnClickListener {
//            restoreSelectedMessages()
//            disableMultiSelection()
//        }
//        binding.icBack.setOnClickListener { onBackPressed() }
//    }
//
//    private fun loadDeletedMessages(threadId: Long) {
//        val db = AppDatabase.getDatabase(this).recycleBinDao()
//        Thread {
//            val deletedMessages = db.getMessagesByThreadId(threadId)
//            val messages = deletedMessages.map {
//                ConversationItem(
//                    id = it.messageId,
//                    threadId = it.threadId,
//                    address = it.address,
//                    body = it.body,
//                    date = it.date,
//                    type = it.type,
//                    read = it.read,
//                    subscriptionId = it.subscriptionId,
//                    profileImageUrl = "",
//                    isHeader = false
//                )
//            }.sortedBy { it.date }
//
//            runOnUiThread {
//                adapter.submitList(messages)
//            }
//        }.start()
//    }
//
//    private fun disableMultiSelection() {
//        if (!isMultiSelectEnabled) return
//
//        isMultiSelectEnabled = false
//        selectedMessages.clear()
//        binding.actionbar.visibility = View.VISIBLE
//        binding.actionSelectItem.visibility = View.GONE
//        adapter.clearSelection()
//    }
//
//
//    private fun deleteSelectedMessages() {
//        val selected = adapter.getSelectedItems()
//        Thread {
//            selected.forEach { db.deleteMessageById(it.id) }
//            loadDeletedMessages(threadId)
//        }.start()
//    }
//
//    private fun restoreSelectedMessages() {
//        val selected = adapter.getSelectedItems()
//        Thread {
//            try {
//                selected.forEach {
//
//                    val values = ContentValues().apply {
//
//                        put(Telephony.Sms.THREAD_ID, threadId)
//                        put(Telephony.Sms.DATE, it.date)
//                        put(Telephony.Sms.BODY, it.body)
//                        put(Telephony.Sms.ADDRESS, it.address)
//                        put(Telephony.Sms.TYPE, it.type)
//                        put(Telephony.Sms.READ, if (it.read) 1 else 0)
//                        put(Telephony.Sms.SUBSCRIPTION_ID, it.subscriptionId)
//                    }
//
//                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
//                    db.deleteMessageById(it.id)
//                }
//
//                runOnUiThread {
//                    loadDeletedMessages(threadId)
//                }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }.start()
//    }
//
//
//    private fun notifySelectionModeChanged(enabled: Boolean) {
//        binding.actionbar.visibility = if (enabled) View.GONE else View.VISIBLE
//        binding.actionSelectItem.visibility = if (enabled) View.VISIBLE else View.GONE
//    }
//
//    private fun notifySelectionChanged(count: Int) {
//        binding.countText.text = "$count selected"
//    }
//
//    override fun onBackPressed() {
//        if (isMultiSelectEnabled) {
//            adapter.clearSelection()
//        } else {
//
//            super.onBackPressed()
//        }
//    }
//
//
//    companion object {
//        const val VIEW_TYPE_SENT = 1
//        const val VIEW_TYPE_RECEIVED = 2
//    }
//
//
//    inner class RecycleConversationAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//        private val messages = mutableListOf<ConversationItem>()
//        private val selectedItems = mutableSetOf<ConversationItem>()
//        var onSelectionChanged: ((Int) -> Unit)? = null
//
//
//        fun submitList(newList: List<ConversationItem>) {
//            messages.clear()
//            messages.addAll(newList)
//            selectedItems.clear()
//            notifyDataSetChanged()
//        }
//
//        fun getSelectedItems(): List<ConversationItem> = selectedItems.toList()
//
//        fun selectAll() {
//            selectedItems.clear()
//            selectedItems.addAll(messages)
//            notifyDataSetChanged()
//            onSelectionChanged?.invoke(selectedItems.size)
//        }
//
//        fun clearSelection() {
//            selectedItems.clear()
//            notifyDataSetChanged()
//            onSelectionChanged?.invoke(0)
//        }
//
//        fun toggleSelection(item: ConversationItem) {
//            if (selectedItems.contains(item)) {
//                selectedItems.remove(item)
//            } else {
//                selectedItems.add(item)
//            }
//            notifyDataSetChanged()
//            onSelectionChanged?.invoke(selectedItems.size)
//        }
//
//
//        fun getSelectedMessages(): List<ConversationItem> {
//            return selectedItems.toList()
//        }
//
//
//        override fun getItemViewType(position: Int): Int {
//            return if (isSentMessage(messages[position].type)) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
//        }
//
//        private fun isSentMessage(type: Int): Boolean {
//            return type == Telephony.Sms.MESSAGE_TYPE_SENT ||    // 2
//                    type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||  // 4
//                    type == Telephony.Sms.MESSAGE_TYPE_FAILED ||  // 5
//                    type == Telephony.Sms.MESSAGE_TYPE_QUEUED     // 6
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//            val layout =
//                if (viewType == VIEW_TYPE_SENT) R.layout.item_message_outgoing else R.layout.item_message_incoming
//            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
//            return MessageViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//            val message = messages[position]
//            (holder as MessageViewHolder).bind(message)
//        }
//
//        override fun getItemCount() = messages.size
//
//
//        inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//            private val messageText: TextView = view.findViewById(R.id.messageBody)
//            private val messageTime: TextView = view.findViewById(R.id.messageDate)
//            private val messageStatus: TextView = view.findViewById(R.id.messageStatus)
//
//            fun bind(item: ConversationItem) {
//                messageStatus.visibility = View.GONE
//                messageText.text = item.body
//                messageTime.text =
//                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.date))
//
//                if (selectedItems.contains(item)) {
//                    messageText.setBackgroundResource(R.drawable.bg_message_selected)
//                    messageText.setTextColor(Color.WHITE)
//                } else {
//                    messageText.setTextColor(itemView.context.resources.getColor(R.color.textcolor))
//                    if (item.isIncoming()) {
//                        messageText.setBackgroundResource(R.drawable.bg_message_incoming)
//                    } else {
//                        messageText.setBackgroundResource(R.drawable.bg_message_outgoing)
//                    }
//                }
//
//                itemView.setOnLongClickListener {
//                    if (!isMultiSelectEnabled) {
//                        isMultiSelectEnabled = true
//                        selectedItems.clear()
//                        toggleSelection(item)
//                        notifySelectionModeChanged(true)
//                    }
//                    true
//                }
//
//                itemView.setOnClickListener {
//                    if (isMultiSelectEnabled) {
//                        toggleSelection(item)
//                        if (selectedItems.isEmpty()) {
//                            isMultiSelectEnabled = false
//                            notifySelectionModeChanged(false)
//                        }
//                        notifySelectionChanged(selectedItems.size)
//                    }
//                }
//            }
//        }
//
//
//    }
//
//
//}