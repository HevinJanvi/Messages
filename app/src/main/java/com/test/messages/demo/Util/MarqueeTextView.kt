package com.test.messages.demo.Util
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        isSingleLine = true
        ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isSelected = true
    }
}