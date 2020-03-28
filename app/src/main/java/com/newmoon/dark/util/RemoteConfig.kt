package com.newmoon.dark.util

import android.util.Log

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.newmoon.common.util.Threads
import com.newmoon.dark.BuildConfig
import com.newmoon.dark.R

class RemoteConfig {

    private val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private object RemoteConfigHolder {
        val sInstance = RemoteConfig()
    }

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        mFirebaseRemoteConfig.setConfigSettings(configSettings)
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)

        fetchConfig()

        Threads.postOnSingleThreadExecutor(Runnable {
            Log.d(
                RemoteConfig::class.java.simpleName,
                "valve is " + getBoolean("AdBottomBannerEnabled")
            )
        })
    }

    private fun fetchConfig() {
        val expiration = if (BuildConfig.DEBUG) 0 else CacheExpirationSeconds
        mFirebaseRemoteConfig.fetch(expiration.toLong())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mFirebaseRemoteConfig.activateFetched()
                    Log.d(RemoteConfig::class.java.simpleName, "fetch succeed!")

                    Threads.postOnSingleThreadExecutor(Runnable {
                        Log.d(
                            RemoteConfig::class.java.simpleName,
                            "valve is " + getBoolean("AdBottomBannerEnabled")
                        )
                    })
                    notifyFetchDone(true)
                } else {
                    Log.d(RemoteConfig::class.java.simpleName, "fetch failed!")
                    notifyFetchDone(false)
                }
            }
    }

    fun getString(key: String): String? {
        var value: String? = null
        try {
            value = mFirebaseRemoteConfig.getString(key)
        } catch (ignored: Exception) {
        }

        return value
    }

    fun getLong(key: String, defValue: Long): Long {
        var code = defValue
        try {
            code = mFirebaseRemoteConfig.getLong(key)
        } catch (ignored: Exception) {
        }

        return code
    }

    fun getBoolean(key: String): Boolean {
        try {
            return mFirebaseRemoteConfig.getBoolean(key)
        } catch (e: Exception) {
        }

        return false
    }

    companion object {

        private val CacheExpirationSeconds = 3600// 1 小时

        val instance: RemoteConfig
            get() = RemoteConfigHolder.sInstance
    }

    private fun notifyFetchDone(succeed: Boolean) {
        for (listener in listeners) {
            listener.onDone(succeed)
        }
    }

    private val listeners = mutableListOf<FetchListener>()

    fun addFetchListener(listener: FetchListener) {
        listeners.add(listener)
    }

    interface FetchListener {
        fun onDone(succeed: Boolean)
    }
}
