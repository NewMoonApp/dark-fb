package com.newmoon.dark.timer

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.newmoon.common.util.BackgroundDrawables
import com.newmoon.common.util.Dimensions
import com.newmoon.dark.R
import com.newmoon.dark.view.timepickerview.TimePickerView
import com.newmoon.dark.view.timepickerview.WheelTime

class TimePickerDialog : Dialog, View.OnClickListener {
    private var mCancel: View? = null
    private var mSet: View? = null
    private var wheelTime: WheelTime? = null
    private var timePickerView: View? = null
    private var h:Int = 0
    private var m:Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {}

    protected constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.time_picker, null)
        setContentView(view)
        initView(view)
    }

    private fun initView(content: View) {
        content.run {
            findViewById<View>(R.id.container).background = BackgroundDrawables.createBackgroundDrawable(
                context.resources.getColor(R.color.time_picker_bg_color),
                Dimensions.pxFromDp(8f).toFloat(),
                true
            )

            mCancel = findViewById(R.id.cancel_action)
            mSet = findViewById(R.id.set_action)

            timePickerView = findViewById(R.id.time_picker_ll)

            wheelTime = WheelTime(timePickerView, TimePickerView.Type.HOURS_MINS)
            wheelTime!!.setPicker(0, 7, 15, h, m)

            mCancel?.background = BackgroundDrawables.createBackgroundDrawable(
                context.resources.getColor(R.color.time_picker_cancel_bg_color),
                Dimensions.pxFromDp(8f).toFloat(),
                true
            )
            mSet?.background = BackgroundDrawables.createBackgroundDrawable(
                context.resources.getColor(R.color.time_picker_set_bg_color),
                Dimensions.pxFromDp(8f).toFloat(),
                true
            )

            mCancel?.setOnClickListener(this@TimePickerDialog)
            mSet?.setOnClickListener(this@TimePickerDialog)
        }
    }
    override fun onClick(v: View?) {
        if (v == mCancel) {
            dismiss()
            l?.onCancel()
        } else if (v == mSet) {
            dismiss()
            l?.onSet(wheelTime!!.hour, wheelTime!!.minute)
        }
    }

    fun setTime(h: Int, m: Int) {
        this.h = h
        this.m = m
        wheelTime?.setPicker(0, 1, 15, h, m)
    }

    fun setListener(listener: PikerListener) {
        l = listener
    }

    private var l: PikerListener? = null
    interface PikerListener {
        fun onCancel()
        fun onSet(hour: Int, minute: Int)
    }
}
