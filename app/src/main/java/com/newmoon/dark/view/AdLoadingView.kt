package com.newmoon.dark.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import com.newmoon.common.util.BackgroundDrawables
import com.newmoon.common.util.Dimensions
import com.newmoon.dark.R

class AdLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private var rotatingAnimation: Animation? = null
    private var progressView: ImageView? = null

    init {
        init()
    }

    fun init() {
        LayoutInflater.from(context).inflate(R.layout.ad_loading_view, this)
        rotatingAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        findViewById<View>(R.id.bg_view).setBackgroundDrawable(
            BackgroundDrawables.createBackgroundDrawable(
                0xc0000000.toInt(),
                Dimensions.pxFromDp(4f).toFloat(),
                false
            )
        )
        progressView = findViewById(R.id.dialog_loading_image_view)
        progressView!!.startAnimation(rotatingAnimation)
    }

    fun setLoadingText(text: String) {
        findViewById<TextView>(R.id.loading_text).text = text
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        progressView!!.clearAnimation()
        rotatingAnimation = null
    }
}
