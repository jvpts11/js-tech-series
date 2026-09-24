/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.util.List;

/**
 * A sequence of tones made into samples as they are read: a PC speaker's beep, a machine's chime, a tune a program
 * plays. Each tone rises and falls over a couple of milliseconds, which is what keeps the edges of a note from
 * clicking. Noise comes from a fixed seed, so the same sequence always sounds the same.
 */
public final class SynthSource implements IPcmSource {

    private final List<Tone> tones;
    private final int rate;
    private int tone;
    private int sampleInTone;
    private double phase;
    private long noise;

    /** How long a note takes to rise to its volume and to fall away at its end, in samples. */
    private static final int EDGE_SAMPLES = 44;
    private static final long NOISE_SEED = 0x2545F4914F6CDD1DL;

    /** The tones played one after the other, at the synthesiser's rate. */
    public SynthSource(final List<Tone> sequence) {
        if (sequence.isEmpty()) {
            throw new IllegalArgumentException("a synthesised sound has at least one tone");
        }
        this.tones = List.copyOf(sequence);
        this.rate = PcmFormat.SYNTH.sampleRate();
        this.noise = NOISE_SEED;
    }

    /** How many samples the whole sequence makes. */
    public long totalSamples() {
        long total = 0;
        for (final Tone one : tones) {
            total += samplesOf(one);
        }
        return total;
    }

    @Override
    public PcmFormat format() {
        return PcmFormat.SYNTH;
    }

    @Override
    public int read(final short[] into, final int offset, final int length) {
        if (tone >= tones.size()) {
            return -1;
        }
        int written = 0;
        while (written < length && tone < tones.size()) {
            final Tone current = tones.get(tone);
            final int count = samplesOf(current);
            into[offset + written] = sample(current, count);
            written++;
            sampleInTone++;
            if (sampleInTone >= count) {
                tone++;
                sampleInTone = 0;
                phase = 0;
            }
        }
        return written;
    }

    private short sample(final Tone current, final int count) {
        if (current.frequency() <= 0 || current.volume() <= 0) {
            return 0;
        }
        final double value = current.wave().at(phase, nextNoise());
        phase += current.frequency() / rate;
        phase -= Math.floor(phase);
        final int edge = Math.min(EDGE_SAMPLES, count / 2);
        final double envelope = edge == 0 ? 1.0
                : Math.min(1.0, Math.min((double) sampleInTone / edge, (double) (count - 1 - sampleInTone) / edge));
        return (short) Math.round(value * current.volume() * Math.max(0.0, envelope) * Short.MAX_VALUE);
    }

    private int samplesOf(final Tone one) {
        return (int) ((long) one.millis() * rate / 1000L);
    }

    /* A xorshift step: cheap, and the same every time from the same seed. */
    private double nextNoise() {
        noise ^= noise << 13;
        noise ^= noise >>> 7;
        noise ^= noise << 17;
        return (noise >>> 11) / (double) (1L << 53);
    }
}
