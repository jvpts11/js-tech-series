/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.pcm.IAudioDecoder;
import dev.jstech.core.audio.pcm.IPcmSource;
import dev.jstech.core.audio.pcm.PcmFormat;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.JOrbisAudioStream;

/**
 * Reads Ogg Vorbis files with the decoder the game reads its own sounds with, which only the client carries: it hands
 * over samples a packet at a time, and they wait here until they are asked for.
 */
final class OggDecoder implements IAudioDecoder {

    @Override
    public IPcmSource open(final InputStream encoded) throws IOException {
        final JOrbisAudioStream ogg;
        try {
            ogg = new JOrbisAudioStream(encoded);
        } catch (final IOException | RuntimeException unreadable) {
            encoded.close();
            throw unreadable;
        }
        try {
            final AudioFormat decoded = ogg.getFormat();
            return new Source(ogg, new PcmFormat((int) decoded.getSampleRate(), decoded.getChannels()));
        } catch (final IllegalArgumentException unplayable) {
            ogg.close();
            throw new IOException(unplayable.getMessage(), unplayable);
        }
    }

    /** The decoded samples, kept from one packet to the next until they are read. */
    private static final class Source implements IPcmSource, FloatConsumer {

        private final JOrbisAudioStream ogg;
        private final PcmFormat format;
        private short[] pending = new short[INITIAL_CAPACITY];
        private int start;
        private int end;
        private boolean ended;

        private static final int INITIAL_CAPACITY = 8192;

        Source(final JOrbisAudioStream ogg, final PcmFormat format) {
            this.ogg = ogg;
            this.format = format;
        }

        @Override
        public PcmFormat format() {
            return format;
        }

        @Override
        public int read(final short[] into, final int offset, final int length) throws IOException {
            while (end - start < length && !ended) {
                ended = !ogg.readChunk(this);
            }
            if (start == end) {
                return ended ? -1 : 0;
            }
            final int count = Math.min(length, end - start);
            System.arraycopy(pending, start, into, offset, count);
            start += count;
            return count;
        }

        /* The same rounding the game uses for its own Ogg sounds, so a file sounds the same played either way. */
        @Override
        public void accept(final float value) {
            if (end == pending.length) {
                makeRoom();
            }
            final int sample = (int) (value * 32767.5F - 0.5F);
            pending[end++] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        }

        @Override
        public void close() throws IOException {
            ogg.close();
        }

        private void makeRoom() {
            final int kept = end - start;
            if (start > 0 && kept < pending.length / 2) {
                System.arraycopy(pending, start, pending, 0, kept);
            } else {
                final short[] grown = new short[pending.length * 2];
                System.arraycopy(pending, start, grown, 0, kept);
                pending = grown;
            }
            start = 0;
            end = kept;
        }
    }
}
