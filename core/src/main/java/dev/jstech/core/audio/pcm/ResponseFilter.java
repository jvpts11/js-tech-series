/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import dev.jstech.core.audio.FrequencyResponse;

import java.io.IOException;

/**
 * A recording as hardware that cannot play it whole plays it: its bass and its treble cut away, sampled no finer than
 * the hardware samples, and in no more bits than it keeps. Hardware that plays everything leaves the recording as it
 * is.
 *
 * <p>Each cut is two one-pole filters in a row, twelve decibels an octave, gentle enough to sound like cheap hardware
 * rather than like a filter. The coarse sampling holds each sample for as long as the speaker's rate lasts, which is
 * what gives such a speaker its grain; the treble above half that rate is cut first, as the hardware's own filter did.
 */
public final class ResponseFilter implements IPcmSource {

    private final IPcmSource source;
    private final int channels;
    /** How much of the way to the new sample each low-pass step goes; 1 when there is no high cut. */
    private final double lowPass;
    /** How much of the old output each high-pass step keeps; 0 when there is no low cut. */
    private final double highPass;
    /** How many samples each coarse sample is held for; 1 when the speaker samples as finely as the recording. */
    private final int hold;
    /** The step each sample is rounded to, from the bits kept; 1 when all sixteen are. */
    private final int step;
    private final double[] low1;
    private final double[] low2;
    private final double[] high1;
    private final double[] high2;
    private final double[] lastIn1;
    private final double[] lastIn2;
    private final short[] held;
    private int heldFor;
    /** Which channel the next sample read belongs to, carried across reads. */
    private int channel;

    private ResponseFilter(final IPcmSource source, final FrequencyResponse response) {
        this.source = source;
        this.channels = source.format().channels();
        final int rate = source.format().sampleRate();
        this.hold = response.maxSampleRate() > 0 && response.maxSampleRate() < rate
                ? Math.max(1, Math.round((float) rate / response.maxSampleRate())) : 1;
        int highCut = response.highCutHz();
        if (hold > 1) {
            final int nyquist = response.maxSampleRate() / 2;
            highCut = highCut == 0 ? nyquist : Math.min(highCut, nyquist);
        }
        this.lowPass = highCut > 0 && highCut < rate / 2 ? 1.0 - Math.exp(-2.0 * Math.PI * highCut / rate) : 1.0;
        this.highPass = response.lowCutHz() > 0 ? Math.exp(-2.0 * Math.PI * response.lowCutHz() / rate) : 0.0;
        this.step = response.bits() > 0 && response.bits() < 16 ? 1 << (16 - response.bits()) : 1;
        this.low1 = new double[channels];
        this.low2 = new double[channels];
        this.high1 = new double[channels];
        this.high2 = new double[channels];
        this.lastIn1 = new double[channels];
        this.lastIn2 = new double[channels];
        this.held = new short[channels];
    }

    /** {@code source} as a speaker with {@code response} plays it. */
    public static IPcmSource of(final IPcmSource source, final FrequencyResponse response) {
        return response.full() ? source : new ResponseFilter(source, response);
    }

    @Override
    public PcmFormat format() {
        return source.format();
    }

    @Override
    public int read(final short[] into, final int start, final int length) throws IOException {
        final int read = source.read(into, start, length);
        for (int i = 0; i < read; i++) {
            into[start + i] = filter(into[start + i]);
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    private short filter(final short sample) {
        final int c = channel;
        double x = sample;
        if (highPass > 0.0) {
            final double first = highPass * (high1[c] + x - lastIn1[c]);
            lastIn1[c] = x;
            high1[c] = first;
            final double second = highPass * (high2[c] + first - lastIn2[c]);
            lastIn2[c] = first;
            high2[c] = second;
            x = second;
        }
        if (lowPass < 1.0) {
            low1[c] += lowPass * (x - low1[c]);
            low2[c] += lowPass * (low1[c] - low2[c]);
            x = low2[c];
        }
        if (step > 1) {
            x = Math.round(x / step) * (double) step;
        }
        short out = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE - (step - 1), Math.round(x)));
        if (hold > 1) {
            if (heldFor == 0) {
                held[c] = out;
            }
            out = held[c];
        }
        channel = (channel + 1) % channels;
        if (channel == 0 && hold > 1) {
            heldFor = (heldFor + 1) % hold;
        }
        return out;
    }
}
