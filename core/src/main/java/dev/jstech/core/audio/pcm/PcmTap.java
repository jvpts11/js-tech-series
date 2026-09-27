/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * A recording read through as it plays, keeping the last few seconds of it so a screen can show what is being heard:
 * the bars of a music player's analyser, say.
 *
 * <p>The sound engine reads a stream some way ahead of what comes out of the speakers, so the newest samples read are
 * not the ones being heard. The point being heard is worked out from the clock instead, from the moment the first
 * samples were read, and what is handed back ends there.
 *
 * <p>Read on the sound engine's thread and looked at from the game's, so everything it keeps is behind one lock.
 */
public final class PcmTap implements IPcmSource {

    private final IPcmSource source;
    private final int channels;
    private final int rate;
    private final LongSupplier clock;
    private final float[] ring;
    /* How many mono samples have been kept since the start. */
    private long kept;
    /* When the first samples were read, by the clock; -1 until then. */
    private long startedAt = -1L;

    /** How many seconds of the recording are kept, which is more than the sound engine reads ahead. */
    private static final int SECONDS = 4;

    /**
     * @param clock the time in milliseconds, the game's own clock outside a test
     */
    public PcmTap(final IPcmSource source, final LongSupplier clock) {
        this.source = source;
        this.channels = Math.max(1, source.format().channels());
        this.rate = source.format().sampleRate();
        this.clock = clock;
        this.ring = new float[Math.max(1, rate * SECONDS)];
    }

    @Override
    public PcmFormat format() {
        return source.format();
    }

    @Override
    public int read(final short[] into, final int offset, final int length) throws IOException {
        final int read = source.read(into, offset, length);
        if (read > 0) {
            keep(into, offset, read);
        }
        return read;
    }

    /** The samples a point this far after it began is hearing: its sample rate. */
    public int rate() {
        return rate;
    }

    /**
     * Fills {@code out} with the samples being heard now, the newest last, mixed to one channel from -1 to 1; with
     * silence for whatever came before the start or has not been read yet.
     *
     * @return whether anything has been heard yet
     */
    public synchronized boolean heard(final float[] out) {
        if (startedAt < 0) {
            Arrays.fill(out, 0.0F);
            return false;
        }
        final long now = Math.min(kept, (clock.getAsLong() - startedAt) * rate / 1000L);
        for (int i = 0; i < out.length; i++) {
            final long at = now - out.length + i;
            out[i] = at < 0 || at < kept - ring.length ? 0.0F : ring[(int) (at % ring.length)];
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    private synchronized void keep(final short[] samples, final int offset, final int count) {
        if (startedAt < 0) {
            startedAt = clock.getAsLong();
        }
        for (int i = offset; i + channels <= offset + count; i += channels) {
            float mixed = 0.0F;
            for (int c = 0; c < channels; c++) {
                mixed += samples[i + c];
            }
            ring[(int) (kept % ring.length)] = mixed / (channels * 32768.0F);
            kept++;
        }
    }
}
