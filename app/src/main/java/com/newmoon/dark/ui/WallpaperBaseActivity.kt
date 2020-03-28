package com.newmoon.dark.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.newmoon.common.util.Threads
import com.newmoon.common.util.Toasts
import com.newmoon.dark.R
import com.newmoon.dark.util.ActivityUtils
import com.newmoon.dark.wallpaper.WallpaperInfo
import com.newmoon.dark.wallpaper.WallpaperManagerProxy
import java.io.IOException


abstract class WallpaperBaseActivity : AppCompatActivity() {

    protected var mDialog: ProgressDialog? = null
    protected var mCurrentWallpaper: WallpaperInfo? = null
    protected var isSettingWallpaper = false
        private set

    protected abstract val currentWallpaper: WallpaperInfo?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mDialog != null) {
            mDialog!!.dismiss(true)
        }
    }

    protected fun startGuideAnimation(activity: Activity, resId: Int, width: Int, height: Int) {
        val drawView = activity.findViewById<DrawView>(resId) ?: return
        drawView.post {
            val drawer = EditWallpaperHintDrawer(drawView, width, height)
            drawView.setDrawer(drawer)
            drawer.start()
            drawView.visibility = View.VISIBLE
        }
    }

    protected abstract fun refreshButtonState()

    @JvmOverloads
    protected fun applyWallpaper(isScroll: Boolean, logEvent: Boolean = true) {
        if (ActivityUtils.isDestroyed(this)) return

        isSettingWallpaper = true
        mCurrentWallpaper = currentWallpaper

        val mHandler = Handler()
        mDialog = ProgressDialog.createDialog(
            this,
            getString(R.string.wallpaper_setting_progress_dialog_text)
        )
        mDialog!!.show()
        mDialog!!.setCancelable(false)
        val wallpaper = tryGetWallpaperToSet()
        if (wallpaper != null) {
            Threads.postOnThreadPoolExecutor(Runnable {
                val success = setWallpaper(wallpaper, logEvent)
                var delays = wallpaper.width * wallpaper.height / 10000
                if (delays > 1000) {
                    delays = 1000
                }
                mHandler.postDelayed({
                    mDialog!!.setOnDismissListener { applyWallPaperFinish(isScroll) }
                    if (success) {
                        mDialog!!.dismiss(false)
                    } else {
                        mDialog!!.dismiss(true)
                        Toasts.showToast(R.string.wallpaper_toast_set_failed)
                    }
                }, delays.toLong())
            })
        } else {
            mDialog!!.dismiss(true)
            Toasts.showToast(R.string.wallpaper_toast_set_failed)
            finish()
        }
    }

    open protected fun applyWallPaperFinish(isScroll: Boolean) {
        finish()
    }

    private fun setWallpaper(wallpaper: Bitmap?, logEvent: Boolean): Boolean {
        if (wallpaper != null) {
            try {
                WallpaperManagerProxy.instance.setSystemBitmap(this, wallpaper)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            return true
        }
        return false
    }

    protected abstract fun tryGetWallpaperToSet(): Bitmap?
}
