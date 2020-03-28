package com.newmoon.dark.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

import com.newmoon.dark.R

class PreviewViewPage : FrameLayout {

    lateinit var largeWallpaperImageView: ImageView
    lateinit var loadingView: FrameLayout
    lateinit var retryLayout: LinearLayout
    var w: Int = 0
    var h: Int = 0
    private var listener: PreviewPageListener? = null

    interface PreviewPageListener {
        fun onRetryButtonPressed(page: PreviewViewPage)
    }

    fun setListener(listener: PreviewPageListener) {
        this.listener = listener
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    fun setProgress(progress: String) {

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        this.largeWallpaperImageView =
            findViewById<View>(R.id.large_wallpaper_image_view) as ImageView
        this.loadingView = findViewById<View>(R.id.wallpaper_preview_loading_view) as FrameLayout
        this.retryLayout = findViewById<View>(R.id.retry_downloading_layout) as LinearLayout

        retryLayout.findViewById<View>(R.id.retry_downloading_button).setOnClickListener {
            if (listener != null) {
                listener!!.onRetryButtonPressed(this@PreviewViewPage)
            }
        }
    }

}
