package com.newmoon.dark.nightscreen

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.newmoon.adengine.base.NmAdConfig
import com.newmoon.adengine.interstitial.InterstitialAdManager
import com.newmoon.adengine.nativead.NmNativeAd
import com.newmoon.adengine.nativead.NmNativeAdIconView
import com.newmoon.adengine.nativead.NmNativeAdListener
import com.newmoon.adengine.nativead.NmNativeAdLoader
import com.newmoon.common.util.*
import com.newmoon.dark.R
import com.newmoon.dark.ad.AD_PLACEMENT_NIGHT_SCREEN_WIRE
import com.newmoon.dark.ad.AdConfig
import com.newmoon.dark.extensions.canDrawOverlays
import com.newmoon.dark.extensions.openPurchasePage
import com.newmoon.dark.extensions.openTimerPickerDialog
import com.newmoon.dark.extensions.updateNotification
import com.newmoon.dark.logEvent
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.timer.*
import com.newmoon.dark.ui.HomeActivity
import com.newmoon.dark.ui.NightScreenFaqActivity
import com.newmoon.dark.ui.OVERLAY_PERMISSION_REQUEST_CODE
import com.newmoon.dark.util.ActivityUtils.contextToActivitySafely
import com.newmoon.dark.util.RemoteConfig
import com.newmoon.dark.util.fromJson
import com.newmoon.dark.view.SmartSeekBar
import kotlinx.android.synthetic.main.night_screen_container.*
import kotlinx.android.synthetic.main.night_screen_container.view.*
import java.util.*

