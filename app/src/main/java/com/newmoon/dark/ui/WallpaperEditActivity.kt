package com.newmoon.dark.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable

import androidx.annotation.StringRes
import com.bumptech.glide.Glide

import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.newmoon.common.util.BackgroundDrawables
import com.newmoon.common.util.Dimensions
import com.newmoon.common.util.Threads
import com.newmoon.common.util.Toasts
import com.newmoon.dark.R
import com.newmoon.dark.logEvent
import com.newmoon.dark.util.ActivityUtils
import com.newmoon.dark.wallpaper.WallpaperInfo
import com.newmoon.dark.wallpaper.WallpaperUtils
import com.newmoon.dark.wallpaper.crop.CropImageOptions
import com.newmoon.dark.wallpaper.crop.CropOverlayView

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.experimental.and

class WallpaperEditActivity : WallpaperBaseActivity(), View.OnClickListener {

    private var mBitmapHeight: Int = 0
    private var mBitmapWidth: Int = 0

    private var mWallpaperView: ImageView? = null
    private var mApplyButton: TextView? = null
    private val mMatrixValues = FloatArray(9)
    private var mStartX = 0f
    private var mStartY = 0f
    private var mWidth = 0f
    private var mHeight = 0f
    private var mBitmap: Bitmap? = null
    private var mOverlayBounds: RectF? = null
    private var mCropOverlayView: CropOverlayView? = null
    private var mIsScrollable: Boolean = false

    private var mFixedBtn: View? = null
    private var mScrollBtn: View? = null
    private var mResetBtn: View? = null
    private var mCurrentMatrix: Matrix? = null
    private var mIsFromLocalGallery = false

    private val overlayViewRect: RectF
        get() {
            val point = ratio
            val k = point.x / point.y
            val width = mOverlayBounds!!.width()
            val height = mOverlayBounds!!.height()
            val overlayHeight: Float
            val overlayWidth: Float
            if (mIsScrollable) {
                if (width > height * k) {
                    overlayHeight = height
                    overlayWidth = overlayHeight * k
                } else {
                    overlayWidth = width
                    overlayHeight = width / k
                }
            } else {
                if (height > width / k) {
                    overlayWidth = width
                    overlayHeight = width / k
                } else {
                    overlayHeight = height
                    overlayWidth = height * k
                }
            }
            val overlayRect = RectF(0f, 0f, overlayWidth, overlayHeight)
            val matrix = Matrix()
            matrix.setRectToRect(overlayRect, mOverlayBounds, Matrix.ScaleToFit.CENTER)
            matrix.mapRect(overlayRect)
            return overlayRect
        }

    private val edit: String
        get() {
            val sb = StringBuilder()
            mWallpaperView!!.imageMatrix.getValues(mMatrixValues)
            sb.append("").append(mMatrixValues[Matrix.MSCALE_X]).append(",")
                .append(mMatrixValues[Matrix.MSCALE_Y]).append(",")
                .append(mMatrixValues[Matrix.MSKEW_X]).append(",")
                .append(mMatrixValues[Matrix.MSKEW_Y]).append(",")
                .append(mMatrixValues[Matrix.MTRANS_X]).append(",")
                .append(mMatrixValues[Matrix.MTRANS_Y])
            return sb.toString()
        }

    private val ratio: PointF
        get() {
            val point = WallpaperUtils.getWindowSize(this)
            val pointF = PointF(point)
            if (mIsScrollable) {
                pointF.x = pointF.x * 2
            }
            return pointF
        }

