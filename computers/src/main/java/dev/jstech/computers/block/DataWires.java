/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridMember;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.INetworkBridge;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The data cables as the computers read them in the Core's cable blocks: which wires carry the network, the crafting
 * wire kept apart from it, and what a cable block's data reaches.
 */
public final class DataWires {

    private DataWires() {
    }

    /** The data cable {@code wire} is, its line and era, or null for a wire that carries no data. */
    public static @Nullable DataLink linkOf(final Wire wire) {
        return wire.type().grid().carriesNetwork() ? DataLines.linkOf(wire.type().line()) : null;
    }

    /** Whether {@code wire} carries the network: data, and not the crafting line, which runs apart from it. */
    public static boolean isNetwork(final Wire wire) {
        final DataLink link = linkOf(wire);
        return link != null && link.line() != DataLine.CRAFTING;
    }

    /** Whether {@code wire} is the crafting line, from Crafting Switches to their Crafting Computer. */
    public static boolean isCrafting(final Wire wire) {
        final DataLink link = linkOf(wire);
        return link != null && link.line() == DataLine.CRAFTING;
    }

    /** Whether {@code wire} is a data cable of {@code line}, in any era. */
    public static Predicate<Wire> of(final DataLine line) {
        return wire -> {
            final DataLink link = linkOf(wire);
            return link != null && link.line() == line;
        };
    }

    /** The first wire of the cable block that carries the network, or null when it holds none. */
    public static @Nullable Wire networkWire(final CableBlockEntity cable) {
        for (final Wire wire : cable.wires()) {
            if (isNetwork(wire)) {
                return wire;
            }
        }
        return null;
    }

    /** Whether the cable block holds a wire that {@code which} picks. */
    public static boolean holds(final CableBlockEntity cable, final Predicate<Wire> which) {
        for (final Wire wire : cable.wires()) {
            if (which.test(wire)) {
                return true;
            }
        }
        return false;
    }

    /** The network the cable block's data reaches, from the first of its network wires that is on one. */
    public static @Nullable NetworkUuid networkOf(final CableBlockEntity cable) {
        for (final Wire wire : cable.wires()) {
            if (isNetwork(wire)) {
                final NetworkUuid network = cable.network(wire.type()).orElse(null);
                if (network != null) {
                    return network;
                }
            }
        }
        return null;
    }

    /** The Mainframe that owns {@code network}, or null when it has none loaded. */
    public static @Nullable MainframeBlockEntity mainframeOf(final ServerLevel level,
                                                            final @Nullable NetworkUuid network) {
        if (network == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(network)
                .map(pos -> Loaded.blockEntity(level, BlockPos.of(pos)))
                .filter(MainframeBlockEntity.class::isInstance)
                .map(MainframeBlockEntity.class::cast)
                .orElse(null);
    }

    /**
     * Puts the router at {@code pos} in the data grid, as a whole block that joins every data wire crossing into it
     * and every other bridge beside it; nothing changes when it is in already.
     */
    public static void placeRouter(final ServerLevel level, final BlockPos pos) {
        if (routerNumber(level, pos).isPresent()) {
            return;
        }
        final List<GridPlace> joined = new ArrayList<>();
        for (final Direction face : Direction.values()) {
            final BlockPos next = pos.relative(face);
            for (final Wire wire : Cables.reaching(level, pos, face)) {
                if (linkOf(wire) != null) {
                    joined.add(GridPlace.wire(next.asLong(), wire.slot().id()));
                }
            }
            if (level.getBlockState(next).getBlock() instanceof INetworkBridge) {
                joined.add(GridPlace.whole(next.asLong()));
            }
        }
        CoreGrids.place(level, GridKind.DATA, GridPlace.whole(pos.asLong()), GridMember.DEVICE, joined);
    }

    /** Takes the router at {@code pos} out of the data grid. */
    public static void removeRouter(final ServerLevel level, final BlockPos pos) {
        CoreGrids.remove(level, GridKind.DATA, GridPlace.whole(pos.asLong()));
    }

    /**
     * Puts the repeater at {@code pos} in the data grid: a place in the lane of each line it carries, joined to the
     * wires of that lane on its faces, so a line runs on through it and no two lines meet in it. A place that is in
     * already stays as it is.
     */
    public static void placeRepeater(final ServerLevel level, final BlockPos pos) {
        for (final DataLine line : RepeaterBlock.LINES) {
            final Lane lane = laneOf(line);
            final List<GridPlace> joined = new ArrayList<>();
            for (final Direction face : Direction.values()) {
                final BlockPos next = pos.relative(face);
                for (final Wire wire : Cables.reaching(level, pos, face)) {
                    if (wire.slot() == lane && linkOf(wire) != null) {
                        joined.add(GridPlace.wire(next.asLong(), lane.id()));
                    }
                }
            }
            CoreGrids.place(level, GridKind.DATA, GridPlace.wire(pos.asLong(), lane.id()), GridMember.DEVICE, joined);
        }
    }

    /** Takes the repeater at {@code pos} out of the data grid, every lane of it. */
    public static void removeRepeater(final ServerLevel level, final BlockPos pos) {
        for (final DataLine line : RepeaterBlock.LINES) {
            CoreGrids.remove(level, GridKind.DATA, GridPlace.wire(pos.asLong(), laneOf(line).id()));
        }
    }

    /**
     * The lane each data line runs in when it shares a cable block, the same in every era of it: access top left,
     * backbone top middle, high compute in the middle, crafting middle right. The long distance line shares no block,
     * and takes the middle as a cable alone does.
     */
    public static Lane laneOf(final DataLine line) {
        return switch (line) {
            case ACCESS -> Lane.TOP_LEFT;
            case BACKBONE -> Lane.TOP;
            case HPC, LONG_DISTANCE -> Lane.MIDDLE;
            case CRAFTING -> Lane.RIGHT;
        };
    }

    /** The number the router at {@code pos} is known by in the data grid, if it is in. */
    public static OptionalLong routerNumber(final ServerLevel level, final BlockPos pos) {
        final OptionalLong number = CoreGrids.places(level).find(GridPlace.whole(pos.asLong()));
        return number.isPresent() && CoreGrids.of(level, GridKind.DATA).contains(number.getAsLong())
                ? number : OptionalLong.empty();
    }

    /**
     * The grid numbers of the wires {@code which} picks among those of the cable block beyond {@code face} of the
     * block at {@code pos} that reach into it; only wires that are in their grid.
     */
    public static List<Long> numbersReaching(final ServerLevel level, final BlockPos pos, final Direction face,
                                             final Predicate<Wire> which) {
        final List<Long> numbers = new ArrayList<>(1);
        final BlockPos cablePos = pos.relative(face);
        for (final Wire wire : Cables.reaching(level, pos, face)) {
            if (which.test(wire)) {
                final OptionalLong number = Cables.number(level, cablePos, wire.type());
                if (number.isPresent()) {
                    numbers.add(number.getAsLong());
                }
            }
        }
        return numbers;
    }
}
