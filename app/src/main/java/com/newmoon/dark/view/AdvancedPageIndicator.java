package com.newmoon.dark.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import com.newmoon.common.util.Dimensions;
import com.newmoon.dark.BuildConfig;
import com.newmoon.dark.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedPageIndicator extends View {

    private static final String TAG = AdvancedPageIndicator.class.getSimpleName();

    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_VERBOSE = false && BuildConfig.DEBUG;

    private float mSpacing;
    private float mRadiusMin;
    private float mCircleRadius;
    private float mIndicatorRadius;
    private int mCircleColor;
    private int mIndicatorColor;

    private Paint mPathPaint;

    private int mCurIndex;
    private int mWidth;

    // Reused objects for performance
    private Path mPath = new Path();
    private RectF mDesRect;
    private RectF mPlusHorRect;
    private RectF mPlusVerRect1;
    private RectF mPlusVerRect2;
    private int mPlusColor;
    private float mPlusRadius;
    private int mRectColor;
    private float mRectRadius;
    private float mCircleVerticalPadding;
    private float mCircleHorizontalPadding;

    private List<IndicatorMark.MarkerType> mMarkList;
    private Map<IndicatorMark.MarkerType, Boolean> mMarkerVisible;
    private IndicatorMark mIndicatorMark;

    private boolean mIsRtl;

    public AdvancedPageIndicator(Context context) {
        this(context, null);
    }

    public AdvancedPageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdvancedPageIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void addMarkers(List<IndicatorMark.MarkerType> list) {
        mMarkList.clear();
        mMarkList.addAll(list);
        invalidate();
        requestLayout();
    }

    public void addMarker(int x, IndicatorMark.MarkerType type) {
        if (!checkIndexValidity(x, true)) {
            return;
        }
        mMarkList.add(x, type);
        invalidate();
        requestLayout();
    }

    public void setActiveMarker(int index) {
    }

    public void removeAllMarkers() {
        mMarkList.clear();
        invalidate();
    }

    public void removeMarker(int x, boolean moveForward) {
        if (!checkIndexValidity(x, false)) {
            return;
        }
        if (moveForward && x <= mCurIndex) {
            mCurIndex--;
        }
        mMarkList.remove(x);
        invalidate();
        requestLayout();
        setIndex(mCurIndex);
    }

    public void updateMarker(int x, IndicatorMark.MarkerType type) {
        mMarkList.set(x, type);
        invalidate();
        requestLayout();
        setIndex(mCurIndex);
    }

    private boolean checkIndexValidity(int index, boolean add) {
        if (add ? (index > mMarkList.size()) : (index >= mMarkList.size())) {
            return false;
        }
        return true;
    }

    public void setMarkerTypeVisible(IndicatorMark.MarkerType type, boolean visible) {
        mMarkerVisible.put(type, visible);
        invalidate();
        requestLayout();
        setIndex(mCurIndex);
    }

    private void init(Context context, AttributeSet attrs) {
        mIsRtl = Dimensions.isRtl();

        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdvancedPageIndicator);
        mSpacing = a.getDimension(R.styleable.AdvancedPageIndicator_circleSpacing, Dimensions.pxFromDp(5));
        mRadiusMin = a.getDimension(R.styleable.AdvancedPageIndicator_minRadius, Dimensions.pxFromDp(0.01f));
        mCircleRadius = a.getDimension(R.styleable.AdvancedPageIndicator_circleRadiusPageIndicator, Dimensions.pxFromDp(2.33f));
        mIndicatorRadius = a.getDimension(R.styleable.AdvancedPageIndicator_indicatorRadius, mCircleRadius);
        mCircleVerticalPadding = a.getDimension(R.styleable.AdvancedPageIndicator_circleVerticalPadding, Dimensions.pxFromDp(1f));
        mCircleHorizontalPadding = a.getDimension(R.styleable.AdvancedPageIndicator_circleHorizontalPadding, Dimensions.pxFromDp(1f));
        mCircleColor = a.getColor(R.styleable.AdvancedPageIndicator_circleColor, 0x66FFFFFF);
        mIndicatorColor = a.getColor(R.styleable.AdvancedPageIndicator_indicatorColor, 0xFFFFFFFF);
        a.recycle();

        mDesRect = new RectF();
        mPlusHorRect = new RectF();
        mPlusVerRect1 = new RectF();
        mPlusVerRect2 = new RectF();

        mMarkList = new ArrayList<>();
        mMarkerVisible = new HashMap<>();
        for (IndicatorMark.MarkerType type : IndicatorMark.MarkerType.values()) {
            mMarkerVisible.put(type, true);
        }
        mIndicatorMark = new IndicatorMark(
                new IndicatorMark.Marker(IndicatorMark.MarkerType.CIRCLE, mIndicatorRadius + mCircleHorizontalPadding, mIndicatorRadius + mCircleVerticalPadding, mRadiusMin),
                new IndicatorMark.Marker(IndicatorMark.MarkerType.CIRCLE, mIndicatorRadius + mCircleHorizontalPadding, mIndicatorRadius + mCircleVerticalPadding, mIndicatorRadius),
                mIndicatorColor);

        mPlusColor = mCircleColor;
        mPlusRadius = mCircleRadius;
        mRectColor = mCircleColor;
        mRectRadius = mCircleRadius;
    }

    public void setIndicatorColor(int color) {
        mCircleColor = (mCircleColor & 0xff000000) | (color & 0x00ffffff);
        mRectColor = mCircleColor;
        mIndicatorColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = getDefaultWidth();
        setMeasuredDimension(mWidth, getDefaultHeight());
    }

    public int getDefaultWidth() {
        int visibleCount = getVisibleMarkerCount();
        return (int) Math.ceil(visibleCount * 2 * mCircleRadius + (visibleCount - 1) * mSpacing
                + (mIndicatorRadius - mCircleRadius) * 2 + mCircleHorizontalPadding * 2);
    }

    public int getDefaultHeight() {
        return (int) Math.ceil(2 * mIndicatorRadius + mCircleVerticalPadding * 2);
    }

    private int getVisibleMarkerCount() {
        int visibleCount = 0;
        for (IndicatorMark.MarkerType type : mMarkList) {
            if (mMarkerVisible.get(type)) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    private int getVisibleIndex(int index) {
        if (index < 0 || index >= mMarkList.size()) {
            return 0;
        }
        int visibleIndex = -1;
        for (int i = 0; i <= index; i++) {
            if (mMarkerVisible.get(mMarkList.get(i))) {
                visibleIndex++;
            }
        }
        return visibleIndex;
    }

    private int getFirstVisibleIndex() {
        for (int i = 0; i < mMarkList.size(); i++) {
            if (mMarkerVisible.get(mMarkList.get(i))) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibleMarkerCount() < 1) {
            // No page so draw nothing
            return;
        }

        // Draw circle
        mPathPaint.setAntiAlias(true);
        mPathPaint.setStyle(Paint.Style.FILL);
        float positionX;
        float radius = mCircleRadius;
        if (mMarkList.get(getFirstVisibleIndex()) == IndicatorMark.MarkerType.RECT) radius = mRectRadius;
        if (mMarkList.get(getFirstVisibleIndex()) == IndicatorMark.MarkerType.PLUS_SIGN) radius = mPlusRadius;
        if (mIsRtl) {
            positionX = mWidth - (mIndicatorRadius - radius) - 2 * radius - mCircleHorizontalPadding;
        } else {
            positionX = mIndicatorRadius - radius + mCircleHorizontalPadding;
        }
        for (IndicatorMark.MarkerType type : mMarkList) {
            if (!mMarkerVisible.get(type)) {
                continue;
            }
            if (type == IndicatorMark.MarkerType.PLUS_SIGN) {
                mDesRect.set(positionX, mIndicatorRadius - mPlusRadius + mCircleVerticalPadding, 2 * mPlusRadius + positionX, mIndicatorRadius + mPlusRadius + mCircleVerticalPadding);
                float plusWidth = mDesRect.width() / 3;
                float delta = mDesRect.height() / 2 - plusWidth / 2;
                mPlusHorRect.set(mDesRect.left, mDesRect.top + delta, mDesRect.right, mDesRect.bottom - delta);
                mPlusVerRect1.set(mDesRect.left + delta, mDesRect.top, mDesRect.right - delta, mDesRect.bottom - delta - plusWidth);
                mPlusVerRect2.set(mDesRect.left + delta, mDesRect.top + delta + plusWidth, mDesRect.right - delta, mDesRect.bottom);
                mPathPaint.setColor(mPlusColor);
                canvas.drawRoundRect(mPlusHorRect, 1, 1, mPathPaint);
                canvas.drawRoundRect(mPlusVerRect1, 1, 1, mPathPaint);
                canvas.drawRoundRect(mPlusVerRect2, 1, 1, mPathPaint);
            } else if (type == IndicatorMark.MarkerType.RECT) {
                mDesRect.set(positionX, mIndicatorRadius - mRectRadius + mCircleVerticalPadding, 2 * mRectRadius + positionX, mIndicatorRadius + mRectRadius + mCircleVerticalPadding);
                mPathPaint.setColor(mRectColor);
                canvas.drawRoundRect(mDesRect, 1, 1, mPathPaint);
            } else {
                mPathPaint.setColor(mCircleColor);
                canvas.drawCircle(positionX + mCircleRadius, mIndicatorRadius + mCircleVerticalPadding, mCircleRadius, mPathPaint);
            }
            positionX = positionX + (mIsRtl ? -1 : 1) * (2 * mCircleRadius + mSpacing);
        }

        // Draw indicator
        if (mIndicatorMark.getHeadDot().getRadius() == 0 && mIndicatorMark.getFootDot().getRadius() == 0) {
            return;
        }
        mPathPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPathPaint.setStrokeWidth(1);
        mPathPaint.setColor(mIndicatorColor);
        mIndicatorMark.makePath(mPath);
        canvas.drawPath(mPath, mPathPaint);
        canvas.drawCircle(mIndicatorMark.getHeadDot().getCenterX() + mCircleHorizontalPadding, mIndicatorMark.getHeadDot().getCenterY(), mIndicatorMark.getHeadDot().getRadius(), mPathPaint);
        canvas.drawCircle(mIndicatorMark.getFootDot().getCenterX() + mCircleHorizontalPadding, mIndicatorMark.getFootDot().getCenterY(), mIndicatorMark.getFootDot().getRadius(), mPathPaint);
    }

    public void setIndex(int index) {
        if (index < 0 || index >= mMarkList.size()) {
            return;
        }
        mWidth = getDefaultWidth();

        mPlusColor = mCircleColor;
        mPlusRadius = mCircleRadius;
        mRectColor = mCircleColor;
        mRectRadius = mCircleRadius;

        if (mMarkList.get(index) == IndicatorMark.MarkerType.PLUS_SIGN) {
            mPlusColor = mIndicatorColor;
            mPlusRadius = mIndicatorRadius;
            mIndicatorMark.getHeadDot().setRadius(0);
            mIndicatorMark.getFootDot().setRadius(0);
        } else if (mMarkList.get(index) == IndicatorMark.MarkerType.RECT) {
            mRectColor = mIndicatorColor;
            mRectRadius = mIndicatorRadius;
            mIndicatorMark.getHeadDot().setRadius(0);
            mIndicatorMark.getFootDot().setRadius(0);
        } else {
            float centerX;
            if (mIsRtl) {
                centerX = mWidth - mIndicatorRadius - getVisibleIndex(index) * (2 * mCircleRadius + mSpacing);
            } else {
                centerX = mIndicatorRadius + getVisibleIndex(index) * (2 * mCircleRadius + mSpacing);
            }
            mIndicatorMark.getHeadDot().setCenterX(centerX);
            mIndicatorMark.getFootDot().setCenterX(centerX);
            mIndicatorMark.getHeadDot().setRadius(mIndicatorRadius);
            mIndicatorMark.getFootDot().setRadius(mIndicatorRadius);
        }

        invalidate();
        mCurIndex = index;
    }

    public void onScrolling(int position, float offsetPercentage) {
        if (DEBUG_VERBOSE) {
            Log.d(TAG, "position : " + position + " offset: " + offsetPercentage);
        }
        if (Float.isInfinite(offsetPercentage) || Float.isNaN(offsetPercentage)) {
            if (DEBUG_VERBOSE) {
                Log.e(TAG, "Page offset percentage is NaN or Infinite");
            }
            return;
        }
        // Unify offset to positive
        float unifiedOffset = offsetPercentage;
        int unifiedPosition = position;
        if (unifiedPosition > 0 && offsetPercentage <= 0) {
            unifiedOffset += 1;
            unifiedPosition -= 1;
        }

        // Radius
        float radiusOffset = mIndicatorRadius - mRadiusMin;
        float radiusOffsetHead = 0.25f;
        if (offsetPercentage > 0) {
            if (unifiedOffset < radiusOffsetHead) {
                mIndicatorMark.getHeadDot().setRadius(mRadiusMin);
            } else {
                mIndicatorMark.getHeadDot().setRadius((unifiedOffset - radiusOffsetHead) / (1 - radiusOffsetHead) * radiusOffset + mRadiusMin);
            }
            mIndicatorMark.getFootDot().setRadius((1 - unifiedOffset) * radiusOffset + mRadiusMin);
        } else if (offsetPercentage < 0) {
            mIndicatorMark.getHeadDot().setRadius(unifiedOffset * radiusOffset + mRadiusMin);
            float radiusOffsetFoot = 0.75f;
            if (unifiedOffset < radiusOffsetFoot) {
                mIndicatorMark.getFootDot().setRadius((radiusOffsetFoot - unifiedOffset) / radiusOffsetFoot * radiusOffset + mRadiusMin);
            } else {
                mIndicatorMark.getFootDot().setRadius(mRadiusMin);
            }
        }

        // x
        float headX = 1f;
        float headMoveOffset = 0.5f;
        float footMoveOffset = 1 - headMoveOffset;
        float acceleration = 0.5f;
        float positionX;
        if (mIsRtl) {
            positionX = mWidth - mIndicatorRadius - getVisibleIndex(unifiedPosition) * (2 * mCircleRadius + mSpacing);
        } else {
            positionX = mIndicatorRadius + getVisibleIndex(unifiedPosition) * (2 * mCircleRadius + mSpacing);
        }
        if (unifiedOffset < headMoveOffset) {
            float positionOffsetTemp = unifiedOffset / headMoveOffset;
            headX = (float) ((Math.atan(positionOffsetTemp * acceleration * 2 - acceleration) + (Math.atan(acceleration))) / (2 * (Math.atan(acceleration))));
        }
        mIndicatorMark.getHeadDot().setCenterX(positionX + (mIsRtl ? -1 : 1) * headX * (2 * mCircleRadius + mSpacing));
        float footX = 0f;
        if (unifiedOffset > footMoveOffset) {
            float positionOffsetTemp = (unifiedOffset - footMoveOffset) / (1 - footMoveOffset);
            footX = (float) ((Math.atan(positionOffsetTemp * acceleration * 2 - acceleration) + (Math.atan(acceleration))) / (2 * (Math.atan(acceleration))));
        }
        mIndicatorMark.getFootDot().setCenterX(positionX + (mIsRtl ? -1 : 1) * footX * (2 * mCircleRadius + mSpacing));

        int alphaCircle = mCircleColor >>> 6;
        int alphaIndicator = mIndicatorColor >>> 6;

        // Special Case: plus marker
        float offsetM = headMoveOffset * (mCircleRadius + mSpacing) / (2 * mCircleRadius + mSpacing);
        int lastIndex = (int) Math.ceil(unifiedPosition + unifiedOffset);
        if (lastIndex >= 0 && lastIndex < mMarkList.size() && mMarkList.get(lastIndex) == IndicatorMark.MarkerType.PLUS_SIGN && (unifiedOffset > offsetM)) {
            mIndicatorMark.getHeadDot().setRadius((offsetM - radiusOffsetHead) / (1 - radiusOffsetHead) * radiusOffset + mRadiusMin);
            if (mIsRtl) {
                mIndicatorMark.getHeadDot().setCenterX(mWidth - mIndicatorRadius - getVisibleIndex(unifiedPosition) * (2 * mCircleRadius + mSpacing) - mCircleRadius - mSpacing);
            } else {
                mIndicatorMark.getHeadDot().setCenterX(mIndicatorRadius + getVisibleIndex(unifiedPosition) * (2 * mCircleRadius + mSpacing) + mCircleRadius + mSpacing);
            }
            int alphaPlus = alphaCircle + (int) ((unifiedOffset - offsetM) * (alphaIndicator - alphaCircle) / (1 - offsetM));
            mPlusColor = (mCircleColor & 0x00FFFFFF) | alphaPlus << 6;
            mPlusRadius = mCircleRadius + (unifiedOffset - offsetM) * (mIndicatorRadius - mCircleRadius) / (1 - offsetM);
        } else {
            mPlusColor = mCircleColor;
            mPlusRadius = mCircleRadius;
        }

        // Special Case: RECT Marker
        if (unifiedPosition >= 0 && unifiedPosition < mMarkList.size()
                && mMarkList.get(unifiedPosition) == IndicatorMark.MarkerType.RECT && unifiedOffset < headMoveOffset) {
            int alphaRect = alphaCircle + (int) ((headMoveOffset - unifiedOffset) * (alphaIndicator - alphaCircle) / (1 - headMoveOffset));
            mRectColor = (mCircleColor & 0x00FFFFFF) | alphaRect << 6;
            mRectRadius = mCircleRadius + (headMoveOffset - unifiedOffset) * (mIndicatorRadius - mCircleRadius) / (1 - headMoveOffset);
        } else {
            mRectColor = mCircleColor;
            mRectRadius = mCircleRadius;
        }

        mCurIndex = unifiedPosition;
        if (unifiedOffset == 1) {
            mCurIndex++;
            setIndex(mCurIndex);
        }

        invalidate();
    }
}
