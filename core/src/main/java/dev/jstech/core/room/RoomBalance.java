/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

/**
 * The world's rules for the consequences of sealed rooms, as its settings file gives them: whether decompression pulls
 * things towards the hole, whether fire in pure oxygen turns into an inferno, whether fire needs oxygen, whether mobs
 * breathe, whether plants need air, whether blocks burst; and the largest room, in blocks.
 */
public final class RoomBalance {

    /** The largest room the Core looks for, in blocks, by default: a hangar. */
    public static final int LARGEST_DEFAULT = 16_384;
    private static volatile boolean decompressionPull = true;
    private static volatile boolean fireInPureOxygen = true;
    private static volatile boolean fireNeedsOxygen = true;
    private static volatile boolean mobsBreathe = true;
    private static volatile boolean plantsNeedAir = true;
    private static volatile boolean blocksBurst = true;
    private static volatile int largestRoom = LARGEST_DEFAULT;

    private RoomBalance() {
    }

    /** Whether the air rushing out of a room pulls things and players towards the hole. */
    public static boolean decompressionPull() {
        return decompressionPull;
    }

    public static void setDecompressionPull(final boolean value) {
        decompressionPull = value;
    }

    /** Whether any flame in air mostly of oxygen spreads at once. */
    public static boolean fireInPureOxygen() {
        return fireInPureOxygen;
    }

    public static void setFireInPureOxygen(final boolean value) {
        fireInPureOxygen = value;
    }

    /** Whether fire goes out, and will not light, where there is too little oxygen. */
    public static boolean fireNeedsOxygen() {
        return fireNeedsOxygen;
    }

    public static void setFireNeedsOxygen(final boolean value) {
        fireNeedsOxygen = value;
    }

    /** Whether mobs that breathe suffocate in air they cannot breathe. */
    public static boolean mobsBreathe() {
        return mobsBreathe;
    }

    public static void setMobsBreathe(final boolean value) {
        mobsBreathe = value;
    }

    /** Whether plants need air to grow. */
    public static boolean plantsNeedAir() {
        return plantsNeedAir;
    }

    public static void setPlantsNeedAir(final boolean value) {
        plantsNeedAir = value;
    }

    /** Whether a block facing more pressure than it stands bursts. */
    public static boolean blocksBurst() {
        return blocksBurst;
    }

    public static void setBlocksBurst(final boolean value) {
        blocksBurst = value;
    }

    /** The largest room the Core looks for, in blocks: a larger space is open to the place round it. */
    public static int largestRoom() {
        return largestRoom;
    }

    public static void setLargestRoom(final int value) {
        largestRoom = value;
    }
}
