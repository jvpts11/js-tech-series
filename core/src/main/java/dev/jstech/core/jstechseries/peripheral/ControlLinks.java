/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.peripheral;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.IGridMover;
import dev.jstech.core.grid.IGridPolicy;
import dev.jstech.core.jstechseries.network.NetworkAttachments;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.util.Loaded;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The industrial control cable: a line of the series' cable block that runs from a controller to the machines it
 * reads and commands, point to point over at most eight blocks of cable, as a screen's cable runs to its computer
 * (industrial/docs/design/computers.md). Its wires plug into any block that offers its values
 * ({@link IMachineParameters}): every machine of the Core does, and so can a valve or a tank. A controller owns links
 * on it ({@link IPeripheralOwner#linksOver} {@link PeripheralCableType#INDUSTRIAL_CONTROL}) on ports of
 * {@link PortKind#CONTROL}, as many machines as its cards reach.
 *
 * <p>The controller keeps its links ({@link #keep}): it walks its cables and links what they reach, nearest first,
 * while it has a port free, and lets go of what they no longer reach. A machine is linked to one controller at a time,
 * the first to reach it, and keeps nothing itself, so a world full of machines pays nothing for the cable; only the
 * controllers walk it.
 *
 * <p>The Core declares the line and its grid, so every mod's controllers and machines meet on one cable; a mod
 * declares the cable itself on {@link #LINE} and {@link #GRID}, in its lane, with its plugs.
 */
public final class ControlLinks {

    /* What its wires plug into: a block whose values can be read, and a controller. It carries nothing to share. */
    private static final IGridPolicy POLICY = new IGridPolicy() {
        @Override
        public Optional<IGridMover> mover(final GridKind kind) {
            return Optional.empty();
        }

        @Override
        public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
            return level.getCapability(IMachineParameters.CAPABILITY, pos, face) != null
                    || controllerAt(level, pos) != null;
        }
    };

    /** The grid its runs join on. */
    public static final GridKind GRID = GridKind.declare(id("industrial_control"), POLICY);
    /** Its line, of one generation: a field bus cable of any age is the same cable. */
    public static final Connection LINE = Connection.of(id("industrial_control"));
    /** How often, in ticks, a controller walks its cables again to keep its links. */
    public static final int KEEP_EVERY = 20;

    private ControlLinks() {
    }

    /** Declares the line and its grid, from the Core's constructor, before any mod declares a cable on them. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * Keeps the links of {@code controller}, standing at {@code at}: lets go of what its cables no longer reach, of
     * what no longer offers its values, and of what is past its ports; then links what they reach, nearest first,
     * while a port is free and the block is no other controller's. Called by the controller on its server tick, every
     * {@link #KEEP_EVERY} ticks or so.
     */
    public static void keep(final ServerLevel level, final BlockPos at, final IPeripheralOwner controller) {
        final Claims claims = level.getData(NetworkAttachments.CONTROL_CLAIMS.get());
        final long self = at.asLong();
        final List<Long> reached = reached(level, at, controller);
        final Set<Long> reachable = new HashSet<>(reached);
        for (final long linked : List.copyOf(controller.linkedEndpoints())) {
            if (controller.kindOf(linked) != PortKind.CONTROL) {
                continue;
            }
            if (!reachable.contains(linked) || !controller.holdsPort(linked)) {
                controller.onEndpointUnlinked(linked);
                claims.release(linked, self);
            } else {
                // A controller loaded from its save claims its machines again.
                claims.claim(linked, self);
            }
        }
        for (final long found : reached) {
            if (controller.portsInUse(PortKind.CONTROL) >= controller.ports(PortKind.CONTROL)) {
                return;
            }
            if (controller.isLinked(found) || heldByAnother(level, claims, found, self)) {
                continue;
            }
            controller.onEndpointLinked(found, PortKind.CONTROL);
            claims.claim(found, self);
        }
    }

    /** Lets go of every link of {@code controller}, standing at {@code at}: it was broken, or it lost its cards. */
    public static void release(final ServerLevel level, final BlockPos at, final IPeripheralOwner controller) {
        final Claims claims = level.getData(NetworkAttachments.CONTROL_CLAIMS.get());
        for (final long linked : List.copyOf(controller.linkedEndpoints())) {
            if (controller.kindOf(linked) == PortKind.CONTROL) {
                controller.onEndpointUnlinked(linked);
                claims.release(linked, at.asLong());
            }
        }
    }

    /** The controller the block at {@code pos} is linked to, while it is loaded and still holds it. */
    public static Optional<BlockPos> controllerOf(final ServerLevel level, final BlockPos pos) {
        final Claims claims = level.getData(NetworkAttachments.CONTROL_CLAIMS.get());
        final Long owner = claims.byEndpoint.get(pos.asLong());
        if (owner == null) {
            return Optional.empty();
        }
        final IPeripheralOwner controller = controllerAt(level, BlockPos.of(owner));
        return controller != null && controller.isLinked(pos.asLong()) ? Optional.of(BlockPos.of(owner))
                : Optional.empty();
    }

    /**
     * The blocks offering their values that the control cables of the controller at {@code at} reach, nearest first:
     * walked along the wires that join, no further than the shortest reach on the way, and never more than
     * {@link PeripheralCableType#maxLength} cables.
     */
    public static List<Long> reached(final ServerLevel level, final BlockPos at, final IPeripheralOwner controller) {
        final Set<Long> found = new LinkedHashSet<>();
        final Set<Long> visited = new HashSet<>(controller.occupiedPositions(at.asLong()));
        // Each queued cable: its position, how many cables from the controller, and the shortest reach on the way.
        final Deque<long[]> queue = new ArrayDeque<>();
        for (final long from : controller.occupiedPositions(at.asLong())) {
            for (final Direction face : Direction.values()) {
                final long next = BlockPos.of(from).relative(face).asLong();
                final int reach = Math.min(PeripheralCableType.INDUSTRIAL_CONTROL.maxLength(), reachAt(level, next));
                if (reach > 0 && joins(level, next, face.getOpposite()) && visited.add(next)) {
                    queue.addLast(new long[] {next, 1L, reach});
                }
            }
        }
        while (!queue.isEmpty()) {
            final long[] cable = queue.pollFirst();
            final BlockPos here = BlockPos.of(cable[0]);
            for (final Direction face : Direction.values()) {
                if (!joins(level, cable[0], face)) {
                    continue;
                }
                final BlockPos there = here.relative(face);
                final int reach = reachAt(level, there.asLong());
                if (reach > 0) {
                    if (cable[1] < cable[2] && joins(level, there.asLong(), face.getOpposite())
                            && visited.add(there.asLong())) {
                        queue.addLast(new long[] {there.asLong(), cable[1] + 1, Math.min(cable[2], reach)});
                    }
                } else if (level.getCapability(IMachineParameters.CAPABILITY, there, face.getOpposite()) != null) {
                    found.add(there.asLong());
                }
            }
        }
        return List.copyOf(found);
    }

    /* Whether another controller, loaded or away, holds the block at {@code pos}. */
    private static boolean heldByAnother(final ServerLevel level, final Claims claims, final long pos,
                                         final long self) {
        final Long owner = claims.byEndpoint.get(pos);
        if (owner == null || owner == self) {
            return false;
        }
        final BlockPos ownerAt = BlockPos.of(owner);
        if (!level.isLoaded(ownerAt)) {
            // A controller whose chunk is away keeps its machines until it can be asked.
            return true;
        }
        final IPeripheralOwner other = controllerAt(level, ownerAt);
        return other != null && other.isLinked(pos);
    }

    /* The controller standing at {@code pos}, loaded and owning links on the control cable; null for none. */
    private static @Nullable IPeripheralOwner controllerAt(final ServerLevel level, final BlockPos pos) {
        return Loaded.blockEntity(level, pos) instanceof IPeripheralOwner owner
                && owner.linksOver(PeripheralCableType.INDUSTRIAL_CONTROL) ? owner : null;
    }

    /* Whether the control wire at {@code pos} joins toward its {@code face}: to a cable, a machine or a controller. */
    private static boolean joins(final ServerLevel level, final long pos, final Direction face) {
        final BlockPos at = BlockPos.of(pos);
        if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof CableBlockEntity cable)) {
            return false;
        }
        final Wire wire = wireOf(cable);
        return wire != null && (cable.links(wire.slot()) & 1 << face.get3DDataValue()) != 0;
    }

    /* How many cables a link may cross from the control wire at {@code pos}; none where there is no wire. */
    private static int reachAt(final ServerLevel level, final long pos) {
        final BlockPos at = BlockPos.of(pos);
        if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof CableBlockEntity cable)) {
            return 0;
        }
        final Wire wire = wireOf(cable);
        return wire == null ? 0 : wire.type().range();
    }

    private static @Nullable Wire wireOf(final CableBlockEntity cable) {
        for (final Wire wire : cable.wires()) {
            if (wire.type().grid() == GRID) {
                return wire;
            }
        }
        return null;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /**
     * Which controller each block is linked to, in one dimension, kept while the server runs: a controller loaded
     * from its save claims its machines again the first time it keeps its links.
     */
    @ApiStatus.Internal
    public static final class Claims {

        private final Map<Long, Long> byEndpoint = new HashMap<>();

        void claim(final long endpoint, final long controller) {
            this.byEndpoint.put(endpoint, controller);
        }

        void release(final long endpoint, final long controller) {
            this.byEndpoint.remove(endpoint, controller);
        }
    }
}
