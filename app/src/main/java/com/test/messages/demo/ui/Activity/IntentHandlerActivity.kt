package com.test.messages.demo.ui.Activity

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.EXTRA_TXT
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.send.getThreadId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class IntentHandlerActivity : AppCompatActivity() {
    private val viewModel: MessageViewModel by viewModels()
    private var sharetxt: String? = null
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent?.action
        val scheme = intent?.data?.scheme
        val number = intent?.data?.schemeSpecificPart ?: intent?.getStringExtra("address") ?: ""

        if ((action == Intent.ACTION_SENDTO || action == Intent.ACTION_SEND || action == Intent.ACTION_VIEW) &&
            (scheme == "sms" || scheme == "smsto") && number.isNotEmpty()) {

            val uri = intent.data
            val number = uri?.schemeSpecificPart ?: ""
            if (number.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val threadId = getThreadId(setOf(number))
                    val contactName = viewModel.getContactName(this@IntentHandlerActivity, number)

                    sharetxt = intent.getStringExtra(Intent.EXTRA_TEXT)
                    withContext(Dispatchers.Main) {
                        val convoIntent = Intent(this@IntentHandlerActivity, ConversationActivity::class.java).apply {
                            putExtra(EXTRA_THREAD_ID, threadId)
                            putExtra(NUMBER, number)
                            putExtra(NAME, contactName ?: number)
                            putExtra(EXTRA_TXT, sharetxt)
                        }

                        val stackBuilder = TaskStackBuilder.create(this@IntentHandlerActivity)
                        stackBuilder.addNextIntentWithParentStack(Intent(this@IntentHandlerActivity, MainActivity::class.java))
                        stackBuilder.addNextIntent(convoIntent)
                        stackBuilder.startActivities()
                        finish()
                    }
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

}
