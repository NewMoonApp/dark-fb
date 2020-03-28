package com.newmoon.dark.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.newmoon.adengine.base.NmAdConfig
import com.newmoon.adengine.base.NmAdListener
import com.newmoon.adengine.interstitial.InterstitialAdManager
import com.newmoon.adengine.interstitial.InterstitialAdRequest
import com.newmoon.adengine.nativead.NmNativeAd
import com.newmoon.adengine.nativead.NmNativeAdListener
import com.newmoon.adengine.nativead.NmNativeAdLoader
import com.newmoon.common.BaseApplication
import com.newmoon.common.feedback.FeedbackActivity
import com.newmoon.common.util.*
import com.newmoon.dark.R
import com.newmoon.dark.ad.*
import com.newmoon.dark.extensions.*
import com.newmoon.dark.logEvent
import com.newmoon.dark.nightscreen.*
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.service.*
import com.newmoon.dark.util.HomeKeyWatcher
import com.newmoon.dark.util.RemoteConfig
import com.newmoon.dark.util.fromJson
import com.newmoon.dark.util.launchAppDetail
import com.newmoon.dark.wallpaper.WallpaperContainer
import com.newmoon.dark.wallpaper.getWallpapers
import com.newmoon.rate.FiveRateDialog
import com.pitchedapps.frost.StartActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.layout_home_navigation.*
import kotlinx.android.synthetic.main.night_screen_container.*

const val MODE = "ui_mode"
const val NOTIFICATION = "notification"
const val MODE_CHANGED = "mode_changed_time"

const val NIGHT_SCREEN_TIMING = "night_screen_timing"

const val NIGHT_MODE_TIMING = UiModeManager.MODE_NIGHT_YES * 2

const val OVERLAY_PERMISSION_REQUEST_CODE = 0

