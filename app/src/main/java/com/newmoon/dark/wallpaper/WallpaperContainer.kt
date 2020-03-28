package com.newmoon.dark.wallpaper

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.newmoon.common.util.Dimensions
import com.newmoon.common.util.Navigations
import com.newmoon.dark.BuildConfig
import com.newmoon.dark.R
import com.newmoon.dark.logEvent
import com.newmoon.dark.ui.HomeActivity
import com.newmoon.dark.ui.WallpaperPreviewActivity
import kotlinx.android.synthetic.main.wallpaper_container.view.*

class WallpaperContainer : RelativeLayout {
    private var mWallpaperAdapter: WallpapersAdapter? = null
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        setupWallpaper()
    }

    fun switch() {
        if (mWallpaperAdapter == null) {
            mWallpaperAdapter = WallpapersAdapter(getWallpapers())
            wallpaper_content.layoutManager = GridLayoutManager(context, 3)
            wallpaper_content.adapter = mWallpaperAdapter
            wallpaper_content.addItemDecoration(CustomItemDecoration())
        }
    }

    private fun setupWallpaper() {
        wallpaper_to_top.setOnClickListener {
            wallpaper_content.scrollToPosition(0)
        }
        srl?.setOnRefreshListener {
            if (mWallpaperAdapter == null) {
                mWallpaperAdapter = WallpapersAdapter(getWallpapers())
                wallpaper_content.layoutManager = GridLayoutManager(context, 3)
                wallpaper_content.adapter = mWallpaperAdapter
                wallpaper_content.addItemDecoration(CustomItemDecoration())
            } else {
                mWallpaperAdapter!!.notifyDataSetChanged()
            }
            srl?.isRefreshing = false
        }
    }

    inner class WallpapersAdapter(val list: List<WallpaperInfo>) :
        RecyclerView.Adapter<WallpapersAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.wallpaper_item,
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
                Glide.with(wallpaper.context)
                    .setDefaultRequestOptions(
                        RequestOptions().centerInside().placeholder(R.drawable.wallpaper_plachholder).error(
                            R.drawable.wallpaper_plachholder
                        )
                    )
                    .load(info.thumbnail)
                    .into(wallpaper)
                wallpaper.setOnClickListener {
                    Navigations.startActivitySafely(
                        context,
                        WallpaperPreviewActivity.getLaunchIntent(context, position)
                    )
                    logEvent(context, "Wallpaper_Click")
                }
                if (BuildConfig.DEBUG) {
                    name.visibility = View.VISIBLE
                    name.text = info.name
                } else {
                    name.visibility = View.GONE
                }

                val outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height,
                            Dimensions.pxFromDp(0f).toFloat()
                        )
                    }
                }

                holder.itemView.clipToOutline = true
                holder.itemView.outlineProvider = outlineProvider
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val container: View = view.findViewById(R.id.container)
            val wallpaper: ImageView = view.findViewById(R.id.iv_wallpaper)
            val name: TextView = view.findViewById(R.id.name)

            init {
                val phoneWidth = Dimensions.getPhoneWidth(container.context)
                container.layoutParams.height =
                    ((phoneWidth - Dimensions.pxFromDp(4f) * 4) / 3 / 0.56f).toInt()
                wallpaper.layoutParams.height = container.layoutParams.height
            }
        }
    }

    private class CustomItemDecoration : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
            if (itemPosition == 0 || itemPosition == 1 || itemPosition == 2) {
                outRect.top = Dimensions.pxFromDp(0f)
            } else {
                outRect.top = 0
            }

            outRect.bottom = Dimensions.pxFromDp(3f)

            if (itemPosition % 3 == 0) {
                outRect.left = Dimensions.pxFromDp(0f)
                outRect.right = Dimensions.pxFromDp(3f)
            } else if (itemPosition % 3 == 2) {
                outRect.left = Dimensions.pxFromDp(3f)
                outRect.right = Dimensions.pxFromDp(0f)
            } else {
                outRect.right = 0
                outRect.left = 0
            }
        }
    }
}
