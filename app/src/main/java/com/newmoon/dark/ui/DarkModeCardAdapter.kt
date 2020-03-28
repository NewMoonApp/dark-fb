package com.newmoon.dark.ui

import android.app.UiModeManager
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.newmoon.adengine.base.NmAdConfig
import com.newmoon.adengine.base.NmAdListener
import com.newmoon.adengine.interstitial.InterstitialAdManager
import com.newmoon.adengine.nativead.NmNativeAd
import com.newmoon.adengine.nativead.NmNativeAdIconView
import com.newmoon.common.util.*
import com.newmoon.dark.R
import com.newmoon.dark.ad.AD_PLACEMENT_SUPPORT_APPS
import com.newmoon.dark.ad.AdConfig
import com.newmoon.dark.extensions.openPurchasePage
import com.newmoon.dark.extensions.openTimerPickerDialog
import com.newmoon.dark.extensions.updateNotification
import com.newmoon.dark.logEvent
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.timer.*
import com.newmoon.dark.util.RemoteConfig
import com.newmoon.dark.util.fromJson
import com.newmoon.dark.view.AdLoadingView
import com.newmoon.dark.wallpaper.WallpaperInfo
import kotlinx.android.synthetic.main.card_mode_switch.view.*
import kotlinx.android.synthetic.main.card_supported_apps.view.*
import java.util.*

data class AppInfo(
    val appName: String,
    val iconId: Int,
    val appPackageName: String
)

const val CARD_MODE_SWITCH = 1
const val CARD_SUPPORTED_APPS = 2
const val CARD_INFO_FLOW = 3
const val CARD_AD = 4
const val CARD_WALLPAPER = 5
const val CARD_SHARE = 6

abstract class Card {
    abstract fun getType(): Int
}

internal class ModeSwitchCard : Card() {
    override fun getType(): Int {
        return CARD_MODE_SWITCH
    }
}

internal class SupportedAppsCard(val apps: List<AppInfo>) : Card() {
    override fun getType(): Int {
        return CARD_SUPPORTED_APPS
    }
}

internal class InformationFlowCard(
    val iconUrl: String,
    val title: String,
    val content: String,
    val url: String
) : Card() {
    override fun getType(): Int {
        return CARD_INFO_FLOW
    }
}

internal class AdCard(val ad: NmNativeAd) : Card() {
    override fun getType(): Int {
        return CARD_AD
    }
}

internal class WallpaperCard(val wallpaper: WallpaperInfo) : Card() {
    override fun getType(): Int {
        return CARD_WALLPAPER
    }
}

internal class ShareCard : Card() {
    override fun getType(): Int {
        return CARD_SHARE
    }
}

