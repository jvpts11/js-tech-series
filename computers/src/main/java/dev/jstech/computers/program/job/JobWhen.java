/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.job;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * When a job runs: as soon as the machine can, or at an hour of the world's own day.
 *
 * <p>The clock is the world's, because everything a player would set a job by is the world's: dawn, dusk, the
 * hour the farm is full. A day here is twenty thousand ticks of daylight and four of dark, and an hour is a
 * thousand of them, which is why an hour is the shortest anything may repeat.
 *
 * <p>Pure, and it is the one place the arithmetic lives, so {@code cron} and {@code AT} cannot fall out of
 * step about what "every day at six" means.
 *
 * @param hour     the hour of the day it runs at, or {@code -1} for a job that runs once, at once
 * @param days     the days of the week it runs on, from 0 for the first; empty means every day
 */
@TextHolder
public record JobWhen(int hour, List<Integer> days) {

    /** What a job left running says in place of an hour. */
    private static final TextKey RUNNING = TextKey.of("jsc.cli.job.running", "running");

    /** Ticks in a day of this world, daylight and dark together. */
    public static final int DAY_TICKS = 24000;

    /** Ticks in an hour of it. */
    public static final int HOUR_TICKS = 1000;

    /** A day here starts at six in the morning, which is where the world's own clock starts counting. */
    public static final int DAY_STARTS_AT = 6;

    /** As soon as the machine can, once. */
    public static final JobWhen AT_ONCE = new JobWhen(-1, List.of());

    public JobWhen {
        days = List.copyOf(days);
    }

    /** At that hour of every day. */
    public static JobWhen at(final int hour) {
        return new JobWhen(Math.floorMod(hour, 24), List.of());
    }

    /** At that hour, on those days of the week. */
    public static JobWhen at(final int hour, final List<Integer> days) {
        return new JobWhen(Math.floorMod(hour, 24), days);
    }

    /** Whether it runs once and is then done with. */
    public boolean once() {
        return this.hour < 0;
    }

    /**
     * Whether the job is due.
     *
     * <p>A job that runs once is due the moment it is asked about. One on a schedule is due when the world
     * has reached its hour and has not run it in this hour already, which is what {@code lastRun} says.
     */
    public boolean dueAt(final long dayTime, final long lastRun) {
        if (once()) {
            return true;
        }
        if (!this.days.isEmpty() && !this.days.contains(dayOfWeek(dayTime))) {
            return false;
        }
        return hourOf(dayTime) == this.hour && (lastRun < 0L || hoursBetween(lastRun, dayTime) >= 1L);
    }

    /** The hour of the day the world's clock is showing, counting from midnight. */
    public static int hourOf(final long dayTime) {
        return (int) ((dayTime % DAY_TICKS) / HOUR_TICKS + DAY_STARTS_AT) % 24;
    }

    /** Which day of the week it is, from 0 for the first. */
    public static int dayOfWeek(final long dayTime) {
        return (int) (dayTime / DAY_TICKS % 7L);
    }

    /** How many whole hours passed between two readings of the clock. */
    public static long hoursBetween(final long from, final long to) {
        return (to - from) / HOUR_TICKS;
    }

    /** How a person reads it: the word for a job left running, or the hour and the days, which are data. */
    public Text shown() {
        if (once()) {
            return RUNNING.text();
        }
        final String when = String.format(Locale.ROOT, "%02d:00", this.hour);
        return Text.literal(this.days.isEmpty() ? when : when + " " + dayNames());
    }

    /** The same in English, the language a machine writes it down in. */
    public String label() {
        return shown().english();
    }

    /** The days as the letters a schedule is written with. */
    public String dayNames() {
        final StringBuilder out = new StringBuilder();
        for (final int day : this.days) {
            out.append(out.isEmpty() ? "" : " ").append(NAMES[Math.floorMod(day, 7)]);
        }
        return out.toString();
    }

    /** The letters the days are written with, in the order a week runs. */
    private static final String[] NAMES = {"M", "T", "W", "Th", "F", "Sa", "Su"};

    /**
     * The days a written list names: {@code M,W,F} and {@code m,w,f} alike.
     *
     * <p>Two letters before one, so Th and Sa are not read as T and S.
     */
    public static List<Integer> daysOf(final String written) {
        final List<Integer> days = new ArrayList<>();
        for (final String part : written.split(",")) {
            final String name = part.trim().toLowerCase(Locale.ROOT);
            for (int i = 0; i < NAMES.length; i++) {
                if (NAMES[i].toLowerCase(Locale.ROOT).equals(name)) {
                    days.add(i);
                    break;
                }
            }
        }
        return days;
    }

    /** The hour a written time names, from {@code 06:00} or {@code 6}, or {@code -1} when it names none. */
    public static int hourOf(final String written) {
        final String text = written.trim();
        final int colon = text.indexOf(':');
        try {
            final int hour = Integer.parseInt(colon < 0 ? text : text.substring(0, colon));
            return hour >= 0 && hour < 24 ? hour : -1;
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }

    /** The days as the numbers a machine writes down. */
    public int[] dayNumbers() {
        final int[] out = new int[this.days.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.days.get(i);
        }
        return out;
    }

    /** The same read back. */
    public static JobWhen of(final int hour, final int[] days) {
        final List<Integer> kept = new ArrayList<>(days.length);
        for (final int day : days) {
            kept.add(day);
        }
        return new JobWhen(hour, kept);
    }
}
