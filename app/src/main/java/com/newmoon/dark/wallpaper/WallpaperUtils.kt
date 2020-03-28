package com.newmoon.dark.wallpaper

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import com.newmoon.common.BaseApplication
import com.newmoon.common.util.ATLEAST_JB_MR1
import com.newmoon.common.util.Dimensions
import com.newmoon.dark.BuildConfig

object WallpaperUtils {

    val WALLPAPER_TYPE_SCROLLABLE_STANDARD = 1
    val WALLPAPER_TYPE_STATIC_STANDARD = 2

    private val TAG = WallpaperUtils::class.java.simpleName

    private val COLOR_SAMPLE_COUNT = 6

    // TODO: 13/12/2016 remove this method

    /**
     * use Activity context
     */
    private var sCachePoint: Point? = null

    val WALLPAPER_SCREENS_SPAN = 2f

    private var sDefaultWallpaperSize: Point? = null

    fun centerCrop(dWidth: Int, dHeight: Int, imageView: ImageView): Matrix {

        val newMatrix = Matrix()

        val vWidth = Dimensions.getPhoneWidth(BaseApplication.context)
        val vHeight = Dimensions.getPhoneHeight(BaseApplication.context)
        Log.i(TAG, "centerCrop  screen  vWidth $vWidth vHeight $vHeight")
        val scale: Float
        var dx = 0f
        var dy = 0f

        if (dWidth * vHeight > vWidth * dHeight) {
            scale = vHeight.toFloat() / dHeight.toFloat()
            dx = (vWidth - dWidth * scale) * 0.5f
        } else {
            scale = vWidth.toFloat() / dWidth.toFloat()
            dy = (vHeight - dHeight * scale) * 0.5f
        }

        Log.i(
            TAG,
            "centerCrop  dWidth " + dWidth + " dHeight " + dHeight + " vWidth " + vWidth + " vHeight " + vHeight
                    + " scale " + scale + " dx " + Math.round(dx) + " dy " + Math.round(dy)
        )

        newMatrix.setScale(scale, scale)
        newMatrix.postTranslate(Math.round(dx).toFloat(), Math.round(dy).toFloat())
        return newMatrix
    }

    fun centerInside(dWidth: Int, dHeight: Int, top: Int, bottom: Int): Matrix {
        val bitmapRect = RectF()
        val vWidth = Dimensions.getPhoneWidth(BaseApplication.context)
        val vHeight = BaseApplication.context!!.resources.displayMetrics.heightPixels

        bitmapRect.set(0f, 0f, dWidth.toFloat(), dHeight.toFloat())
        val imgRect = RectF(0f, top.toFloat(), vWidth.toFloat(), bottom.toFloat())
        Log.i(
            TAG,
            "centerInside  dWidth " + dWidth + " dHeight " + dHeight + " vWidth " + vWidth + " vHeight "
                    + vHeight + " top " + top + " bottom " + bottom
        )

        val matrix = Matrix()
        matrix.setRectToRect(bitmapRect, imgRect, Matrix.ScaleToFit.CENTER)
        return matrix
    }

    fun centerInside(src: Bitmap): Bitmap {
        val bitmapRect = Rect()
        val point = getWindowSize(BaseApplication.context)
        val vWidth = point.x
        val vHeight = point.y

        bitmapRect.set(0, 0, src.width, src.height)
        val windowRect = Rect(0, 0, vWidth, vHeight)
        Log.i(
            TAG,
            "centerInside  dWidth " + src.width + " dHeight " + src.height + " vWidth " + vWidth + " vHeight " + vHeight
        )
        val windowRectF = RectF(0f, 0f, vWidth.toFloat(), vHeight.toFloat())
        val bitmapRectF = RectF(bitmapRect)

        val matrix = Matrix()
        matrix.setRectToRect(windowRectF, bitmapRectF, Matrix.ScaleToFit.CENTER)
        matrix.mapRect(windowRectF)

        val bg = Bitmap.createBitmap(point.x, point.y, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bg)
        val paint = Paint()

        bitmapRect.set(
            windowRectF.left.toInt(),
            windowRectF.top.toInt(),
            windowRectF.right.toInt(),
            windowRectF.bottom.toInt()
        )

        canvas.drawBitmap(src, bitmapRect, windowRect, paint)
        return bg
    }

