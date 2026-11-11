/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

/**
 * The numbers the server's settings give Teracoin (computers/docs/design/teracoin.md): a block's target time, the
 * reward of a block before any halving, whether it halves and every how many blocks, every how many world days the
 * difficulty's floor grows by its start, and the share of blocks that bring a rare item. The numbers are estimates.
 * Pure.
 *
 * @param targetTicks   the time a block should take, in ticks
 * @param reward        what a block pays before any halving, in hundred-millionths of a TRC
 * @param halving       whether the reward halves
 * @param halvingBlocks every how many blocks it halves
 * @param floorDays     every how many world days the difficulty's floor grows by the difficulty's start
 * @param rareChance    the share of blocks that bring a rare item, from 0 to 1
 */
public record TeracoinRules(long targetTicks, long reward, boolean halving, int halvingBlocks, int floorDays,
                            double rareChance) {

    /** The difficulty the chain starts at: one Transition rig of four cards finds a block about every ten minutes. */
    public static final long START_DIFFICULTY = 8_000_000L;
    /** Every how many blocks the difficulty is adjusted. */
    public static final int ADJUST_EVERY = 10;
    /** The most the difficulty moves at one adjustment, up or down, as the real one does. */
    public static final double MOST_STEP = 4.0;
    /** How long a send waits for a block before it is confirmed without one: ten minutes. */
    public static final long CONFIRM_TICKS = 12_000L;
    /** The ticks in a minute. */
    public static final long TICKS_PER_MINUTE = 1_200L;
    /** The settings' defaults. */
    public static final TeracoinRules DEFAULT = new TeracoinRules(10L * TICKS_PER_MINUTE, Trc.of(50), true, 2_100,
            100, 0.05);

    public TeracoinRules {
        targetTicks = Math.max(TICKS_PER_MINUTE, targetTicks);
        reward = Math.max(0L, reward);
        halvingBlocks = Math.max(1, halvingBlocks);
        floorDays = Math.max(1, floorDays);
        rareChance = Math.max(0.0, Math.min(1.0, rareChance));
    }

    /** The lowest the difficulty may be after {@code worldDays} days of the world: its start x (1 + days / floor). */
    public long floor(final long worldDays) {
        final long days = Math.max(0L, worldDays);
        return START_DIFFICULTY + START_DIFFICULTY * days / floorDays;
    }
}
