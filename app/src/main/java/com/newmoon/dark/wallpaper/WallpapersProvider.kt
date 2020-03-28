package com.newmoon.dark.wallpaper

import com.newmoon.dark.util.RemoteConfig
import java.io.Serializable

data class WallpaperInfo(
    var name: String,
    val thumbnail: String,
    val bigPicture: String
) : Serializable

const val THUMBNAIL_FOLDER =
    "https://darkmode.s3.us-east-2.amazonaws.com/darkwebp/low_revolution/"
const val BIG_PICTURE_FOLDER =
    "https://darkmode.s3.us-east-2.amazonaws.com/darkwebp/high_revolution/"

fun getWallpapers(): List<WallpaperInfo> {
    val list = mutableListOf<WallpaperInfo>()

    var wallpaperStr = RemoteConfig.instance.getString("Wallpapers")!!
    var wallpaperArr = wallpaperStr.substring(1, wallpaperStr.length-1).split(",")

    for (w in wallpaperArr){
        list.add(
            WallpaperInfo(
                w,
                "$THUMBNAIL_FOLDER$w.webp",
                "$BIG_PICTURE_FOLDER$w.webp"
            )
        )
    }
    return list
}