package com.newmoon.dark.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.newmoon.common.util.Dimensions
import com.newmoon.common.util.Navigations
import com.newmoon.common.util.immersive
import com.newmoon.common.util.setStatusBarTheme
import com.newmoon.dark.R
import kotlinx.android.synthetic.main.activity_web.*


class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        immersive(window)
        setStatusBarTheme(window, false)

        setContentView(R.layout.activity_web)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }

        tvTitle.text = intent.getStringExtra("title")

        webview.setBackgroundColor(Color.TRANSPARENT)
        val statusBarHeight = Dimensions.getStatusBarHeight(this)
        (toolbar_container.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight

        webview.loadUrl(getUrl())
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return true
            }
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT in 21..22) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private fun getUrl(): String? {
        return intent.getStringExtra("url")
    }
}

fun openWebPageWithUrl(context: Context, url: String, title: String) {
    val intent = Intent(context, WebViewActivity::class.java)
    intent.putExtra("url", url)
    intent.putExtra("title", title)
    Navigations.startActivitySafely(context, intent)
}
