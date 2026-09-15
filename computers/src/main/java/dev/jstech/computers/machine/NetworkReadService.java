/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.program.cli.ICliRemote;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The data network a machine is on, as what runs on the machine reads it.
 *
 * <p>Every read comes from the index the network already keeps, so none of it submits an Operation or waits on
 * anything: whoever asks what the network holds is told in the same tick. A machine with no cable is not on a
 * network, and says so rather than pretending.
 */
public final class NetworkReadService {

    /**
     * How many rows one read may gather. It is not a cap on the answer, which is why it is set far past any real
     * network: whoever asks for every kind of thing there is is handed all of them, and pays for all of them.
     */
    private static final int EVERYTHING = 1_000_000;

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /**
     * The network as the machine's shell reads it, for what this service does not read itself yet: the rows of a
     * query, which are answered here in their own right from the next step on.
     */
    private final ICliNetwork network;
    /** The other machines of the network as the machine's shell reaches them. */
    private final ICliRemote remote;

    public NetworkReadService(final IComputerTerminalHost terminal, final ServerLevel level,
                              final ICliNetwork network, final ICliRemote remote) {
        this.terminal = terminal;
        this.level = level;
        this.network = network;
        this.remote = remote;
    }

    /** Whether the machine is on a network at all. */
    public boolean online() {
        return this.terminal.networkUuid() != null;
    }

    /** The short id of the network the machine is on, or {@code ""} when it is on none. */
    public String id() {
        final NetworkUuid net = this.terminal.networkUuid();
        return net == null ? "" : ShortId.of(net.asString());
    }

    /** The network the machine is on, by its short id, or null when it is on none. */
    @Nullable
    public String current() {
        final NetworkUuid net = this.terminal.networkUuid();
        return net == null ? null : ShortId.of(net.asString());
    }

    /** Whether the machine is itself the network's Mainframe, which is what gates the maintenance commands. */
    public boolean isMainframe() {
        return this.terminal.isMainframeHost();
    }

    /** The counts that describe the network at a glance: its servers, computers, kinds of thing and Mainframe. */
    public ICliComputer.NetSummary summary() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return new ICliComputer.NetSummary(false, 0, 0, 0, 0, false);
        }
        final NetworkSystem system = NetworkSystem.get(this.level);
        final MainframeBlockEntity mainframe = this.mainframe(net);
        final int types = mainframe == null ? 0 : mainframe.networkIndex().catalogSize();
        return new ICliComputer.NetSummary(true, system.serversOf(net).size(),
                system.personalComputersOf(net).size(), system.subframesOf(net).size(), types, mainframe != null);
    }

    /** What the whole network holds and could hold, under the network's own short id. */
    public ICliComputer.ServerUse use() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return new ICliComputer.ServerUse("", 0L, 0L);
        }
        final NetworkStorage storage = NetworkStorage.of(this.level, net);
        return new ICliComputer.ServerUse(this.id(), storage.used(), storage.capacity());
    }

    /** How much the network's servers can hold in all. */
    public long capacity() {
        return this.use().capacity();
    }

    /** How much the network's servers hold. */
    public long used() {
        return this.use().stored();
    }

    /** How much of an item the whole network holds, counting every server that has any. */
    public long total(final String item) {
        long sum = 0;
        for (final ICliComputer.Holding holding : this.find(item)) {
            sum += holding.quantity();
        }
        return sum;
    }

    /** Every kind of thing the network holds. */
    public List<String> types() {
        final List<String> names = new ArrayList<>();
        for (final ICliComputer.StoredItem item : this.network.query(null, "", EVERYTHING)) {
            names.add(item.name());
        }
        return names;
    }

    /** Which servers hold an item, and how much each holds; a server holding none of it is left out. */
    public List<ICliComputer.Holding> find(final String item) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (net == null || key == null) {
            return List.of();
        }
        final Map<NodeUuid, Long> perServer = NetworkStorage.of(this.level, net).breakdown(key);
        final List<ICliComputer.Holding> rows = new ArrayList<>();
        for (final Map.Entry<NodeUuid, Long> entry : perServer.entrySet()) {
            if (entry.getValue() > 0L) {
                rows.add(new ICliComputer.Holding(NetworkLookup.serverLabel(this.level, entry.getKey()),
                        entry.getValue()));
            }
        }
        return rows;
    }

    /** The network's servers, with what each holds and can hold. */
    public List<ICliComputer.ServerUse> servers() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return List.of();
        }
        final NetworkStorage storage = NetworkStorage.of(this.level, net);
        final List<ICliComputer.ServerUse> rows = new ArrayList<>();
        for (final ServerNode server : NetworkSystem.get(this.level).serversOf(net)) {
            rows.add(new ICliComputer.ServerUse(NetworkLookup.serverLabel(this.level, server.nodeUuid()),
                    storage.usedOf(server.nodeUuid()), storage.capacityOf(server.nodeUuid())));
        }
        return rows;
    }

    /** The other computers the machine can reach on its network. */
    public List<ICliComputer.RemoteHost> computers() {
        return this.remote.reachableHosts();
    }

    /** The computer that name picks out, by its host name or by the name its owner gave it, or null. */
    @Nullable
    public ICliComputer.RemoteHost computer(final String name) {
        for (final ICliComputer.RemoteHost host : this.remote.reachableHosts()) {
            if (host.hostname().equalsIgnoreCase(name) || host.name().equalsIgnoreCase(name)) {
                return host;
            }
        }
        return null;
    }

    /**
     * How much of an item the whole network holds, for the programs watching it: one count across the network's
     * stores, since nothing needs building for a number that is only added up. Off a network everything reads as
     * none, so a watch on a machine with no cable simply never goes off.
     */
    public long stock(final String item) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey key = ServerCliComputer.itemKey(item);
        return net == null || key == null ? 0L : NetworkStorage.of(this.level, net).count(key);
    }

    /** The Mainframe of the network, or null when it has none running. */
    @Nullable
    private MainframeBlockEntity mainframe(@Nullable final NetworkUuid net) {
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
