package com.newmoon.dark

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.flurry.android.FlurryAgent
import com.google.firebase.analytics.FirebaseAnalytics

const val toFirebase = true
const val toFlurry = true
fun logEvent(context: Context, eventName: String, map: Map<String, String> = mapOf()) {
    if (toFirebase) {
        val bundle = Bundle()
        map.forEach {
            bundle.putString(it.key, it.value)
        }
        FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
    }
    if (toFlurry) {
        if (map.isEmpty()) {
            FlurryAgent.logEvent(eventName)
        } else {
            FlurryAgent.logEvent(eventName, map)
        }
    }
    Log.d("event", eventName)
}