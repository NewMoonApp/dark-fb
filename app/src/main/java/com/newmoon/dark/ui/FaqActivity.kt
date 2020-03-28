package com.newmoon.dark.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

import com.newmoon.common.util.Navigations
import com.newmoon.common.util.immersive
import com.newmoon.common.util.setStatusBarTheme
import com.newmoon.dark.R

import java.util.LinkedHashMap

class FaqActivity : AppCompatActivity() {

    lateinit var qContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        immersive(window)
        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))

        setContentView(R.layout.activity_faq)

        qContainer = findViewById(R.id.q_container)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }

        for ((key, value) in faq) {
            val itemView = FaqItemView(this)
            itemView.tvTitle.text = key
            itemView.setOnClickListener {
                FaqDetailActivity.answer = value
                FaqDetailActivity.question = key
                Navigations.startActivitySafely(
                    this@FaqActivity,
                    Intent(this@FaqActivity, FaqDetailActivity::class.java)
                )
            }
            qContainer.addView(itemView)
        }
    }

    companion object {
        var faq = LinkedHashMap<String, String>()

        init {
            faq["Why dark mode not working on my Mi device?"] =
                "<p>Dark Mode is disabled by MIUI system since MIUI 11. </p>\n" +
                        "\t\t\t\t\t<p>Sorry for that, but there's nothing we can do to solve this problem.</p>"
            faq["Dark mode is continuously switching back to day mode, what's wrong?"] =
                "<p>On some Samsung devices, this issue is normal. The Samsung Email app resets the dark mode setting once its alive, so it's not a problem of Dark Mode app, but a problem of the Email app.</p>\n" +
                        "\t\t\t\t\t<p><b>You could solve this by disabling the Samsung Email app</p>\n" +
                        "\t\t\t\t\t<ol>\n" +
                        "\t\t\t\t\t\t<li>Open system \"Settings\"</li>\n" +
                        "\t\t\t\t\t\t<li>Click \"Apps\"</li>\n" +
                        "\t\t\t\t\t\t<li>Select Samsung Email</li>\n" +
                        "\t\t\t\t\t\t<li>Click on the settings icon in the top right corner</li>\n" +
                        "\t\t\t\t\t\t<li>Click on \"Uninstall updates\"</li>\n" +
                        "\t\t\t\t\t</ol>\n"
            faq["This app does not work on my phone, what can I do?"] =
                "<p>Since some phone manufacturer has disabled Dark Mode setting, some users may find this app doesn't do the job.</p>\n" +
                        "\t\t\t\t\t<p>Sorry for that, but there's nothing we can do to solve this problem.</p>\n"
        }
    }
}
