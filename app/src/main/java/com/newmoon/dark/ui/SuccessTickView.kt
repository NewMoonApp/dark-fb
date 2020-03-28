package com.newmoon.dark.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

import com.newmoon.common.util.Dimensions


class SuccessTickView : View {

    private var mMaxLeftRectWidth: Float = 0.toFloat()
    private var mLeftRectWidth: Float = 0.toFloat()
    private var mRightRectWidth: Float = 0.toFloat()
    private var mLeftRectGrowMode: Boolean = false
    private var mPaint: Paint? = null
    private var mInternalAnimationListener: AnimatorListenerAdapter? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        mPaint = Paint()
        mPaint!!.color = Color.WHITE
        mLeftRectWidth = CONST_LEFT_RECT_W
        mRightRectWidth = CONST_RIGHT_RECT_W
        mLeftRectGrowMode = false
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        var totalW = width
        var totalH = height

        // rotate canvas first
        canvas.rotate(45f, (totalW / 2).toFloat(), (totalH / 2).toFloat())

        totalW = (totalW / 1.2).toInt()
        totalH = (totalH / 1.4).toInt()
        mMaxLeftRectWidth = (totalW + CONST_LEFT_RECT_W) / 2 + CONST_RECT_WEIGHT - 1

        val leftRect = RectF()
        if (mLeftRectGrowMode) {
            leftRect.left = 0f
            leftRect.right = leftRect.left + mLeftRectWidth
            leftRect.top = (totalH + CONST_RIGHT_RECT_W) / 2
            leftRect.bottom = leftRect.top + CONST_RECT_WEIGHT
        } else {
            leftRect.right = (totalW + CONST_LEFT_RECT_W) / 2 + CONST_RECT_WEIGHT - 1
            leftRect.left = leftRect.right - mLeftRectWidth
            leftRect.top = (totalH + CONST_RIGHT_RECT_W) / 2
            leftRect.bottom = leftRect.top + CONST_RECT_WEIGHT
        }

        canvas.drawRoundRect(leftRect, CONST_RADIUS, CONST_RADIUS, mPaint!!)

        val rightRect = RectF()
        rightRect.bottom = (totalH + CONST_RIGHT_RECT_W) / 2 + CONST_RECT_WEIGHT - 1
        rightRect.left = (totalW + CONST_LEFT_RECT_W) / 2
        rightRect.right = rightRect.left + CONST_RECT_WEIGHT
        rightRect.top = rightRect.bottom - mRightRectWidth
        canvas.drawRoundRect(rightRect, CONST_RADIUS, CONST_RADIUS, mPaint!!)
    }

    fun setInternalAnimationListener(internalAnimationListener: AnimatorListenerAdapter) {
        mInternalAnimationListener = internalAnimationListener
    }

    fun startTickAnim() {
        // hide tick
        mLeftRectWidth = 0f
        mRightRectWidth = 0f
        invalidate()
        val animator = ObjectAnimator.ofFloat(this, "tickPosition", 0f, 1f)
        animator.addListener(mInternalAnimationListener)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 900
        animator.start()
    }

    private fun setTickPosition(interpolatedTime: Float) {
        if (0.54 < interpolatedTime && 0.7 >= interpolatedTime) {  // grow left and right rect to right
            mLeftRectGrowMode = true
            mLeftRectWidth = mMaxLeftRectWidth * ((interpolatedTime - 0.54f) / 0.16f)
            if (0.65 < interpolatedTime) {
                mRightRectWidth = MAX_RIGHT_RECT_W * ((interpolatedTime - 0.65f) / 0.19f)
            }
            invalidate()
        } else if (0.7 < interpolatedTime && 0.84 >= interpolatedTime) { // shorten left rect from right, still grow right rect
            mLeftRectGrowMode = false
            mLeftRectWidth = mMaxLeftRectWidth * (1 - (interpolatedTime - 0.7f) / 0.14f)
            mLeftRectWidth =
                if (mLeftRectWidth < MIN_LEFT_RECT_W) MIN_LEFT_RECT_W else mLeftRectWidth
            mRightRectWidth = MAX_RIGHT_RECT_W * ((interpolatedTime - 0.65f) / 0.19f)
            invalidate()
        } else if (0.84 < interpolatedTime && 1 >= interpolatedTime) { // restore left rect width, shorten right rect to const
            mLeftRectGrowMode = false
            mLeftRectWidth =
                MIN_LEFT_RECT_W + (CONST_LEFT_RECT_W - MIN_LEFT_RECT_W) * ((interpolatedTime - 0.84f) / 0.16f)
            mRightRectWidth =
                CONST_RIGHT_RECT_W + (MAX_RIGHT_RECT_W - CONST_RIGHT_RECT_W) * (1 - (interpolatedTime - 0.84f) / 0.16f)
            invalidate()
        }
    }

    companion object {

        private val CONST_RADIUS = Dimensions.pxFromDp(1.2f).toFloat()
        private val CONST_RECT_WEIGHT = Dimensions.pxFromDp(3f).toFloat()
        private val CONST_LEFT_RECT_W = Dimensions.pxFromDp(15f).toFloat()
        private val CONST_RIGHT_RECT_W = Dimensions.pxFromDp(25f).toFloat()
        private val MIN_LEFT_RECT_W = Dimensions.pxFromDp(Math.PI.toFloat()).toFloat()
        private val MAX_RIGHT_RECT_W = CONST_RIGHT_RECT_W + Dimensions.pxFromDp(3.7f)
    }

}