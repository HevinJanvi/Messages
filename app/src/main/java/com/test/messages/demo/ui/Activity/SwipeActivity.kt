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

        val savedLeftAction = ViewUtils.getSwipeAction(this, false)
        val savedRightAction = ViewUtils.getSwipeAction(this, true)
        updateSwipeUI(savedLeftAction, savedRightAction)

        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnLeftChangeSwipe.setOnClickListener {
            SwipeActionDialog(this, savedLeftAction) { selectedAction ->
                ViewUtils.saveSwipeAction(this, selectedAction, false)
                updateSwipeUI(selectedAction, savedRightAction)
                EventBus.getDefault().post(SwipeActionEvent(selectedAction, isRightSwipe = false))
            }.show()
        }

        binding.btnRightChangeSwipe.setOnClickListener {
            SwipeActionDialog(this, savedRightAction) { selectedAction ->
                ViewUtils.saveSwipeAction(this, selectedAction, true)
                updateSwipeUI(savedLeftAction, selectedAction)
                EventBus.getDefault().post(SwipeActionEvent(selectedAction, isRightSwipe = true))
            }.show()
        }

    }


    private fun updateSwipeUI(leftAction: Int, rightAction: Int) {
        binding.tvLeftSwipeAction.text = leftAction.toString()
        val leftIconResId = getSwipeIcon(leftAction, false)
        binding.ivSwipeActionImg.setImageResource(leftIconResId)

        binding.tvRightSwipeAction.text = rightAction.toString()
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