class DarkModeCardAdapter(val mActivity: HomeActivity, val mCards: List<Card>) :
    RecyclerView.Adapter<DarkModeCardAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        var vh: VH = ModeSwitchVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.card_mode_switch,
                parent,
                false
            ))
        when (viewType) {
            CARD_MODE_SWITCH -> {
                vh = ModeSwitchVH(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.card_mode_switch,
                        parent,
                        false
                    ))
            }
            CARD_SUPPORTED_APPS -> {
                vh = SupportedAppsVH(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.card_supported_apps,
                        parent,
                        false
                    ))
            }
            CARD_INFO_FLOW -> {
                vh = InformationFlowVH(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.card_information_flow,
                        parent,
                        false
                    ))
            }
            CARD_AD -> {
                vh = AdVH(
                    FrameLayout(parent.context)
                )
            }
            CARD_WALLPAPER -> {
                vh = WallpaperVH(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.card_wallpaper,
                        parent,
                        false
                    ))
            }
            CARD_SHARE -> {

            }
        }

        return vh
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bindData(mCards[position])
    }

    override fun getItemViewType(position: Int): Int {
        val card = mCards[position]
        return card.getType()
    }

    override fun getItemCount(): Int {
        return mCards.size
    }

    abstract inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bindData(data: Card)
    }

    inner class ModeSwitchVH(view: View) : VH(view) {
        private val mUiModeManager = mActivity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        init {
            itemView.run {
                setupModeSwitch()
                setupDarkModeTiming()
            }
        }

        private fun getModeName(mode: Int): String {
            when (mode) {
                UiModeManager.MODE_NIGHT_NO -> {
                    return "day"
                }
                UiModeManager.MODE_NIGHT_YES -> {
                    return "night"
                }
                UiModeManager.MODE_NIGHT_AUTO -> {
                    return "auto"
                }
                NIGHT_MODE_TIMING -> {
                    return "timing"
                }
            }
            return "day"
        }

        private fun timingModeUpdate() {
            itemView.run {
                day_mode_switch.isChecked = false
                night_mode_switch.isChecked = false
                auto_mode_switch.isChecked = false
                timing_mode_switch.isChecked = true

                val calendar: Calendar = Calendar.getInstance()

                val h = calendar.get(Calendar.HOUR_OF_DAY)
                val m = calendar.get(Calendar.MINUTE)

                val time = getDarkModeTimer()
                val mode = if (h in time.beginHour..23 || h in 0..time.endHour) {
                    if ((h == time.beginHour && m < time.beginMinute) ||
                        (h == time.endHour && m >= time.endMinute)
                    ) {
                        UiModeManager.MODE_NIGHT_NO
                    } else {
                        UiModeManager.MODE_NIGHT_YES
                    }
                } else {
                    UiModeManager.MODE_NIGHT_NO
                }

                if (mode != mUiModeManager.nightMode) {
                    mUiModeManager.nightMode = mode
                }
            }
        }

        private fun onModeClick(mode: Int) {
            itemView.run {
                if (mode == NIGHT_MODE_TIMING) {
                    if (!BillingManager.isPremiumUser()) {
                        mActivity.openPurchasePage("DarkMode")
                        return
                    }
                }
                HomeActivity.sIsRecreate = true
                val time = Preferences.default.getLong(MODE_CHANGED, -1L)
                if (time == -1L) {
                    Preferences.default.putLong(MODE_CHANGED, System.currentTimeMillis())
                }
                mActivity.updateNotification()
                val oldMode = Preferences.default.getInt(MODE, UiModeManager.MODE_NIGHT_NO)
                if (oldMode != mode) {
                    logEvent(
                        mActivity, "main_page_mode_switch",
                        mapOf(
                            "oldmode" to getModeName(oldMode),
                            "newmode" to getModeName(mode),
                            "switch" to getModeName(oldMode) + getModeName(mode)
                        )
                    )
                }
                Preferences.default.putInt(MODE, mode)
                when (mode) {
                    UiModeManager.MODE_NIGHT_NO -> {
                        day_mode_switch.isChecked = true
                        night_mode_switch.isChecked = false
                        auto_mode_switch.isChecked = false
                        timing_mode_switch.isChecked = false

                        mUiModeManager.nightMode = mode
                    }
                    UiModeManager.MODE_NIGHT_YES -> {
                        day_mode_switch.isChecked = false
                        night_mode_switch.isChecked = true
                        auto_mode_switch.isChecked = false
                        timing_mode_switch.isChecked = false

                        mUiModeManager.nightMode = mode
                    }
                    UiModeManager.MODE_NIGHT_AUTO -> {
                        day_mode_switch.isChecked = false
                        night_mode_switch.isChecked = false
                        auto_mode_switch.isChecked = true
                        timing_mode_switch.isChecked = false

                        mUiModeManager.nightMode = mode
                    }
                    NIGHT_MODE_TIMING -> {
                        timingModeUpdate()
                    }
                }

                if (!BillingManager.isPremiumUser()
                    && System.currentTimeMillis() - Preferences.default.getLong(
                        "last_mode_change_ad_time",
                        -1
                    )
                    > DateUtils.SECOND_IN_MILLIS * RemoteConfig.instance.getLong(
                        "AdSupportAppsInterval",
                        30
                    )
                ) {
                    var ad = InterstitialAdManager.fetch(AD_PLACEMENT_SUPPORT_APPS)
                    if (ad != null && ad.isLoaded) {
                        ad.show()
                        logEvent(mActivity, "Main_ModeChangeAd_Show")
                        Preferences.default.putLong("last_mode_change_ad_time", System.currentTimeMillis())

                        var adConfigs =
                            Gson().fromJson<List<NmAdConfig>>(AdConfig.getSupportAppsAdConfig())
                        InterstitialAdManager.preload(AD_PLACEMENT_SUPPORT_APPS, adConfigs)
                    }
                    logEvent(mActivity, "Main_ModeChangeAd_ShouldShow")
                }
            }
        }

        private fun setupModeSwitch() {
            itemView.run {
                val onClickListener: (view: View) -> Unit = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (it.tag != Preferences.default.getInt(MODE, UiModeManager.MODE_NIGHT_NO)) {
                            with(it.tag as Int) {
                                onModeClick(this)
                            }
                        }
                    }
                }
                day_mode_switch.isClickable = false
                night_mode_switch.isClickable = false
                auto_mode_switch.isClickable = false
                timing_mode_switch.isClickable = false

                day_mode_container.tag = UiModeManager.MODE_NIGHT_NO
                night_mode_container.tag = UiModeManager.MODE_NIGHT_YES
                auto_mode_container.tag = UiModeManager.MODE_NIGHT_AUTO
                timing_mode_container.tag = NIGHT_MODE_TIMING

                day_mode_container.setOnClickListener(onClickListener)
                night_mode_container.setOnClickListener(onClickListener)
                auto_mode_container.setOnClickListener(onClickListener)
                timing_mode_container.setOnClickListener(onClickListener)

                val mode = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    Preferences.default.getInt(
                        MODE,
                        UiModeManager.MODE_NIGHT_NO
                    ) else mUiModeManager.nightMode
                with(mode) {
                    context.updateNotification()
                    setupModeUi(this)
                    logEvent(
                        context, "main_page_show", mapOf(
                            "mode" to getModeName(this),
                            "widthdp" to Dimensions.dpFromPx(Dimensions.getPhoneWidth(mActivity)).toString(),
                            "heightdp" to Dimensions.dpFromPx(Dimensions.getPhoneHeight(mActivity)).toString(),
                            "densitydpi" to resources.displayMetrics.densityDpi.toString(),
                            "screensize" to (Dimensions.getPhoneWidth(mActivity).toString()
                                    + "x" + Dimensions.getPhoneHeight(mActivity).toString())
                        )
                    )
                }
            }
        }

        private fun setupModeUi(mode: Int) {
            itemView.run {
                with(mode) {
                    when (this) {
                        UiModeManager.MODE_NIGHT_NO -> {
                            day_mode_switch.isChecked = true
                            night_mode_switch.isChecked = false
                            auto_mode_switch.isChecked = false
                            timing_mode_switch.isChecked = false

                            Preferences.default.putInt(MODE, this)
                            mUiModeManager.nightMode = this
                        }
                        UiModeManager.MODE_NIGHT_YES -> {
                            day_mode_switch.isChecked = false
                            night_mode_switch.isChecked = true
                            auto_mode_switch.isChecked = false
                            timing_mode_switch.isChecked = false

                            Preferences.default.putInt(MODE, this)
                            mUiModeManager.nightMode = this
                        }
                        UiModeManager.MODE_NIGHT_AUTO -> {
                            day_mode_switch.isChecked = false
                            night_mode_switch.isChecked = false
                            auto_mode_switch.isChecked = true
                            timing_mode_switch.isChecked = false

                            Preferences.default.putInt(MODE, this)
                            mUiModeManager.nightMode = this
                        }
                        NIGHT_MODE_TIMING -> {
                            if (!BillingManager.isPremiumUser()) {
                                return
                            }

                            Preferences.default.putInt(MODE, this)
                            timingModeUpdate()

                        }
                    }
                }
            }
        }

        private fun setupDarkModeTiming() {
            itemView.run {
                val timer = getDarkModeTimer()
                timing_start_time.text = getTimeString(timer.beginHour, timer.beginMinute)
                timing_end_time.text = getTimeString(timer.endHour, timer.endMinute, "+")
                timing_start_time.setOnClickListener {
                    logEvent(it.context, "Pro_DarkMode_Timing_Click")
                    if (!BillingManager.isPremiumUser()) {
                        mActivity.openPurchasePage("DarkMode")
                        return@setOnClickListener
                    }
                    val l = object : TimePickerDialog.PikerListener {
                        override fun onCancel() {
                            // todo:log
                        }

                        override fun onSet(hour: Int, minute: Int) {
                            timing_start_time.text = getTimeString(hour, minute)
                            setDarkModeTimerBegin(hour, minute)

                            timing_mode_container.performClick()
                        }

                    }
                    val t = getDarkModeTimer()
                    mActivity.openTimerPickerDialog(l, t.beginHour, t.beginMinute)
                }

                timing_end_time.setOnClickListener {
                    logEvent(it.context, "Pro_DarkMode_Timing_Click")
                    if (!BillingManager.isPremiumUser()) {
                        mActivity.openPurchasePage("DarkMode")
                        return@setOnClickListener
                    }
                    val l = object : TimePickerDialog.PikerListener {
                        override fun onCancel() {
                            // todo:log
                        }

                        override fun onSet(hour: Int, minute: Int) {
                            timing_end_time.text = getTimeString(hour, minute, "+")
                            setDarkModeTimerEnd(hour, minute)

                            timing_mode_container.performClick()
                        }

                    }
                    val t = getDarkModeTimer()
                    mActivity.openTimerPickerDialog(l, t.endHour, t.endMinute)
                }

                if (RemoteConfig.instance.getBoolean("AutoTimerDarkModeEnabled")) {
                    timing_container.visibility = View.VISIBLE
                } else {
                    timing_container.visibility = View.GONE
                }
            }
        }
        override fun bindData(data: Card) {
            itemView.run {
                if (BillingManager.isPremiumUser()) {
                    dark_mode_pro.visibility = View.GONE
                } else {
                    dark_mode_pro?.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class SupportedAppsVH(view: View) : VH(view) {
        init {
            setupSupportedApps()
        }

        private fun setupSupportedApps() {
            itemView.run {
                supported_apps_list.isFocusable = false
                supported_apps_list.layoutManager = GridLayoutManager(context, 4)
                supported_apps_list.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildAdapterPosition(view)
                        if (position > 3) {
                            outRect.top = Dimensions.pxFromDp(16f)
                        }
                    }
                })

                supported_apps_title.visibility = View.VISIBLE
                supported_apps_list.visibility = View.VISIBLE
            }
        }
        override fun bindData(data: Card) {
            val d = data as SupportedAppsCard
            itemView.run {
                supported_apps_list.adapter = SupportedAppsAdapter(d.apps)
            }
        }

        inner class SupportedAppsAdapter(val list: List<AppInfo>) :
            RecyclerView.Adapter<SupportedAppsAdapter.VH>() {
            var supportAppAction: Runnable? = null
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                return VH(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.supported_apps_item,
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
                    appName.text = info.appName
                    appIcon.setImageResource(info.iconId)

                    itemView.setOnClickListener {
                        openApp(it.context, info.appPackageName, info.appName)
                    }
                }
            }

            inner class VH(view: View) : RecyclerView.ViewHolder(view) {
                val appName = view.findViewById<TextView>(R.id.name)
                val appIcon = view.findViewById<ImageView>(R.id.icon)
            }

            private fun openApp(context: Context, packageName: String, appName: String) {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(packageName)
                if (intent == null) {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.not_installed),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    supportAppAction = Runnable {
                        mActivity.isShowLoadingAd = true
                        var adLoadingView = AdLoadingView(mActivity)
                        adLoadingView.setLoadingText("Opening $appName...")
                        val adLoadingContainer =
                            mActivity.findViewById<FrameLayout>(R.id.ad_loading_view_container)
                        adLoadingContainer.addView(
                            adLoadingView, FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        Threads.postOnMainThreadDelayed(Runnable {
                            adLoadingContainer.removeAllViews()
                            mActivity.isShowLoadingAd = false
                            Navigations.startActivitySafely(context, intent)
                        }, 1200)
                    }
                    if (!BillingManager.isPremiumUser()
                        && System.currentTimeMillis() - Preferences.default.getLong(
                            "last_mode_change_ad_time",
                            -1
                        )
                        > DateUtils.SECOND_IN_MILLIS * RemoteConfig.instance.getLong(
                            "AdSupportAppsInterval",
                            30
                        )
                    ) {
                        if (RemoteConfig.instance.getBoolean("AdSupportAppsEnabled")) {
                            logEvent(context, "Main_SupportAppsAd_ShouldShow")
                        }
                        var ad = InterstitialAdManager.fetch(AD_PLACEMENT_SUPPORT_APPS)
                        if (ad != null && ad.isLoaded) {
                            ad.listener = object : NmAdListener {
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

                                override fun onAdClose() {
                                    supportAppAction?.run()
                                    supportAppAction = null
                                }
                            }
                            ad.show()
                            logEvent(context, "Main_SupportAppsAd_Show")
                            Preferences.default.putLong(
                                "last_mode_change_ad_time",
                                System.currentTimeMillis()
                            )

                            var adConfigs =
                                Gson().fromJson<List<NmAdConfig>>(AdConfig.getSupportAppsAdConfig())
                            InterstitialAdManager.preload(AD_PLACEMENT_SUPPORT_APPS, adConfigs)
                        } else {
                            supportAppAction?.run()
                            supportAppAction = null
                        }
                    } else {
                        supportAppAction?.run()
                        supportAppAction = null
                    }
                }

                logEvent(
                    context, "main_page_supported_apps_click", mapOf(
                        "app" to packageName,
                        "success" to if (intent == null) "false" else "true"
                    )
                )
            }
        }
    }

    inner class InformationFlowVH(view: View) : VH(view) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)
        val content = view.findViewById<TextView>(R.id.content)
        val read = view.findViewById<View>(R.id.read)
        val share = view.findViewById<View>(R.id.share)
        override fun bindData(data: Card) {
            val d = data as InformationFlowCard
            Glide.with(itemView.context)
                .load(d.iconUrl)
                .into(icon)
            title.text = d.title
            content.text = d.content

            read.setOnClickListener{

            }
            share.setOnClickListener {

            }
        }
    }

    inner class WallpaperVH(view: View) : VH(view) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)
        val wallpaper = view.findViewById<ImageView>(R.id.wallpaper)
        val more = view.findViewById<View>(R.id.more)
        val share = view.findViewById<View>(R.id.share)
        override fun bindData(data: Card) {
            val w = data as WallpaperCard
            Glide.with(itemView.context)
                .load(w.wallpaper.thumbnail)
                .into(wallpaper)

            more.setOnClickListener{
                mActivity.switchWallpaper()
            }
            share.setOnClickListener {

            }
        }
    }

    inner class AdVH(view: View) : VH(view) {
        override fun bindData(data: Card) {
            val c = data as AdCard
            val ad = c.ad
            var adContent =
                mActivity.layoutInflater.inflate(R.layout.main_ad_view, null)

            var adContainer = ad.container
            adContainer.setContent(itemView as ViewGroup, adContent)

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
                mActivity.resources.getColor(R.color.faq_content_color),
                PorterDuff.Mode.SRC_ATOP
            )

            adContainer.fillNativeAd(ad)

            logEvent(mActivity, "Main_BottomBannerAd_Show")
        }
    }
}

