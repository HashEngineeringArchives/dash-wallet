/**
 * Copyright © 2019 Paul Schaub
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schildbach.wallet.ui.dashpay.utils;

import androidx.annotation.ColorInt;

/**
 * Utility class for colors. This class is necessary, since many useful methods for dealing with
 * colors (eg. {@link android.graphics.Color#rgb(int, int, int)} were only introduced in API level
 * 26 ({@link android.os.Build.VERSION_CODES#O}).
 */
public class ColorUtil {

    /**
     * Encode ARGB values as a color int.
     * Reimplementation of {@link android.graphics.Color#argb(float, float, float, float)} for
     * API levels < 26.
     *
     * @param a alpha
     * @param r red
     * @param g green
     * @param b blue
     * @return color int
     */
    @ColorInt
    public static int argb(float a, float r, float g, float b) {
        int A = float2int(a);
        int R = float2int(r);
        int G = float2int(g);
        int B = float2int(b);

        return argb(A, R, G, B);
    }

    /**
     * Encode ARGB values as a color int.
     * Reimplementation of {@link android.graphics.Color#argb(int, int, int, int)} for
     * API levels < 26.
     *
     * @see <a href="https://developer.android.com/reference/android/graphics/Color#encoding">
     *     Android developer reference: Color - Encoding</a>
     *
     * @param a alpha
     * @param r red
     * @param g green
     * @param b blue
     * @return color int
     */
    @ColorInt
    public static int argb(int a, int r, int g, int b) {
        return (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    /**
     * Encode RGB values as a color int.
     * Reimplementation of {@link android.graphics.Color#rgb(float, float, float)} for API levels
     * < 26.
     * @param r red
     * @param g green
     * @param b blue
     * @return color int
     */
    @ColorInt
    public static int rgb(float r, float g, float b) {
        return argb(1f, r, g, b);
    }

    /**
     * Encode RGB values as a color int.
     * Reimplementation of {@link android.graphics.Color#rgb(int, int, int)} for API levels
     * < 26.
     *
     * @param r red
     * @param g green
     * @param b blue
     * @return color int
     */
    @ColorInt
    public static int rgb(int r, int g, int b) {
        return argb(255, r, g, b);
    }

    /**
     * Return the alpha component of the color.
     * Reimplementation of {@link android.graphics.Color#alpha(int)} for APIs lower than 26.
     *
     * @param color color
     * @return alpha component
     *
     * @see <a href="https://developer.android.com/reference/android/graphics/Color#decoding">
     *     Android developer reference: Color - Decoding</a>
     */
    public static int alpha(@ColorInt int color) {
        return (color >> 24) & 0xff;
    }

    /**
     * Return the red component of the color.
     * Reimplementation of {@link android.graphics.Color#red(int)} for APIs lower than 26.
     *
     * @param color color
     * @return red component
     *
     * @see <a href="https://developer.android.com/reference/android/graphics/Color#decoding">
     *      *     Android developer reference: Color - Decoding</a>
     */
    public static int red(@ColorInt int color) {
        return (color >> 16) & 0xff;
    }

    /**
     * Return the green component of the color.
     * Reimplementation of {@link android.graphics.Color#green(int)} for APIs lower than 26.
     *
     * @param color color
     * @return green component
     *
     * @see <a href="https://developer.android.com/reference/android/graphics/Color#decoding">
     *      *     Android developer reference: Color - Decoding</a>
     */
    public static int green(@ColorInt int color) {
        return (color >> 8) & 0xff;
    }

    /**
     * Return the blue component of the color.
     * Reimplementation of {@link android.graphics.Color#blue(int)} for APIs lower than 26.
     *
     * @param color color
     * @return blue component
     *
     * @see <a href="https://developer.android.com/reference/android/graphics/Color#decoding">
     *      *     Android developer reference: Color - Decoding</a>
     */
    public static int blue(@ColorInt int color) {
        return (color) & 0xff;
    }

    private static int float2int(float f) {
        return Math.round(255 * f);
    }

    public static String toString(int color) {
        return "#" + String.format("%08x", color);
    }
}