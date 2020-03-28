package com.newmoon.dark.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import com.newmoon.common.util.Navigations
import com.newmoon.common.util.Preferences
import com.newmoon.dark.R
import com.newmoon.dark.pro.BillingActivity
import com.newmoon.dark.service.MyForegroundService
import com.newmoon.dark.timer.TimePickerDialog
import com.newmoon.dark.ui.FaqActivity
import com.newmoon.dark.ui.NOTIFICATION
import com.newmoon.dark.ui.SettingsActivity
import com.newmoon.dark.ui.openWebPageWithUrl

fun Context.canDrawOverlays(): Boolean {
    return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
}

fun View.canDrawOverlays(): Boolean {
    return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this.context))
}

fun Context.updateNotification() {
    val show = Preferences.default.getBoolean(NOTIFICATION, true)
    if (show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MyForegroundService::class.java))
        } else {
            startService(Intent(this, MyForegroundService::class.java))
        }
    } else {
        stopService(Intent(this, MyForegroundService::class.java))
    }
}

fun Context.openPrivacyPolicy() {
    openWebPageWithUrl(
        this,
        "https://darkmode.s3.us-east-2.amazonaws.com/privacy.html",
        resources.getString(R.string.privacy_policy)
    )
}

fun Context.openTermOfService() {
    openWebPageWithUrl(
        this,
        "https://darkmode.s3.us-east-2.amazonaws.com/terms.html",
        resources.getString(R.string.terms_of_service)
    )
}

fun Context.share() {
    var share_intent = Intent()

    share_intent.action = Intent.ACTION_SEND
    share_intent.type = "text/plain"
    share_intent.putExtra(Intent.EXTRA_SUBJECT, "share")
    share_intent.putExtra(
        Intent.EXTRA_TEXT,
        "Here's an amazing app " + "https://play.google.com/store/apps/details?id=${packageName}" + ", install & enjoy!"
    )
    share_intent =
        Intent.createChooser(share_intent, resources.getString(R.string.app_name))
    Navigations.startActivitySafely(this, share_intent)
}

fun Context.openSettings() {
    Navigations.startActivitySafely(this, Intent(this, SettingsActivity::class.java))
}

fun Context.openQA() {
    Navigations.startActivitySafely(this, Intent(this, FaqActivity::class.java))
}

fun Context.openPurchasePage(from: String) {
    val intent = Intent(this, BillingActivity::class.java)
    intent.putExtra(BillingActivity.EXTRA_FROM, from)
    Navigations.startActivitySafely(this, intent)
}

fun Activity.openTimerPickerDialog(
    listener: TimePickerDialog.PikerListener,
    h: Int = 0,
    m: Int = 0
) {
    val picker = TimePickerDialog(this, R.style.Transparent)
    picker.setListener(listener)
    picker.setTime(h, m)
    picker.show()
}