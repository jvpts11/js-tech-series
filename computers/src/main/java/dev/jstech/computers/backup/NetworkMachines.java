/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.ServerNode;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The computers of one network a Backup Service backs up and restores: the Mainframe, every personal computer and every
 * server in its rack, each machine by its node on the network, its name, and the machine itself.
 */
public final class NetworkMachines {

    private NetworkMachines() {
    }

    /** Every computer of {@code network} that is in a loaded part of the world, the Mainframe first. */
    public static List<Machine> of(final ServerLevel level, final NetworkUuid network) {
        final List<Machine> out = new ArrayList<>();
        final NetworkSystem system = NetworkSystem.get(level);
        system.mainframePositionOf(network).ifPresent(pos -> {
            if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity mainframe) {
                add(out, mainframe);
            }
        });
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(network)) {
            if (Loaded.blockEntity(level, BlockPos.of(pc.pos())) instanceof PersonalComputerBlockEntity computer) {
                add(out, computer);
            }
        }
        for (final ServerNode server : system.serversOf(network)) {
            system.locationOf(server.nodeUuid()).ifPresent(at -> {
                if (Loaded.blockEntity(level, BlockPos.of(at.rackPos())) instanceof ServerRackBlockEntity rack) {
                    add(out, rack.unitHost(at.slot()));
                }
            });
        }
        return out;
    }

    /** The computer of {@code network} with the node {@code node}, or null when none is loaded. */
    @Nullable
    public static Machine find(final ServerLevel level, final NetworkUuid network, final NodeUuid node) {
        for (final Machine machine : of(level, network)) {
            if (machine.node().equals(node)) {
                return machine;
            }
        }
        return null;
    }

    private static void add(final List<Machine> out, final IOsHost host) {
        final NodeUuid node = host.nodeUuid();
        if (node != null) {
            out.add(new Machine(node, NetworkEvents.machineName(host.customName(), host.machine(), node), host));
        }
    }

    /**
     * One computer of the network.
     *
     * @param node its node on the network
     * @param name what it is called, its own name or else its kind and short id
     * @param host the machine itself
     */
    public record Machine(NodeUuid node, String name, IOsHost host) {
    }
}
