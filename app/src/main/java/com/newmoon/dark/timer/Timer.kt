package com.newmoon.dark.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import com.newmoon.common.util.Preferences
import com.newmoon.dark.service.TimerService
import java.util.*

const val KEY_DARK_MODE_BEGIN_HOUR = "KEY_DARK_MODE_BEGIN_HOUR"
const val KEY_DARK_MODE_BEGIN_MINUTE = "KEY_DARK_MODE_BEGIN_MINUTE"
const val KEY_DARK_MODE_END_HOUR = "KEY_DARK_MODE_END_HOUR"
const val KEY_DARK_MODE_END_MINUTE = "KEY_DARK_MODE_END_MINUTE"

const val KEY_NIGHT_SCREEN_BEGIN_HOUR = "KEY_NIGHT_SCREEN_BEGIN_HOUR"
const val KEY_NIGHT_SCREEN_BEGIN_MINUTE = "KEY_NIGHT_SCREEN_BEGIN_MINUTE"
const val KEY_NIGHT_SCREEN_END_HOUR = "KEY_NIGHT_SCREEN_END_HOUR"
const val KEY_NIGHT_SCREEN_END_MINUTE = "KEY_NIGHT_SCREEN_END_MINUTE"

fun getDarkModeTimer(): Timer {
    val p = Preferences.default
    return Timer(
        p.getInt(KEY_DARK_MODE_BEGIN_HOUR,22),
        p.getInt(KEY_DARK_MODE_BEGIN_MINUTE,0),
        p.getInt(KEY_DARK_MODE_END_HOUR,6),
        p.getInt(KEY_DARK_MODE_END_MINUTE,0))
}

fun setDarkModeTimerBegin(h: Int, m: Int) {
    val p = Preferences.default
    p.putInt(KEY_DARK_MODE_BEGIN_HOUR, h)
    p.putInt(KEY_DARK_MODE_BEGIN_MINUTE, m)
}

fun setDarkModeTimerEnd(h: Int, m: Int) {
    val p = Preferences.default
    p.putInt(KEY_DARK_MODE_END_HOUR, h)
    p.putInt(KEY_DARK_MODE_END_MINUTE, m)
}


fun getNightScreenTimer(): Timer {
    val p = Preferences.default
    return Timer(
        p.getInt(KEY_NIGHT_SCREEN_BEGIN_HOUR, 22),
        p.getInt(KEY_NIGHT_SCREEN_BEGIN_MINUTE, 0),
        p.getInt(KEY_NIGHT_SCREEN_END_HOUR, 6),
        p.getInt(KEY_NIGHT_SCREEN_END_MINUTE, 0)
    )
}

fun setNightScreenTimerBegin(h: Int, m: Int) {
    val p = Preferences.default
    p.putInt(KEY_NIGHT_SCREEN_BEGIN_HOUR, h)
    p.putInt(KEY_NIGHT_SCREEN_BEGIN_MINUTE, m)
}

fun setNightScreenTimerEnd(h: Int, m: Int) {
    val p = Preferences.default
    p.putInt(KEY_NIGHT_SCREEN_END_HOUR, h)
    p.putInt(KEY_NIGHT_SCREEN_END_MINUTE, m)
}

data class Timer(
    val beginHour: Int,
    val beginMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

fun inTimeRange(current: Long, time: Timer): Boolean {
    val calendar: Calendar = Calendar.getInstance()
    val h = calendar.get(Calendar.HOUR_OF_DAY)
    val m = calendar.get(Calendar.MINUTE)
    return if (h in time.beginHour..23 || h in 0..time.endHour) {
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
}

fun scheduledTimerTask(context: Context) {
    val dt = getDarkModeTimer()
    val nt = getNightScreenTimer()

    var h1 = 0
    var m1 = 0

    var h2 = 0
    var m2 = 0

    val current = System.currentTimeMillis()

    if (inTimeRange(System.currentTimeMillis(), dt)) {
        h1 = dt.endHour
        m1 = dt.endMinute
    } else {
        h1 = dt.beginHour
        m1 = dt.beginMinute
    }

    if (inTimeRange(System.currentTimeMillis(), nt)) {
        h2 = nt.endHour
        m2 = nt.endMinute
    } else {
        h2 = nt.beginHour
        m2 = nt.beginMinute
    }

    val calendar: Calendar = Calendar.getInstance()
    calendar.timeInMillis = current
    calendar.set(Calendar.HOUR_OF_DAY, h1)
    calendar.set(Calendar.MINUTE, m1)
    calendar.set(Calendar.SECOND, 0)

    if (calendar.timeInMillis > current) {
        addScheduledTask(context, 1, calendar.timeInMillis)
    } else {
        addScheduledTask(context, 1, calendar.timeInMillis + DateUtils.DAY_IN_MILLIS)
    }

    calendar.set(Calendar.HOUR_OF_DAY, h2)
    calendar.set(Calendar.MINUTE, m2)
    calendar.set(Calendar.SECOND, 0)

    if (calendar.timeInMillis > current) {
        addScheduledTask(context, 2, calendar.timeInMillis)
    } else {
        addScheduledTask(context, 2, calendar.timeInMillis + DateUtils.DAY_IN_MILLIS)
    }

}


fun addScheduledTask(context: Context, messageId: Int, scheduledTime: Long) {
    val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, TimerService::class.java)

    val pendingIntent =
        PendingIntent.getService(context, messageId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            scheduledTime,
            pendingIntent
        )
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
    }
}

fun getTimeString(hour: Int, minute: Int, prefix: String = ""): String {
    val hS =
        if (hour < 10) {
            "0${hour}"
        } else {
            "${hour}"
        }
    val mS =
        if (minute < 10) {
            "0${minute}"
        } else {
            "${minute}"
        }

    return "$prefix${hS}:${mS}"
}