package com.test.messages.demo.ui.Activity

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class IntentHandlerActivity : AppCompatActivity() {
    private val viewModel: MessageViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        if (Intent.ACTION_SENDTO == intent.action && intent.data != null) {
            val uri = intent.data
            val number = uri?.schemeSpecificPart ?: ""

            if (number.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val threadId = ViewUtils.getThreadId(this@IntentHandlerActivity,number)
                    val contactName = viewModel.getContactName(this@IntentHandlerActivity, number)

                    withContext(Dispatchers.Main) {
                        val convoIntent = Intent(this@IntentHandlerActivity, ConversationActivity::class.java).apply {
                            putExtra(EXTRA_THREAD_ID, threadId)
                            putExtra(NUMBER, number)
                            putExtra(NAME, contactName ?: number)
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
