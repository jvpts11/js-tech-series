/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.blockentity.MainframePartBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralHub;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.peripheral.PeripheralLinkValidator;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.util.Loaded;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Wires the pure {@link PeripheralLinkValidator} to the world, and finds the computer a peripheral hangs from.
 *
 * <p>A peripheral reaches its computer by standing against it, or along the peripheral wires of the Core's cable
 * blocks. A wire leads only where it crosses: to the cables of its own era it joins and to the blocks it plugs into,
 * and it plugs into a peripheral or a computer only where that block's port takes its era's cable, so an older device
 * never hangs from a newer cable. A run reaches as far as its era's cable does.
 */
public final class PeripheralLinks {

    /**
     * How a peripheral declared with a {@link PeripheralLink} finds its computer: against it, or along the peripheral
     * wires that plug into it.
     */
    public static final PeripheralLink.ILinkWorld COMPUTING = new PeripheralLink.ILinkWorld() {
        @Override
        public OptionalLong discoverOwner(final ServerLevel level, final long endpoint) {
            return PeripheralLinks.discoverOwner(level, endpoint);
        }

        @Override
        public PeripheralLinkValidator validator(final ServerLevel level) {
            return PeripheralLinks.validator(level);
        }
    };

    private PeripheralLinks() {
    }

    public static PeripheralLinkValidator validator(final ServerLevel level) {
        return new PeripheralLinkValidator(
                pos -> cableTypeAt(level, pos),
                pos -> ownerAt(level, pos),
                pos -> endpointAt(level, pos),
                pos -> neighbors(level, pos),
                pos -> reachAt(level, pos));
    }

    /** Finds the computer reachable from {@code endpointPos}: one against any of its faces, or along its cables. */
    public static OptionalLong discoverOwner(final ServerLevel level, final long endpointPos) {
        return discoverFrom(level, endpointPos, sixAround(endpointPos));
    }

    /**
     * Finds the computer reachable through one face only: {@code socketPos} is the neighbour on that face.
     * For devices with a single cable socket, so a cable against another face is not a link.
     */
    public static OptionalLong discoverOwnerThrough(final ServerLevel level, final long endpointPos,
                                                    final long socketPos) {
        return discoverFrom(level, endpointPos, List.of(socketPos));
    }

    /**
     * Whether the block at {@code socketPos} carries a link of {@code type} to {@code ownerPos}: a peripheral cable,
     * or the owner itself (or one of its parts) standing there.
     */
    public static boolean socketReaches(final ServerLevel level, final long socketPos, final long ownerPos,
                                        final PeripheralCableType type) {
        if (cableTypeAt(level, socketPos).filter(t -> t == type).isPresent()) {
            return true;
        }
        final OptionalLong owner = resolveOwnerPos(level, socketPos, type);
        return owner.isPresent() && owner.getAsLong() == ownerPos;
    }

    private static Optional<PeripheralCableType> cableTypeAt(final ServerLevel level, final long pos) {
        return peripheralWire(level, pos) != null ? Optional.of(PeripheralCableType.COMPUTING) : Optional.empty();
    }

    /* How far a run of the peripheral cable at {@code pos} reaches, its era's reach; 0 where there is none. */
    private static int reachAt(final ServerLevel level, final long pos) {
        final Wire wire = peripheralWire(level, pos);
        return wire == null ? 0 : wire.type().range();
    }

    private static Optional<IPeripheralOwner> ownerAt(final ServerLevel level, final long pos) {
        return Loaded.blockEntity(level, BlockPos.of(pos)) instanceof IPeripheralOwner owner
                ? Optional.of(owner) : Optional.empty();
    }

    private static Optional<IPeripheralEndpoint> endpointAt(final ServerLevel level, final long pos) {
        return Loaded.blockEntity(level, BlockPos.of(pos)) instanceof IPeripheralEndpoint endpoint
                ? Optional.of(endpoint) : Optional.empty();
    }

    /*
     * Where {@code pos} leads: a cable block with a peripheral wire only across the faces that wire crosses, to the
     * cables it joins and the blocks it plugs into; any other block to the six beside it.
     */
    private static List<Long> neighbors(final ServerLevel level, final long pos) {
        final BlockPos at = BlockPos.of(pos);
        if (level.isLoaded(at) && level.getBlockEntity(at) instanceof CableBlockEntity cable) {
            final Wire wire = peripheralWireOf(cable);
            if (wire != null) {
                final int crossed = cable.links(wire.slot());
                final List<Long> result = new ArrayList<>(Direction.values().length);
                for (final Direction face : Direction.values()) {
                    if ((crossed & 1 << face.get3DDataValue()) != 0) {
                        result.add(at.relative(face).asLong());
                    }
                }
                return result;
            }
        }
        return sixAround(pos);
    }

