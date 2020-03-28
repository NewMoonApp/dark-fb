package com.newmoon.dark.view;

import android.graphics.Path;

/**
 * Indicator marker used by {@link AdvancedPageIndicator}.
 */
public class IndicatorMark {

    public enum MarkerType {
        CIRCLE,
        RECT,
        PLUS_SIGN,
    }

    static class Marker {

        private MarkerType markerType;
        private float centerX;
        private float centerY;
        private float radius;

        Marker(MarkerType type) {
            this.markerType = type;
        }

        Marker(MarkerType type, float x, float y, float r) {
            this(type);
            this.centerX = x;
            this.centerY = y;
            this.radius = r;
        }

        public MarkerType getMarkerType() {
            return markerType;
        }

        public void setMarkerType(MarkerType type) {
            this.markerType = type;
        }

        public float getCenterX() {
            return centerX;
        }

        public void setCenterX(float x) {
            this.centerX = x;
        }

        public float getCenterY() {
            return centerY;
        }

        public void setCenterY(float y) {
            this.centerY = y;
        }

        public float getRadius() {
            return radius;
        }

        public void setRadius(float radius) {
            this.radius = radius;
        }
    }

    private Marker headDot;
    private Marker footDot;
    private int color;

    IndicatorMark(Marker head, Marker foot, int color) {
        this.headDot = head;
        this.footDot = foot;
        this.color = color;
    }

    public Marker getHeadDot() {
        return headDot;
    }

    public Marker getFootDot() {
        return footDot;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    void makePath(Path path) {
        double angle = Math.atan((footDot.getCenterY() - headDot.getCenterY()) / (footDot.getCenterX() - headDot.getCenterX()));

        float headOffsetX = (float) (headDot.getRadius() * Math.sin(angle));
        float headOffsetY = (float) (headDot.getRadius() * Math.cos(angle));
        float footOffsetX = (float) (footDot.getRadius() * Math.sin(angle));
        float footOffsetY = (float) (footDot.getRadius() * Math.cos(angle));

        float x1 = headDot.getCenterX() - headOffsetX;
        float y1 = headDot.getCenterY() + headOffsetY;
        float x2 = headDot.getCenterX() + headOffsetX;
        float y2 = headDot.getCenterY() - headOffsetY;
        float x3 = footDot.getCenterX() - footOffsetX;
        float y3 = footDot.getCenterY() + footOffsetY;
        float x4 = footDot.getCenterX() + footOffsetX;
        float y4 = footDot.getCenterY() - footOffsetY;

        float anchorX = (footDot.getCenterX() + headDot.getCenterX()) / 2;
        float anchorY = (footDot.getCenterY() + headDot.getCenterY()) / 2;

        path.reset();
        path.moveTo(x1, y1);
        path.quadTo(anchorX, anchorY, x3, y3);
        path.lineTo(x4, y4);
        path.quadTo(anchorX, anchorY, x2, y2);
        path.lineTo(x1, y1);
    }
}
