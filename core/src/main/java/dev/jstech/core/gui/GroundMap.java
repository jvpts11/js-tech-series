/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

/**
 * The grounds painted on a frame, kept so that text drawn over one can ask what it is written on.
 *
 * <p>Whoever paints a background says here which colour it is and where, in the screen's own units; text drawn
 * afterwards looks up the point it starts at and gets the ground painted there last. A screen paints its grounds
 * before its text (the background pass first, the labels after it), so the last ground under a point is the one the
 * text is seen on. A see-through ground is laid over whatever was declared under its middle, which is what the eye
 * sees through it.
 *
 * <p>It keeps the latest {@link #CAPACITY} grounds and forgets the oldest past that, so a frame that paints more
 * cannot make it grow or slow it down. It is emptied as each frame begins. Pure arithmetic, no game types.
 */
public final class GroundMap {

    private final float[] left = new float[CAPACITY];
    private final float[] top = new float[CAPACITY];
    private final float[] right = new float[CAPACITY];
    private final float[] bottom = new float[CAPACITY];
    private final int[] colour = new int[CAPACITY];
    /** How many grounds are kept, up to {@link #CAPACITY}. */
    private int size;
    /** Where the next ground goes; the newest is just before it, round the ring. */
    private int next;

    /** How many grounds a frame keeps before the oldest are forgotten. */
    public static final int CAPACITY = 1024;

    /** Full alpha. */
    private static final int OPAQUE = 0xFF << 24;

    /**
     * Says the rectangle between two corners is painted in {@code argb}. The corners may come in either order; an
     * empty rectangle and a fully clear colour paint nothing, so they are no ground.
     */
    public void declare(final float x1, final float y1, final float x2, final float y2, final int argb) {
        final int alpha = argb >>> 24;
        if (alpha == 0 || x1 == x2 || y1 == y2) {
            return;
        }
        final float l = Math.min(x1, x2);
        final float t = Math.min(y1, y2);
        final float r = Math.max(x1, x2);
        final float b = Math.max(y1, y2);
        final int seen = alpha == 0xFF ? argb : over(at((l + r) / 2, (t + b) / 2, argb | OPAQUE), argb);
        left[next] = l;
        top[next] = t;
        right[next] = r;
        bottom[next] = b;
        colour[next] = seen;
        next = (next + 1) % CAPACITY;
        size = Math.min(size + 1, CAPACITY);
    }

    /**
     * The ground painted last under the point, or {@code otherwise} when nothing was declared there. A ground it keeps
     * is always opaque, so a clear colour is safe to pass as {@code otherwise} and stands for nothing.
     */
    public int at(final float x, final float y, final int otherwise) {
        for (int k = 1; k <= size; k++) {
            final int i = (next - k + CAPACITY) % CAPACITY;
            if (x >= left[i] && x < right[i] && y >= top[i] && y < bottom[i]) {
                return colour[i];
            }
        }
        return otherwise;
    }

    /** How many grounds are kept. */
    public int size() {
        return size;
    }

    /** Forgets every ground, as a new frame begins. */
    public void clear() {
        size = 0;
        next = 0;
    }

    /* The opaque colour seen where {@code layer} is painted over {@code base}, channel by channel. */
    private static int over(final int base, final int layer) {
        final double alpha = (layer >>> 24) / 255.0;
        int out = OPAQUE;
        for (int shift = 0; shift <= 16; shift += 8) {
            final int under = base >> shift & 0xFF;
            final int above = layer >> shift & 0xFF;
            out |= (int) Math.round(under + (above - under) * alpha) << shift;
        }
        return out;
    }
}
