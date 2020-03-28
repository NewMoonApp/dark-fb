package com.newmoon.dark.ui

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.newmoon.common.util.immersive
import com.newmoon.common.util.setStatusBarTheme

import com.newmoon.dark.R

class FaqDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        immersive(window)
        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))

        setContentView(R.layout.activity_faq_detail)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }

        (findViewById<View>(R.id.question) as TextView).text = question
        if (!TextUtils.isEmpty(answer)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                (findViewById<View>(R.id.answer) as TextView).text =
                    Html.fromHtml(answer, Html.FROM_HTML_MODE_COMPACT)
            } else {
                (findViewById<View>(R.id.answer) as TextView).text = Html.fromHtml(answer)
            }
        }
    }

    companion object {

        var question: String? = null
        var answer: String? = null
    }
}
