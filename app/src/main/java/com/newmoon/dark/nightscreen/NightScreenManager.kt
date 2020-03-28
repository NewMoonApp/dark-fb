package com.newmoon.dark.nightscreen

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.newmoon.common.util.Dimensions
import com.newmoon.common.util.Preferences
import com.newmoon.dark.extensions.canDrawOverlays
import com.newmoon.dark.ui.NIGHT_SCREEN_TIMING

const val LAST_INTENSITY_COLOR_KEY = "last_intensity_color_key"
const val LAST_INTENSITY_ALPHA_KEY = "last_intensity_alpha_key"
const val LAST_BRIGHTNESS_ALPHA_KEY = "last_screenDim_alpha_key"

const val COLOR_TEMPERATURE_DARK = 0xdf5B2E00.toInt()
const val COLOR_TEMPERATURE_CANDLELIGHT = 0xFFFF3900.toInt()
const val COLOR_TEMPERATURE_DIM = 0xdfFF7A00.toInt()
const val COLOR_TEMPERATURE_STARRY = 0xdf0B0B0B.toInt()
const val COLOR_TEMPERATURE_FOREST = 0xdf134609.toInt()
class NightScreenManager private constructor(context: Context) {
    private val mApplicationContext: Context = context.applicationContext
    private var mIntensityView: View? = null
    private var mScreenDimView: View? = null
    private var mAdded: Boolean = false

    private val mWindowManager: WindowManager

    private var mIntensityColor = COLOR_TEMPERATURE_DARK
    private var mIntensityAlpha = 0.3f / 0.8f
    private var mScreenDimAlpha = 0f

    init {
        mWindowManager =
            mApplicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun addNightScreen() {
        if (!mApplicationContext.canDrawOverlays()) {
            return
        }

        if (mAdded) {
            return
        }

        mIntensityView = View(mApplicationContext)
        mScreenDimView = View(mApplicationContext)
        mScreenDimView?.background = ColorDrawable(Color.BLACK)
        setIntensityColor(mIntensityColor)
        setIntensityAlpha(mIntensityAlpha)
        setScreenDim(mScreenDimAlpha)

        val layoutParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        layoutParams.flags =
            layoutParams.flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = Dimensions.getPhoneHeight(mApplicationContext) + Dimensions.pxFromDp(50f)

        mWindowManager.addView(mScreenDimView!!, layoutParams)
        mWindowManager.addView(mIntensityView!!, layoutParams)

        mAdded = true
    }

    fun removeNightScreen() {
        if (mAdded) {
            mScreenDimView?.let {
                try {
                    mWindowManager.removeView(it)
                } catch (e: Exception) {
                    //
                }
            }
            mIntensityView?.let {
                try {
                    mWindowManager.removeView(it)
                } catch (e: Exception) {
                    //
                }
            }

            mAdded = false
        }
    }

    fun setIntensityColor(color: Int) {
        mIntensityView?.run {
            background = ColorDrawable(color)
        }
        mIntensityColor = color
    }

    fun setIntensityAlpha(opaqueness: Float) {
        mIntensityView?.run {
            alpha = opaqueness
        }
        mIntensityAlpha = opaqueness
    }

    fun setScreenDim(opaqueness: Float) {
        mScreenDimView?.run {
            alpha = opaqueness
        }
        mScreenDimAlpha = opaqueness
    }

    fun getIntensityColor(): Int {
        return mIntensityColor
    }

    fun getIntensityAlpha(): Float {
        return mIntensityAlpha
    }

    fun getScreenDimAlpha(): Float {
        return mScreenDimAlpha
    }

    fun isOn(): Boolean {
        return mAdded
    }

    fun setNightScreenTimingEnabled(enable: Boolean) {
        Preferences.default.putBoolean(NIGHT_SCREEN_TIMING, enable)
    }
    fun isNightScreenTimingEnabled(): Boolean {
        return Preferences.default.getBoolean(NIGHT_SCREEN_TIMING, false)
    }

    companion object {

        @Volatile
        private var sInstance: NightScreenManager? = null

        fun getInstance(context: Context): NightScreenManager {
            if (sInstance == null) {
                synchronized(NightScreenManager::class.java) {
                    if (sInstance == null) {
                        sInstance = NightScreenManager(context)
                    }
                }
            }
            return sInstance!!
        }
    }
}
