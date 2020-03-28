package com.newmoon.dark.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

import com.newmoon.dark.R

/**
 * @author haitao.liu
 */
class SmartSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint: Paint

    private var mLineHeight: Float = 0.toFloat()
    @ColorInt
    private var mNormalLineColor: Int = 0
    @ColorInt
    private var mActiveLineColor: Int = 0

    @ColorInt
    private var mCursorColor: Int = 0
    private var mCursorSize: Float = 0.toFloat()

    private var mMove: Boolean = false

    private var mCurrentProcess: Float = 0f

    private var mListener: OnProgressChangeListener? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SmartSeekBar, defStyleAttr, 0)
        mLineHeight = a.getDimension(R.styleable.SmartSeekBar_ssb_lineHeight, dp2px(8).toFloat())

        mNormalLineColor = a.getColor(R.styleable.SmartSeekBar_ssb_normalLineColor, 0)
        mActiveLineColor = a.getColor(R.styleable.SmartSeekBar_ssb_activeLineColor, 0)

        mCursorColor = a.getColor(R.styleable.SmartSeekBar_ssb_cursorColor, 0)
        mCursorSize = a.getDimension(R.styleable.SmartSeekBar_ssb_cursorSize, dp2px(16).toFloat())

        a.recycle()

        mPaint = Paint()
        mPaint.strokeCap = Paint.Cap.ROUND

        mPaint.strokeWidth = mLineHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = measureWidth(widthMeasureSpec)
        val measuredHeight = measureHeight(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun measureWidth(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        // Default size if no limits are specified.
        var result = minimumWidth
        if (specMode == MeasureSpec.AT_MOST) {
            // apply parentSize
            result = specSize
        } else if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        }
        return result
    }

    private fun measureHeight(measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        // Default size if no limits are specified.
        var result = minimumHeight
        if (specMode == View.MeasureSpec.AT_MOST) { // just set true
            val contentHeight = Math.max(
                mCursorSize,
                mLineHeight
            ) + paddingBottom.toFloat() + paddingTop.toFloat()
            result = Math.max(result.toFloat(), contentHeight).toInt()
        } else if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        }
        return result
    }

    /**
     * Set line height with pixel
     * @param height line height
     *
     * @attr ref R.styleable#SmartSeekBar_ssb_lineHeight
     */
    fun setLineHeight(height: Float) {
        mLineHeight = height
        mPaint.strokeWidth = height
        // maybe need to recalculate height
        requestLayout()
        // redraw line
        invalidate()
    }


    fun setLineColors(@ColorInt normal: Int, @ColorInt active: Int) {
        mNormalLineColor = normal
        mActiveLineColor = active
        invalidate()
    }

    /**
     * Set cursor size
     * @param size cursor width
     *
     * @attr ref R.styleable#SmartSeekBar_ssb_cursorSize
     */
    fun setCursorSize(size: Float) {
        mCursorSize = size
        // maybe need to recalculate height
        requestLayout()
        invalidate()
    }


    fun setProgress(progress: Float) {
        mCurrentProcess = progress
        invalidate()

        notifyProcessChange(false)
    }

    fun getProgress(): Float {
        return mCurrentProcess
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val action = event.action

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                mMove = true
                mCurrentProcess = calculateProcess(x)
                parent.requestDisallowInterceptTouchEvent(true)

                invalidate()
                notifyProcessChange(true)
            }
            MotionEvent.ACTION_UP -> {
                mMove = false
            }
            MotionEvent.ACTION_CANCEL -> mMove = false
        }
        return mMove
    }

    private fun calculateProcess(x: Float): Float {
        val availableWidth = width - paddingStart - paddingEnd
        val normalLineStopX = paddingStart + availableWidth - mCursorSize / 2
        val activeLineStartX = paddingStart + mCursorSize / 2
        var result = (x - activeLineStartX) / normalLineStopX
        result = Math.min(Math.max(result, 0f), 1f)
        return result
    }

    override fun onDraw(canvas: Canvas) {
        // 1 draw bg if set
        super.onDraw(canvas)


        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingStart = paddingStart
        val paddingEnd = paddingEnd

        val availableWidth = width - paddingStart - paddingEnd
        val availableHeight = height - paddingTop - paddingBottom

        // 2 draw line
        val lineStartY = (paddingTop + availableHeight / 2).toFloat()
        val normalLineStopX = paddingStart + availableWidth - mCursorSize / 2

        val activeLineStartX = paddingStart + mCursorSize / 2
        val activeLineStopX =
            activeLineStartX + mCurrentProcess * (normalLineStopX - activeLineStartX)

        // draw active line
        mPaint.color = mActiveLineColor
        canvas.drawLine(
            activeLineStartX,
            lineStartY,
            activeLineStopX,
            lineStartY,
            mPaint
        )
        // draw normal line
        mPaint.color = mNormalLineColor
        canvas.drawLine(
            activeLineStopX,
            lineStartY,
            normalLineStopX,
            lineStartY,
            mPaint
        )

        // 3 draw cursor
        mPaint.color = mCursorColor
        canvas.drawCircle(
            activeLineStopX,
            height / 2.toFloat(),
            mCursorSize / 2,
            mPaint
        )
    }

    fun setOnProgressChangeListener(listener: OnProgressChangeListener) {
        mListener = listener
    }

    private fun notifyProcessChange(fromUser: Boolean) {
        if (mListener != null) {
            mListener!!.onProgressChange(mCurrentProcess, fromUser)
        }
    }

    interface OnProgressChangeListener {
        /**
         *
         * @param progress
         * @param fromUser
         */
        fun onProgressChange(progress: Float, fromUser: Boolean)
    }

    companion object {
        internal fun dp2px(dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                Resources.getSystem().displayMetrics
            ).toInt()
        }

        internal fun sp2px(sp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp.toFloat(),
                Resources.getSystem().displayMetrics
            ).toInt()
        }
    }
}
