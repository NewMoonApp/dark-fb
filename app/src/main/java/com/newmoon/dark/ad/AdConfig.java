package com.newmoon.dark.ad;

import com.newmoon.dark.BuildConfig;
import com.newmoon.dark.util.RemoteConfig;

public class AdConfig {

    public static String getHomeNativeAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/2247696110\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdHomeNativeConfig");
    }

    public static String getNightScreenNativeAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/2247696110\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdNightScreenNativeConfig");
    }

    public static String getSupportAppsAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/1033173712\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdSupportAppsConfig");
    }

    public static String getWallpaperExitAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/1033173712\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdWallpaperExitConfig");
    }

    public static String getExitWireAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/1033173712\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdExitWireConfig");
    }

    public static String getNightScreenWireAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/1033173712\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdNightScreenWireConfig");
    }

    public static String getAppOpenAdConfig() {
        if (BuildConfig.DEBUG) {
            return "[{\"vendorName\":\"admob\", \"adUnitId\":\"ca-app-pub-3940256099942544/1033173712\", \"index\":1}]";
        }

        return RemoteConfig.Companion.getInstance().getString("AdAppOpenConfig");
    }
}
