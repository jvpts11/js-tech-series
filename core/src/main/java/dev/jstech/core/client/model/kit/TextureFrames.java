/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import java.util.List;

/**
 * The frames of an animated texture, laid one under another in its picture, as its {@code .mcmeta} file declares them:
 * how many the picture holds, which is shown in turn and for how many ticks. The frame is chosen by the tick, so every
 * model painted with the texture moves in step; a still texture has one frame. Frames are not blended into each other.
 */
public final class TextureFrames {

    private final int count;
    private final int[] order;
    private final int[] ends;

    /** A texture that does not move. */
    public static final TextureFrames STILL = new TextureFrames(1, new int[] {0}, new int[] {1});

    private TextureFrames(final int count, final int[] order, final int[] ends) {
        this.count = count;
        this.order = order;
        this.ends = ends;
    }

    /**
     * The frames of a picture of {@code count} frames: shown in the order of {@code frames}, each an index and its
     * ticks (a negative number of ticks meaning {@code defaultTicks}); every frame in turn when none is listed.
     */
    public static TextureFrames of(final int count, final List<int[]> frames, final int defaultTicks) {
        if (count <= 1) {
            return STILL;
        }
        final int fallback = Math.max(1, defaultTicks);
        final int shown = frames.isEmpty() ? count : frames.size();
        final int[] order = new int[shown];
        final int[] ends = new int[shown];
        int total = 0;
        for (int i = 0; i < shown; i++) {
            final int index = frames.isEmpty() ? i : frames.get(i)[0];
            final int ticks = frames.isEmpty() || frames.get(i)[1] < 0 ? fallback : Math.max(1, frames.get(i)[1]);
            order[i] = Math.floorMod(index, count);
            total += ticks;
            ends[i] = total;
        }
        return new TextureFrames(count, order, ends);
    }

    /** How many frames the picture holds. */
    public int count() {
        return this.count;
    }

    /** Whether it does not move. */
    public boolean still() {
        return this.count <= 1;
    }

    /** The frame shown at tick {@code tick}. */
    public int frameAt(final long tick) {
        if (still()) {
            return 0;
        }
        final long into = Math.floorMod(tick, (long) this.ends[this.ends.length - 1]);
        for (int i = 0; i < this.ends.length; i++) {
            if (into < this.ends[i]) {
                return this.order[i];
            }
        }
        return this.order[this.order.length - 1];
    }

    /** What a face's V is multiplied by, so it covers one frame of the picture. */
    public float vScale() {
        return 1.0F / this.count;
    }

    /** What is added to a face's V at tick {@code tick}, so it shows that tick's frame. */
    public float vOffset(final long tick) {
        return still() ? 0.0F : frameAt(tick) / (float) this.count;
    }
}
