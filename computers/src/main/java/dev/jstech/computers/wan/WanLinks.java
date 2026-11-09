/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.block.WanGatewayPartBlock;
import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.WanGatewayPartBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.jstechseries.network.ConnectivityIndex;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Finds where the long-distance line plugged into a port of a WAN Gateway Computer goes: along its run of cables, a
 * chain whose every block joins at most two, to the port of another gateway at its other end.
 */
public final class WanLinks {

    private static final List<Direction> SIDES = List.of(Direction.values());

    private WanLinks() {
    }

    /**
     * The link from port {@code port} of the gateway standing at {@code origin} facing {@code facing}, or null when no
     * long-distance cable is plugged there, or its run ends anywhere but in another gateway's port.
     */
    @Nullable
    public static WanLink find(final ServerLevel level, final BlockPos origin, final Direction facing, final int port) {
        final BlockPos block = WanGatewayStructure.portBlock(origin, facing, port);
        final List<Long> plugged = DataWires.numbersReaching(level, block, facing.getOpposite(),
                DataWires.of(DataLine.LONG_DISTANCE));
        if (plugged.isEmpty()) {
            return null;
        }
        final ConnectivityIndex index = NetworkSystem.get(level).connectivity();
        final long start = plugged.getFirst();
        final DataLink line = index.linkOf(start).orElse(null);
        if (line == null) {
            return null;
        }
        final Set<Long> run = index.reachableFrom(start, Set.of());
        for (final long wire : run) {
            // Only a run's two ends can meet a port; every other cable of it joins two cables of the run.
            if (index.grid().neighbours(wire).size() > 1) {
                continue;
            }
            final BlockPos at = Cables.blockOf(level, wire);
            if (at == null) {
                continue;
            }
            for (final Direction side : SIDES) {
                // An end in a chunk that is not loaded is no gateway answering; reading it would load the chunk.
                if (!Loaded.at(level, at.relative(side))) {
                    continue;
                }
                final WanLink link = portAt(level, at.relative(side), wire, origin, port, line, start, run);
                if (link != null) {
                    return link;
                }
            }
        }
        return null;
    }

    /*
     * The link to the gateway port at {@code candidate}, when that block is the back of another gateway's port and the
     * run's cable {@code wire} crosses into it there.
     */
    @Nullable
    private static WanLink portAt(final ServerLevel level, final BlockPos candidate, final long wire,
                                  final BlockPos origin, final int port, final DataLink line, final long plugged,
                                  final Set<Long> run) {
        final BlockState state = level.getBlockState(candidate);
        if (!(state.getBlock() instanceof WanGatewayPartBlock) || state.getValue(WanGatewayPartBlock.PORT) == 0
                || !(level.getBlockEntity(candidate) instanceof WanGatewayPartBlockEntity part)
                || part.originPos() == null) {
            return null;
        }
        final int farPort = state.getValue(WanGatewayPartBlock.PORT);
        final BlockPos far = part.originPos();
        if (far.equals(origin) && farPort == port) {
            return null;
        }
        final Direction back = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        if (!DataWires.numbersReaching(level, candidate, back, DataWires.of(DataLine.LONG_DISTANCE)).contains(wire)) {
            return null;
        }
        return new WanLink(origin, port, far, farPort, line, plugged, run);
    }
}
