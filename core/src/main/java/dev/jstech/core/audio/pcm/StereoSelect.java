/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import dev.jstech.core.audio.StereoSide;

import java.io.IOException;

/**
 * One side of a stereo recording, read as a mono one: what the left or the right speaker of a pair plays. A mono
 * recording has no sides to choose from and is read as it is, and so is a stereo one both of whose sides are wanted.
 */
public final class StereoSelect implements IPcmSource {

    private final IPcmSource source;
    private final int offset;
    private final PcmFormat format;
    /** The source's samples, both sides interleaved, read before the chosen side is taken from them. */
    private short[] pairs = new short[0];
    /** A sample left over from the last read, when the source stopped between the two sides of one moment. */
    private short carried;
    private boolean carrying;

    private StereoSelect(final IPcmSource source, final StereoSide side) {
        this.source = source;
        this.offset = side == StereoSide.LEFT ? 0 : 1;
        this.format = new PcmFormat(source.format().sampleRate(), 1);
    }

    /** The side of {@code source} a speaker playing {@code side} hears. */
    public static IPcmSource of(final IPcmSource source, final StereoSide side) {
        if (side == StereoSide.BOTH || source.format().channels() == 1) {
            return source;
        }
        return new StereoSelect(source, side);
    }

    @Override
    public PcmFormat format() {
        return format;
    }

    @Override
    public int read(final short[] into, final int start, final int length) throws IOException {
        final int wanted = length * 2;
        if (pairs.length < wanted) {
            pairs = new short[wanted];
        }
        int have = 0;
        if (carrying) {
            pairs[have++] = carried;
            carrying = false;
        }
        final int read = source.read(pairs, have, wanted - have);
        if (read < 0 && have == 0) {
            return -1;
        }
        have += Math.max(0, read);
        if (have % 2 == 1) {
            carried = pairs[have - 1];
            carrying = true;
            have--;
        }
        final int frames = have / 2;
        for (int frame = 0; frame < frames; frame++) {
            into[start + frame] = pairs[frame * 2 + offset];
        }
        return frames == 0 && read < 0 ? -1 : frames;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
