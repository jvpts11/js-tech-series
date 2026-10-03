/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finding the machine on a network that is running a given server service.
 *
 * <p>A service of this kind belongs to a server, not to the network: somebody has to mount a machine in a
 * rack, install the software on it and switch it on, and pulling that machine takes the service with it.
 * What the network provides is only the way for a player sitting at any of its computers to reach it.
 *
 * <p>The first machine found answers, unless the program lets the player pick among them. Two servers running
 * the same service is a player's choice rather than a mistake, and the alternative, refusing to answer at all
 * until they take one down, would punish them for it.
 */
public final class ServerServices {

    private ServerServices() {
    }

    /** A running service: the rack it is mounted in and the row it occupies. */
    public record Host(ServerRackBlockEntity rack, int slot) {

        /** What the machine calls itself, or nothing when nobody has named it. */
        public String name() {
            return rack.consoleOf(slot) == null ? "" : rack.consoleOf(slot).computerName();
        }

        /** What the machine goes by on the network, as the other machines list it. */
        public String hostname() {
            return rack.asUnit(slot, rack::hostname);
        }

        /** Where the machine is, told apart from every other: its rack and its row in it. */
        public String id() {
            return rack.getBlockPos().asLong() + ":" + slot;
        }

        /** The machine as a host of its own: its disks, its system, its memory. */
        public IOsHost machine() {
            return rack.unitHost(slot);
        }

        /** Writes what the service changed back onto the Server item. */
        public void changed() {
            rack.flushServices(slot);
        }
    }

    /**
     * The first machine on {@code network} running {@code program}, or null when none is.
     *
     * <p>Null is the answer a window is shown as "no service on this network", which is the truth a player
     * can act on: mount a server, install the software, switch it on.
     */
    @Nullable
    public static Host find(final ServerLevel level, @Nullable final NetworkUuid network,
                            final ResourceLocation program) {
        final List<Host> all = all(level, network, program);
        return all.isEmpty() ? null : all.getFirst();
    }

    /** Every machine on {@code network} running {@code program}, in the order the network lists its servers. */
    public static List<Host> all(final ServerLevel level, @Nullable final NetworkUuid network,
                                 final ResourceLocation program) {
        if (network == null) {
            return List.of();
        }
        final List<Host> found = new ArrayList<>();
        final NetworkSystem system = NetworkSystem.get(level);
        for (final ServerNode node : system.serversOf(network)) {
            final NetworkSystem.ServerLocation where = system.locationOf(node.nodeUuid()).orElse(null);
            if (where == null) {
                continue;
            }
            if (!(Loaded.blockEntity(level, BlockPos.of(where.rackPos())) instanceof ServerRackBlockEntity rack)) {
                continue;
            }
            if (rack.hasService(where.slot(), program) && rack.unitRunning(where.slot())) {
                found.add(new Host(rack, where.slot()));
            }
        }
        return found;
    }
}