    internal fun translateToFixedWallpaper(src: Bitmap, context: Context): Bitmap {
        var src = src
        val point = getWindowSize(context)
        if (src.width == point.x && src.height == point.y) {
            return src
        } else {
            src = getSameRatioBitmap(src, point.x, point.y)
        }

        val bg = Bitmap.createBitmap(point.x, point.y, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bg)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        val dst = Rect(0, 0, point.x, point.y)
        canvas.drawBitmap(src, null, dst, paint)
        return bg
    }

    internal fun getSameRatioBitmap(src: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap {
        var src = src
        val result: Bitmap
        val srcWidth = src.width
        val srcHeight = src.height

        val srcRatio = srcHeight / srcWidth.toFloat()
        val screenRatio = screenHeight / screenWidth.toFloat()
        if (srcRatio == screenRatio) {
            if (srcWidth > screenWidth) {
                val scale = screenWidth / srcWidth.toFloat()
                result = getScaledBitmap(src, scale)
            } else {
                result = src
            }
        } else if (srcRatio < screenRatio) {
            val scale = screenWidth / srcWidth.toFloat()
            src = getScaledBitmap(src, scale)
            val h = src.height
            val w = (h * (screenWidth / screenHeight.toFloat())).toInt()
            val left = (src.width - w) / 2
            val rect = Rect(left, 0, left + w, h)
            result = getAppointedRectBitmap(src, rect, w, h)
        } else {
            val scale = screenHeight / srcHeight.toFloat()
            src = getScaledBitmap(src, scale)
            val w = src.width
            val h = (w * (screenHeight / screenWidth.toFloat())).toInt()
            val top = (src.height - h) / 2
            val rect = Rect(0, top, w, top + h)
            result = getAppointedRectBitmap(src, rect, w, h)
        }
        return result
    }

    internal fun getScaledBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    internal fun getAppointedRectBitmap(
        bitmap: Bitmap,
        src: Rect,
        width: Int,
        height: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        val dst = Rect(0, 0, width, height)
        canvas.drawBitmap(bitmap, src, dst, paint)
        return result
    }

    internal fun translateToScrollWallpaper(src: Bitmap, context: Context): Bitmap {
        val point = getWindowSize(context)
        if (src.width == 2 * point.x && src.height == point.y) {
            return src
        }
        // avoid crash
        if (point.x <= 0 || point.y <= 0) {
            point.x = Dimensions.DEFAULT_DEVICE_SCREEN_WIDTH
            point.y = Dimensions.DEFAULT_DEVICE_SCREEN_HEIGHT
        }
        val bg = Bitmap.createBitmap(2 * point.x, point.y, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bg)
        val pfd = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        canvas.drawFilter = pfd
        val dst = Rect(0, 0, 2 * point.x, point.y)
        canvas.drawBitmap(src, null, dst, paint)
        return bg
    }

    fun canScroll(context: Context, wallpaper: Bitmap?): Boolean {
        if (wallpaper == null || wallpaper.isRecycled) {
            return false
        }
        val width = wallpaper.width
        val height = wallpaper.height
        val wallpaperRatio = width.toFloat() / height.toFloat()
        val size = WallpaperUtils.getWindowSize(context)
        val windowRatio = size.x.toFloat() / size.y.toFloat()
        val detla = wallpaperRatio / windowRatio

        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "wallpaper width " + width + " height " + height + " windowsSize width " + size.x + " height " + size.y
            )
            Log.i(TAG, "wallpaper rate $wallpaperRatio windowRatio $windowRatio detla $detla")
        }

