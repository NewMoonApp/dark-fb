package com.newmoon.dark.view

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import androidx.annotation.ColorInt
import androidx.annotation.Size

class PopupView(private val mContext: Context, private val mRootView: ViewGroup) {
    protected var mContentViewParent: ContainerView
    protected var mContentView: View? = null
    private val mContentLp: ViewGroup.LayoutParams
    protected var mAnchorView: View? = null

    private var mBgColor = -0x56000000

    private var mOutSideOnClickListener: View.OnClickListener? = null

    constructor(activity: Activity) : this(
        activity,
        activity.findViewById<View>(android.R.id.content) as ViewGroup
    ) {
    }

    init {
        mContentViewParent = ContainerView(mContext)
        mContentViewParent.isFocusableInTouchMode = true
        mContentViewParent.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            // Key code home is never delivered to applications
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss()
                return@OnKeyListener true
            }
            false
        })
        mContentLp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun setContentView(contentView: View) {
        if (mContentView != null) {
            mContentViewParent.removeView(mContentView)
        }
        mContentView = contentView
    }

    protected fun show() {
        mContentViewParent.setBackgroundColor(mBgColor)
        mContentViewParent.setOnClickListener(mOutSideOnClickListener)
        if (mContentViewParent.parent != null) {
            (mContentViewParent.parent as ViewGroup).removeAllViews()
        }
        mRootView.addView(mContentViewParent, mContentLp)
        mContentViewParent.requestFocus()
    }

    fun showInCenter() {
        // Measure content view
        val layoutParams = mContentView!!.layoutParams
        val widthSpec: Int
        if (layoutParams != null && layoutParams.width > 0) {
            widthSpec =
                View.MeasureSpec.makeMeasureSpec(layoutParams.width, View.MeasureSpec.EXACTLY)
        } else {
            widthSpec = View.MeasureSpec.makeMeasureSpec(mRootView.width, View.MeasureSpec.AT_MOST)
        }
        val heightSpec: Int
        if (layoutParams != null && layoutParams.height > 0) {
            heightSpec =
                View.MeasureSpec.makeMeasureSpec(layoutParams.height, View.MeasureSpec.EXACTLY)
        } else {
            heightSpec =
                View.MeasureSpec.makeMeasureSpec(mRootView.height, View.MeasureSpec.AT_MOST)
        }
        mContentView!!.measure(widthSpec, heightSpec)

        val left = (mRootView.width - mContentView!!.measuredWidth) / 2
        val top = (mRootView.height - mContentView!!.measuredHeight) / 2
        showAtPosition(left, top)
    }

    fun setDropDownAnchor(anchor: View) {
        mAnchorView = anchor
    }

    fun showAsDropDown() {
        checkNotNull(mAnchorView) { "Anchor view must not be null" }
        val dropDownPosition = getDropDownPosition(mAnchorView!!, 0, 0)
        showAtPosition(dropDownPosition[0], dropDownPosition[1])
    }

    @JvmOverloads
    fun showAsDropDown(anchor: View, xOffset: Int = 0, yOffset: Int = 0) {
        val dropDownPosition = getDropDownPosition(anchor, xOffset, yOffset)
        showAtPosition(dropDownPosition[0], dropDownPosition[1])
    }

    @Size(2)
    private fun getDropDownPosition(anchor: View, xOffset: Int, yOffset: Int): IntArray {
        val anchorHeight = anchor.height
        val drawingLocation = IntArray(2)
        anchor.getLocationInWindow(drawingLocation)
        var left = drawingLocation[0] + xOffset
        val top = drawingLocation[1] + anchorHeight + yOffset

        // TODO: 1/5/17 To be optimized like PopupWindow
        // Get display bounds
        val displayFrame = Rect()
        anchor.getWindowVisibleDisplayFrame(displayFrame)

        // Measure content view
        val layoutParams = mContentView!!.layoutParams
        val widthSpec: Int
        if (layoutParams != null && layoutParams.width > 0) {
            widthSpec =
                View.MeasureSpec.makeMeasureSpec(layoutParams.width, View.MeasureSpec.EXACTLY)
        } else {
            widthSpec = View.MeasureSpec.makeMeasureSpec(mRootView.width, View.MeasureSpec.AT_MOST)
        }
        val heightSpec: Int
        if (layoutParams != null && layoutParams.height > 0) {
            heightSpec =
                View.MeasureSpec.makeMeasureSpec(layoutParams.height, View.MeasureSpec.EXACTLY)
        } else {
            heightSpec =
                View.MeasureSpec.makeMeasureSpec(mRootView.height, View.MeasureSpec.AT_MOST)
        }
        mContentView!!.measure(widthSpec, heightSpec)

        // Fix position
        val outOfLeftBounds = left < displayFrame.left
        val outOfRightBounds = left + mContentView!!.measuredWidth > displayFrame.right
        if (mContentView!!.measuredWidth <= displayFrame.right - displayFrame.left) {
            if (outOfLeftBounds) {
                left = xOffset
            } else if (outOfRightBounds) {
                left = displayFrame.right + xOffset - mContentView!!.measuredWidth
            }
        }
        val position = IntArray(2)
        position[0] = left
        position[1] = top
        return position
    }

    protected fun showAtPosition(x: Int, y: Int) {
        if (mContentView != null) {
            mContentViewParent.setPopuptPosition(x, y)
            if (mContentView!!.parent != null) {
                mContentView!!.requestLayout()
            } else {
                val lp: ViewGroup.LayoutParams
                if (mContentView!!.layoutParams != null) {
                    lp = mContentView!!.layoutParams
                } else {
                    lp = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup
                            .LayoutParams.WRAP_CONTENT
                    )
                }
                mContentViewParent.addView(mContentView, lp)
            }
        }
        show()
    }

    fun dismiss() {
        mRootView.removeView(mContentViewParent)
    }

    fun setOutSideBackgroundColor(@ColorInt color: Int) {
        mBgColor = color
    }

    protected fun shouldDispatchTouchEvent(): Boolean {
        return true
    }

    fun setOutSideClickListener(onClickListener: View.OnClickListener) {
        mOutSideOnClickListener = onClickListener
    }

    protected inner class ContainerView(context: Context) : FrameLayout(context) {

        private var mChildStart: Int = 0
        private var mChildTop: Int = 0

        internal fun setPopuptPosition(start: Int, top: Int) {
            this.mChildStart = start
            this.mChildTop = top
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            if (childCount > 0) {
                val child = getChildAt(0)
                child.layout(
                    mChildStart,
                    mChildTop,
                    mChildStart + child.measuredWidth,
                    mChildTop + child.measuredHeight
                )
            }
        }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

            return if (shouldDispatchTouchEvent()) super.dispatchTouchEvent(ev) else true
        }
    }
}
