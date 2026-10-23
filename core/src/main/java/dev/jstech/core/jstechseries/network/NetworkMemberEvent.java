/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * A machine of any mod joined a data network or left one, posted on the game's bus as the change happens: a member
 * placed beside a cable of a network, a cable laid or broken, a network formed or parted around it, the member broken
 * or unloaded. A member that moves from one network to another leaves the first and joins the second.
 */
public abstract sealed class NetworkMemberEvent extends Event {

    private final ServerLevel level;
    private final BlockPos pos;
    private final NodeUuid node;
    private final NetworkUuid network;

    protected NetworkMemberEvent(final ServerLevel level, final BlockPos pos, final NodeUuid node,
                                 final NetworkUuid network) {
        this.level = level;
        this.pos = pos;
        this.node = node;
        this.network = network;
    }

    public ServerLevel level() {
        return this.level;
    }

    /** Where the member stands. */
    public BlockPos pos() {
        return this.pos;
    }

    /** The member's identity on the network. */
    public NodeUuid node() {
        return this.node;
    }

    /** The network it joined or left. */
    public NetworkUuid network() {
        return this.network;
    }

    /** A member joined a network. */
    public static final class Joined extends NetworkMemberEvent {

        public Joined(final ServerLevel level, final BlockPos pos, final NodeUuid node, final NetworkUuid network) {
            super(level, pos, node, network);
        }
    }

    /** A member left a network. */
    public static final class Left extends NetworkMemberEvent {

        public Left(final ServerLevel level, final BlockPos pos, final NodeUuid node, final NetworkUuid network) {
            super(level, pos, node, network);
        }
    }
}
