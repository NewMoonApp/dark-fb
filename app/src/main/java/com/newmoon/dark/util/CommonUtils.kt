package com.newmoon.dark.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils

fun launchAppDetail(context: Context, appPkg: String) {    //appPkg 是应用的包名
    val GOOGLE_PLAY = "com.android.vending"//这里对应的是谷歌商店，跳转别的商店改成对应的即可
    try {
        if (TextUtils.isEmpty(appPkg))
            return
        val uri = Uri.parse("market://details?id=" + appPkg)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(GOOGLE_PLAY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
    }
}



