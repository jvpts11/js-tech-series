/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Time under a test's control: a block entity's own ticks run many at once, so a machine that takes an hour of game
 * time is shown to finish in one tick of the test; and the whole server run as fast as it can for a while, its ticks
 * no shorter in game time, so a long wait passes in a few seconds.
 *
 * <pre>{@code
 * TestTime.advance(helper, KILN, 20 * 60);   // a minute of the kiln's ticks, now
 * }</pre>
 */
public final class TestTime {

    private TestTime() {
    }

    /**
     * Runs the ticker of the block entity at the arena's place {@code at} {@code ticks} times, now, as the level
     * would over that many ticks; nothing else in the world moves meanwhile.
     *
     * @return how many ticks ran: fewer when the block entity is gone or stops ticking on the way
     */
    public static int advance(final GameTestHelper helper, final BlockPos at, final int ticks) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(at);
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            throw Expect.failure(helper, at, "ticks run at once: expected a block entity, found "
                    + level.getBlockState(pos));
        }
        return advance(level, entity, ticks);
    }

    /** Runs {@code entity}'s ticker {@code ticks} times, now. */
    public static <T extends BlockEntity> int advance(final ServerLevel level, final T entity, final int ticks) {
        final BlockState state = entity.getBlockState();
        @SuppressWarnings("unchecked")
        final BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) state.getTicker(level, entity.getType());
        if (ticker == null) {
            return 0;
        }
        for (int i = 0; i < ticks; i++) {
            if (entity.isRemoved() || level.getBlockEntity(entity.getBlockPos()) != entity) {
                return i;
            }
            ticker.tick(level, entity.getBlockPos(), entity.getBlockState(), entity);
        }
        return ticks;
    }

    /**
     * Has the server run its next {@code ticks} ticks as fast as it can, as the game's {@code /tick sprint} does: the
     * same ticks, in less time. Every test running meanwhile is sped up alike.
     *
     * @return whether the sprint began; false when one was already running
     */
    public static boolean sprint(final GameTestHelper helper, final int ticks) {
        return helper.getLevel().getServer().tickRateManager().requestGameToSprint(ticks);
    }
}
