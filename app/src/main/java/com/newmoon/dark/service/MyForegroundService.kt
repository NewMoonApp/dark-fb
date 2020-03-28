package com.newmoon.dark.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.newmoon.common.util.Navigations
import com.newmoon.common.util.Preferences
import com.newmoon.dark.R
import com.newmoon.dark.extensions.updateNotification
import com.newmoon.dark.nightscreen.NightScreenManager
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.ui.HomeActivity
import com.newmoon.dark.ui.MODE
import com.newmoon.dark.ui.NIGHT_SCREEN_TIMING
import com.newmoon.dark.ui.NOTIFICATION

const val CHANNEL_DEFAULT_IMPORTANCE = "1"
const val ONGOING_NOTIFICATION_ID = 100

const val FROM = "from"
const val TOOLBAR = "toolbar"

const val TAB = "tab"
const val DARKMODE = "darkmode"
const val NIGHTSCREEN = "nightscreen"

const val ACTION_TOOLBAR_UPDATE= "action_toolbar_update"
const val ACTION_UPDATE_MAIN_PAGE= "action_update_main_page"

class MyForegroundService : Service() {

    inner class ToolbarUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.run {
                val tab = getStringExtra(TAB)
                tab?.let {
                    if (it == DARKMODE) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Navigations.startActivitySafely(
                                applicationContext,
                                Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            )
                        } else {
                            val uiModeManager =
                                getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                            val isDarkMode = resources.getBoolean(R.bool.dark_mode)
                            uiModeManager.nightMode = if (isDarkMode) {
                                Preferences.default.putInt(
                                    MODE,
                                    UiModeManager.MODE_NIGHT_NO
                                )
                                UiModeManager.MODE_NIGHT_NO
                            } else {
                                Preferences.default.putInt(
                                    MODE,
                                    UiModeManager.MODE_NIGHT_YES
                                )
                                UiModeManager.MODE_NIGHT_YES
                            }
                            updateNotification()
                        }
                    } else if (it == NIGHTSCREEN) {
                        // disable night screen timing
                        Preferences.default.putBoolean(NIGHT_SCREEN_TIMING, false)
                        val nsm = NightScreenManager.getInstance(applicationContext)

                        if (nsm.isOn()) {
                            nsm.removeNightScreen()
                        } else {
                            nsm.addNightScreen()
                        }
                        updateNotification()
                        sendBroadcast(Intent(ACTION_UPDATE_MAIN_PAGE).also { i ->
                            i.putExtras(this)
                        })
                    }
                }
            }
        }
    }


    private lateinit var mToolbarUpdateReceiver: ToolbarUpdateReceiver
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mToolbarUpdateReceiver = ToolbarUpdateReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_TOOLBAR_UPDATE)
        registerReceiver(mToolbarUpdateReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mToolbarUpdateReceiver)
        stopForeground(true)
    }

    private fun startForeground() {
        if (BillingManager.isPremiumUser()) {
            val mBuilder = NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            val b: NotificationChannel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = NotificationChannel(
                    CHANNEL_DEFAULT_IMPORTANCE,
                    resources.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(b)
                mBuilder.setChannelId(CHANNEL_DEFAULT_IMPORTANCE)
            }
            val mRemoteViews = RemoteViews(packageName, R.layout.notification_pro)

            val isDarkMode = resources.getBoolean(R.bool.dark_mode)
            if (isDarkMode) {
                mRemoteViews.setImageViewResource(
                    R.id.navigation_dark_mode_img,
                    R.drawable.navigation_dark_mode_selected
                )
                mRemoteViews.setTextColor(
                    R.id.navigation_dark_mode_text,
                    resources.getColor(R.color.navigation_text_selected_color)
                )
            } else {
                mRemoteViews.setImageViewResource(
                    R.id.navigation_dark_mode_img,
                    R.drawable.navigation_dark_mode_normal
                )
                mRemoteViews.setTextColor(
                    R.id.navigation_dark_mode_text,
                    resources.getColor(R.color.navigation_text_normal_color)
                )
            }

            val nsm = NightScreenManager.getInstance(this)
            if (nsm.isOn()) {
                mRemoteViews.setImageViewResource(
                    R.id.navigation_night_screen_img,
                    R.drawable.navigation_night_screen_selected
                )
                mRemoteViews.setTextColor(
                    R.id.navigation_night_screen_text,
                    resources.getColor(R.color.navigation_text_selected_color)
                )
            } else {
                mRemoteViews.setImageViewResource(
                    R.id.navigation_night_screen_img,
                    R.drawable.navigation_night_screen_normal
                )
                mRemoteViews.setTextColor(
                    R.id.navigation_night_screen_text,
                    resources.getColor(R.color.navigation_text_normal_color)
                )
            }

            val darkModePendingIntent =
                PendingIntent.getBroadcast(this, 1, Intent(ACTION_TOOLBAR_UPDATE).also {
                    it.putExtra(TAB, DARKMODE)
                }, PendingIntent.FLAG_UPDATE_CURRENT)

            val nightScreenPendingIntent =
                PendingIntent.getBroadcast(this, 2, Intent(ACTION_TOOLBAR_UPDATE).also {
                    it.putExtra(TAB, NIGHTSCREEN)
                }, PendingIntent.FLAG_UPDATE_CURRENT)

            mRemoteViews.setOnClickPendingIntent(R.id.navigation_dark_mode, darkModePendingIntent)
            mRemoteViews.setOnClickPendingIntent(R.id.navigation_night_screen, nightScreenPendingIntent)
            //主要设置setContent参数  其他参数 按需设置
            mBuilder.setContent(mRemoteViews)
            mBuilder.setSmallIcon(R.drawable.notification_small_icon)
            mBuilder.setOngoing(true)
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.notification_icon))
            mBuilder.setAutoCancel(true)

            startForeground(ONGOING_NOTIFICATION_ID, mBuilder.build())
        } else {
            val mBuilder = NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            val b: NotificationChannel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = NotificationChannel(
                    CHANNEL_DEFAULT_IMPORTANCE,
                    resources.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(b)
                mBuilder.setChannelId(CHANNEL_DEFAULT_IMPORTANCE)
            }
            val mRemoteViews = RemoteViews(packageName, R.layout.notification)
            val pendingIntet =
                PendingIntent.getActivity(
                    this,
                    1,
                    Intent(this, HomeActivity::class.java).also {
                        //it.addCategory(Intent.CATEGORY_LAUNCHER)
                        it.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        it.putExtra("from", "toolbar")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            mRemoteViews.setOnClickPendingIntent(R.id.content, pendingIntet)
            //主要设置setContent参数  其他参数 按需设置
            mBuilder.setContent(mRemoteViews)
            mBuilder.setSmallIcon(R.drawable.notification_small_icon)
            mBuilder.setOngoing(true)
            mBuilder.setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.notification_icon
                )
            )
            mBuilder.setAutoCancel(true)

            startForeground(ONGOING_NOTIFICATION_ID, mBuilder.build())
        }
    }
}
