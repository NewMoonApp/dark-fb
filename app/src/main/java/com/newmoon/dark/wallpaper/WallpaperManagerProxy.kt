package com.newmoon.dark.wallpaper

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.IBinder
import com.newmoon.common.BaseApplication
import com.newmoon.dark.BuildConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * A proxy that delegates calls to [WallpaperManager] after applying appropriate size transformations
 * when setting wallpaper.
 */
class WallpaperManagerProxy private constructor() {

    private val mWm: WallpaperManager

    private var mScrollable: Boolean? = null

    // Multiple crashes are reported to occur due to internal exceptions:
    // (1) IllegalStateException on Huawei Android 4.4.2 (API 19) devices
    // (2) IOException on Meizu M2 and alps devices
    val systemDrawable: BitmapDrawable?
        get() {
            var bitmapDrawable: BitmapDrawable? = null
            try {
                bitmapDrawable = mWm.drawable as BitmapDrawable
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return bitmapDrawable
        }

    val systemBitmap: Bitmap?
        get() {
            var result: Bitmap? = null
            val drawable = systemDrawable
            if (drawable != null) {
                result = drawable.bitmap
            }
            return result
        }

    val wallpaperInfo: WallpaperInfo
        get() = mWm.wallpaperInfo

    val desiredMinimumWidth: Int
        get() = mWm.desiredMinimumWidth

    val desiredMinimumHeight: Int
        get() = mWm.desiredMinimumHeight

    init {
        mWm = WallpaperManager.getInstance(BaseApplication.context)
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun setSystemBitmap(
        context: Context,
        wallpaper: Bitmap?,
        scrollable: Boolean = WallpaperUtils.canScroll(context, wallpaper)
    ) {
        var wallpaper = wallpaper
        if (wallpaper == null || wallpaper.height == 0 || wallpaper.width == 0) {
            if (BuildConfig.DEBUG) {
                throw NullPointerException("Wallpaper bitmap can't be null")
            }
            return
        }
        mScrollable = scrollable
        val point = WallpaperUtils.getWindowSize(context)

        try {
            if (scrollable) {
                wallpaper = WallpaperUtils.translateToScrollWallpaper(wallpaper, context)
            } else {
                wallpaper = WallpaperUtils.translateToFixedWallpaper(wallpaper, context)
            }
        } catch (error: OutOfMemoryError) {
        }

        if (wallpaper != null) {
            try {
                try {
                    val stream = bitmapToStream(wallpaper)
                    mWm.setStream(stream)
                    if (scrollable) {
                        mWm.suggestDesiredDimensions(point.x * 2, point.y)
                    } else {
                        mWm.suggestDesiredDimensions(point.x, point.y)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } catch (error: OutOfMemoryError) {
                try {
                    mWm.setBitmap(wallpaper)
                    if (scrollable) {
                        mWm.suggestDesiredDimensions(point.x * 2, point.y)
                    } else {
                        mWm.suggestDesiredDimensions(point.x, point.y)
                    }
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }
    }

    fun restore(windowToken: IBinder?) {
        if (windowToken != null) {
            try {
                mWm.setWallpaperOffsets(windowToken, 0f, 0.5f)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun sendWallpaperCommand(
        windowToken: IBinder, action: String,
        x: Int, y: Int, z: Int, extras: Bundle
    ) {
        try {
            mWm.sendWallpaperCommand(windowToken, action, x, y, z, extras)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun setWallpaperOffsets(windowToken: IBinder, xOffset: Float, yOffset: Float) {
        try {
            mWm.setWallpaperOffsets(windowToken, xOffset, yOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun suggestDesiredDimensions(minimumWidth: Int, minimumHeight: Int) {
        try {
            mWm.suggestDesiredDimensions(minimumWidth, minimumHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {

        @Volatile
        private var sInstance: WallpaperManagerProxy? = null

        val instance: WallpaperManagerProxy
            get() {
                if (sInstance == null) {
                    synchronized(WallpaperManagerProxy::class.java) {
                        if (sInstance == null) {
                            sInstance = WallpaperManagerProxy()
                        }
                    }
                }
                return sInstance!!
            }

        @Throws(IOException::class)
        private fun bitmapToStream(bitmap: Bitmap): ByteArrayInputStream {
            val stream = ByteArrayOutputStream(2048)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            return ByteArrayInputStream(stream.toByteArray())
        }
    }
}
