/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.instancing;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/**
 * The numbers of the things drawn many at once in a frame, four to a texel, as a shader reads them: written as each
 * thing is drawn, sent to the graphics card at once ({@link InstanceTexels}), and written again the next frame in the
 * same room, which only grows. Kept outside the Java heap, where the graphics card reads from; freed with
 * {@link #close}. On the render thread.
 */
public final class InstanceData implements AutoCloseable {

    private FloatBuffer numbers = MemoryUtil.memAllocFloat(FIRST);

    private static final int FIRST = 4096;
    private static final int TEXEL = 4;

    /** Empties it, keeping its room. */
    public void clear() {
        this.numbers.clear();
    }

    /** How many texels are written. */
    public int texels() {
        return this.numbers.position() / TEXEL;
    }

    /** Writes one texel. */
    public void put(final float a, final float b, final float c, final float d) {
        room(TEXEL);
        this.numbers.put(a).put(b).put(c).put(d);
    }

    /** Writes a place as three texels: the rows of its turn, each with its move. */
    public void putRows(final Matrix4f place) {
        room(TEXEL * 3);
        this.numbers.put(place.m00()).put(place.m10()).put(place.m20()).put(place.m30())
                .put(place.m01()).put(place.m11()).put(place.m21()).put(place.m31())
                .put(place.m02()).put(place.m12()).put(place.m22()).put(place.m32());
    }

    /** Writes {@code count} texels of nothing. */
    public void putZeros(final int count) {
        room(TEXEL * count);
        for (int i = 0; i < TEXEL * count; i++) {
            this.numbers.put(0.0F);
        }
    }

    /** Where texel {@code texel} is, for the graphics card to read from. */
    public long address(final int texel) {
        return MemoryUtil.memAddress(this.numbers, 0) + (long) texel * TEXEL * Float.BYTES;
    }

    /** Frees its room; it is not written again. */
    @Override
    public void close() {
        MemoryUtil.memFree(this.numbers);
    }

    private void room(final int more) {
        if (this.numbers.remaining() < more) {
            this.numbers = MemoryUtil.memRealloc(this.numbers, Math.max(this.numbers.capacity() * 2,
                    this.numbers.position() + more));
        }
    }
}
