package com.newmoon.dark.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.newmoon.dark.R;

public class ProgressFrameLayout extends FrameLayout {
    private SuccessTickView mSuccessTickView;
    private ProgressWheel mProgressWheel;
    private Runnable mLastHideRunnable;
    private boolean isFinish = false;

    public ProgressFrameLayout(Context context) {
        super(context);
    }

    public ProgressFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mProgressWheel = (ProgressWheel) findViewById(R.id.progressWheel);
        mSuccessTickView = (SuccessTickView) findViewById(R.id.tickview);
        reset();
    }

    public void reset() {
        isFinish = false;
        mProgressWheel.setFinishSpeed(500f / 360f);
        mProgressWheel.setSpinSpeed(125f / 360f);
        mProgressWheel.setBarSpinCycleTime(530f);
        mProgressWheel.setVisibility(VISIBLE);
        mProgressWheel.spin();
        if (mSuccessTickView != null) {
            mSuccessTickView.setVisibility(View.GONE);
        }
    }

    public void finish(final Runnable hideRunnable) {
        mProgressWheel.postProgress(100);
        mProgressWheel.setCallback(new ProgressWheel.ProgressCallback() {
            @Override
            public void onProgressUpdate(float progress) {
                if (!isFinish && progress >= 0.63f) {
                    isFinish = true;
                    if (mSuccessTickView != null) {
                        mSuccessTickView.setVisibility(View.VISIBLE);
                        mSuccessTickView.startTickAnim();
                    }
                    if (mLastHideRunnable != null) {
                        removeCallbacks(mLastHideRunnable);
                    }
                    if (hideRunnable != null) {
                        mLastHideRunnable = hideRunnable;
                        hideRunnable.run();
                    }
                }
            }
        });
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public SuccessTickView getSuccessTickView() {
        return mSuccessTickView;
    }
}
