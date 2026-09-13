/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.terminal;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.pcLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;

/**
 * Where items moved out of the network land, and which servers a move leaves out of its sources.
 */
public final class MoveDestinations {

    private MoveDestinations() {
    }

    /**
     * A resolved SELECT destination: where the pulled items land, the provenance label, whether it is a MOVE (into another Server), and that target Server's node (so it can be excluded as a source).
     */
    public record Dest(dev.jstech.computers.storage.IDataSink handler, String label,
                       boolean move, @Nullable NodeUuid target) {
    }

    @Nullable
    public static Dest resolveDest(final IComputerTerminalHost host, final ServerLevel level,
                                   final NetworkUuid net, final int kind, final String serverKey) {
        return kind == TerminalSelectPayload.DEST_SERVER
                ? resolveComputerDest(level, net, serverKey)
                : resolveTerminalDest(host);
    }

    @Nullable
    private static Dest resolveComputerDest(final ServerLevel level, final NetworkUuid net, final String key) {
        final NodeUuid target;
        try {
            target = NodeUuid.fromString(key);
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
        final MainframeBlockEntity mf = resolveMainframe(level, net);
        if (mf != null && mf.nodeUuid() != null && mf.nodeUuid().equals(target)) {
            return new Dest(new dev.jstech.computers.storage.StoreSink(mf.localStore()),
                    "Mainframe", false, null);
        }
        // A Personal Computer on the network: a SELECT into its own local storage (leaves the network).
        for (final NetworkSystem.PersonalComputerNode pc : NetworkSystem.get(level).personalComputersOf(net)) {
            if (pc.nodeUuid().equals(target)
                    && level.getBlockEntity(BlockPos.of(pc.pos())) instanceof PersonalComputerBlockEntity pcBe) {
                return new Dest(new dev.jstech.computers.storage.StoreSink(pcBe.localStore()),
                        pcLabel(pcBe, target), false, null);
            }
        }
        return resolveServerDest(level, net, key);
    }

    @Nullable
    private static Dest resolveTerminalDest(final IComputerTerminalHost host) {
        return host.usableStorageSlots() > 0 ? new Dest(host.localStorage(), "storage", false, null) : null;
    }

    @Nullable
    private static Dest resolveServerDest(final ServerLevel level, final NetworkUuid net, final String serverKey) {
        final NodeUuid target;
        try {
            target = NodeUuid.fromString(serverKey);
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
        final NetworkSystem system = NetworkSystem.get(level);
        boolean onNetwork = false;
        for (final ServerNode server : system.serversOf(net)) {
            if (server.nodeUuid().equals(target)) {
                onNetwork = true;
                break;
            }
        }
        if (!onNetwork) {
            return null;
        }
        return system.locationOf(target)
                .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                        instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                        ? new Dest(new dev.jstech.computers.storage.StoreSink(
                                rack.getServerStorage(loc.slot())),
                                serverLabel(level, target), true, target)
                        : null)
                .orElse(null);
    }

    public static Set<NodeUuid> sourcesWithout(final ServerLevel level, final NetworkUuid net,
                                               @Nullable final Set<NodeUuid> sources, final NodeUuid target) {
        final Set<NodeUuid> result;
        if (sources != null) {
            result = new HashSet<>(sources);
        } else {
            result = new HashSet<>();
            for (final ServerNode server : NetworkSystem.get(level).serversOf(net)) {
                result.add(server.nodeUuid());
            }
        }
        result.remove(target);
        return result;
    }

    /**
     * A stop condition for a pull into a computer's own storage: once that computer is gone from the world,
     * nothing more is taken out of the network for it.
     */
    public static java.util.function.BooleanSupplier gone(final IComputerTerminalHost host) {
        return host instanceof net.minecraft.world.level.block.entity.BlockEntity be ? be::isRemoved : () -> false;
    }

    public static Set<NodeUuid> toNodes(final List<String> keys) {
        final Set<NodeUuid> nodes = new HashSet<>();
        for (final String key : keys) {
            try {
                nodes.add(NodeUuid.fromString(key));
            } catch (final IllegalArgumentException ignored) {
                // skip a malformed key rather than fail the whole request
            }
        }
        return nodes;
    }
}
