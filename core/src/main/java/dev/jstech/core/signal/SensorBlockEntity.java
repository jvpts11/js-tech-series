/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sensor's block entity: what it reads ({@link #readings()}, written by the mod), which reading its signal speaks,
 * and the redstone it gives, read again every {@link #READ_EVERY} ticks and handed to its neighbours only when it
 * changed. Offered as {@link Signals#OUTPUT}, everything that reads sensors reads it.
 */
public abstract class SensorBlockEntity extends SyncedBlockEntity implements ISignalOutput {

    private final IntField chosen = fields().integer("chosen", 0).save().toClient();
    private final SignalLevel output = SignalLevel.in(fields(), "signal");

    /** How often a sensor reads, in ticks. */
    public static final int READ_EVERY = 10;

    protected SensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public abstract List<Reading> readings();

    @Override
    public int chosen() {
        return chosen.get();
    }

    /** Has its signal speak its reading numbered {@code index}, from the next reading on. */
    public void choose(final int index) {
        chosen.set(Math.max(0, index));
    }

    /** The redstone it gives now, as last read, 0 to 15. */
    public int signal() {
        return output.strength();
    }

    /** Reads again, and gives what it reads; its neighbours are told only of a change. */
    public void read() {
        if (level != null && !level.isClientSide()) {
            output.set(level, worldPosition, strength());
        }
    }
}
