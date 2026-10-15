/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.IntField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The redstone a block gives, kept so its neighbours hear of it only when it changes: a block that gives a signal sets
 * it here each time it reads, and the blocks around it are told only on a change. No storm of updates, however often
 * it reads. It is saved with its block entity, so a world read back gives what it gave.
 */
public final class SignalLevel {

    private final IntField strength;

    private SignalLevel(final IntField strength) {
        this.strength = strength;
    }

    /** A signal kept in {@code fields} under {@code key}, saved with them and sent to the players who see it. */
    public static SignalLevel in(final BlockEntityFields fields, final String key) {
        return new SignalLevel(fields.integer(key, 0).save().toClient());
    }

    /** The signal it gives now, 0 to 15. */
    public int strength() {
        return this.strength.get();
    }

    /**
     * Gives {@code to} from the block at {@code pos} in {@code level}, held to 0 to 15, telling its neighbours only
     * when it changed; whether it changed.
     */
    public boolean set(final Level level, final BlockPos pos, final int to) {
        final int held = Math.max(0, Math.min(Reading.FULL, to));
        if (held == this.strength.get()) {
            return false;
        }
        this.strength.set(held);
        if (!level.isClientSide()) {
            final BlockState state = level.getBlockState(pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }
        return true;
    }
}
