/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.wan.FederationPath;
import dev.jstech.computers.wan.WanFederation;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What a program says on its network (computers/docs/design/sigma.md): an entry it writes to the network's event log,
 * which every program listening to the log hears, and a value it sends on a channel, which reaches the programs of
 * the other machines hearing that channel on its network and on the networks federated with it that let it send there.
 * A value takes a tick for every hop on its way: a tick to cross its own network, another when the machine sending it
 * is on Wi-Fi, and one for every border it crosses.
 */
public final class NetworkMessagingService {

    private final IOsHost machine;
    private final BlockEntity block;
    private final ServerLevel level;

    NetworkMessagingService(final IOsHost machine, final BlockEntity block, final ServerLevel level) {
        this.machine = machine;
        this.block = block;
        this.level = level;
    }

    /** The network the machine is on, or null for none. */
    public NetworkUuid network() {
        return this.machine.networkUuid();
    }

    /** Writes {@code text} to the network's event log at {@code what}, from this machine; false off a network. */
    public boolean log(final EventLevel what, final String text) {
        final NetworkUuid network = this.machine.networkUuid();
        final NodeUuid node = this.machine.nodeUuid();
        if (network == null || node == null) {
            return false;
        }
        NetworkEvents.record(this.level, network, what, node,
                NetworkEvents.machineName(this.machine.customName(), this.machine.machine(), node),
                Text.literal(text));
        return true;
    }

    /**
     * Sends {@code value}, as it travels, on {@code channel}: to the machines of this network and of every network
     * federated with it that lets this one send there. False off a network.
     */
    public boolean send(final String channel, final String value) {
        final NetworkUuid network = this.machine.networkUuid();
        final NodeUuid node = this.machine.nodeUuid();
        if (network == null || node == null) {
            return false;
        }
        final String sender = NetworkEvents.machineName(this.machine.customName(), this.machine.machine(), node);
        final int here = this.block instanceof AbstractComputerBlockEntity computer
                && computer.wirelessLink(this.level) != null ? 2 : 1;
        NetworkListening.send(this.level, this.block.getBlockPos(), network, channel, sender, value, here);
        for (final FederationPath path : WanFederation.of(this.level).reachable(network, this.level.getGameTime())) {
            if (path.allows(WanRight.SEND)) {
                NetworkListening.send(this.level, this.block.getBlockPos(), path.target(), channel, sender, value,
                        here + path.hops());
            }
        }
        return true;
    }
}