    override val currentWallpaper: WallpaperInfo?
        get() = mCurrentWallpaper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_edit_new)
        mWallpaperView = findViewById(R.id.wallpaper_edit_image)
        mCropOverlayView = findViewById(R.id.wallpaper_overlay_view)
        mApplyButton = findViewById(R.id.wallpaper_edit_apply_button)
        mFixedBtn = findViewById(R.id.wallpaper_edit_fixed)
        mScrollBtn = findViewById(R.id.wallpaper_edit_scroll)
        mResetBtn = findViewById(R.id.wallpaper_edit_reset_button)
        findViewById<View>(R.id.wallpaper_view_return).setOnClickListener(this)
        init()
    }

    private fun bindEvents() {
        mResetBtn!!.setOnClickListener(this)
        mFixedBtn!!.setOnClickListener(this)
        mScrollBtn!!.setOnClickListener(this)
        mApplyButton!!.setOnClickListener(this)
        mCropOverlayView!!.setCropWindowChangeListener { b ->
            if (b) {
                showResetBtn()
            }
        }
    }

    private fun showError(@StringRes messageId: Int) {
        Threads.postOnMainThread(Runnable {
            if (mDialog != null && mDialog!!.isShowing && !ActivityUtils.isDestroyed(this@WallpaperEditActivity)) {
                mDialog!!.dismiss()
            }
            if (messageId != 0) {
                Toasts.showToast(messageId)
            } else {
                Toasts.showToast(R.string.local_wallpaper_pick_error)
            }
            finish()
        })
    }

    private fun isGif(header: ByteArray?): Boolean {
        val stringBuilder = StringBuilder()
        if (header == null || header.size <= 0) {
            return false
        }
        for (aByte in header) {
            val v = aByte and 0xFF.toByte()
            val hv = Integer.toHexString(v.toInt())
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        // 47494638 is hex number for string "gif"
        return stringBuilder.toString().toUpperCase().contains("47494638")
    }

    private fun errorState(msg: String) {
        finish()
    }

    private fun init() {
        val intent = intent
        if (intent.extras == null) {
            errorState("intent.getExtras() == null")
            return
        }
        if (intent.extras!!.containsKey(INTENT_KEY_WALLPAPER_INFO)) {
            mCurrentWallpaper =
                getIntent().getSerializableExtra(INTENT_KEY_WALLPAPER_INFO) as WallpaperInfo?
            mIsFromLocalGallery = false
            initData()
            mIsScrollable = TextUtils.isEmpty(mCurrentWallpaper!!.bigPicture)
        }
        mFixedBtn!!.isSelected = !mIsScrollable
        mScrollBtn!!.isSelected = mIsScrollable
        mApplyButton!!.background = BackgroundDrawables.createBackgroundDrawable(
            0xff5685FD.toInt(),
            Dimensions.pxFromDp(8f).toFloat(),
            true
        )
        hideResetBtn()
    }

    private fun initData() {
        val url: String
        url = mCurrentWallpaper!!.bigPicture
        mWallpaperView!!.isEnabled = false
        Glide.with(this@WallpaperEditActivity).asBitmap().load(url)
            .into(object : ImageViewTarget<Bitmap>(mWallpaperView!!) {

                override fun onResourceReady(resource: Bitmap, @Nullable transition: Transition<in Bitmap>?) {
                    mWallpaperView!!.setImageBitmap(resource)
                    view.postDelayed({ loadBitmapComplete(view, resource) }, 10)
                }

                override fun setResource(@Nullable resource: Bitmap?) {

                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    if (mDialog != null && !ActivityUtils.isDestroyed(this@WallpaperEditActivity)) {
                        mDialog!!.dismiss()
                    }
                    finish()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    super.onLoadCleared(placeholder)
                    if (mDialog != null && !ActivityUtils.isDestroyed(this@WallpaperEditActivity)) {
                        mDialog!!.dismiss()
                    }
                    finish()
                }
            })
        refreshButtonState()
    }

    private fun loadBitmapComplete(view: View, bitmap: Bitmap) {
        mBitmap = bitmap
        val vWidth =
            if (mWallpaperView!!.width == 0) mWallpaperView!!.measuredWidth else mWallpaperView!!.width
        val vHeight =
            if (mWallpaperView!!.height == 0) mWallpaperView!!.measuredHeight else mWallpaperView!!.height
        mCropOverlayView!!.visibility = View.VISIBLE
        centerInside()
        updateOverlayView()
        mWallpaperView!!.isEnabled = true
        bindEvents()
    }

    private fun centerInside() {
        mBitmapWidth = mBitmap!!.width
        mBitmapHeight = mBitmap!!.height
        mOverlayBounds =
            RectF(0f, 0f, mWallpaperView!!.width.toFloat(), mWallpaperView!!.height.toFloat())
        val bitmapRect = RectF(0f, 0f, mBitmapWidth.toFloat(), mBitmapHeight.toFloat())
        mCurrentMatrix = Matrix()
        mCurrentMatrix!!.setRectToRect(bitmapRect, mOverlayBounds, Matrix.ScaleToFit.CENTER)
        mCurrentMatrix!!.mapRect(mOverlayBounds, bitmapRect)
        mWallpaperView!!.imageMatrix = mCurrentMatrix
        initOverlayView()
    }

    private fun initOverlayView() {
        //初始化配置
        mCropOverlayView!!.setInitialAttributeValues(CropImageOptions())
        //GuideLine显示状态
        mCropOverlayView!!.guidelines = CropImageOptions.Guidelines.OFF
        //Crop 形状
        mCropOverlayView!!.cropShape = CropImageOptions.CropShape.RECTANGLE
        //是否多点触控
        mCropOverlayView!!.setMultiTouchEnabled(true)

    }

    private fun updateOverlayViewBounds() {
        val bounds = FloatArray(8)
        mapRectToPoint(mOverlayBounds!!, bounds)
        mCropOverlayView!!.setBounds(
            bounds,
            mOverlayBounds!!.right.toInt(),
            mOverlayBounds!!.bottom.toInt()
        )
        mCropOverlayView!!.setCropWindowLimits(
            mOverlayBounds!!.right,
            mOverlayBounds!!.bottom,
            2f,
            2f
        )
    }

    private fun updateOverlayView() {
        updateOverlayViewBounds()
        val ratio = ratio
        if (mIsScrollable) {
            mCropOverlayView!!.setAspectRatio(ratio.x.toInt(), ratio.y.toInt())
            mCropOverlayView!!.cropWindowRect = overlayViewRect
            mCropOverlayView!!.invalidate()
        } else {
            mCropOverlayView!!.setAspectRatio(ratio.x.toInt(), ratio.y.toInt())
            mCropOverlayView!!.cropWindowRect = overlayViewRect
            mCropOverlayView!!.invalidate()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.wallpaper_edit_reset_button -> {
                reset()
            }
            R.id.wallpaper_edit_apply_button -> apply()
            R.id.wallpaper_view_return -> cancel()
            R.id.wallpaper_edit_fixed -> {
                mIsScrollable = false
                reset()
            }
            R.id.wallpaper_edit_scroll -> {
                mIsScrollable = true
                reset()
            }
        }
    }

    private fun calculateCropParams(matrix: Matrix) {
        val viewRect = RectF(0f, 0f, mBitmapWidth.toFloat(), mBitmapHeight.toFloat())
        matrix.mapRect(viewRect)
        val cropOverlayRectF = mCropOverlayView!!.cropWindowRect
        matrix.getValues(mMatrixValues)
        val scale =
            Math.sqrt((mMatrixValues[Matrix.MSCALE_X] * mMatrixValues[Matrix.MSCALE_Y] - mMatrixValues[Matrix.MSKEW_X] * mMatrixValues[Matrix.MSKEW_Y]).toDouble())
                .toFloat()
        //Anticlockwise rotation
        if (mMatrixValues[Matrix.MSCALE_X] > 0) {
            //0try
            mStartX = Math.abs(cropOverlayRectF.left - viewRect.left) / scale
            mStartY = Math.abs(cropOverlayRectF.top - viewRect.top) / scale

            mWidth = cropOverlayRectF.width() / scale
            mHeight = cropOverlayRectF.height() / scale
        } else if (mMatrixValues[Matrix.MSCALE_X] < 0) {
            // 180
            mStartX = Math.abs(cropOverlayRectF.right - viewRect.right) / scale
            mStartY = Math.abs(cropOverlayRectF.bottom - viewRect.bottom) / scale

            mWidth = cropOverlayRectF.width() / scale
            mHeight = cropOverlayRectF.height() / scale
        } else if (mMatrixValues[Matrix.MSKEW_X] > 0) {
            //90
            mStartX = Math.abs(cropOverlayRectF.bottom - viewRect.bottom) / scale
            mStartY = Math.abs(cropOverlayRectF.left - viewRect.left) / scale

            mWidth = cropOverlayRectF.height() / scale
            mHeight = cropOverlayRectF.width() / scale
        } else if (mMatrixValues[Matrix.MSKEW_X] < 0) {
            // 270
            mStartX = Math.abs(cropOverlayRectF.top - viewRect.top) / scale
            mStartY = Math.abs(cropOverlayRectF.right - viewRect.right) / scale

            mWidth = cropOverlayRectF.height() / scale
            mHeight = cropOverlayRectF.width() / scale
        }
    }

    private fun apply() {
        if (mCurrentWallpaper == null) {
            return
        }
        mApplyButton!!.setTextColor(-0x7f000001)
        mApplyButton!!.isClickable = false

        val edit = edit
        applyWallpaper(mIsScrollable)

        logEvent(this, "Wallpaper_Edit_Apply")

        sendBroadcast(Intent().also {
            it.putExtra("action", NOTIFICATION_WALLPAPER_APPLIED_FROM_EDIT)
        })
    }

    private fun cancel() {
        finish()
    }

    fun reset() {

        // resetImage(mWallpaperView, true);
        // refreshButtonState();
        mFixedBtn!!.isSelected = !mIsScrollable
        mScrollBtn!!.isSelected = mIsScrollable
        updateOverlayView()
        hideResetBtn()
    }

    private fun hideResetBtn() {
        mResetBtn!!.visibility = View.INVISIBLE
    }

    private fun showResetBtn() {
        mResetBtn!!.visibility = View.VISIBLE
    }

    override fun tryGetWallpaperToSet(): Bitmap? {
        if (mCurrentWallpaper == null) {
            return null
        }
        calculateCropParams(mWallpaperView!!.imageMatrix)
        val bitmap = mBitmap

        val startX = Math.round(mStartX)
        val startY = Math.round(mStartY)
        var width = Math.round(mWidth)
        var height = Math.round(mHeight)
        if (startX + width > bitmap!!.width) {
            // Clamp to avoid out-of-range error due to rounding
            width = bitmap.width - startX
        }
        if (startY + height > bitmap.height) {
            height = bitmap.height - startY
        }
        val matrix = Matrix()
        val wallpaper: Bitmap
        try {
            wallpaper = Bitmap.createBitmap(bitmap, startX, startY, width, height, matrix, false)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return wallpaper
    }

    override fun refreshButtonState() {
        mApplyButton!!.isClickable = true
        mApplyButton!!.alpha = 1.0f
    }

    companion object {

        val INTENT_KEY_WALLPAPER_INFO = "wallpaper_info"
        private val TAG = WallpaperEditActivity::class.java.simpleName
        val INTENT_KEY_WALLPAPER_URI = "wallpaperData"

        internal val NOTIFICATION_WALLPAPER_APPLIED_FROM_EDIT = "wallpaper_applied_from_edit"

        private val REQUEST_CODE_SYSTEM_THEME_ALERT = 1


        fun getLaunchIntent(context: Context, wallpaperInfo: WallpaperInfo): Intent {
            val intent = Intent(context, WallpaperEditActivity::class.java)
            intent.putExtra(WallpaperEditActivity.INTENT_KEY_WALLPAPER_INFO, wallpaperInfo)
            return intent
        }

        private fun mapRectToPoint(rect: RectF, points: FloatArray) {
            points[0] = rect.left
            points[1] = rect.top
            points[2] = rect.right
            points[3] = rect.top
            points[4] = rect.right
            points[5] = rect.bottom
            points[6] = rect.left
            points[7] = rect.bottom
        }
    }
}