        /**
         * 1.125 = 1440/1280 Our wallpaper's size is 1440*1280
         */
        return if (Math.abs(detla - 2) <= 0.05 || Math.abs(wallpaperRatio - 1.125) <= 0.05) {
            true
        } else {
            false
        }
    }

    fun getWindowSize(context: Context?): Point {
        val point = Point()
        if (sCachePoint != null) {
            point.set(sCachePoint!!.x, sCachePoint!!.y)
            return point
        }
        var screenTotalHeight = 0
        var screenTotalWidth = 0
        if (context is Activity) {
            val rootView = context.window.decorView
            screenTotalHeight = rootView.height
            screenTotalWidth = rootView.width
        }
        if (screenTotalWidth != 0 && screenTotalHeight != 0) {
            point.x = screenTotalWidth
            point.y = screenTotalHeight
            sCachePoint = Point()
            sCachePoint!!.x = screenTotalWidth
            sCachePoint!!.y = screenTotalHeight
        } else {
            point.x = Dimensions.getPhoneWidth(context)
            point.y = Dimensions.getPhoneHeight(context)
        }
        return point
    }

    fun suggestWallpaperDimension(
        res: Resources,
        windowManager: WindowManager,
        wallpaperManager: WallpaperManagerProxy, fallBackToDefaults: Boolean
    ) {
        val defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(res, windowManager)

        val savedWidth: Int
        val savedHeight: Int

        if (!fallBackToDefaults) {
            return
        } else {
            savedWidth = defaultWallpaperSize?.x ?: 0
            savedHeight = defaultWallpaperSize?.y ?: 0
        }

        if (savedWidth != wallpaperManager.desiredMinimumWidth || savedHeight != wallpaperManager.desiredMinimumHeight) {
            wallpaperManager.suggestDesiredDimensions(savedWidth, savedHeight)
        }
    }

    /**
     * As a ratio of screen height, the total distance we want the parallax effect to span
     * horizontally
     */
    fun wallpaperTravelToScreenWidthRatio(width: Int, height: Int): Float {
        val aspectRatio = width / height.toFloat()

        // At an aspect ratio of 16/10, the wallpaper parallax effect should span 1.5 * screen width
        // At an aspect ratio of 10/16, the wallpaper parallax effect should span 1.2 * screen width
        // We will use these two data points to extrapolate how much the wallpaper parallax effect
        // to span (ie travel) at any aspect ratio:

        val ASPECT_RATIO_LANDSCAPE = 16 / 10f
        val ASPECT_RATIO_PORTRAIT = 10 / 16f
        val WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.5f
        val WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.2f

        // To find out the desired width at different aspect ratios, we use the following two
        // formulas, where the coefficient on x is the aspect ratio (width/height):
        //   (16/10)x + y = 1.5
        //   (10/16)x + y = 1.2
        // We solve for x and y and end up with a final formula:
        val x =
            (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT) / (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT)
        val y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT
        return x * aspectRatio + y
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun getDefaultWallpaperSize(res: Resources, windowManager: WindowManager): Point? {
        if (sDefaultWallpaperSize == null) {
            val minDims = Point()
            val maxDims = Point()
            windowManager.defaultDisplay.getCurrentSizeRange(minDims, maxDims)

            var maxDim = Math.max(maxDims.x, maxDims.y)
            var minDim = Math.max(minDims.x, minDims.y)

            if (ATLEAST_JB_MR1) {
                val realSize = Point()
                windowManager.defaultDisplay.getRealSize(realSize)
                maxDim = Math.max(realSize.x, realSize.y)
                minDim = Math.min(realSize.x, realSize.y)
            }

            // We need to ensure that there is enough extra space in the wallpaper
            // for the intended parallax effects
            val defaultWidth: Int
            val defaultHeight: Int
            if (res.configuration.smallestScreenWidthDp >= 720) {
                defaultWidth = (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim)).toInt()
                defaultHeight = maxDim
            } else {
                defaultWidth = Math.max((minDim * WALLPAPER_SCREENS_SPAN).toInt(), maxDim)
                defaultHeight = maxDim
            }
            sDefaultWallpaperSize = Point(defaultWidth, defaultHeight)
        }
        return sDefaultWallpaperSize
    }

    /*public static boolean textColorLightForWallPaper(Bitmap bitmap) {
        try {
            Palette palette = Palette.from(bitmap).clearFilters().maximumColorCount(COLOR_SAMPLE_COUNT).generate();
            return textColorLightForWallPaper(palette);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private static boolean textColorLightForWallPaper(Palette palette) {
        int size = palette.getSwatches().size();
        if (size == 0) {
            return true;
        }

        float max = 0;
        float min = 1;
        float lightTemp;

        float totalLightness = 0;
        long totalPop = 0;
        for (Palette.Swatch swatch : palette.getSwatches()) {
            HSLog.d("Palette", "WallPaper swatch: " + swatch);
            lightTemp = swatch.getHsl()[2];
            if (max < lightTemp) {
                max = lightTemp;
            }
            if (min > lightTemp) {
                min = lightTemp;
            }
            totalLightness += lightTemp * swatch.getPopulation();
            totalPop += swatch.getPopulation();
        }
        float lightness = totalLightness / (float) totalPop;
        HSLog.d("Palette", "WallPaper lightness average : " + lightness + ", max = " + max + ", min = " + min);

        // When the brightest swatch is bright enough, we loosen requirement for average lightness
        if (max > 0.89f && lightness > 0.57f) return false;
        if (max > 0.85f && lightness > 0.64f) return false;
        if (max > 0.80f && lightness > 0.71f) return false;

        // Basic threshold for average lightness
        return lightness < 0.74f;
    }*/
}
