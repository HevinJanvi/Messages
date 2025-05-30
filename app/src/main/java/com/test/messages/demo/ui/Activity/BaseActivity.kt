package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants.PREFS_NAME
import com.test.messages.demo.Util.Constants.THEMEMODE
import com.test.messages.demo.Util.ViewUtils
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { setLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setLanguage(this)

        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(R.color.bg, theme)
        }
        updateSystemBarsColor()

    }

    protected fun applyWindowInsetsToView(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun View.hideKeyboard(context: Context) {
        try {
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
        } catch (e: Exception) {

        }
    }

    fun View.showKeyboard() {
        try {
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
        }
    }

     fun setLanguage(context: Context): Context? {
        val languageCode = ViewUtils.getSelectedLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val selectedMode = sharedPref.getInt(THEMEMODE, 1)

        if (selectedMode == 1) {
            recreate()
        }
    }

    fun handleDrawerState(drawerLayout: DrawerLayout, drawerWasOpen: Boolean = false) {

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val scaledSlideOffset = slideOffset * 0.6f

                if (scaledSlideOffset >= 0.6f) {
                    val blendedColor =
                        blendColors(getColor(R.color.popup_bg), getColor(R.color.popup_bg), 0.6f)
                    setSystemBarsColor(blendedColor)
                } else {
                    val blendedColor = blendColors(
                        getColor(R.color.popup_bg),
                        getColor(R.color.popup_bg),
                        scaledSlideOffset
                    )
                    setSystemBarsColor(blendedColor)
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                val shadowColor =
                    blendColors(getColor(R.color.popup_bg), getColor(R.color.popup_bg), 0.6f)
                setSystemBarsColor(shadowColor)
            }

            override fun onDrawerClosed(drawerView: View) {
                resetSystemBarsColorWithDelay()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
        if (drawerWasOpen) {
            val blendedColor =
                blendColors(getColor(R.color.popup_bg), getColor(R.color.popup_bg), 0.6f)
            setSystemBarsColor(blendedColor)
        }
    }

    private fun resetSystemBarsColorWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            updateSystemBarsColor()
        }, 1)
    }

    private fun updateSystemBarsColor() {
        val primaryColor = getColor(R.color.bg)
        setSystemBarsColor(primaryColor)
    }

    private fun setSystemBarsColor(backgroundColor: Int) {
        val window: Window = window

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        window.decorView.post {
            val isDarkBackground = isColorDark(backgroundColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController: WindowInsetsController? = window.insetsController
                insetsController?.apply {

                    setSystemBarsAppearance(
                        if (isDarkBackground) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                    setSystemBarsAppearance(
                        if (isDarkBackground) 0 else WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            } else {
                val decorView: View = window.decorView
                var flags = decorView.systemUiVisibility
                if (!isDarkBackground) {
                    flags =
                        flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags =
                        flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                decorView.systemUiVisibility = flags
            }
        }
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.rgb(r, g, b)
    }


    override fun onResume() {
        super.onResume()
        setLanguage(this@BaseActivity)
    }

}