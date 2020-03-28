package com.newmoon.dark.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.newmoon.dark.R;

public class FaqItemView extends FrameLayout {

    public TextView tvTitle;

    public FaqItemView(Context context) {
        this(context, null);
    }

    public FaqItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaqItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_faq, this);

        tvTitle = findViewById(R.id.title);
    }
}
