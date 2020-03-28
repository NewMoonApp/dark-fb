package com.newmoon.dark.pro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import androidx.annotation.DrawableRes;
import androidx.viewpager.widget.PagerAdapter;

import com.newmoon.common.view.TypefacedTextView;
import com.newmoon.dark.R;

import java.util.ArrayList;
import java.util.List;

class WelcomePagerAdapter extends PagerAdapter {

    private final static int GUIDE_VIEWPAGER_COUNT = 3;

    private final static int[] VIEWPAGER_ITEMS_IDS_IMAGE = new int[]{
            R.drawable.pro_image_1,
            R.drawable.pro_image_2,
            R.drawable.pro_image_3
    };
    private final static int[] VIEWPAGER_ITEMS_IDS_TITLE = new int[]{
            R.string.message_welcome_viewpager_item_title_0,
            R.string.message_welcome_viewpager_item_title_1,
            R.string.message_welcome_viewpager_item_title_2
    };

    private final static int[] VIEWPAGER_ITEMS_IDS_BODY = new int[]{
            R.string.message_welcome_viewpager_item_body_0,
            R.string.message_welcome_viewpager_item_body_1,
            R.string.message_welcome_viewpager_item_body_2,
    };

    private final List<ViewGroup> mItemList = new ArrayList<>();

    WelcomePagerAdapter(Context context) {
        for (int i = 0; i < GUIDE_VIEWPAGER_COUNT; i++) {
            ViewGroup item = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.item_welcome_guide_viewpager, null);
            TypefacedTextView title = item.findViewById(R.id.welcome_guide_viewpager_title);
            TypefacedTextView body = item.findViewById(R.id.welcome_guide_viewpager_body);
            ImageView imageView = item.findViewById(R.id.viewpager_image);

            title.setText(context.getResources().getString(VIEWPAGER_ITEMS_IDS_TITLE[i]));
            body.setText(context.getResources().getString(VIEWPAGER_ITEMS_IDS_BODY[i]));
            imageView.setImageResource(VIEWPAGER_ITEMS_IDS_IMAGE[i]);
            mItemList.add(item);
        }
    }

    @Override
    public int getCount() {
        return GUIDE_VIEWPAGER_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @DrawableRes
    int getImageResId() {
        return 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mItemList.get(position));
        return mItemList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mItemList.get(position));
    }
}
