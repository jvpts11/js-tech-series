/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.util.Arrays;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tests of cost: what a block entity's tick costs the server, measured on its own, with a limit a test fails past, so
 * a change that makes a machine dearer fails the build instead of being found on a server.
 *
 * <pre>{@code
 * Costs.below(helper, KILN, 25.0);   // the kiln's tick costs less than 25 microseconds
 * }</pre>
 *
 * <p>The ticks are run at once, as {@link TestTime#advance} runs them: first a share to warm the code up, then each
 * tick timed alone, and the cost is the median of them, which a pause of the collector does not move.
 */
public final class Costs {

    /** How many ticks are run and timed when a test does not say. */
    public static final int TICKS = 200;
    /** How many ticks are run first, untimed, for the code to warm up. */
    public static final int WARM_UP = 200;

    private static final double NANOS_PER_MICRO = 1_000.0;

    private Costs() {
    }

    /** What a tick of the block entity at the arena's place {@code at} costs, in microseconds. */
    public static double perTick(final GameTestHelper helper, final BlockPos at, final int ticks) {
        final ServerLevel level = helper.getLevel();
        final BlockEntity entity = level.getBlockEntity(helper.absolutePos(at));
        if (entity == null) {
            throw Expect.failure(helper, at, "a cost measured: expected a block entity, found "
                    + level.getBlockState(helper.absolutePos(at)));
        }
        return perTick(level, entity, ticks);
    }

    /** What a tick of {@code entity} costs, in microseconds: the median of {@code ticks} ticks, after a warm-up. */
    public static double perTick(final ServerLevel level, final BlockEntity entity, final int ticks) {
        TestTime.advance(level, entity, WARM_UP);
        final long[] nanos = new long[Math.max(1, ticks)];
        for (int i = 0; i < nanos.length; i++) {
            final long start = System.nanoTime();
            if (TestTime.advance(level, entity, 1) == 0) {
                throw new IllegalStateException("the block entity at " + entity.getBlockPos().toShortString()
                        + " does not tick, so it has no cost a tick to measure");
            }
            nanos[i] = System.nanoTime() - start;
        }
        Arrays.sort(nanos);
        return nanos[nanos.length / 2] / NANOS_PER_MICRO;
    }

    /**
     * That a tick of the block entity at {@code at} costs less than {@code micros} microseconds; else a failure saying
     * what it cost.
     *
     * @return what it cost
     */
    public static double below(final GameTestHelper helper, final BlockPos at, final double micros) {
        final double cost = perTick(helper, at, TICKS);
        if (!(cost < micros)) {
            throw Expect.failure(helper, at, String.format(Locale.ROOT,
                    "a tick costs %.2f microseconds, more than its limit of %.2f", cost, micros));
        }
        return cost;
    }
}
