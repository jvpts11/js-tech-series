/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Chance with a fixed seed, so a test of something random comes out the same on every run: a random source of its own
 * for what takes one (a loot table, a recipe's chance), and a level's or an entity's own random set to a seed for
 * work that reads it.
 *
 * <pre>{@code
 * final int drops = TestChance.seeded(helper.getLevel(), 42L, () -> rollOre(helper));
 * }</pre>
 *
 * <p>A level's random is shared by every test running in it, so work given a seeded level random runs whole inside
 * the call, before any other test's tick; the random is given a fresh seed again afterwards.
 */
public final class TestChance {

    private TestChance() {
    }

    /** A random source of its own, the same on every run for the same {@code seed}. */
    public static RandomSource source(final long seed) {
        return RandomSource.create(seed);
    }

    /** What {@code work} gives with {@code level}'s random seeded with {@code seed}, run whole now. */
    public static <T> T seeded(final ServerLevel level, final long seed, final Supplier<T> work) {
        level.random.setSeed(seed);
        try {
            return work.get();
        } finally {
            level.random.setSeed(RandomSource.create().nextLong());
        }
    }

    /** Sets {@code entity}'s own random to {@code seed}, for what it does with it from now on. */
    public static void seed(final Entity entity, final long seed) {
        entity.getRandom().setSeed(seed);
    }
}