    private static List<Long> sixAround(final long pos) {
        final BlockPos p = BlockPos.of(pos);
        final List<Long> result = new ArrayList<>(Direction.values().length);
        for (final Direction direction : Direction.values()) {
            result.add(p.relative(direction).asLong());
        }
        return result;
    }

    /* The peripheral wire of the cable block at {@code pos}, or null when there is none or the block is away. */
    private static @Nullable Wire peripheralWire(final ServerLevel level, final long pos) {
        final BlockPos at = BlockPos.of(pos);
        return level.isLoaded(at) && level.getBlockEntity(at) instanceof CableBlockEntity cable
                ? peripheralWireOf(cable) : null;
    }

    private static @Nullable Wire peripheralWireOf(final CableBlockEntity cable) {
        for (final Wire wire : cable.wires()) {
            if (wire.type().grid() == GridKind.PERIPHERAL) {
                return wire;
            }
        }
        return null;
    }

    private static OptionalLong resolveOwnerPos(final ServerLevel level, final long pos,
                                                final PeripheralCableType type) {
        final BlockEntity be = Loaded.blockEntity(level, BlockPos.of(pos));
        if (be instanceof IPeripheralOwner owner && owner.cableType() == type) {
            return OptionalLong.of(pos);
        }
        // A mainframe or a rack cabinet spans several blocks; a cable touching any part links to its controller.
        if ((be instanceof MainframePartBlockEntity || be instanceof ServerRackPartBlockEntity)
                && be instanceof MultiblockPartBlockEntity part) {
            final BlockPos controller = part.controllerPos();
            if (controller != null && Loaded.blockEntity(level, controller) instanceof IPeripheralOwner owner
                    && owner.cableType() == type) {
                return OptionalLong.of(controller.asLong());
            }
        }
        return OptionalLong.empty();
    }

    /*
     * The computer a linked hub at {@code pos} hangs from, when it passes a peripheral taking a port of {@code kind}:
     * a peripheral reaching that hub hangs from the same computer, on one of the hub's ports.
     */
    private static OptionalLong ownerThroughHub(final ServerLevel level, final long pos, final PortKind kind) {
        if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof IPeripheralHub hub && hub.passes(kind)) {
            final Optional<Long> owner = hub.linkedOwner();
            if (owner.isPresent()) {
                return OptionalLong.of(owner.get());
            }
        }
        return OptionalLong.empty();
    }

    /* The computer standing at {@code pos}, or the one a hub standing there hangs from. */
    private static OptionalLong ownerAtOrBehind(final ServerLevel level, final long pos, final PortKind kind) {
        final OptionalLong owner = resolveOwnerPos(level, pos, PeripheralCableType.COMPUTING);
        return owner.isPresent() ? owner : ownerThroughHub(level, pos, kind);
    }

    /*
     * The nearest computer from the peripheral at {@code endpointPos}: one on a seed place, against it, or one a
     * peripheral wire leads to, entering each wire only where it plugs back into the place before it, or the one a
     * linked hub so reached hangs from. A run stops at the reach of the shortest-reaching cable on it.
     */
    private static OptionalLong discoverFrom(final ServerLevel level, final long endpointPos,
                                             final List<Long> seeds) {
        final PortKind kind = endpointAt(level, endpointPos).map(IPeripheralEndpoint::portKind)
                .orElse(PortKind.DEVICE);
        final Set<Long> visited = new HashSet<>();
        // Each queued cable: its position, how many cables from the peripheral, and the shortest reach on the way.
        final Deque<long[]> queue = new ArrayDeque<>();
        visited.add(endpointPos);
        for (final long neighbor : seeds) {
            if (!visited.add(neighbor)) {
                continue;
            }
            final OptionalLong owner = ownerAtOrBehind(level, neighbor, kind);
            if (owner.isPresent()) {
                return owner;
            }
            final int reach = reachAt(level, neighbor);
            if (reach > 0 && neighbors(level, neighbor).contains(endpointPos)) {
                queue.addLast(new long[]{neighbor, 1L, reach});
            }
        }
        while (!queue.isEmpty()) {
            final long[] current = queue.pollFirst();
            final int distance = (int) current[1];
            final int limit = (int) current[2];
            for (final long neighbor : neighbors(level, current[0])) {
                if (!visited.add(neighbor)) {
                    continue;
                }
                final OptionalLong owner = ownerAtOrBehind(level, neighbor, kind);
                if (owner.isPresent()) {
                    return owner;
                }
                final int reach = reachAt(level, neighbor);
                if (distance < limit && reach > 0 && neighbors(level, neighbor).contains(current[0])) {
                    queue.addLast(new long[]{neighbor, distance + 1L, Math.min(limit, reach)});
                }
            }
        }
        return OptionalLong.empty();
    }
}
