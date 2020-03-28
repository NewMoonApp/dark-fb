// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.newmoon.dark.wallpaper.crop;

import android.graphics.Rect;
import android.graphics.RectF;


/**
 * Utility class that deals with operations with an ImageView.
 */
final class CropUtils {

    static final Rect EMPTY_RECT = new Rect();

    static final RectF EMPTY_RECT_F = new RectF();


    /**
     * Get left value of the bounding rectangle of the given points.
     */
    static float getRectLeft(float[] points) {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    static float getRectTop(float[] points) {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    static float getRectRight(float[] points) {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    static float getRectBottom(float[] points) {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
    }
}