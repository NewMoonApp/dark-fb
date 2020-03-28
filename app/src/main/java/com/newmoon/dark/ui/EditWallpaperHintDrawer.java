package com.newmoon.dark.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.widget.RelativeLayout;

import com.newmoon.common.util.Dimensions;
import com.newmoon.dark.util.LauncherAnimUtils;


public class EditWallpaperHintDrawer {
    // Repeat count + 1
    private static final int PLAY_COUNT = 2;

    /**
     * Minimum animation repeat count once started. If {@link #requestCancel(OnCancelCompleteListener)} is invoked
     * before completing this count, animation would continue to play until this count.
     */
    private static final int MIN_COUNT = 1;

    private ValueAnimator mAnimator;
    private int mRepeatCount;
    private boolean mCancelRequested;
    private DrawView mCanvasView;
    private OnCancelCompleteListener mCancelCompleteListener;

    private AnimationDelegate mAnimDelegate;

    public interface OnCancelCompleteListener {
        void onCancelComplete(EditWallpaperHintDrawer drawer);
    }

    public EditWallpaperHintDrawer(final DrawView canvasView, int width, int height) {
        mCanvasView = canvasView;
        mAnimDelegate = new AnimationDelegate(canvasView, width, height);

        mAnimator = LauncherAnimUtils.ofFloat(canvasView, 0f, 1f);
        mAnimator.setDuration(1000);
        mAnimator.setInterpolator(LauncherAnimUtils.ACCELERATE_DECELERATE);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimDelegate.postAnimationProgress(animation.getAnimatedFraction());
                canvasView.invalidate();
            }

        });
        mAnimator.addListener(new AnimatorListener());
    }

    public void start() {
        mAnimDelegate.setIsRight(false);
        mAnimator.start();

    }

    public boolean isRunning() {
        return mAnimator.isRunning();
    }

    public void draw(Canvas canvas) {
        mAnimDelegate.draw(canvas);
    }

    public void requestCancel(OnCancelCompleteListener cancelCompleteListener) {
        mCancelRequested = true;
        mCancelCompleteListener = cancelCompleteListener;
    }

    private class AnimatorListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationStart(Animator animation) {
            mCancelRequested = false;
            mCanvasView.setClear(false);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mCancelRequested && mRepeatCount >= MIN_COUNT) {
                animation.cancel();
                if (mCancelCompleteListener != null) {
                    mCancelCompleteListener.onCancelComplete(EditWallpaperHintDrawer.this);
                }
                return;
            }

            if (++mRepeatCount < PLAY_COUNT) {
                mCanvasView.setClear(true);
                mCanvasView.invalidate();
                mAnimDelegate.setIsRight(mRepeatCount % 2 == 1);
                animation.setStartDelay(200);
                animation.start();
            } else {
                RelativeLayout parent = (RelativeLayout) mCanvasView.getParent();
                if (parent != null) {
                    parent.removeView(mCanvasView);
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            RelativeLayout parent = (RelativeLayout) mCanvasView.getParent();
            if (parent != null) {
                parent.removeView(mCanvasView);
            }
        }
    }

    /**
     * Delegate for drawing news hint animation.
     */
    private static class AnimationDelegate {
        private static final int TAIL_COLOR_UP = 0x77ffffff;
        private static final int TAIL_COLOR_DOWN = Color.TRANSPARENT;

        private static final int CIRCLE_COLOR_UP = 0xffffffff;
        private static final int CIRCLE_COLOR_DOWN = 0xffffffff;

        private static final int CIRCLE_RADIUS = Dimensions.INSTANCE.pxFromDp(12);

        private View mCanvasView;

        private float mUCx;
        private float mUCy;

        private float mOUCx;

        private Path mUpPath;

        private Paint mCirclePaint;
        private Paint mTailPaint;

        private boolean mIsRight;

        public AnimationDelegate(final View canvasView, int width, int height) {
            mCanvasView = canvasView;

            mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCirclePaint.setColor(CIRCLE_COLOR_UP);
            mCirclePaint.setStyle(Paint.Style.FILL);

            mTailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTailPaint.setColor(TAIL_COLOR_UP);
            mTailPaint.setStyle(Paint.Style.FILL);

            mUpPath = new Path();
            mUpPath.setFillType(Path.FillType.EVEN_ODD);

            mUCx = width / 2;
            mUCy = height / 2;

            mOUCx = mUCx;
        }

        public void postAnimationProgress(float progress) {
            progress *= (mIsRight ? 1 : -1);
            mUCx = mCanvasView.getWidth() * (1.0f + progress) / 2 - progress * CIRCLE_RADIUS;

            mUpPath.reset();
            mUpPath.moveTo(mOUCx, mUCy - CIRCLE_RADIUS);
            mUpPath.lineTo(mUCx, mUCy - CIRCLE_RADIUS);
            mUpPath.lineTo(mUCx, mUCy + CIRCLE_RADIUS);
            mUpPath.lineTo(mOUCx, mUCy + CIRCLE_RADIUS);
            mUpPath.close();

            mTailPaint.setAlpha(200);
        }

        public void draw(Canvas canvas) {

            mTailPaint.setShader(new LinearGradient(mOUCx, mUCy, mUCx, mUCy,
                    TAIL_COLOR_DOWN, TAIL_COLOR_UP, Shader.TileMode.CLAMP));
            mCirclePaint.setShader(new LinearGradient(mUCx - CIRCLE_RADIUS,
                    mUCy, mUCx + CIRCLE_RADIUS,
                    mUCy, CIRCLE_COLOR_UP, CIRCLE_COLOR_DOWN, Shader.TileMode.CLAMP));

            canvas.drawPath(mUpPath, mTailPaint);
            canvas.drawArc(new RectF(mOUCx - CIRCLE_RADIUS, mUCy - CIRCLE_RADIUS, mOUCx + CIRCLE_RADIUS,
                    mUCy + CIRCLE_RADIUS), mIsRight ? 90 : 270, 180, false, mTailPaint);
            canvas.drawCircle(mUCx, mUCy, CIRCLE_RADIUS, mCirclePaint);
        }

        public void setIsRight(boolean isRight) {
            mIsRight = isRight;
        }
    }
}
