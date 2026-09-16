/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.core.network.IDataNetworkConnectable;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Where one computer stands on the data network: the node it is known by, the network it is on, and the
 * network it is registered with.
 *
 * <p>An ordinary computer is passive and reads its network off the cable at its side, which is what
 * {@link #tick} does. A Mainframe owns a network instead and never comes through there; it says where it
 * stands through {@link #attachTo} and {@link #registeredAs}.
 */
final class NetworkAttachment {

    private final AbstractComputerBlockEntity machine;
    @Nullable
    private NodeUuid node;
    @Nullable
    private NetworkUuid network;
    @Nullable
    private NetworkUuid registered;
    /** The client's copy of whether this machine is on a network; the server answers from the network itself. */
    private boolean clientAttached;
    /*
     * The cable this machine read its network off last time. Held from tick to tick so an attached machine
     * asks after that one block instead of looking round all six of its faces again; a tick that finds it
     * gone falls back to the full look. Never saved: a machine that has just loaded looks properly once.
     */
    private long cable;

    /** No cable on any face this computer would take one on. */
    static final long NO_CABLE = Long.MIN_VALUE;

    /* Every face, held once: Direction.values() hands back a fresh copy of the array on every call. */
    private static final Direction[] FACES = Direction.values();

    NetworkAttachment(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
        this.cable = NO_CABLE;
    }

    /** The node this computer is known by on a network, drawn the first time anyone asks. */
    NodeUuid node() {
        if (this.node == null) {
            this.node = NodeUuid.random();
            this.machine.setChanged();
        }
        return this.node;
    }

    @Nullable
    NetworkUuid network() {
        return this.network;
    }

    @Nullable
    NetworkUuid registered() {
        return this.registered;
    }

    /**
     * Whether this machine is attached to a data network. On the server that is simply whether it resolved
     * one; on the client the network's identity never travels, only this answer does.
     */
    boolean attached() {
        final Level level = this.machine.getLevel();
        return level != null && level.isClientSide ? this.clientAttached : this.network != null;
    }

    /** Puts this machine on that network, or on none when null. */
    void attachTo(@Nullable final NetworkUuid network) {
        this.network = network;
    }

    /** Writes down the network this machine is registered with, or none when null. */
    void registeredAs(@Nullable final NetworkUuid network) {
        this.registered = network;
    }

    /**
     * The passive path, once a tick: the machine reads its network off the cable at its side, registers with
     * what it finds and lets go of what it no longer reaches.
     */
    void tick(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        NetworkUuid resolved = null;
        if (this.machine.isRunning()) {
            final long found = cable(level);
            resolved = found == NO_CABLE ? null : system.connectivity().networkOf(found).orElse(null);
        }
        if (this.registered != null && !this.registered.equals(resolved)) {
            this.machine.unregisterNode(system, this.registered);
            this.registered = null;
        }
        final boolean wasAttached = this.network != null;
        this.network = resolved;
        if (resolved != null) {
            this.machine.registerNode(system, resolved);
            this.registered = resolved;
        }
        if (wasAttached != (resolved != null)) {
            /*
             * The desktop's notification area shows whether this machine is on a network, so a cable cut or
             * laid has to reach the client rather than wait for the next time the monitor is opened.
             */
            this.machine.setChanged();
            final BlockPos pos = this.machine.getBlockPos();
            final BlockState state = this.machine.getBlockState();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Lets go of the network this machine was registered with, if any. Safe to run twice. */
    void leave(final ServerLevel level) {
        if (this.registered != null) {
            this.machine.unregisterNode(NetworkSystem.get(level), this.registered);
            this.registered = null;
        }
    }

    void save(final CompoundTag tag) {
        if (this.node != null) {
            tag.putString("NodeUuid", this.node.asString());
        }
    }

    void load(final CompoundTag tag) {
        if (tag.contains("NodeUuid")) {
            this.node = NodeUuid.fromString(tag.getString("NodeUuid"));
        }
    }

    /**
     * Whether this machine is on a data network, for the client: the desktop's notification area reads it, so
     * it has to travel and be refreshed when a cable comes or goes. The network's identity never travels.
     */
    void saveForClient(final CompoundTag tag) {
        tag.putBoolean("Networked", this.network != null);
    }

    void loadFromClient(@Nullable final CompoundTag tag) {
        this.clientAttached = tag != null && tag.getBoolean("Networked");
    }

    /**
     * The cable this computer reads its network off, or {@link #NO_CABLE} when it touches none.
     *
     * <p>The one it found last time is asked after first, which is a single block to look at rather than six;
     * only when that one has gone, or when there was none, is every face looked at again.
     */
    private long cable(final ServerLevel level) {
        if (this.cable != NO_CABLE && cableAt(level, BlockPos.of(this.cable))) {
            return this.cable;
        }
        this.cable = adjacentCable(level);
        return this.cable;
    }

    /**
     * Looks round every face this computer would take a data cable on, which the block's
     * {@link IDataNetworkConnectable#connectsOnFace} decides, so the device's attachment and the cable's
     * rendered connection always agree. A standalone computer offers only its rear; the Mainframe (a separate
     * block entity) and the cluster nodes offer every face.
     */
    private long adjacentCable(final ServerLevel level) {
        final BlockPos pos = this.machine.getBlockPos();
        final BlockState state = this.machine.getBlockState();
        final IDataNetworkConnectable device =
                state.getBlock() instanceof IDataNetworkConnectable connectable ? connectable : null;
        for (final Direction direction : FACES) {
            if (device != null && !device.connectsOnFace(state, direction)) {
                continue;
            }
            final BlockPos neighbor = pos.relative(direction);
            if (cableAt(level, neighbor)) {
                return neighbor.asLong();
            }
        }
        return NO_CABLE;
    }

    /** Whether that block is a data cable of a kind this computer takes. */
    private boolean cableAt(final ServerLevel level, final BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DataCableBlock cable
                && this.machine.acceptsTier(cable.tier());
    }
}
