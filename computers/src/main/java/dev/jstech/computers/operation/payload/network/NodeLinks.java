/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.block.OpticalPort;
import dev.jstech.computers.block.RouterBlock;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How the machines of one network are linked to it, for the Network Manager: the cable each is plugged into, how long
 * that cable's run is, the optical router its fibre goes through; and the machines joined to the network that lost
 * their link, with why: a fibre that bends where no optical router turns it, or a run longer than its cable reaches.
 *
 * <p>Worked out when the Network Manager asks, never per tick: each question walks the network's places once.
 */
final class NodeLinks {

    private final ServerLevel level;
    private final ConnectivityIndex index;
    private final NetworkUuid network;
    /* The network's optical routers and its Mainframe, by the numbers the grid knows them by: where a fibre's way to
     * the network is looked along up to, never through. */
    private final Set<Long> stops = new HashSet<>();
    private final Set<Long> opticalRouters = new HashSet<>();

    NodeLinks(final ServerLevel level, final NetworkUuid network) {
        this.level = level;
        this.network = network;
        final NetworkSystem system = NetworkSystem.get(level);
        this.index = system.connectivity();
        for (final long place : index.positionsOf(network)) {
            final BlockPos block = Cables.blockOf(level, place);
            if (block != null && level.isLoaded(block)
                    && level.getBlockState(block).getBlock() instanceof RouterBlock router
                    && router.optical()) {
                opticalRouters.add(place);
            }
        }
        stops.addAll(opticalRouters);
        system.mainframePositionOf(network).ifPresent(stops::add);
    }

    /** The link of a machine plugged into {@code cables}: the fastest of them, and whether it holds a card. */
    NodeLink of(final Collection<Long> cables, final boolean optical) {
        DataLink best = null;
        long bestCable = 0L;
        for (final long cable : cables) {
            final Optional<DataLink> link = index.linkOf(cable);
            if (link.isPresent() && (best == null || link.get().throughput() > best.throughput())) {
                best = link.get();
                bestCable = cable;
            }
        }
        if (best == null) {
            return optical ? new NodeLink("", true, true, 0, NodeLink.REASON_NONE, NodeLink.NO_PLACE,
                    NodeLink.NO_PLACE) : NodeLink.NONE;
        }
        return NodeLink.up(best, optical, index.runLength(bestCable),
                best.straight() ? routerOf(bestCable) : NodeLink.NO_PLACE);
    }

    /** Whether the machine at {@code pos} holds an Optical Network Card, which its block's state says. */
    boolean optical(final BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false; // reading it would load the chunk
        }
        final BlockState state = level.getBlockState(pos);
        return state.hasProperty(OpticalPort.OPTICAL) && state.getValue(OpticalPort.OPTICAL);
    }

    /**
     * The machines joined to the network that lost their link, each once, by the block it stands at (a cabinet by its
     * controller), with why: first those beyond a fibre that bends, then those beyond a run too long.
     */
    Map<BlockPos, NodeLink> lost() {
        final Map<BlockPos, NodeLink> lost = new LinkedHashMap<>();
        for (final long place : index.positionsOf(network)) {
            final Optional<DataLink> link = index.linkOf(place);
            if (link.isPresent() && link.get().straight()) {
                bends(place, link.get(), lost);
            }
        }
        final Set<Long> cutOff = index.cutOffPositions(network);
        DataLink tooLong = null;
        int length = 0;
        for (final long place : cutOff) {
            if (index.runTooLong(place)) {
                tooLong = index.linkOf(place).orElse(null);
                length = index.runLength(place);
                break;
            }
        }
        if (tooLong != null) {
            for (final BlockPos machine : machinesAround(cutOff)) {
                lost.putIfAbsent(machine, NodeLink.tooLong(tooLong, optical(machine), length));
            }
        }
        return lost;
    }

    /*
     * A fibre run of the network that meets, at its side, another run of the same fibre heading straight at it: the
     * place the fibre would have to turn, where an optical router turns it. What the other run reaches is cut off.
     */
    private void bends(final long place, final DataLink link, final Map<BlockPos, NodeLink> lost) {
        final BlockPos block = Cables.blockOf(level, place);
        final CableBlockEntity cable = block == null || !level.isLoaded(block) ? null : Cables.at(level, block);
        if (cable == null) {
            return;
        }
        for (final Wire wire : cable.wires()) {
            if (!link.equals(DataWires.linkOf(wire))) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final CableBlockEntity other = cable.crosses(wire.type(), side) || !level.isLoaded(block.relative(side))
                        ? null : Cables.at(level, block.relative(side));
                if (other == null || !other.holds(wire.type()) || !other.crosses(wire.type(), side)
                        || other.crosses(wire.type(), side.getOpposite())) {
                    continue;
                }
                final OptionalLong beyond = Cables.number(level, block.relative(side), wire.type());
                if (beyond.isPresent() && !index.inSameNetwork(place, beyond.getAsLong())) {
                    for (final BlockPos machine : machinesAround(index.componentPositions(beyond.getAsLong()))) {
                        lost.putIfAbsent(machine, NodeLink.bends(link, optical(machine), block.asLong()));
                    }
                }
            }
        }
    }

    /* The first optical router the fibre at {@code cable} reaches on its way, before the Mainframe, or none. */
    private long routerOf(final long cable) {
        if (opticalRouters.isEmpty()) {
            return NodeLink.NO_PLACE;
        }
        final Set<Long> reach = index.reachableFrom(cable, stops);
        for (final long router : opticalRouters) {
            for (final long beside : index.grid().neighbours(router)) {
                if (reach.contains(beside)) {
                    final BlockPos block = Cables.blockOf(level, router);
                    return block == null ? NodeLink.NO_PLACE : block.asLong();
                }
            }
        }
        return NodeLink.NO_PLACE;
    }

    /* The machines standing at or beside the blocks of {@code places} that are not on the network. */
    private Set<BlockPos> machinesAround(final Collection<Long> places) {
        final Set<BlockPos> machines = new LinkedHashSet<>();
        for (final BlockPos block : Cables.blocksOf(level, places)) {
            addMachine(block, machines);
            for (final Direction side : Direction.values()) {
                addMachine(block.relative(side), machines);
            }
        }
        return machines;
    }

    private void addMachine(final BlockPos pos, final Set<BlockPos> machines) {
        final BlockEntity entity = Loaded.blockEntity(level, pos);
        if (entity instanceof ServerRackPartBlockEntity part && part.controllerPos() != null) {
            addMachine(part.controllerPos(), machines);
        } else if (entity instanceof ServerRackBlockEntity rack && !network.equals(rack.networkUuid())) {
            machines.add(pos.immutable());
        } else if (entity instanceof AbstractComputerBlockEntity computer && !(entity instanceof MainframeBlockEntity)
                && !network.equals(computer.networkUuid())) {
            machines.add(pos.immutable());
        }
    }
}