class HomeActivity : AppCompatActivity(), View.OnClickListener, FiveRateDialog.SubmitListener,
    RemoteConfig.FetchListener {

    companion object {
        var sIsRecreate = false
    }

    private lateinit var mUiModeManager: UiModeManager
    private lateinit var mToolbarUpdateReceiver: ToolbarUpdateReceiver
    private var mHomeKeyWatcher: HomeKeyWatcher? = null

    private var dialog: FiveRateDialog? = null

    private lateinit var mNavigationView: NavigationView
    private lateinit var mDrawerLayout: DrawerLayout
    private var mExitAppAnimationViewContainer: ConstraintLayout? = null
    private var mLottieAnimationView: LottieAnimationView? = null

    var isShowLoadingAd = false

    private var mScrollRecorded = false
    private var mIsExitAdShown: Boolean = false

    private var darkModeContainer: RecyclerView? = null
    private var nightScreenContainer: NightScreenContainer? = null
    private var wallpaperContainer: WallpaperContainer? = null

    private var mDarkModeCardAdapter: DarkModeCardAdapter? = null
    private val mDarkModeCards = mutableListOf<Card>()

    // RemoteConfig.FetchListener
    override fun onDone(succeed: Boolean) {
    }

    // FiveRateDialog.SubmitListener
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
            this, "five_star_rating_submit", mapOf(
                "star" to rates.toString()
            )
        )
    }

    private fun parseIntent(intent: Intent?) {
        if (intent?.getStringExtra(FROM)?.equals(TOOLBAR) == true) {
            logEvent(
                this, "toolbar_click"
            )
        }
    }

    private fun registerToolbarUpdateReceiver() {
        mToolbarUpdateReceiver = ToolbarUpdateReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_UPDATE_MAIN_PAGE)
        registerReceiver(mToolbarUpdateReceiver, intentFilter)
    }

    private lateinit var interstitialAdRequest: InterstitialAdRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive(window)
        setStatusBarTheme(window, resources.getBoolean(R.bool.status_bar_light))

        setContentView(R.layout.activity_home)

        if (System.currentTimeMillis()
            - Preferences.default.getLong("pref_key_last_app_open_ad_show_time", -1)
            > DateUtils.MINUTE_IN_MILLIS * RemoteConfig.instance.getLong("AdAppOpenInterval", 60)
            && !sIsRecreate
            && RemoteConfig.instance.getBoolean("AdAppOpenEnabled")
            && !BillingManager.isPremiumUser()
        ) {
            var isCancelled = false
            interstitialAdRequest = InterstitialAdManager.load(
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getAppOpenAdConfig()),
                object : NmAdListener {
                    override fun onAdClose() {

                    }

                    override fun onAdFailedToLoad(var1: Int) {
                    }

                    override fun onAdOpened() {
                    }

                    override fun onAdLoaded() {
                        if (!isCancelled) {
                            interstitialAdRequest.show()
                            Preferences.default.putLong(
                                "pref_key_last_app_open_ad_show_time",
                                System.currentTimeMillis()
                            )
                            Threads.postOnMainThreadDelayed(Runnable {
                                root.visibility = View.VISIBLE
                            }, 500)
                        }
                    }

                    override fun onAdClicked() {
                    }

                    override fun onAdImpression() {
                    }
                }
            )

            root.visibility = View.GONE
            Threads.postOnMainThreadDelayed(Runnable {
                isCancelled = true
                root.visibility = View.VISIBLE
            }, 3000)
        }

        mUiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        initView()

        setupAds()
        registerToolbarUpdateReceiver()
        parseIntent(intent)

        startActivity(Intent(this, StartActivity::class.java))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // 用户在Home page可见状态下从toolbar更新dark mode，Activity重启后几个switch的状态是不变的
        // 是在onCreate()后又走了onRestoreInstanceState(savedInstanceState: Bundle)，错误地恢复了以前的checked状态
        mDarkModeCardAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mToolbarUpdateReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        parseIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (mIsExitAdShown) {
            showExitAppAnimation()
            return
        }

        val time = Preferences.default.getLong(MODE_CHANGED, -1L)
        if (time != -1L && System.currentTimeMillis() - time > 1000 * 15) {
            Preferences.default.doOnce(Runnable {
                openFiveStarsRateDialog()
            }, "show_five_rate_dialog")
        }

        if (BillingManager.isPremiumUser()) {
            updateProUi()
        }
    }

    override fun onBackPressed() {
        if (isShowLoadingAd)
            return

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(mNavigationView)
            return
        }
        if (RemoteConfig.instance.getBoolean("AdExitWireEnabled")) {
            logEvent(this, "Main_ExitWireAd_ShouldShow")
        }
        var ad = InterstitialAdManager.fetch(AD_PLACEMENT_EXIT_WIRE)
        if (ad != null && ad.isLoaded) {
            ad.listener = object : NmAdListener {
                private var mExitAppAnimationViewStub: ViewStub? = null

                override fun onAdFailedToLoad(var1: Int) {

                }

                override fun onAdOpened() {
                    mHomeKeyWatcher = HomeKeyWatcher(BaseApplication.context)
                    mHomeKeyWatcher!!.setOnHomePressedListener(object :
                        HomeKeyWatcher.OnHomePressedListener {
                        override fun onRecentsPressed() {
                        }

                        override fun onHomePressed() {
                            mIsExitAdShown = false
                            if (mExitAppAnimationViewContainer != null) {
                                mExitAppAnimationViewContainer!!.visibility = View.GONE
                            }
                        }
                    })
                    mHomeKeyWatcher!!.startWatch()
                    Threads.postOnMainThreadDelayed(Runnable {
                        if (mExitAppAnimationViewContainer == null) {
                            mExitAppAnimationViewStub = findViewById(R.id.exit_app_stub)
                            mExitAppAnimationViewContainer =
                                mExitAppAnimationViewStub!!.inflate() as ConstraintLayout?
                            mLottieAnimationView = findViewById(R.id.exit_app_lottie)
                            mLottieAnimationView!!.useHardwareAcceleration()
                        } else {
                            mExitAppAnimationViewContainer!!.visibility = View.VISIBLE
                        }
                        mIsExitAdShown = true
                    }, 200)
                }

                override fun onAdLoaded() {

                }

                override fun onAdClicked() {

                }

                override fun onAdImpression() {

                }

                override fun onAdClose() {
                }
            }
            ad.show()
            logEvent(this, "Main_ExitWireAd_Show")
            return
        }

        super.onBackPressed()
        sIsRecreate = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (canDrawOverlays()) {
                switchNightScreen()
                logEvent(this, "night_screen_request_permission_success")
            }
        }
    }

    private fun initView() {
        darkModeContainer = dark_content_frame
        setupTopBar()
        setupDrawer()
        setupBottomNavigation()


        darkModeContainer?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!mScrollRecorded) {
                    logEvent(this@HomeActivity, "main_page_scroll")
                    mScrollRecorded = true
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        mDarkModeCards.add(ModeSwitchCard())
        val list = mutableListOf<AppInfo>()
        list.add(
            AppInfo(
                resources.getString(R.string.instagram),
                R.drawable.insgram,
                "com.instagram.android"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.google_play),
                R.drawable.google_play,
                "com.android.vending"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.chrome),
                R.drawable.chrome,
                "com.android.chrome"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.photos),
                R.drawable.photos,
                "com.google.android.apps.photos"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.google_pay),
                R.drawable.google_pay,
                "com.google.android.apps.walletnfcrel"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.fitness),
                R.drawable.fitness,
                "com.google.android.apps.fitness"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.pocket_casts),
                R.drawable.pocketcasts,
                "au.com.shiftyjelly.pocketcasts"
            )
        )
        list.add(
            AppInfo(
                resources.getString(R.string.ms_launcher),
                R.drawable.microsoft_auncher,
                "com.microsoft.launcher"
            )
        )
        mDarkModeCards.add(SupportedAppsCard(list))

        mDarkModeCards.add(
            InformationFlowCard(
                iconUrl = "",
                title = "A",
                content = "There have been rumors that WhatsApp was working on a dark theme for it" +
                        "`s popular chat app for a while now. Finally, in March 2020 a WhatsApp version…",
                url = ""
            )
        )

        mDarkModeCards.add(
            InformationFlowCard(
                "",
                "A",
                "There have been rumors that WhatsApp was working on a dark theme for it" +
                        "`s popular chat app for a while now. Finally, in March 2020 a WhatsApp version…",
                ""
            )
        )

        mDarkModeCards.add(WallpaperCard(getWallpapers()[12]))

        darkModeContainer?.layoutManager = LinearLayoutManager(this)
        mDarkModeCardAdapter = DarkModeCardAdapter(this, mDarkModeCards)
        darkModeContainer?.adapter = mDarkModeCardAdapter

    }

    private fun setupTopBar() {
        var statusBarHeight = Dimensions.getStatusBarHeight(this)
        if (statusBarHeight == 0) {
            statusBarHeight = Dimensions.pxFromDp(25f)
        }
        content_container.setPadding(0, statusBarHeight, 0, 0)

        menu.setOnClickListener {
            mDrawerLayout.openDrawer(mNavigationView)
        }
        help.setOnClickListener {
            openQA()
            logEvent(this, "main_page_qa_click")
        }

        icon_pro.setOnClickListener {
            logEvent(it.context, "Pro_Icon_Click")
            openPurchasePage("Icon")
        }

        updateProUi()
    }

    private fun setupDrawer() {
        mNavigationView = findViewById(R.id.navigation_view)
        mNavigationView.setItemIconTintList(null)
        val navigationContent = LayoutInflater.from(this)
            .inflate(R.layout.layout_home_navigation, mNavigationView, false)

        mDrawerLayout = findViewById(R.id.drawer_layout)

        mDrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(@NonNull drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(@NonNull drawerView: View) {
                logEvent(this@HomeActivity, "menu_show")
            }

            override fun onDrawerClosed(@NonNull drawerView: View) {
                logEvent(this@HomeActivity, "menu_hide")
            }

            override fun onDrawerStateChanged(newState: Int) {

            }
        })

        mNavigationView.addView(navigationContent)

        navigation_item_dark_pro.setOnClickListener(this)
        navigation_item_dark_mode.setOnClickListener(this)
        navigation_item_night_screen.setOnClickListener(this)
        navigation_item_wallpaper.setOnClickListener(this)
        navigation_item_toolbar.setOnClickListener(this)
        navigation_item_qa.setOnClickListener(this)
        navigation_item_settings.setOnClickListener(this)

        navigation_item_dark_pro.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_dark_mode.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_night_screen.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_wallpaper.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_toolbar.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_qa.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )
        navigation_item_settings.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.primary_background),
                0f,
                true
            )

        toolbar_switch.isClickable = false

        val n = Preferences.default.getBoolean(NOTIFICATION, true)
        toolbar_switch.isChecked = n
        updateNotification()

        if (RemoteConfig.instance.getBoolean("DarkProDrawerEnabled") && !BillingManager.isPremiumUser()) {
            navigation_item_dark_pro.visibility = View.VISIBLE
            navigation_item_dark_pro_bottom_line.visibility = View.VISIBLE

            if (Preferences.default.contains("pref_key_dark_pro_menu_clicked")) {
                navigation_item_dark_pro_new.visibility = View.GONE
            } else {
                navigation_item_dark_pro_new.visibility = View.VISIBLE
            }
        } else {
            navigation_item_dark_pro.visibility = View.GONE
            navigation_item_dark_pro_bottom_line.visibility = View.GONE
        }
    }

    private fun setupBottomNavigation() {
        navigation_dark_mode.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.navigation_bg_color),
                0f,
                true
            )
        navigation_night_screen.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.navigation_bg_color),
                0f,
                true
            )
        navigation_wallpaper.background =
            BackgroundDrawables.createBackgroundDrawable(
                resources.getColor(R.color.navigation_bg_color),
                0f,
                true
            )

        navigation_dark_mode.setOnClickListener {
            if (darkModeContainer?.visibility != View.VISIBLE) {
                logEvent(
                    it.context, "main_tab_click",
                    mapOf(
                        "index" to "darkmode"
                    )
                )
            }
            switchDarkMode()
        }
        navigation_night_screen.setOnClickListener {
            if (nightScreenContainer?.visibility != View.VISIBLE) {
                logEvent(
                    it.context, "main_tab_click",
                    mapOf(
                        "index" to "nightscreen"
                    )
                )
            }
            switchNightScreen()
        }
        navigation_wallpaper.setOnClickListener {
            if (wallpaperContainer?.visibility != View.VISIBLE) {
                logEvent(
                    it.context, "main_tab_click",
                    mapOf(
                        "index" to "wallpaper"
                    )
                )
                logEvent(it.context, "Wallpaper_Page_Show")
            }
            switchWallpaper()
        }
        switchDarkMode()
    }


    private fun updateProUi() {
        if (RemoteConfig.instance.getBoolean("DarkProDrawerEnabled") && !BillingManager.isPremiumUser()) {
            navigation_item_dark_pro?.visibility = View.VISIBLE
            navigation_item_dark_pro_bottom_line?.visibility = View.VISIBLE
        } else {
            navigation_item_dark_pro?.visibility = View.GONE
            navigation_item_dark_pro_bottom_line?.visibility = View.GONE
        }

        if (RemoteConfig.instance.getBoolean("DarkProIconEnabled") && !BillingManager.isPremiumUser()) {
            icon_pro.visibility = View.VISIBLE
        } else {
            val helpLayoutParams = help.layoutParams as ViewGroup.MarginLayoutParams
            val proLayoutParams = icon_pro.layoutParams as ViewGroup.MarginLayoutParams

            helpLayoutParams.marginEnd = proLayoutParams.marginEnd
            help.requestLayout()
            icon_pro.visibility = View.GONE
        }
        if (BillingManager.isPremiumUser()) {
            for (c in mDarkModeCards) {
                if (c is AdCard) {
                    mDarkModeCards.remove(c)
                }
            }
            mDarkModeCardAdapter?.notifyDataSetChanged()
        }
        nightScreenContainer?.updateProUi()
    }

    private fun showExitAppAnimation() {
        if (mLottieAnimationView != null) {
            mLottieAnimationView!!.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finish()
                    overridePendingTransition(0, 0)
                }
            })
            mLottieAnimationView!!.playAnimation()
        }
    }

    private fun openFiveStarsRateDialog() {
        dialog = FiveRateDialog(this, R.style.Transparent)
        dialog?.setSubmitListener(this)
        dialog?.show()
        logEvent(this, "five_star_rating_show")
    }

    private fun setupAds() {
        if (BillingManager.isPremiumUser()) return

        if (RemoteConfig.instance.getBoolean("AdSupportAppsEnabled")) {
            InterstitialAdManager.preload(
                AD_PLACEMENT_SUPPORT_APPS,
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getSupportAppsAdConfig())
            )
        }

        if (RemoteConfig.instance.getBoolean("AdExitWireEnabled")) {
            InterstitialAdManager.preload(
                AD_PLACEMENT_EXIT_WIRE,
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getExitWireAdConfig())
            )
        }
    }

    private fun loadHomeBannerAd() {
        if (RemoteConfig.instance.getBoolean("AdBottomBannerEnabled")
            && !(Compats.IS_XIAOMI_DEVICE && "V11" == Compats.miuiVersionName
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            && !BillingManager.isPremiumUser()
        ) {
            NmNativeAdLoader(Gson().fromJson<List<NmAdConfig>>(AdConfig.getHomeNativeAdConfig()),
                object : NmNativeAdListener {
                    override fun onAdLoaded(ad: NmNativeAd) {
                        for (card in mDarkModeCards) {
                            if (card is AdCard) {
                                mDarkModeCards.remove(card)
                            }
                        }

                        if (mDarkModeCards.size > 3) {
                            mDarkModeCards.add(3, AdCard(ad))
                        } else {
                            mDarkModeCards.add(AdCard(ad))
                        }
                         mDarkModeCardAdapter?.notifyDataSetChanged()
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
                        Log.d("HomeActivity", "home-native-banner log impression")
                        logEvent(this@HomeActivity, "Main_BottomBannerAd_Impression")
                    }
                })

            logEvent(this, "Main_BottomBannerAd_ShouldShow")
        }
    }

    private fun switchDarkMode() {
        updateProUi()

        navigation_dark_mode_img.setImageResource(R.drawable.navigation_dark_mode_selected)
        navigation_dark_mode_text.setTextColor(resources.getColor(R.color.navigation_text_selected_color))

        navigation_night_screen_img.setImageResource(R.drawable.navigation_night_screen_normal)
        navigation_night_screen_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        navigation_wallpaper_img.setImageResource(R.drawable.navigation_wallpaper_normal)
        navigation_wallpaper_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        darkModeContainer?.visibility = View.VISIBLE
        nightScreenContainer?.visibility = View.GONE
        wallpaperContainer?.visibility = View.GONE

        help.visibility = View.VISIBLE

        toolbarTitle.text = getString(R.string.navigation_dark_mode)

        loadHomeBannerAd()

        Log.d("HomeActivity", "switch Dark Mode")
    }

    private fun switchNightScreen() {
        if (nightScreenContainer == null) {
            nightScreenContainer = night_screen_content_frame_stub.inflate() as NightScreenContainer
        }
        updateProUi()

        navigation_dark_mode_img.setImageResource(R.drawable.navigation_dark_mode_normal)
        navigation_dark_mode_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        navigation_night_screen_img.setImageResource(R.drawable.navigation_night_screen_selected)
        navigation_night_screen_text.setTextColor(resources.getColor(R.color.navigation_text_selected_color))

        navigation_wallpaper_img.setImageResource(R.drawable.navigation_wallpaper_normal)
        navigation_wallpaper_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        darkModeContainer?.visibility = View.GONE
        nightScreenContainer?.visibility = View.VISIBLE
        wallpaperContainer?.visibility = View.GONE

        help.visibility = View.GONE
        toolbarTitle.text = getString(R.string.navigation_night_screen)

        nightScreenContainer?.switch()

        if (!BillingManager.isPremiumUser() && RemoteConfig.instance.getBoolean("AdNightScreenWireEnabled")) {
            InterstitialAdManager.preload(
                AD_PLACEMENT_NIGHT_SCREEN_WIRE,
                Gson().fromJson<List<NmAdConfig>>(AdConfig.getNightScreenWireAdConfig())
            )
        }

        logEvent(
            this, "night_screen_page_show", mapOf(
                "mode" to if (resources.getBoolean(R.bool.dark_mode)) "dark mode" else "day mode",
                "night_switch" to if (night_screen_status_switch.isChecked) "on" else "off"
            )
        )
        Log.d("HomeActivity", "switch Night Screen")
    }

    fun switchWallpaper() {
        if (wallpaperContainer == null) {
            wallpaperContainer = wallpaper_content_frame_stub.inflate() as WallpaperContainer
        }
        icon_pro.visibility = View.GONE

        navigation_dark_mode_img.setImageResource(R.drawable.navigation_dark_mode_normal)
        navigation_dark_mode_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        navigation_night_screen_img.setImageResource(R.drawable.navigation_night_screen_normal)
        navigation_night_screen_text.setTextColor(resources.getColor(R.color.navigation_text_normal_color))

        navigation_wallpaper_img.setImageResource(R.drawable.navigation_wallpaper_selected)
        navigation_wallpaper_text.setTextColor(resources.getColor(R.color.navigation_text_selected_color))
        toolbarTitle.text = getString(R.string.navigation_wallpaper)

        darkModeContainer?.visibility = View.GONE
        nightScreenContainer?.visibility = View.GONE
        wallpaperContainer?.visibility = View.VISIBLE

        wallpaperContainer?.switch()

        help.visibility = View.GONE

        Log.d("HomeActivity", "switch Wallpaper")
    }

    override fun onClick(v: View?) {
        when (v) {
            navigation_item_dark_pro -> {
                mDrawerLayout.closeDrawer(mNavigationView)
                navigation_item_dark_pro_new.visibility = View.GONE
                Preferences.default.putBoolean("pref_key_dark_pro_menu_clicked", true)

                logEvent(mNavigationView.context, "Pro_Drawer_Click")
                openPurchasePage("Drawer")
                logEvent(
                    this@HomeActivity, "menu_dark_pro_click"
                )
            }
            navigation_item_dark_mode -> {
                mDrawerLayout.closeDrawer(mNavigationView)
                switchDarkMode()
                logEvent(
                    this@HomeActivity, "menu_dark_mode_click"
                )
            }
            navigation_item_night_screen -> {
                mDrawerLayout.closeDrawer(mNavigationView)
                switchNightScreen()
                logEvent(
                    this@HomeActivity, "menu_night_screen_click"
                )
            }
            navigation_item_wallpaper -> {
                mDrawerLayout.closeDrawer(mNavigationView)
                switchWallpaper()
                logEvent(
                    this@HomeActivity, "menu_wallpaper_click"
                )
            }
            navigation_item_toolbar -> {
                toolbar_switch.isChecked = !toolbar_switch.isChecked
                Preferences.default.putBoolean(NOTIFICATION, toolbar_switch.isChecked)
                updateNotification()
                logEvent(
                    this@HomeActivity, "menu_notification_toolbar_click",
                    mapOf(
                        "to" to if (toolbar_switch.isChecked) "on" else "off"
                    )
                )
            }
            navigation_item_qa -> {
                openQA()
                mDrawerLayout.closeDrawer(mNavigationView)
                logEvent(
                    this@HomeActivity, "menu_qa_click"
                )
            }
            navigation_item_settings -> {
                openSettings()
                mDrawerLayout.closeDrawer(mNavigationView)
                logEvent(
                    this@HomeActivity, "menu_settings_click"
                )
            }
        }
    }

    inner class ToolbarUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.run {
                val tab = getStringExtra(TAB)
                tab?.let {
                    if (it == NIGHTSCREEN) {
                        nightScreenContainer?.onUserClickToolbarNightScreen()
                    }
                }
            }
        }
    }
}
