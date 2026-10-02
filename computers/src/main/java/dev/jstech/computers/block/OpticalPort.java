/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The fibre port of a machine on the backbone. The fibre enters a Mainframe, a rack or a Cluster Management Computer
 * only when the machine holds an Optical Network Card; without one it takes the copper of its backbone alone. Whether
 * it holds one is kept in the block's state, on the controller and on every part of a cabinet, because a cable asks a
 * face what it takes knowing only the block's state.
 */
public final class OpticalPort {

    /** Whether the machine holds an Optical Network Card. */
    public static final BooleanProperty OPTICAL = BooleanProperty.create("optical");

    private OpticalPort() {
    }

    /** Whether a machine in {@code state} lets {@code offered} in: anything but the fibre, and the fibre with a card. */
    public static boolean admits(final BlockState state, final Connection offered) {
        final DataLink link = DataLines.linkOf(offered);
        return link == null || !link.straight() || state.hasProperty(OPTICAL) && state.getValue(OPTICAL);
    }

    /** Whether {@code build} holds an Optical Network Card; a machine not assembled holds none that works. */
    public static boolean holdsCard(@Nullable final ComputerBuild build) {
        return build != null && !build.cardsOfKind(ExpansionCardKind.NETWORK).isEmpty();
    }

    /**
     * Says whether the machine standing in {@code blocks} holds a card. Only the blocks whose state changes are set,
     * and setting them tells the cables beside them, which then join the fibre or let it go.
     */
    public static void set(final Level level, final Iterable<BlockPos> blocks, final boolean optical) {
        for (final BlockPos pos : blocks) {
            final BlockState state = level.getBlockState(pos);
            if (state.hasProperty(OPTICAL) && state.getValue(OPTICAL) != optical) {
                level.setBlock(pos, state.setValue(OPTICAL, optical), Block.UPDATE_ALL);
            }
        }
    }
}
