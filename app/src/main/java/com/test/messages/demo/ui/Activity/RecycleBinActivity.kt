package com.test.messages.demo.ui.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.databinding.ActivityRecyclebinBinding
import com.test.messages.demo.ui.Adapter.RecycleBinAdapter
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecycleBinActivity : BaseActivity() {
    private lateinit var binding: ActivityRecyclebinBinding
    private lateinit var recycleBinAdapter: RecycleBinAdapter
    private val selectedMessages = mutableSetOf<DeletedMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclebinBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.recycleBinRecyclerView.layoutManager = LinearLayoutManager(this)
        recycleBinAdapter = RecycleBinAdapter { selectedCount ->
            updateActionLayout(selectedCount)
        }
        binding.recycleBinRecyclerView.adapter = recycleBinAdapter

        AppDatabase.getDatabase(this).recycleBinDao().getAllDeletedMessages()
            .observe(this) { messages ->
                recycleBinAdapter.submitList(messages)
            }
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnDelete.setOnClickListener {
            deleteSelectedMessages()
        }

        binding.btnRestore.setOnClickListener {
            restoreSelectedMessages()
        }

        binding.btnSelectAll.setOnClickListener {
            if (recycleBinAdapter.selectedMessages.size == recycleBinAdapter.itemCount) {
                recycleBinAdapter.unselectAll()
            } else {
                recycleBinAdapter.selectAll()
            }
            updateActionLayout(recycleBinAdapter.selectedMessages.size)
        }
    }


    private fun deleteSelectedMessages() {
        if (recycleBinAdapter.selectedMessages.isNotEmpty()) {
            val messagesToDelete = recycleBinAdapter.selectedMessages.toList()
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(this@RecycleBinActivity).recycleBinDao().deleteMessages(messagesToDelete)
            }
            recycleBinAdapter.clearSelection()
        }


    }

    private fun restoreSelectedMessages() {
      /*  selectedMessages.forEach { message ->
            // Restore logic here (e.g., insert back into main messages table)
            AppDatabase.getDatabase(this).recycleBinDao().deleteMessage(message.id)
        }
        selectedMessages.clear()
        updateActionLayout()*/
    }

    private fun updateActionLayout(selectedCount: Int) {

        val isSelectionActive = selectedMessages.isNotEmpty()
        binding.selectMenuBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.lySelectedItemsBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.toolBarBin.visibility = if (selectedCount > 0) View.GONE else View.VISIBLE
    }

}