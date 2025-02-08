package com.test.messages.demo.ui.Fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.databinding.FragmentConversationBinding
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.Adapter.MessageAdapter
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.annotations.Nullable

@AndroidEntryPoint
class ConversationFragment : Fragment() {

    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var binding: FragmentConversationBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConversationBinding.inflate(inflater, container, false);

        setupRecyclerView()
        checkPermissionsAndLoadMessages()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_SMS),
                100
            )
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.READ_CONTACTS), 101)
        }

        /* adapter = MessageAdapter1()
         binding.conversationList.adapter = adapter

         val repository = MessageRepository(requireContext().contentResolver)
         val viewModelFactory = MessageViewModelFactory(repository)
         viewModel = ViewModelProvider(this, viewModelFactory).get(MessageViewModel::class.java)
         viewModel.fetchMessages(requireContext())
         viewModel.messages.observe(viewLifecycleOwner) { latestMessages ->

             Log.d("MessageActivity", "Displaying ${latestMessages.size} unique senders with their last message.")
             if (latestMessages.isNotEmpty()) {
                 adapter.submitList(latestMessages)
             } else {
                 Log.d("MessageActivity", "No messages found!")
             }

         }*/

        viewModel.messages.observe(viewLifecycleOwner) { messageList ->
            Log.d("ConversationFragment", "Messages Loaded: ${messageList.size}")
            adapter.submitList(messageList) // Efficiently update list
        }

        return binding.getRoot();

    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        binding.conversationList.layoutManager = LinearLayoutManager(requireActivity())
        binding.conversationList.adapter = adapter

        adapter.onItemClickListener = { threadId ->
//            viewModel.loadConversation(threadId)
            Log.e("TAG", "setupRecyclerView: "+threadId )
            val intent = Intent(requireContext(), ConversationActivity::class.java)
            intent.putExtra(ConversationActivity.EXTRA_THREAD_ID, threadId)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndLoadMessages() {
        // Check if both SMS and Contacts permissions are granted
        val smsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_SMS
        )
        val contactsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_CONTACTS
        )

        // If both permissions are granted, load messages
        if (smsPermission == PackageManager.PERMISSION_GRANTED && contactsPermission == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadMessages()
        } else {
            // If permissions are not granted, request them
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_CONTACTS),
                101
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            101 -> {
                // Check if both permissions were granted
                val smsPermissionGranted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
                val contactsPermissionGranted = grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED

                if (smsPermissionGranted && contactsPermissionGranted) {
                    // Both permissions granted, load messages
                    viewModel.loadMessages()
                } else {
                    // Permissions denied, show a toast message
                    Toast.makeText(requireActivity(), "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


}