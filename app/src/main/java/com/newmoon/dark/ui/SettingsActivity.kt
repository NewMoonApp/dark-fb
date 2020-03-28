package com.newmoon.dark.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.newmoon.common.feedback.FeedbackActivity
import com.newmoon.common.util.BackgroundDrawables
import com.newmoon.common.util.Navigations
import com.newmoon.common.util.immersive
import com.newmoon.common.util.setStatusBarTheme
import com.newmoon.dark.R
import com.newmoon.dark.extensions.openPrivacyPolicy
import com.newmoon.dark.extensions.openTermOfService
import com.newmoon.dark.extensions.share
import com.newmoon.dark.logEvent
import com.newmoon.dark.util.launchAppDetail
import com.newmoon.rate.FiveRateDialog
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity(), View.OnClickListener, FiveRateDialog.SubmitListener {

    var dialog: FiveRateDialog? = null
    override fun onSubmit(rates: Int) {
        if (rates == 0) {
            return
        }
        if (rates < 5) {
            Navigations.startActivity(this, FeedbackActivity::class.java)
        } else {
            launchAppDetail(this, packageName)
        }

        dialog?.cancel()
        logEvent(
            this, "settings_five_star_rating_submit", mapOf(
                "star" to rates.toString()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        immersive(window)
        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))

        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }

        navigation_item_privacy_policy.setOnClickListener(this)
        navigation_item_terms_of_service.setOnClickListener(this)
        navigation_item_rate.setOnClickListener(this)
        navigation_item_feedback.setOnClickListener(this)
        navigation_item_invite.setOnClickListener(this)

        navigation_item_privacy_policy.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_terms_of_service.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_rate.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_feedback.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_invite.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
    }

    private fun openFiveStarsRateDialog() {
        dialog = FiveRateDialog(this, R.style.Transparent)
        dialog?.setSubmitListener(this)
        dialog?.show()
        logEvent(this, "settings_five_star_rating_show")
    }

    override fun onClick(v: View?) {
        when (v) {
            navigation_item_privacy_policy -> {
                openPrivacyPolicy()
                logEvent(
                    this@SettingsActivity, "settings_privacy_policy_click"
                )
            }
            navigation_item_terms_of_service -> {
                openTermOfService()
                logEvent(
                    this@SettingsActivity, "settings_terms_of_service_click"
                )
            }
            navigation_item_rate -> {
                openFiveStarsRateDialog()
                logEvent(
                    this@SettingsActivity, "settings_rating_click"
                )
            }
            navigation_item_feedback -> {
                Navigations.startActivity(this, FeedbackActivity::class.java)
                logEvent(
                    this@SettingsActivity, "settings_feedback_click"
                )
            }
            navigation_item_invite -> {
                share()
                logEvent(
                    this@SettingsActivity, "settings_invite_click"
                )
            }
        }
    }
}
