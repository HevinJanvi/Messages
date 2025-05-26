package com.test.messages.demo.ui.Activity

import android.os.Bundle
import android.view.View
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.databinding.ActivitySwipeBinding
import com.test.messages.demo.ui.Dialogs.SwipeActionDialog
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.SwipeActionEvent
import org.greenrobot.eventbus.EventBus

class SwipeActivity : BaseActivity() {
    private lateinit var binding: ActivitySwipeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySwipeBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)

        val savedLeftAction = ViewUtils.getSwipeAction(this, false)
        val savedRightAction = ViewUtils.getSwipeAction(this, true)
        updateSwipeUI(savedLeftAction, savedRightAction)

        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnLeftChangeSwipe.setOnClickListener {
            val currentLeftAction = ViewUtils.getSwipeAction(this, false)
            SwipeActionDialog(this, currentLeftAction, false) { selectedAction ->
                val updatedRightAction = ViewUtils.getSwipeAction(this, true)
                updateSwipeUI(selectedAction, updatedRightAction)
                EventBus.getDefault().post(SwipeActionEvent(selectedAction, isRightSwipe = false))
            }.show()
        }

        binding.btnRightChangeSwipe.setOnClickListener {
            val currentRightAction = ViewUtils.getSwipeAction(this, true)
            SwipeActionDialog(this, currentRightAction, true) { selectedAction ->
                val updatedLeftAction = ViewUtils.getSwipeAction(this, false)
                updateSwipeUI(updatedLeftAction, selectedAction)
                EventBus.getDefault().post(SwipeActionEvent(selectedAction, isRightSwipe = true))
            }.show()
        }

    }

    private fun getSwipeActionName(action: Int): String {
        return when (action) {
            CommanConstants.SWIPE_NONE -> getString(R.string.none)
            CommanConstants.SWIPE_ARCHIVE -> getString(R.string.archive)
            CommanConstants.SWIPE_DELETE -> getString(R.string.delete)
            CommanConstants.SWIPE_CALL -> getString(R.string.call)
            CommanConstants.SWIPE_MARK_READ -> getString(R.string.mark_as_read)
            CommanConstants.SWIPE_MARK_UNREAD -> getString(R.string.mark_as_unread)
            else -> getString(R.string.none)
        }
    }

    private fun updateSwipeUI(leftAction: Int, rightAction: Int) {
        binding.tvLeftSwipeAction.text = getSwipeActionName(leftAction)
        val leftIconResId = getSwipeIcon(leftAction, false)
        binding.ivSwipeActionImg.setImageResource(leftIconResId)

        binding.tvRightSwipeAction.text = getSwipeActionName(rightAction)
        val rightIconResId = getSwipeIcon(rightAction, true)
        binding.ivSwipeActionImgRight.setImageResource(rightIconResId)
    }

    private fun getSwipeIcon(action: Int, isRightSwipe: Boolean): Int {
        return when (action) {
            CommanConstants.SWIPE_DELETE -> if (isRightSwipe) R.drawable.img_delete_action_right else R.drawable.img_delete_action_left
            CommanConstants.SWIPE_ARCHIVE -> if (isRightSwipe) R.drawable.img_archive_action_right else R.drawable.img_archive_action_left
            CommanConstants.SWIPE_CALL -> if (isRightSwipe) R.drawable.img_call_action_right else R.drawable.img_call_action_left
            CommanConstants.SWIPE_MARK_READ -> if (isRightSwipe) R.drawable.img_read_action_right else R.drawable.img_read_action_left
            CommanConstants.SWIPE_MARK_UNREAD -> if (isRightSwipe) R.drawable.img_unread_action_right else R.drawable.img_unread_action_left
            else -> R.drawable.ic_no_action
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}