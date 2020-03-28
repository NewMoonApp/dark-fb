package com.newmoon.dark.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import android.view.WindowManager

import androidx.appcompat.widget.TintContextWrapper
import com.newmoon.common.util.ATLEAST_LOLLIPOP

object ActivityUtils {

    private val DEFAULT_NAVIGATION_BAR_COLOR = Color.BLACK

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setWhiteStatusBar(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = activity.resources.getColor(android.R.color.white)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setCustomColorStatusBar(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = color
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setStatusBarColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = color
        }
    }

    fun setStatusBarAlpha(activity: Activity, progress: Float) {
        if (ATLEAST_LOLLIPOP) {
            val alpha = (0Xff * progress).toInt()
            activity.window.statusBarColor = Color.argb(alpha, 0xff, 0xff, 0xff)
        }
    }

    fun hideStatusBar(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    fun showStatusBar(activity: Activity) {
        val window = activity.window
        if (window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setNavigationBarAlpha(activity: Activity, alpha: Float) {
        if (ATLEAST_LOLLIPOP) {
            val alphaInt = (0xff * alpha).toInt()
            activity.window.navigationBarColor = Color.argb(alphaInt, 0x00, 0x00, 0x00)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setNavigationBarColor(activity: Activity, color: Int) {
        if (ATLEAST_LOLLIPOP) {
            activity.window.navigationBarColor = color
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setNavigationBarDefaultColor(activity: Activity) {
        if (ATLEAST_LOLLIPOP) {
            setNavigationBarColor(activity, DEFAULT_NAVIGATION_BAR_COLOR)
        }
    }

    fun hideSystemUi(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 19) {
            val decorView = activity.window.decorView
            val uiOptions =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
        }
    }

    fun contextToActivitySafely(context: Context?): Activity? {
        return if (context == null) {
            null
        } else if (context is Activity) {
            context
        } else if (context is ContextThemeWrapper) {
            context.baseContext as Activity
        } else if (context is ContextThemeWrapper) {
            context.baseContext as Activity
        } else if (context is TintContextWrapper) {
            context.baseContext as Activity
        } else {
            null
        }
    }

    fun isDestroyed(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        }

        if (activity.isFinishing) {
            return true
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.isDestroyed
        } else false
    }
}
