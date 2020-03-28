package com.newmoon.dark.ui

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.newmoon.adengine.base.NmAdConfig
import com.newmoon.adengine.base.NmAdListener
import com.newmoon.adengine.interstitial.InterstitialAdManager
import com.newmoon.common.util.BackgroundDrawables
import com.newmoon.common.util.Dimensions
import com.newmoon.common.util.Toasts
import com.newmoon.dark.BuildConfig
import com.newmoon.dark.R
import com.newmoon.dark.ad.AD_PLACEMENT_WALLPAPER_EXIT
import com.newmoon.dark.ad.AdConfig
import com.newmoon.dark.logEvent
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.util.ActivityUtils
import com.newmoon.dark.util.RemoteConfig
import com.newmoon.dark.util.fromJson
import com.newmoon.dark.view.PopupView
import com.newmoon.dark.view.PreviewViewPage
import com.newmoon.dark.wallpaper.WallpaperInfo
import com.newmoon.dark.wallpaper.WallpaperUtils
import com.newmoon.dark.wallpaper.getWallpapers
import java.util.*


class WallpaperPreviewActivity : WallpaperBaseActivity(), ViewPager.OnPageChangeListener,
    PreviewViewPage.PreviewPageListener, OnClickListener {

    private var mInitialized: Boolean = false
    private var mIsGuide: Boolean = false
    private var mIsOnLineWallpaper: Boolean = false
    private var mPaperIndex: Int = 0
    private var mWallpaperMode = MODE_LOCAL_WALLPAPER
    internal var sumPositionAndPositionOffset: Float = 0.toFloat()

    lateinit internal var mViewPager: ViewPager
    private var mWallpapers: MutableList<Any>? = ArrayList()
    override var currentWallpaper: WallpaperInfo? = null
        private set(wallpaper) {
            field = wallpaper
        }
    private var mSetWallpaperButton: TextView? = null
    private var mWallpaperName: TextView? = null
    private var mMyDialog: ProgressDialog? = null
    private var mEdit: ImageView? = null
    private var mReturnArrow: View? = null
    private var mMenuPopupView: PopupView? = null
    private var mAdapter: PreviewViewPagerAdapter? = null
    private val mScrollEventLogger = ScrollEventLogger()
    private val mLoadMap = SparseBooleanArray()

    //selectZoomBtn true zoom_out 缩小 mIsCenterCrop state true
    //selectZoomBtn false zoom_in 放大 mIsCenterCrop state false
    private var mZoomBtn: ImageView? = null
    private var mIsCenterCrop = false

    private val mCurrentRequestCount: Int = 0
    private var mStartIndex = 0
    private var mMaxVisiblePosition: Int = 0
    private var mDestroying = false
    private var mPackageGuideLeftAnimator: ValueAnimator? = null
    private var mPackageGuideRightAnimator: ValueAnimator? = null
    private var mIsGuideInterrupted = false

    private var mZoomAnimator: ValueAnimator? = null

    private val isSucceed: Boolean
        get() = mLoadMap.get(mPaperIndex)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview)
        initView()
        mIsCenterCrop = true
        if (true) {
            if (initData()) {
                refreshButtonState()
            } else {
                finish()
                return
            }
        }

        if (!BillingManager.isPremiumUser()
            && RemoteConfig.instance.getBoolean("AdWallpaperPreviewExitWireEnabled")
        ) {
            InterstitialAdManager.preload(
                AD_PLACEMENT_WALLPAPER_EXIT,
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getWallpaperExitAdConfig())
            )
        }
    }

    private fun showExitWire() {
        if (!BillingManager.isPremiumUser()
            && RemoteConfig.instance.getBoolean("AdWallpaperPreviewExitWireEnabled")
        ) {
            logEvent(this, "WallpaperPreview_ExitWireAd_ShouldShow")
            var ad = InterstitialAdManager.fetch(AD_PLACEMENT_WALLPAPER_EXIT)
            if (ad != null && ad.isLoaded) {
                ad.listener = object : NmAdListener {
                    override fun onAdFailedToLoad(var1: Int) {

                    }

                    override fun onAdOpened() {
                    }

                    override fun onAdLoaded() {

                    }

                    override fun onAdClicked() {

                    }

                    override fun onAdImpression() {

                    }

                    override fun onAdClose() {
                    }
                }
                ad.show()
                logEvent(this, "WallpaperPreview_ExitWireAd_Show")
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showExitWire()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(INTENT_KEY_INDEX, mPaperIndex)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPaperIndex = savedInstanceState.getInt(INTENT_KEY_INDEX)
    }

    private fun initView() {
        mReturnArrow = findViewById(R.id.wallpaper_view_return)
        mReturnArrow!!.setOnClickListener(this)
        mEdit = findViewById(R.id.preview_edit_btn)
        mWallpaperName = findViewById(R.id.wallpaper_name)
        mEdit!!.setOnClickListener(this)
        //mEdit!!.setImageResource(R.drawable.wallpaper_edit_svg)
        mSetWallpaperButton = findViewById(R.id.set_wallpaper_button) as TextView
        mSetWallpaperButton!!.setOnClickListener(this)
        mSetWallpaperButton!!.background = BackgroundDrawables.createBackgroundDrawable(
            0x88000000.toInt(),
            0f,
            true
        )

        var param =
            findViewById<View>(R.id.toolbar_container).layoutParams as RelativeLayout.LayoutParams
        param.topMargin = Dimensions.getStatusBarHeight(this)

        mMenuPopupView = PopupView(this)
        val reportContainer = LayoutInflater.from(this).inflate(
            R.layout.layout_wallpaper_preview_menu_settings,
            findViewById(R.id.container),
            false
        )

        mMenuPopupView!!.setOutSideBackgroundColor(Color.TRANSPARENT)
        mMenuPopupView!!.setContentView(reportContainer)
        mMenuPopupView!!.setOutSideClickListener(OnClickListener {
            mMenuPopupView!!.dismiss()
        })
        val report = reportContainer.findViewById<View>(R.id.tv_report)
        report.setOnClickListener { view: View ->
            mMenuPopupView!!.dismiss()
            //todo: report
        }

        mZoomBtn = findViewById(R.id.preview_zoom_btn) as ImageView
        mZoomBtn!!.setOnClickListener(this)


        selectZoomBtn(!mIsCenterCrop)

        if (mIsOnLineWallpaper) {
            mZoomBtn!!.visibility = View.VISIBLE
        } else {
            mZoomBtn!!.visibility = View.INVISIBLE
        }

        mViewPager = findViewById(R.id.preview_view_pager) as ViewPager
        mAdapter = PreviewViewPagerAdapter()
        mViewPager.setAdapter(mAdapter)
        mViewPager.setFocusable(true)
        mViewPager.setClickable(true)
        mViewPager.setLongClickable(true)
        mViewPager.addOnPageChangeListener(this)
        mViewPager.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && mPackageGuideLeftAnimator != null) {
                mIsGuideInterrupted = true
            }
            false
        })
    }

    private fun selectZoomBtn(isSelected: Boolean) {
        if (isSelected) {
            mZoomBtn!!.setImageResource(
                R.drawable.wallpaper_zoom_out
            )
        } else {
            mZoomBtn!!.setImageResource(
                R.drawable.wallpaper_zoom_in
            )
        }
    }

    private fun setViewsVisibility(visibility: Int) {
        mSetWallpaperButton!!.visibility = visibility
        mZoomBtn!!.visibility = visibility
        mEdit!!.visibility = visibility
        val draw = findViewById<View>(R.id.preview_guide_draw_view)
        draw?.visibility = visibility
    }

    override fun onStop() {
        super.onStop()
        if (mMyDialog != null && !ActivityUtils.isDestroyed(this)) {
            mMyDialog!!.dismiss()
            mMyDialog = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDestroying = true

        if (mPackageGuideLeftAnimator != null) mPackageGuideLeftAnimator!!.cancel()
        if (mPackageGuideRightAnimator != null) mPackageGuideRightAnimator!!.cancel()

    }

    override fun onPageScrollStateChanged(arg0: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (mPackageGuideLeftAnimator == null && positionOffset != 0f) {
            mIsGuideInterrupted = true
        }
        if (position + positionOffset > sumPositionAndPositionOffset) {
            mScrollEventLogger.prepareLeft()
        } else if (position != 0 || positionOffset != 0f || sumPositionAndPositionOffset != 0f || positionOffsetPixels != 0) {
            mScrollEventLogger.prepareRight()
        }
        sumPositionAndPositionOffset = position + positionOffset
    }

    override fun onPageSelected(position: Int) {
        var index = position

        setViewsVisibility(View.VISIBLE)
        mReturnArrow!!.visibility = View.VISIBLE
        mPaperIndex = index
        currentWallpaper = getWallpaperInfoByIndex(mPaperIndex) as WallpaperInfo?
        refreshButtonState()

        mScrollEventLogger.tryLogScrollLeftEvent()
        mScrollEventLogger.tryLogScrollRightEvent()
        if (isFirst) {
            isFirst = false
        } else {
            logEvent(this, "Wallpaper_Preview_Selected")
        }
        logEvent(this, "Wallpaper_Preview_Wallpaper_Shown")
    }

    var isFirst = true

    override fun refreshButtonState() {
        if (currentWallpaper == null) {
            return
        }
        mEdit!!.visibility = View.VISIBLE
        if (isSucceed) {
            mEdit!!.alpha = 1f
            mEdit!!.isClickable = true
            mZoomBtn!!.alpha = 1f
            mZoomBtn!!.isClickable = true
        } else {
            mEdit!!.alpha = 0.5f
            mEdit!!.isClickable = false
            mZoomBtn!!.alpha = 0.5f
            mZoomBtn!!.isClickable = false
        }
        mWallpaperName?.text = currentWallpaper!!.name
        selectZoomBtn(!mIsCenterCrop)
        if (mIsOnLineWallpaper) {
            mZoomBtn!!.visibility = View.VISIBLE
        } else {
            mZoomBtn!!.visibility = View.INVISIBLE
        }
        mSetWallpaperButton!!.visibility = View.VISIBLE
        mSetWallpaperButton!!.setText(R.string.online_wallpaper_apply_btn)
        val isWallpaperReady = isSucceed && !isSettingWallpaper

        if (isWallpaperReady) {
            if (mIsGuide) {
                mIsGuide = false
                startGuideAnimation(
                    this@WallpaperPreviewActivity,
                    R.id.preview_guide_draw_view,
                    Dimensions.pxFromDp(300f),
                    Dimensions.pxFromDp(200f)
                )
            }
            mSetWallpaperButton!!.setTextColor(-0x1)
        } else {
            mSetWallpaperButton!!.setTextColor(-0x7f000001)
        }
    }

    private fun initData(): Boolean {

        mPaperIndex = intent.getIntExtra(INTENT_KEY_INDEX, 0)
        val wallpapers = getWallpapers()
        mWallpapers!!.clear()
        mWallpapers!!.addAll(wallpapers)
        mIsOnLineWallpaper = true

        if (mWallpaperMode == MODE_LOCAL_WALLPAPER) {
            if (mWallpapers!!.isEmpty()) {
                finish()
                return false
            }
            prepareData()
        }
        mInitialized = true
        return true
    }

    private fun prepareData() {
        currentWallpaper = getWallpaperInfoByIndex(mPaperIndex) as WallpaperInfo?
        mAdapter!!.notifyDataSetChanged()
        mViewPager.setCurrentItem(mPaperIndex, false)
        if (mPaperIndex == 0) {
            // Manually invoke this as view pager will not do it for us in such case
            onPageSelected(0)
        }

        // todo:
        mIsGuide = false
    }

    private fun getWallpaperInfoByIndex(index: Int): Any? {
        return if (mWallpapers!!.size == 0) {
            null
        } else if (index < 0) {
            mWallpapers!![0]
        } else if (index >= mWallpapers!!.size) {
            mWallpapers!![mWallpapers!!.size - 1]
        } else {
            mWallpapers!![index]
        }
    }

    private fun displayPage(index: Int, page: PreviewViewPage) {
        val imageView = page.largeWallpaperImageView
        page.retryLayout.setVisibility(View.INVISIBLE)
        val info = getWallpaperInfoByIndex(index) as WallpaperInfo?
        var uri: Uri? = null
        var thumbUrl: String? = null
        uri = Uri.parse(info!!.bigPicture)
        thumbUrl = info.thumbnail
        if (uri == null) {
            return
        }

        val thumbRequest = Glide.with(this).asBitmap().load(thumbUrl)
        thumbRequest.listener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                @Nullable e: GlideException?, model: Any,
                target: Target<Bitmap>,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                bitmap: Bitmap,
                model: Any,
                target: Target<Bitmap>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                imageView.setImageBitmap(bitmap)
                val page = imageView.getTag() as PreviewViewPage
                page.w = bitmap.width
                page.h = bitmap.height

                if (mIsCenterCrop && mIsOnLineWallpaper) {
                    imageView.setImageMatrix(
                        WallpaperUtils.centerCrop(
                            bitmap.width, bitmap.height,
                            imageView
                        )
                    )
                } else {
                    if (mEdit!!.bottom == 0 || mSetWallpaperButton!!.top == 0) {
                        imageView.setImageMatrix(
                            WallpaperUtils.centerInside(
                                bitmap.width,
                                bitmap.height,
                                Dimensions.pxFromDp(80f) + TOP_MARGIN,
                                resources.displayMetrics.heightPixels - Dimensions.pxFromDp(68f) - TOP_MARGIN
                            )
                        )
                    } else {
                        imageView.setImageMatrix(
                            WallpaperUtils.centerInside(
                                bitmap.width, bitmap.height,
                                mEdit!!.bottom + TOP_MARGIN, mSetWallpaperButton!!.top - TOP_MARGIN
                            )
                        )
                    }
                }
                return true
            }
        }).diskCacheStrategy(DiskCacheStrategy.DATA)

        Glide.with(this@WallpaperPreviewActivity).asBitmap().load(uri).thumbnail(thumbRequest)
            .into(CustomImageLoadingTarget(imageView))
    }

    private fun animate(imageView: ImageView, oldMatrix: Matrix, newMatrix: Matrix) {
        var duration = IMAGE_ZOOM_DURATION
        val oldAnimator = mZoomAnimator
        if (oldAnimator != null && oldAnimator.isRunning) {
            duration = (oldAnimator.animatedFraction * oldAnimator.duration).toLong()
            oldAnimator.cancel()
        }
        mZoomAnimator = ObjectAnimator.ofObject(
            imageView, "imageMatrix",
            MatrixEvaluator(), oldMatrix, newMatrix
        )
        mZoomAnimator!!.duration = duration
        mZoomAnimator!!.start()
    }

    private class MatrixEvaluator : TypeEvaluator<Matrix> {
        private val mStartVal = FloatArray(9)
        private val mEndVal = FloatArray(9)
        private val mInterVal = FloatArray(9)

        override fun evaluate(fraction: Float, start: Matrix, end: Matrix): Matrix {
            start.getValues(mStartVal)
            end.getValues(mEndVal)
            for (i in 0..8) {
                mInterVal[i] = (1f - fraction) * mStartVal[i] + fraction * mEndVal[i]
            }
            val inter = Matrix()
            inter.setValues(mInterVal)
            return inter
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.wallpaper_view_return -> {
                finish()
                showExitWire()
            }
            R.id.preview_zoom_btn -> {
                for (i in 0 until mViewPager.getChildCount()) {
                    if (mViewPager.getChildAt(i) !is PreviewViewPage) {
                        continue
                    }
                    val page = mViewPager.getChildAt(i) as PreviewViewPage
                    val matrix: Matrix
                    if (mIsCenterCrop) {
                        matrix = WallpaperUtils.centerInside(
                            page.w, page.h,
                            mEdit!!.bottom + TOP_MARGIN, mSetWallpaperButton!!.top - TOP_MARGIN
                        )
                    } else {
                        matrix = WallpaperUtils.centerCrop(
                            page.w,
                            page.h,
                            page.largeWallpaperImageView
                        )
                    }
                    if (page.getTag() as Int == mPaperIndex) {
                        animate(
                            page.largeWallpaperImageView,
                            page.largeWallpaperImageView.getImageMatrix(),
                            matrix
                        )
                    } else {
                        page.largeWallpaperImageView.setImageMatrix(matrix)
                    }
                }

                selectZoomBtn(mIsCenterCrop)
                mIsCenterCrop = !mIsCenterCrop
                logEvent(this, "Wallpaper_Preview_Zoom")
            }
            R.id.preview_edit_btn -> {
                if (currentWallpaper == null) {
                    return
                }
                val intent = WallpaperEditActivity.getLaunchIntent(this, currentWallpaper!!)
                startActivity(intent)
                logEvent(this, "Wallpaper_Preview_Edit")

            }
            R.id.set_wallpaper_button -> {
                if (currentWallpaper == null) {
                    return
                }

                val isWallpaperReady = isSucceed && !isSettingWallpaper
                if (!isWallpaperReady) {
                    Toasts.showToast(R.string.online_wallpaper_loading)
                    return
                }


                mSetWallpaperButton!!.setTextColor(-0x7f000001)
                mSetWallpaperButton!!.isClickable = false

                applyWallpaper(
                    true,
                    false
                )
                logEvent(this, "Wallpaper_Preview_Apply")
            }
        }
    }

    override fun applyWallPaperFinish(isScroll: Boolean) {
        showExitWire()
        finish()
    }

    /*fun onReceive(s: String, hsBundle: HSBundle) {
        if (WallpaperEditActivity.NOTIFICATION_WALLPAPER_APPLIED_FROM_EDIT.equals(s)) {
            finish()
        }
    }*/

    override fun tryGetWallpaperToSet(): Bitmap? {
        if (currentWallpaper == null) {
            return null
        }
        var wallpaper: Bitmap? = null
        for (i in 0 until mViewPager.getChildCount()) {
            if (mViewPager.getChildAt(i) !is PreviewViewPage) {
                continue
            }
            val page = getPreviewPage(i)
            if (page != null && page.tag as Int == mPaperIndex) {
                val drawable = page.largeWallpaperImageView.drawable as BitmapDrawable
                wallpaper = drawable.bitmap
                break
            }
        }
        return wallpaper
    }

    @Nullable
    private fun getPreviewPage(index: Int): PreviewViewPage? {
        val view = mViewPager.getChildAt(index)
        return if (view is PreviewViewPage) {
            view as PreviewViewPage
        } else null
    }

    override fun onRetryButtonPressed(page: PreviewViewPage) {
        val index = page.getTag() as Int
        displayPage(index, page)
    }

    private fun getPreviewPage(view: ViewGroup, paperIndex: Int): PreviewViewPage {
        val page = this.layoutInflater.inflate(
            R.layout.item_wallpaper_page,
            view,
            false
        ) as PreviewViewPage
        page.setListener(this)
        page.setTag(paperIndex)
        page.largeWallpaperImageView.setTag(page)
        return page
    }

    inner class PreviewViewPagerAdapter : PagerAdapter() {
        override fun getCount(): Int {
            if (null == mWallpapers) {
                return 0
            }
            return mWallpapers!!.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun instantiateItem(view: ViewGroup, position: Int): Any {
            var index = position

            mMaxVisiblePosition = Math.max(mMaxVisiblePosition, index)

            val page = getPreviewPage(view, index)
            displayPage(index, page)
            view.addView(page, 0)
            return page
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
        }
    }

    private inner class ScrollEventLogger {
        private var mLeftEnabled: Boolean = false
        private var mRightEnabled: Boolean = false

        internal fun prepareLeft() {
            mLeftEnabled = true
            mRightEnabled = false
        }

        internal fun prepareRight() {
            mLeftEnabled = false
            mRightEnabled = true
        }

        internal fun reset() {
            mLeftEnabled = false
            mRightEnabled = false
        }

        internal fun tryLogScrollLeftEvent() {
            if (mLeftEnabled) {
                reset()
            }
        }

        internal fun tryLogScrollRightEvent() {
            reset()
        }
    }

    private inner class CustomImageLoadingTarget(view: ImageView) : ImageViewTarget<Bitmap>(view) {

        private var mShouldLogEvent: Boolean = false

        override fun onLoadStarted(placeholder: Drawable?) {
            super.onLoadStarted(placeholder)
            if (view == null) {
                return
            }
            val page = view.tag as PreviewViewPage
            page.loadingView.setVisibility(View.VISIBLE)
            page.retryLayout.setVisibility(View.INVISIBLE)
            mLoadMap.put(page.getTag() as Int, false)
            mShouldLogEvent = true
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            super.onLoadFailed(errorDrawable)
            if (view == null) {
                return
            }
            val page = view.tag as PreviewViewPage
            mLoadMap.put(page.getTag() as Int, false)
            page.postDelayed(Runnable {
                page.loadingView.setVisibility(View.INVISIBLE)
                page.retryLayout.setVisibility(View.VISIBLE)
                page.largeWallpaperImageView.setImageResource(android.R.color.transparent)
                mSetWallpaperButton!!.visibility = View.INVISIBLE
            }, 600)
            if (mShouldLogEvent) {
                mShouldLogEvent = false
            }
        }

        override fun onResourceReady(bitmap: Bitmap, @Nullable transition: Transition<in Bitmap>?) {
            if (view == null) {
                return
            }
            view.setImageBitmap(bitmap)
            val page = view.tag as PreviewViewPage
            page.w = bitmap.width
            page.h = bitmap.height


            if (mIsCenterCrop && mIsOnLineWallpaper) {
                (view as ImageView).imageMatrix =
                    WallpaperUtils.centerCrop(bitmap.width, bitmap.height, view)
            } else {
                if (mEdit!!.bottom == 0 || mSetWallpaperButton!!.top == 0) {
                    (view as ImageView).imageMatrix = WallpaperUtils.centerInside(
                        bitmap.width,
                        bitmap.height,
                        Dimensions.pxFromDp(80f) + TOP_MARGIN,
                        resources.displayMetrics.heightPixels - Dimensions.pxFromDp(68f) - TOP_MARGIN
                    )
                } else {
                    (view as ImageView).imageMatrix = WallpaperUtils.centerInside(
                        bitmap.width, bitmap.height,
                        mEdit!!.bottom + TOP_MARGIN, mSetWallpaperButton!!.top - TOP_MARGIN
                    )
                }
            }
            mLoadMap.put(page.getTag() as Int, true)
            refreshButtonState()
            page.loadingView.setVisibility(View.INVISIBLE)
            page.retryLayout.setVisibility(View.INVISIBLE)
            if (mShouldLogEvent) {
                mShouldLogEvent = false
            }
        }

        override fun setResource(bitmap: Bitmap?) {

        }
    }

    companion object {

        private val TAG = WallpaperPreviewActivity::class.java.simpleName
        private val DEBUG = true && BuildConfig.DEBUG

        val INTENT_KEY_WALLPAPERS = "wallpapers"
        val INTENT_KEY_INDEX = "index"

        private val MODE_LOCAL_WALLPAPER = 1

        private val TOP_MARGIN = Dimensions.pxFromDp(15f)

        private val IMAGE_ZOOM_DURATION: Long = 230

        //ad related
        private val MAX_CONCURRENT_AD_REQUEST_COUNT = 3

        fun getLaunchIntent(
            context: Context, position: Int
        ): Intent {
            val intent = Intent(context, WallpaperPreviewActivity::class.java)
            intent.putExtra(INTENT_KEY_INDEX, position)
            return intent
        }
    }
}
