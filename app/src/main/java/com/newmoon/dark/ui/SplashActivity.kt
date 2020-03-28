package com.newmoon.dark.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.newmoon.common.util.*

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))
//        val clicked = Preferences.default.getBoolean(START_CLICKED, false)
//        if (clicked) {
//            goToHomePage()
//            return
//        }
//        setContentView(R.layout.activity_splash)
//        start.setOnClickListener {
//            goToHomePage()
//            Preferences.default.putBoolean(START_CLICKED, true)
//            logEvent(this, "welcome_start_click")
//        }
//        start.background = BackgroundDrawables.createBackgroundDrawable(
//            0xFF5685FD.toInt(),
//            Dimensions.pxFromDp(8f).toFloat(),
//            true
//        )
//
//        logEvent(this, "welcome_show")
        goToHomePage()
    }

    private fun goToHomePage() {
        Navigations.startActivity(this, HomeActivity::class.java)
        finish()
    }
}
