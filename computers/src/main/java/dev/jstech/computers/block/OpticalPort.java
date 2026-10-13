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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/**
 * The fibre port of a machine on the backbone. The fibre enters a Mainframe or a Cluster Management Computer only when
 * the machine holds an Optical Network Card, and a server rack only as far as the Fibre Optic Interface mounted in it
 * reaches; without either the machine takes the copper of its backbone alone. What the port takes is kept in the
 * block's state, on the controller and on every part of a cabinet, because a cable asks a face what it takes knowing
 * only the block's state.
 */
public final class OpticalPort {

    /** Whether the machine holds an Optical Network Card. */
    public static final BooleanProperty OPTICAL = BooleanProperty.create("optical");
    /** How much of the fibre a server rack takes, from the Fibre Optic Interface mounted in it. */
    public static final EnumProperty<FibreReach> FIBRE = EnumProperty.create("fibre", FibreReach.class);

    private OpticalPort() {
    }

    /** Whether a machine in {@code state} lets {@code offered} in: anything but the fibre, and the fibre it reaches. */
    public static boolean admits(final BlockState state, final Connection offered) {
        final DataLink link = DataLines.linkOf(offered);
        if (link == null || !link.straight()) {
            return true;
        }
        if (state.hasProperty(FIBRE)) {
            return state.getValue(FIBRE).admits(link.era());
        }
        return state.hasProperty(OPTICAL) && state.getValue(OPTICAL);
    }

    /** Whether {@code build} holds an Optical Network Card; a machine not assembled holds none that works. */
    public static boolean holdsCard(@Nullable final ComputerBuild build) {
        return build != null && !build.cardsOfKind(ExpansionCardKind.NETWORK).isEmpty();
    }

    /**
     * Says what the machine standing in {@code blocks} takes of the fibre. Only the blocks whose state changes are
     * set, and setting them tells the cables beside them, which then join the fibre or let it go.
     */
    public static <T extends Comparable<T>> void set(final Level level, final Iterable<BlockPos> blocks,
                                                     final Property<T> port, final T value) {
        for (final BlockPos pos : blocks) {
            final BlockState state = level.getBlockState(pos);
            if (state.hasProperty(port) && !state.getValue(port).equals(value)) {
                level.setBlock(pos, state.setValue(port, value), Block.UPDATE_ALL);
            }
        }
    }
}
