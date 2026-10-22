/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the player's eyes met across their view, as the scene's tool counts it: each kind of block a line of sight
 * ended on, how many lines ended on it, and the nearest of them with where it lies from the player; and the words of
 * a direction, the way the player faces and the part of the day. With no game types, so a unit test reads it.
 */
final class SceneTally {

    private final Map<String, Seen> seen = new LinkedHashMap<>();
    private int lines;
    private int open;

    private static final List<String> COMPASS = List.of("south", "west", "north", "east");
    private static final List<String> AROUND = List.of("ahead", "ahead_right", "right", "behind_right", "behind",
            "behind_left", "left", "ahead_left");
    /* How steep a thing must lie, in degrees, to be above or below rather than level with the eyes. */
    private static final double STEEP = 30;
    private static final long DAY = 24_000;

    /** A line of sight that ended on the block {@code id}, {@code distance} away, at {@code bearing} and height. */
    void block(final String id, final double distance, final String bearing, final String height) {
        this.lines++;
        this.seen.merge(id, new Seen(id, 1, distance, bearing, height), (was, now) -> new Seen(id, was.lines() + 1,
                Math.min(was.nearest(), distance), now.nearest() < was.nearest() ? bearing : was.bearing(),
                now.nearest() < was.nearest() ? height : was.height()));
    }

    /** A line of sight that met nothing as far as it looked. */
    void nothing() {
        this.lines++;
        this.open++;
    }

    /** The kinds of block seen, those most of the view ended on first, at most {@code most}. */
    List<Seen> most(final int most) {
        final List<Seen> out = new ArrayList<>(this.seen.values());
        out.sort(Comparator.comparingInt(Seen::lines).reversed().thenComparing(Seen::id));
        return out.subList(0, Math.min(most, out.size()));
    }

    /** How much of the view met nothing, from none to all. */
    double openShare() {
        return this.lines == 0 ? 0 : (double) this.open / this.lines;
    }

    /** How many kinds of block were seen. */
    int kinds() {
        return this.seen.size();
    }

    /**
     * Where a thing {@code dx} east and {@code dz} south of the player lies from the way they face, {@code yaw} as
     * the game turns them (south at none, west at a quarter turn): ahead, ahead_right, right and so round.
     */
    static String bearing(final double yaw, final double dx, final double dz) {
        if (dx == 0 && dz == 0) {
            return AROUND.getFirst();
        }
        final double toward = Math.toDegrees(Math.atan2(-dx, dz));
        final double turned = Math.floorMod(Math.round(toward - yaw), 360L);
        return AROUND.get((int) Math.floorMod(Math.round(turned / 45.0), 8L));
    }

    /** Whether a thing {@code dy} up and {@code across} away lies above, below or level with the eyes. */
    static String height(final double dy, final double across) {
        final double angle = Math.toDegrees(Math.atan2(dy, across));
        return angle > STEEP ? "above" : angle < -STEEP ? "below" : "level";
    }

    /** The way the player faces at {@code yaw}: south, west, north or east. */
    static String facing(final double yaw) {
        return COMPASS.get((int) Math.floorMod(Math.round(yaw / 90.0), 4L));
    }

    /** The part of the day at {@code dayTime}, the game's ticks since its first sunrise. */
    static String partOfDay(final long dayTime) {
        final long tick = Math.floorMod(dayTime, DAY);
        if (tick < 6_000) {
            return "morning";
        }
        if (tick < 12_000) {
            return "afternoon";
        }
        if (tick < 13_800) {
            return "evening";
        }
        return tick < 22_550 ? "night" : "dawn";
    }

    /**
     * A kind of block seen.
     *
     * @param id      its id
     * @param lines   how many lines of sight ended on it
     * @param nearest how far the nearest of it is, in blocks
     * @param bearing where the nearest lies from the way the player faces
     * @param height  whether the nearest lies above, below or level with the eyes
     */
    record Seen(String id, int lines, double nearest, String bearing, String height) {
    }
}
