package com.newmoon.dark.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.newmoon.common.util.immersive
import com.newmoon.common.util.setStatusBarTheme
import com.newmoon.dark.R

class NightScreenFaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        immersive(window)
        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))

        setContentView(R.layout.activity_night_screen_faq)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }
    }
}
