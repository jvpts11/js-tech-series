/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;

/**
 * A recording heard from a point in it rather than from its start: a song a player walks up to half way through, or
 * one taken up again where it was paused. Samples are read from the start, never jumped to, so the ones before the
 * point are read and thrown away the first time anything is asked for, off the game's own thread.
 */
public final class PcmSkip implements IPcmSource {

    private final IPcmSource source;
    private long toSkip;

    private static final int SCRATCH = 8192;

    private PcmSkip(final IPcmSource source, final long samples) {
        this.source = source;
        this.toSkip = samples;
    }

    /** {@code source} from {@code millis} into it; from its start when that is none. */
    public static IPcmSource from(final IPcmSource source, final long millis) {
        if (millis <= 0) {
            return source;
        }
        final PcmFormat format = source.format();
        final long frames = millis * format.sampleRate() / 1000L;
        return new PcmSkip(source, frames * format.channels());
    }

    @Override
    public PcmFormat format() {
        return source.format();
    }

    @Override
    public int read(final short[] into, final int offset, final int length) throws IOException {
        if (toSkip > 0) {
            final short[] scratch = new short[SCRATCH];
            while (toSkip > 0) {
                final int read = source.read(scratch, 0, (int) Math.min(SCRATCH, toSkip));
                if (read < 0) {
                    toSkip = 0;
                    return -1;
                }
                if (read == 0) {
                    // Nothing to be had yet: the rest is thrown away on a later read.
                    return 0;
                }
                toSkip -= read;
            }
        }
        return source.read(into, offset, length);
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