class NightScreenContainer : NestedScrollView {
    val nsm = NightScreenManager.getInstance(context.applicationContext)
    val activity = contextToActivitySafely(context)!!
    private var permissionDialog: AlertDialog? = null

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setupNightScreen()
    }

    fun switch(){
        if (!canDrawOverlays()) {
            showDrawOverlaysPermissionDialog()
        } else {

        }
        loadNightScreenNativeAd()
    }

    fun updateProUi() {
        if (BillingManager.isPremiumUser()) {
            night_screen_pro.visibility = View.GONE
        } else {
            night_screen_pro?.visibility = View.VISIBLE
        }

    }

    fun onUserClickToolbarNightScreen() {
        justSetNightScreenTimingSwitchChecked(false)
        justSetNightScreenSwitchChecked(nsm.isOn() && !nsm.isNightScreenTimingEnabled())
    }

    private fun loadNightScreenNativeAd() {
        if (RemoteConfig.instance.getBoolean("AdNightScreenBannerEnabled")
            && !BillingManager.isPremiumUser()
        ) {
            NmNativeAdLoader(
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getNightScreenNativeAdConfig()),
                object : NmNativeAdListener {
                    override fun onAdLoaded(ad: NmNativeAd) {
                        var adContent =
                            activity.layoutInflater.inflate(R.layout.nightscreen_ad_view, null)
                        var adTitleContainer =
                            adContent.findViewById<ViewGroup>(R.id.night_screen_ad_title_container)
                        adTitleContainer.viewTreeObserver.addOnGlobalLayoutListener(object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                adTitleContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                adTitleContainer.layoutParams.width =
                                    (adTitleContainer.parent as ViewGroup).measuredWidth - Dimensions.pxFromDp(
                                        62f
                                    )
                                adTitleContainer.requestLayout()
                            }
                        })

                        var adContainer = ad.container
                        findViewById<View>(R.id.adViewNightScreenContainer).visibility =
                            View.VISIBLE
                        adContainer.setContent(
                            findViewById(R.id.adViewNightScreenContainer),
                            adContent
                        )

                        var iconView =
                            adContent.findViewById<NmNativeAdIconView>(R.id.banner_icon_image)
                        adContainer.setIconView(iconView)

                        var title = adContent.findViewById<TextView>(R.id.banner_title)
                        title.text = ad.title
                        adContainer.setTitle(title)

                        var body = adContent.findViewById<TextView>(R.id.banner_des)
                        body.text = ad.body
                        adContainer.setBody(body)

                        var cta = adContent.findViewById<TextView>(R.id.banner_action)
                        cta.background = BackgroundDrawables.createBackgroundDrawable(
                            0xff568FFD.toInt(),
                            Dimensions.pxFromDp(8f).toFloat(),
                            false
                        )
                        cta.text = ad.cta
                        adContainer.setCTA(cta)

                        adContainer.setAdChoiceView(adContent.findViewById(R.id.ad_choice))

                        var adPreview = adContent.findViewById<ImageView>(R.id.icon_ad_preview)
                        adPreview.drawable.setColorFilter(
                            resources.getColor(R.color.faq_content_color),
                            PorterDuff.Mode.SRC_ATOP
                        )

                        adContainer.setAdMediaView(adContent.findViewById(R.id.media_view))

                        adContainer.fillNativeAd(ad)

                        logEvent(context, "NightScreen_NativeAd_Show")
                    }

                    override fun onAdClose() {
                    }

                    override fun onAdFailedToLoad(var1: Int) {
                    }

                    override fun onAdOpened() {
                    }

                    override fun onAdLoaded() {

                    }

                    override fun onAdClicked() {
                    }

                    override fun onAdImpression() {
                    }
                })
            logEvent(context, "NightScreen_NativeAd_ShouldShow")
        }
    }

    private fun setupNightScreen() {
        night_screen_color_temperatures.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val list = mutableListOf<ColorTemperature>()
        list.add(
            ColorTemperature(
                R.drawable.color_temperature_dark,
                resources.getString(R.string.color_temperature_dark),
                COLOR_TEMPERATURE_DARK,
                0x333B517A
            )
        )
        list.add(
            ColorTemperature(
                R.drawable.color_temperature_candlelight,
                resources.getString(R.string.color_temperature_candlelight),
                COLOR_TEMPERATURE_CANDLELIGHT,
                0x33F46E6E
            )
        )
        list.add(
            ColorTemperature(
                R.drawable.color_temperature_dim,
                resources.getString(R.string.color_temperature_dim),
                COLOR_TEMPERATURE_DIM,
                0x33F5AE1F
            )
        )
        list.add(
            ColorTemperature(
                R.drawable.color_temperature_starry,
                resources.getString(R.string.color_temperature_starry),
                COLOR_TEMPERATURE_STARRY,
                0x334775CC
            )
        )
        list.add(
            ColorTemperature(
                R.drawable.color_temperature_forest,
                resources.getString(R.string.color_temperature_forest),
                COLOR_TEMPERATURE_FOREST,
                0x3398D312
            )
        )
        night_screen_color_temperatures.adapter = ColorTemperaturesAdapter(list)

        night_screen_intensity_seekbar.setOnProgressChangeListener(object :
            SmartSeekBar.OnProgressChangeListener {
            override fun onProgressChange(progress: Float, fromUser: Boolean) {
                if (!canDrawOverlays()) {
                    showDrawOverlaysPermissionDialog()
                    return
                }
                val isNightScreenTimingMode = nsm.isNightScreenTimingEnabled()
                if (!isNightScreenTimingMode) {
                    night_screen_status_switch.isChecked = true
                    night_screen_status_text.text = resources.getString(R.string.night_screen_on)
                }
                onIntensityChange(progress, fromUser)
            }
        })

        night_screen_intensity_seekbar.setProgress(nsm.getIntensityAlpha() / 0.8f)

        night_screen_screenDim_seekbar.setOnProgressChangeListener(object :
            SmartSeekBar.OnProgressChangeListener {
            override fun onProgressChange(progress: Float, fromUser: Boolean) {
                if (!canDrawOverlays()) {
                    showDrawOverlaysPermissionDialog()
                    return
                }
                val isNightScreenTimingMode = nsm.isNightScreenTimingEnabled()
                if (!isNightScreenTimingMode) {
                    night_screen_status_switch.isChecked = true
                    night_screen_status_text.text = resources.getString(R.string.night_screen_on)
                }

                onScreenDimChange(progress, fromUser)
            }
        })
        night_screen_screenDim_seekbar.setProgress(nsm.getScreenDimAlpha() / 0.75f)

        night_screen_help.setOnClickListener {
            Navigations.startActivitySafely(
                context,
                Intent(context, NightScreenFaqActivity::class.java)
            )
            logEvent(it.context, "night_screen_tip_click")
        }

        night_screen_status_switch.isChecked =
            nsm.isOn() && !nsm.isNightScreenTimingEnabled()
        night_screen_status_text.text =
            if (night_screen_status_switch.isChecked) resources.getString(R.string.night_screen_on) else resources.getString(
                R.string.night_screen_off
            )

        setNightScreenSwitchCheckedChangeListener()

        setupNightScreenTiming()
    }

    private fun setupNightScreenTiming() {
        val timer = getNightScreenTimer()
        night_screen_timing_start_time.text = getTimeString(timer.beginHour, timer.beginMinute)
        night_screen_timing_end_time.text = getTimeString(timer.endHour, timer.endMinute, "+")
        night_screen_timing_start_time.setOnClickListener {
            logEvent(it.context, "Pro_NightScreen_Timing_Click")
            if (!BillingManager.isPremiumUser()) {
                context.openPurchasePage("NightScreen")
                return@setOnClickListener
            }
            if (!canDrawOverlays()) {
                showDrawOverlaysPermissionDialog()
                return@setOnClickListener
            }
            val l = object : TimePickerDialog.PikerListener {
                override fun onCancel() {
                    // todo:log
                }

                override fun onSet(hour: Int, minute: Int) {
                    night_screen_timing_start_time.text = getTimeString(hour, minute)
                    setNightScreenTimerBegin(hour, minute)

                    night_screen_timing_switch.isChecked = true
                }

            }
            val t = getNightScreenTimer()
            activity.openTimerPickerDialog(l, t.beginHour, t.beginMinute)
        }

        night_screen_timing_end_time.setOnClickListener {
            logEvent(it.context, "Pro_NightScreen_Timing_Click")
            if (!BillingManager.isPremiumUser()) {
                context.openPurchasePage("NightScreen")
                return@setOnClickListener
            }
            if (!canDrawOverlays()) {
                showDrawOverlaysPermissionDialog()
                return@setOnClickListener
            }
            val l = object : TimePickerDialog.PikerListener {
                override fun onCancel() {
                    // todo:log
                }

                override fun onSet(hour: Int, minute: Int) {
                    night_screen_timing_end_time.text = getTimeString(hour, minute, "+")
                    setNightScreenTimerEnd(hour, minute)

                    night_screen_timing_switch.isChecked = true
                }

            }
            val t = getNightScreenTimer()
            activity.openTimerPickerDialog(l, t.endHour, t.endMinute)
        }

        night_screen_timing_mode_container.setOnClickListener {
            if (!BillingManager.isPremiumUser()) {
                context.openPurchasePage("NightScreen")
                justSetNightScreenTimingSwitchChecked(false)
                return@setOnClickListener
            }
            if (!canDrawOverlays()) {
                showDrawOverlaysPermissionDialog()
                justSetNightScreenTimingSwitchChecked(false)
                return@setOnClickListener
            }

            night_screen_timing_switch.isChecked = true
            nsm.setNightScreenTimingEnabled(true)
        }

        setNightScreenTimingSwitchCheckedChangeListener()
        night_screen_timing_switch.isChecked = nsm.isNightScreenTimingEnabled()

        if (RemoteConfig.instance.getBoolean("AutoTimerNightScreenEnabled")) {
            night_screen_timing_container.visibility = View.VISIBLE
        } else {
            night_screen_timing_container.visibility = View.GONE
        }
    }

    private fun justSetNightScreenTimingSwitchChecked(isChecked: Boolean) {
        removeNightScreenTimingSwitchCheckedChangeListener()
        night_screen_timing_switch.isChecked = isChecked
        nsm.setNightScreenTimingEnabled(isChecked)
        setNightScreenTimingSwitchCheckedChangeListener()
    }

    private fun setNightScreenTimingSwitchCheckedChangeListener() {
        night_screen_timing_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked && !BillingManager.isPremiumUser()) {
                context.openPurchasePage("NightScreen")
                justSetNightScreenTimingSwitchChecked(false)
                return@setOnCheckedChangeListener
            }
            if (isChecked && !canDrawOverlays()) {
                showDrawOverlaysPermissionDialog()
                justSetNightScreenTimingSwitchChecked(false)
                return@setOnCheckedChangeListener
            }
            nsm.setNightScreenTimingEnabled(isChecked)
            if (isChecked) {
                timingNightScreenModeUpdate()
            } else {
                nsm.removeNightScreen()
            }
            justSetNightScreenSwitchChecked(false)
        }
    }

    private fun setNightScreenSwitchCheckedChangeListener() {
        night_screen_status_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked && !canDrawOverlays()) {
                showDrawOverlaysPermissionDialog()
                justSetNightScreenSwitchChecked(false)
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                justSetNightScreenTimingSwitchChecked(false)
                nsm.addNightScreen()
                logEvent(
                    context, "night_screen_switch_click", mapOf(
                        "result" to "open"
                    )
                )

                if (!BillingManager.isPremiumUser()
                    && System.currentTimeMillis() - Preferences.default.getLong(
                        "last_night_screen_on_ad_time",
                        -1
                    )
                    > DateUtils.MINUTE_IN_MILLIS * 1
                ) {
                    var ad = InterstitialAdManager.fetch(AD_PLACEMENT_NIGHT_SCREEN_WIRE)
                    if (ad != null && ad.isLoaded) {
                        ad.show()
                        logEvent(context, "NightScreen_Wire_Show")
                        Preferences.default.putLong(
                            "last_night_screen_on_ad_time",
                            System.currentTimeMillis()
                        )
                    }
                    logEvent(context, "NightScreen_Wire_ShouldShow")
                }
            } else {
                nsm.removeNightScreen()
                logEvent(
                    context, "night_screen_switch_click", mapOf(
                        "result" to "close"
                    )
                )
            }
            night_screen_status_text.text =
                if (night_screen_status_switch.isChecked) resources.getString(R.string.night_screen_on) else resources.getString(
                    R.string.night_screen_off
                )

            context.updateNotification()
        }
    }

    private fun justSetNightScreenSwitchChecked(isChecked: Boolean) {
        night_screen_status_switch.setOnCheckedChangeListener { buttonView, isChecked ->  }
        night_screen_status_switch.isChecked =
            isChecked
        night_screen_status_text.text =
            if (isChecked) resources.getString(R.string.night_screen_on) else resources.getString(
                R.string.night_screen_off
            )
        setNightScreenSwitchCheckedChangeListener()

    }

    private fun removeNightScreenTimingSwitchCheckedChangeListener() {
        night_screen_timing_switch.setOnCheckedChangeListener { buttonView, isChecked ->
        }
    }

    private fun timingNightScreenModeUpdate() {
        val calendar: Calendar = Calendar.getInstance()

        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)

        val time = getNightScreenTimer()
        val mode = if (h in time.beginHour..23 || h in 0..time.endHour) {
            if ((h == time.beginHour && m < time.beginMinute) ||
                (h == time.endHour && m >= time.endMinute)
            ) {
                false
            } else {
                true
            }
        } else {
            false
        }

        if (mode) {
            nsm.addNightScreen()
        } else {
            nsm.removeNightScreen()
        }
        context.updateNotification()

    }

    private fun showDrawOverlaysPermissionDialog() {
        if (night_screen_content_frame.visibility != View.VISIBLE) {
            return
        }
        if (permissionDialog?.isShowing == true) {
            return
        }
        val builder = AlertDialog.Builder(
            activity,
            if (resources.getBoolean(R.bool.dark_mode))
                R.style.Theme_AppCompat_DayNight_Dialog else R.style.Theme_AppCompat_Light_Dialog
        )

        builder.setTitle(R.string.overlay_permission_title)
        builder.setMessage(R.string.overlay_permission_message)
        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            logEvent(context, "night_screen_request_permission_click")
            try {
                activity.startActivityForResult(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ), OVERLAY_PERMISSION_REQUEST_CODE
                )
            } catch (e: Exception) {
                Toasts.showToast(R.string.error_permission)
            }
        }
        permissionDialog = builder.create()
        permissionDialog?.show()
        logEvent(context, "night_screen_request_permission_show")
    }

    private fun onIntensityChange(progress: Float, fromUser: Boolean) {
        night_screen_intensity_text.text = (progress * 80).toInt().toString()
        nsm.setIntensityAlpha(progress * 0.8f)

        logEvent(
            context, "night_screen_intensity_change", mapOf(
                "result" to (progress * 80).toString()
            )
        )
    }

    private fun onScreenDimChange(progress: Float, fromUser: Boolean) {
        night_screen_screenDim_text.text = (progress * 75).toInt().toString()
        nsm.setScreenDim(progress * 0.75f)

        logEvent(
            context, "night_screen_brightness_change", mapOf(
                "result" to (progress * 75).toString()
            )
        )
    }

    inner class ColorTemperaturesAdapter(val list: List<ColorTemperature>) :
        RecyclerView.Adapter<ColorTemperaturesAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.color_temperature_item,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.run {
                val info = list[position]
                name.text = info.name
                icon.setImageResource(info.iconId)

                val cic = nsm.getIntensityColor()
                if (cic == info.value) {
                    icon.background = BackgroundDrawables.createBackgroundDrawable(
                        info.selectedBg,
                        1000f,
                        false
                    )
                } else {
                    icon.background = null
                }

                icon.setOnClickListener {
                    itemView.performClick()
                }
                itemView.setOnClickListener {
                    if (!canDrawOverlays()) {
                        showDrawOverlaysPermissionDialog()
                        return@setOnClickListener
                    }
                    val isNightScreenTimingMode = nsm.isNightScreenTimingEnabled()
                    if (!isNightScreenTimingMode) {
                        night_screen_status_switch.isChecked = true
                        night_screen_status_text.text = resources.getString(R.string.night_screen_on)
                    }
                    nsm.setIntensityColor(info.value)
                    notifyDataSetChanged()

                    logEvent(
                        it.context, "night_screen_color_click", mapOf(
                            "index" to (position + 1).toString()
                        )
                    )
                }
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.findViewById<TextView>(R.id.name)
            val icon = view.findViewById<ImageView>(R.id.icon)
        }
    }

    data class ColorTemperature(
        val iconId: Int,
        val name: String,
        val value: Int,
        val selectedBg: Int
    )

}
