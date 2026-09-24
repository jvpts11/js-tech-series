/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.pcm.IPcmSource;
import dev.jstech.core.audio.pcm.PcmFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

/**
 * Samples handed to the game's speakers as they are asked for, a second at a time, in the form the game's own Ogg
 * files take: 16-bit, in the machine's own byte order, in a buffer outside the Java heap.
 *
 * <p>A sound of the world is made mono: the speakers place only a mono sound in the world, and play a stereo one as
 * if it were inside the listener's head, wherever it comes from.
 */
public final class PcmAudioStream implements AudioStream {

    private final IPcmSource source;
    private final boolean downmix;
    private final AudioFormat format;
    private short[] samples = new short[0];
    private boolean ended;

    private static final int BYTES_PER_SAMPLE = 2;

    /** Reads that source, made mono when {@code mono} asks for it and it is not already. */
    public PcmAudioStream(final IPcmSource source, final boolean mono) {
        final PcmFormat in = source.format();
        this.source = source;
        this.downmix = mono && in.channels() == 2;
        this.format = new AudioFormat(in.sampleRate(), Short.SIZE, downmix ? 1 : in.channels(), true, false);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Up to {@code size} bytes of samples, whole frames only, or null once there are none left, which the game takes
     * as nothing more to queue.
     */
    @Nullable
    @Override
    public ByteBuffer read(final int size) throws IOException {
        if (ended) {
            return null;
        }
        final int channels = source.format().channels();
        final int frames = Math.max(1, size / (BYTES_PER_SAMPLE * format.getChannels()));
        final int wanted = frames * channels;
        if (samples.length < wanted) {
            samples = new short[wanted];
        }
        int got = 0;
        while (got < wanted) {
            final int read = source.read(samples, got, wanted - got);
            if (read < 0) {
                ended = true;
                break;
            }
            if (read == 0) {
                break;
            }
            got += read;
        }
        got -= got % channels;
        if (got == 0) {
            return null;
        }
        final int written = downmix ? got / 2 : got;
        final ByteBuffer out = BufferUtils.createByteBuffer(written * BYTES_PER_SAMPLE);
        final ShortBuffer shorts = out.asShortBuffer();
        if (downmix) {
            for (int i = 0; i < got; i += 2) {
                shorts.put((short) ((samples[i] + samples[i + 1]) / 2));
            }
        } else {
            shorts.put(samples, 0, got);
        }
        return out;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
