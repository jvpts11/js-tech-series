/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A redstone port of a multiblock's shell: one that reads tells its terminal the signal round it, as a lever beside
 * a machine would; one that gives puts out its terminal's signal, as a comparator reading it would. Which it does
 * is its block's state ({@link RedstonePortBlock#GIVES}), so it never reads what it gives.
 */
public class RedstonePortBlockEntity extends MultiblockPartBlockEntity {

    public RedstonePortBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** Tells its terminal what it hears now, or nothing once it gives. */
    public void hear(final ServerLevel level) {
        final IRedstonePortHost host = host(level);
        if (host != null) {
            final boolean gives = getBlockState().hasProperty(RedstonePortBlock.GIVES)
                    && getBlockState().getValue(RedstonePortBlock.GIVES);
            host.heard(this.worldPosition, gives ? 0 : level.getBestNeighborSignal(this.worldPosition));
        }
    }

    /** What it puts out while it gives: its terminal's signal, or none while it is part of nothing. */
    public int given(final BlockGetter level) {
        final IRedstonePortHost host = host(level);
        return host == null ? 0 : host.givenSignal();
    }

    @Nullable
    private IRedstonePortHost host(final BlockGetter level) {
        final BlockPos terminal = terminalPos();
        if (terminal == null) {
            return null;
        }
        return level.getBlockEntity(terminal) instanceof IRedstonePortHost host ? host : null;
    }
}
