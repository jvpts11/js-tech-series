/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.ArrayList;
import java.util.List;

/*
 * What a machine did over its last ten minutes, as its screen's page of statistics shows it: how many ticks of each of
 * the last sixty seconds it worked, and fifty readings twelve seconds apart of what it made a minute and of what it
 * drew a tick on the whole, oldest first; and the last time something other than its player stopped it. Counted on
 * the server as it ticks and never saved: the page starts again when the machine is loaded.
 */
final class MachineStatistics {

    private final int[] workedSeconds = new int[SECONDS_A_MINUTE];
    private final int[] made = new int[SAMPLES];
    private final long[] power = new long[SAMPLES];
    /* How many of the readings are taken, and where the next goes. */
    private int samples;
    private int nextSample;
    /* How many of the sixty seconds are counted, and where the next goes. */
    private int seconds;
    private int nextSecond;
    /* This second's ticks worked; this reading's ticks, what they drew, and its seconds so far. */
    private int workedNow;
    private int ticksNow;
    private long drawnNow;
    private int secondsNow;
    private String stoppedBy = "";
    private long stoppedAt;

    /** The readings the page draws, twelve seconds apart: ten minutes. */
    static final int SAMPLES = 50;
    static final int SAMPLE_SECONDS = 12;
    static final int TICKS_A_SECOND = 20;
    static final int SECONDS_A_MINUTE = 60;
    /** How long a stop is remembered: as long as the page looks back. */
    static final long REMEMBERED = (long) SAMPLES * SAMPLE_SECONDS * TICKS_A_SECOND;
    private static final int PERCENT = 100;

    /* Counts one tick: whether it worked, and what it drew. */
    void tick(final boolean working, final long drawn) {
        if (working) {
            workedNow++;
        }
        ticksNow++;
        drawnNow += Math.max(0L, drawn);
    }

    /* Closes a second; every twelfth takes a reading of {@code perMinute} and of what it drew on the whole. */
    void second(final int perMinute) {
        workedSeconds[nextSecond] = Math.min(TICKS_A_SECOND, workedNow);
        nextSecond = (nextSecond + 1) % SECONDS_A_MINUTE;
        seconds = Math.min(SECONDS_A_MINUTE, seconds + 1);
        workedNow = 0;
        if (++secondsNow < SAMPLE_SECONDS) {
            return;
        }
        made[nextSample] = Math.max(0, perMinute);
        power[nextSample] = ticksNow == 0 ? 0L : drawnNow / ticksNow;
        nextSample = (nextSample + 1) % SAMPLES;
        samples = Math.min(SAMPLES, samples + 1);
        ticksNow = 0;
        drawnNow = 0L;
        secondsNow = 0;
    }

    /* Remembers that {@code why} stopped it at {@code gameTime}. */
    void stopped(final String why, final long gameTime) {
        stoppedBy = why;
        stoppedAt = gameTime;
    }

    /* The ticks it worked of the last minute, or of the seconds counted so far when it has run less than one. */
    int workedTicks() {
        int worked = 0;
        for (int i = 0; i < seconds; i++) {
            worked += workedSeconds[i];
        }
        return worked;
    }

    /* How much of the seconds counted it worked, in percent; nothing before its first second. */
    int efficiency() {
        return seconds == 0 ? 0 : workedTicks() * PERCENT / (seconds * TICKS_A_SECOND);
    }

    /* What it made a minute at each reading, oldest first. */
    List<Integer> made() {
        final List<Integer> out = new ArrayList<>(samples);
        for (int i = 0; i < samples; i++) {
            out.add(made[(nextSample - samples + i + SAMPLES) % SAMPLES]);
        }
        return out;
    }

    /* What it drew a tick on the whole between readings, oldest first. */
    List<Long> power() {
        final List<Long> out = new ArrayList<>(samples);
        for (int i = 0; i < samples; i++) {
            out.add(power[(nextSample - samples + i + SAMPLES) % SAMPLES]);
        }
        return out;
    }

    /* What last stopped it, by its status's name; empty when nothing has, or not as recently as the page looks back. */
    String stoppedBy(final long now) {
        return stoppedBy.isEmpty() || now - stoppedAt > REMEMBERED ? "" : stoppedBy;
    }

    /* How many seconds ago it last stopped. */
    int stoppedSecondsAgo(final long now) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (now - stoppedAt) / TICKS_A_SECOND));
    }
}
