package com.newmoon.dark.timer

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.newmoon.common.util.Preferences
import com.newmoon.dark.R
import com.newmoon.dark.extensions.updateNotification
import com.newmoon.dark.nightscreen.NightScreenManager
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.ui.MODE
import com.newmoon.dark.ui.NIGHT_MODE_TIMING
import com.newmoon.dark.ui.NIGHT_SCREEN_TIMING
import kotlinx.android.synthetic.main.activity_home.*
import java.util.*

class CheckThread(val context: Context) : Thread() {
    private val calendar: Calendar = Calendar.getInstance()
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var run: Boolean = false

    override fun run() {
        while (run) {
            SystemClock.sleep(5000)
            checkModeManager()
            checkDarkModeTimer()
            checkNightScreenTimer()
        }
    }

    private fun checkModeManager() {
        val mode = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            Preferences.default.getInt(
                MODE,
                UiModeManager.MODE_NIGHT_NO
            ) else uiModeManager.nightMode

        if (mode != NIGHT_MODE_TIMING && mode != uiModeManager.nightMode) {
            handler.post {
                uiModeManager.nightMode = mode
                context.updateNotification()
            }
        }
    }

    private fun checkDarkModeTimer() {
        if (!BillingManager.isPremiumUser()) {
            return
        }
        val mode = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            Preferences.default.getInt(
                MODE,
                UiModeManager.MODE_NIGHT_NO
            ) else uiModeManager.nightMode
        if (mode == NIGHT_MODE_TIMING) {
            calendar.timeInMillis = System.currentTimeMillis()


            val time = getDarkModeTimer()

            val h = calendar.get(Calendar.HOUR_OF_DAY)
            val m = calendar.get(Calendar.MINUTE)

            val mode: Int

            mode = if (h in time.beginHour..23 || h in 0..time.endHour) {
                if ((h == time.beginHour && m < time.beginMinute) ||
                    (h == time.endHour && m >= time.endMinute)
                ) {
                    UiModeManager.MODE_NIGHT_NO
                } else {
                    UiModeManager.MODE_NIGHT_YES
                }
            } else {
                UiModeManager.MODE_NIGHT_NO
            }

            if (mode != uiModeManager.nightMode) {
                handler.post {
                    uiModeManager.nightMode = mode
                    context.updateNotification()
                }
            }
        }
    }

    private fun checkNightScreenTimer() {
        val isNightScreenTimingMode = Preferences.default.getBoolean(NIGHT_SCREEN_TIMING, false)
        if (isNightScreenTimingMode) {
            calendar.timeInMillis = System.currentTimeMillis()


            val time = getNightScreenTimer()

            val h = calendar.get(Calendar.HOUR_OF_DAY)
            val m = calendar.get(Calendar.MINUTE)

            val mode = if (h in time.beginHour..23 || h in 0..time.endHour) {
                if ((h == time.beginHour && m < time.beginMinute) ||
                    (h == time.endHour && m >= time.endMinute)
                ) {
                    false
                } else {
                    true
                }
            } else {
                false
            }

            val nsm = NightScreenManager.getInstance(context.applicationContext)
            handler.post {
                if (mode) {
                    if (!nsm.isOn()) {
                        nsm.addNightScreen()
                        context.updateNotification()
                    }
                } else {
                    if (nsm.isOn()) {
                        nsm.removeNightScreen()
                        context.updateNotification()
                    }
                }
            }
        }
    }


    override fun start() {
        run = true
        super.start()
    }

    fun terminate() {
        run = false
    }

}
