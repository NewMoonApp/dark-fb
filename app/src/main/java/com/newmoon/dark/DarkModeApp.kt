package com.newmoon.dark

import android.app.Activity
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.multidex.MultiDex
import ca.allanwang.kau.logging.KL
import ca.allanwang.kau.utils.buildIsLollipopAndUp
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ApplicationVersionSignature
import com.crashlytics.android.Crashlytics
import com.facebook.ads.AudienceNetworkAds
import com.flurry.android.FlurryAgent
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.newmoon.common.BaseApplication
import com.newmoon.common.util.Calendars
import com.newmoon.common.util.Preferences
import com.newmoon.dark.pro.BillingManager
import com.newmoon.dark.timer.CheckThread
import com.newmoon.dark.timer.scheduledTimerTask
import com.newmoon.dark.util.RemoteConfig
import com.pitchedapps.frost.BuildConfig
import com.pitchedapps.frost.db.FrostDatabase
import com.pitchedapps.frost.glide.GlideApp
import com.pitchedapps.frost.services.scheduleNotificationsFromPrefs
import com.pitchedapps.frost.services.setupNotificationChannels
import com.pitchedapps.frost.utils.BuildUtils
import com.pitchedapps.frost.utils.FrostPglAdBlock
import com.pitchedapps.frost.utils.L
import com.pitchedapps.frost.utils.Prefs
import com.pitchedapps.frost.utils.Showcase
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.Random

class DarkModeApp : BaseApplication() {

    private val AF_DEV_KEY = "TAMFuVKCcfij3mZxyAzj5P"

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MultiDex.install(this)

        FlurryAgent.Builder()
            .withLogEnabled(true)
            .build(this, "2PGGV9V3ZTYY453WGBHT")

        Fabric.with(this, Crashlytics())

        RemoteConfig.instance

        AudienceNetworkAds.initialize(this)

        MobileAds.initialize(this, "ca-app-pub-9614858052567286~1279081364")

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity is com.google.android.gms.ads.AdActivity) {
                    activity.finish()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

        })

        logStartEvents()

        val conversionListener = object : AppsFlyerConversionListener {

            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {

                for (attrName in conversionData.keys) {
                    Log.d("LOG_TAG", "attribute: " + attrName + " = " + conversionData[attrName])
                }
            }

            override fun onConversionDataFail(errorMessage: String) {
                Log.d("LOG_TAG", "error getting conversion data: $errorMessage")
            }

            override fun onAppOpenAttribution(conversionData: Map<String, String>) {

                for (attrName in conversionData.keys) {
                    Log.d("LOG_TAG", "attribute: " + attrName + " = " + conversionData[attrName])
                }

            }

            override fun onAttributionFailure(errorMessage: String) {
                Log.d("LOG_TAG", "error onAttributionFailure : $errorMessage")
            }
        }

        AppsFlyerLib.getInstance().init(AF_DEV_KEY, conversionListener, applicationContext)
        AppsFlyerLib.getInstance().startTracking(this)

        CheckThread(this).start()
        scheduledTimerTask(this)

        BillingManager.init(this)
        fonCreate()
    }

    private fun logStartEvents() {
        logEvent(this, "process_start")

        val last_record = "last record"
        val last = Preferences.default.getLong(last_record, 0)
        val now = System.currentTimeMillis()
        val isSameday = Calendars.isSameDay(last, now)
        if (!isSameday) {
            logEvent(this, "process_start_daily")
            Preferences.default.putLong(last_record, now)
        }
    }

    fun fonCreate() {
        if (!buildIsLollipopAndUp) { // not supported
            super.onCreate()
            return
        }

//        if (LeakCanary.isInAnalyzerProcess(this)) return
//        refWatcher = LeakCanary.install(this)
        initPrefs()
        initBugsnag()

        L.i { "Begin Frost for Facebook" }
        FrostPglAdBlock.init(this)

        super.onCreate()

        setupNotificationChannels(applicationContext)

        scheduleNotificationsFromPrefs()

        /**
         * Drawer profile loading logic
         * Reload the image on every version update
         */
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String) {
                val c = imageView.context
                val request = GlideApp.with(c)
                val old = request.load(uri).apply(RequestOptions().placeholder(placeholder))
                request.load(uri).apply(
                    RequestOptions()
                        .signature(ApplicationVersionSignature.obtain(c))
                )
                    .thumbnail(old).into(imageView)
            }
        })
        if (BuildConfig.DEBUG) {
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityDestroyed(activity: Activity) {
                    L.d { "Activity ${activity.localClassName} destroyed" }
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    L.d { "Activity ${activity.localClassName} created" }
                }
            })
        }
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@DarkModeApp)
            modules(FrostDatabase.module(this@DarkModeApp))
        }
    }

    private fun initPrefs() {
        Showcase.initialize(this, "${BuildConfig.APPLICATION_ID}.showcase")
        Prefs.initialize(this, "${BuildConfig.APPLICATION_ID}.prefs")
        KL.shouldLog = { BuildConfig.DEBUG }
        Prefs.verboseLogging = false
        if (Prefs.installDate == -1L) {
            Prefs.installDate = System.currentTimeMillis()
        }
        if (Prefs.identifier == -1) {
            Prefs.identifier = Random().nextInt(Int.MAX_VALUE)
        }
        Prefs.lastLaunch = System.currentTimeMillis()
    }

    private fun initBugsnag() {
        if (BuildConfig.DEBUG) {
            return
        }
        if (!BuildConfig.APPLICATION_ID.startsWith("com.pitchedapps.frost")) {
            return
        }
        val version = BuildUtils.match(BuildConfig.VERSION_NAME)
            ?: return L.d { "Bugsnag disabled for ${BuildConfig.VERSION_NAME}" }
        val config = Configuration("83cf680ed01a6fda10fe497d1c0962bb").apply {
            appVersion = version.versionName
            releaseStage = BuildUtils.getStage(BuildConfig.BUILD_TYPE)
            notifyReleaseStages = BuildUtils.getAllStages()
            autoCaptureSessions = Prefs.analytics
            enableExceptionHandler = Prefs.analytics
        }
        Bugsnag.init(this, config)
        L.bugsnagInit = true
        Bugsnag.setUserId(Prefs.frostId)
        Bugsnag.addToTab("Build", "Application", BuildConfig.APPLICATION_ID)
        Bugsnag.addToTab("Build", "Version", BuildConfig.VERSION_NAME)

        Bugsnag.beforeNotify { error ->
            when {
                error.exception.stackTrace.any { it.className.contains("XposedBridge") } -> false
                else -> true
            }
        }
    }
}