package com.test.messages.demo.ui.Activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.ACTION_START_BACKUP
import com.test.messages.demo.Util.Constants.ACTION_START_RESTORE
import com.test.messages.demo.Util.Constants.EXTRA_JSON_DATA
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.data.reciever.RestoreService
import com.test.messages.demo.data.viewmodel.BackupViewModel
import com.test.messages.demo.databinding.ActivityBakupRestoreBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BakupRestoreActivity : BaseActivity() {
    private lateinit var binding: ActivityBakupRestoreBinding
    private val backupViewModel: BackupViewModel by viewModels()
    private val EXPORT_JSON_REQUEST_CODE = 1001
    private val IMPORT_JSON_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakupRestoreBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        loadLastBackupTime()

        binding.lybackup.visibility = View.VISIBLE
        binding.lyRestore.visibility = View.GONE

        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnBackup.setOnClickListener {
            animateButton(it)
            Backup()
        }
        binding.btnRestore.setOnClickListener {
            animateButton(it)
            Restore()
        }


    }

    private fun animateButton(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun Restore() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, IMPORT_JSON_REQUEST_CODE)
    }

    private fun Backup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, Constants.BACKUP_FILE)
        }
        startActivityForResult(intent, EXPORT_JSON_REQUEST_CODE)

    }

    private fun saveLastBackupTime() {
        val sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentTime =
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        editor.putString(Constants.KEY_LAST_BACKUP, currentTime)
        editor.apply()
    }

    private fun loadLastBackupTime() {
        val sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val lastBackupTime = sharedPreferences.getString(Constants.KEY_LAST_BACKUP, null)
        binding.txtviw2.apply {
            if (lastBackupTime.isNullOrEmpty()) {
                text = getString(R.string.messages_can_be_backed)
                setTextColor(ContextCompat.getColor(context, R.color.gray_txtcolor))
            } else {
                text = getString(R.string.last_backup,lastBackupTime)
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        backupViewModel.clearRestoreProgress()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Constants.SEND_BROADCAST_TO_SEND_RESTORE_COUNT_PROGRESS_BAR)
            addAction(Constants.SEND_RESTORE_COMPLETED_BROADCAST)
            addAction(Constants.SEND_BACKUP_COMPLETED_BROADCAST)
            addAction(Constants.SEND_BROADCAST_RESTORE_FAILED)
            addAction(Constants.SEND_BROADCAST_CANCEL)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(progressBarReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressBarReceiver)
    }

    private val progressBarReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.SEND_BROADCAST_TO_SEND_RESTORE_COUNT_PROGRESS_BAR -> {
                    val progress = intent.getIntExtra(Constants.SEND_PROGRESS_VALUE, 0)
                    runOnUiThread {
                        showProgress()
                        binding.progressBar.progress = progress
                    }
                }

                Constants.SEND_RESTORE_COMPLETED_BROADCAST -> {
                    Toast.makeText(
                        this@BakupRestoreActivity,
                        getString(R.string.restore_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    runOnUiThread {
                        hideProgress()
                        EventBus.getDefault().post(MessagesRestoredEvent(true))

                    }
                }

                Constants.SEND_BACKUP_COMPLETED_BROADCAST -> {
                    saveLastBackupTime()
                    loadLastBackupTime()
                    Toast.makeText(
                        this@BakupRestoreActivity,
                        getString(R.string.backup_saved_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    runOnUiThread {
                        hideProgress()

                    }
                }

                Constants.SEND_BROADCAST_CANCEL -> {
                    runOnUiThread {
                        hideProgress()
                    }
                    EventBus.getDefault().post(MessagesRestoredEvent(true))

                }
                Constants.SEND_BROADCAST_RESTORE_FAILED -> {
                    val error =
                        intent.getStringExtra(Constants.SEND_ERROR_MESSAGE) ?: "Unknown error"
                    Toast.makeText(context, getString(R.string.restore_failed), Toast.LENGTH_LONG)
                        .show()
                    runOnUiThread {
                        hideProgress()
                    }
                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                when (requestCode) {
                    IMPORT_JSON_REQUEST_CODE -> {
                        showProgress()
                        binding.txtviw.setText(getString(R.string.restoring))
                        val intent = Intent(this, RestoreService::class.java).apply {
                            action = ACTION_START_RESTORE
                            putExtra(EXTRA_JSON_DATA, uri.toString())
                        }
                        ContextCompat.startForegroundService(this, intent)

                        /*  CoroutineScope(Dispatchers.Main).launch {
                              try {
                                  showProgress()
                                  backupViewModel.restoreMessages(
                                      uri,
                                      { progress -> binding.progressBar.progress = progress },
                                      { restoredList ->
                                          hideProgress()
                                          if (restoredList.isNotEmpty()) {
                                              Toast.makeText(
                                                  this@BakupRestoreActivity,
                                                  getString(R.string.restore_success),
                                                  Toast.LENGTH_SHORT
                                              ).show()
                                              EventBus.getDefault().post(MessagesRestoredEvent(true))
                                          } else {
                                              Toast.makeText(
                                                  this@BakupRestoreActivity,
                                                  getString(R.string.no_new_messages_restored),
                                                  Toast.LENGTH_SHORT
                                              ).show()
                                          }
                                      },
                                      {
                                          hideProgress()
                                          Toast.makeText(
                                              this@BakupRestoreActivity,
                                              getString(R.string.restore_failed),
                                              Toast.LENGTH_SHORT
                                          ).show()
                                      }
                                  )

                              } catch (e: Exception) {
                                  hideProgress()
                                  Toast.makeText(
                                      this@BakupRestoreActivity,
                                      getString(R.string.restore_failed) + "${e.message}",
                                      Toast.LENGTH_LONG
                                  ).show()
                              }
                          }*/

                    }

                    EXPORT_JSON_REQUEST_CODE -> {
                        binding.txtviw.setText(getString(R.string.backing_up))
                        showProgress()
                        val intent = Intent(this, RestoreService::class.java).apply {
                            action = ACTION_START_BACKUP
                            putExtra(EXTRA_JSON_DATA, uri.toString())
                        }
                        ContextCompat.startForegroundService(this, intent)


                        /*backupViewModel.backupMessages(uri,
                            onProgress = { progress ->
                                runOnUiThread {
                                    binding.progressBar.progress = progress
                                }
                            },
                            onComplete = { success ->
                                runOnUiThread {

                                    if (success) {
                                        saveLastBackupTime()
                                        loadLastBackupTime()
                                        Toast.makeText(
                                            this,
                                            getString(R.string.backup_saved_successfully),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        hideProgress()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.failed_to_save_backup),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )*/

                    }

                    else -> {}
                }
            }
        }
    }


    private fun showProgress() {
        binding.lyRestore.visibility = View.VISIBLE
        binding.lybackup.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.lybackup.visibility = View.VISIBLE
        binding.lyRestore.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

}