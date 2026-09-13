/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.blockentity.DataCableBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkManagerPayload;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.RenameServerRouterPayload;
import dev.jstech.computers.operation.payload.RequestNetworkManagerPayload;
import dev.jstech.computers.operation.payload.RequestStorageInsightsPayload;
import dev.jstech.computers.operation.payload.SetBusNamePayload;
import dev.jstech.computers.operation.payload.StorageInsightsPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.network.SubframeNode;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.jstech.computers.operation.payload.machine.MachineLabels.osLabelOf;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.pcLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The network's payloads: the Network Manager, the lists of servers and computers, Storage Insights, and renaming
 * routers and buses.
 */
public final class NetworkPayloads {

    private NetworkPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestNetworkManagerPayload.TYPE, RequestNetworkManagerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNetworkManagerPayload::hostPos),
                NetworkPayloads::handleRequestNetworkManager);
        registrar.playToClient(NetworkManagerPayload.TYPE, NetworkManagerPayload.STREAM_CODEC,
                NetworkPayloads::handleNetworkManager);
        ComputerAccess.accept(registrar, RequestStorageInsightsPayload.TYPE, RequestStorageInsightsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestStorageInsightsPayload::host),
                NetworkPayloads::handleRequestStorageInsights);
        registrar.playToClient(StorageInsightsPayload.TYPE, StorageInsightsPayload.STREAM_CODEC,
                NetworkPayloads::handleStorageInsights);
        registrar.playToClient(NetworkServersPayload.TYPE, NetworkServersPayload.STREAM_CODEC,
                NetworkPayloads::handleNetworkServers);
        ComputerAccess.accept(registrar, RenameServerRouterPayload.TYPE, RenameServerRouterPayload.STREAM_CODEC,
                ComputerAccess.menu(ServerRouterMenu.class, ServerRouterMenu::routerPos, RenameServerRouterPayload::routerPos),
                NetworkPayloads::handleRenameServerRouter);
        ComputerAccess.accept(registrar, SetBusNamePayload.TYPE, SetBusNamePayload.STREAM_CODEC,
                ComputerAccess.menu(AbstractBusMenu.class, AbstractBusMenu::cablePos, SetBusNamePayload::cablePos),
                NetworkPayloads::handleSetBusName);
    }

    private static void handleRenameServerRouter(final RenameServerRouterPayload payload,
                                                 final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getBlockEntity(payload.routerPos())
                            instanceof ServerRouterBlockEntity router) {
                router.setCustomName(payload.name());
            }
        });
    }

    private static void handleSetBusName(final SetBusNamePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof AbstractBusMenu menu
                    && menu.cablePos().equals(payload.cablePos())
                    && menu.face().get3DDataValue() == payload.face()
                    && player.level().getBlockEntity(payload.cablePos()) instanceof DataCableBlockEntity cable
                    && cable.getPart(Direction.from3DDataValue(payload.face())) instanceof AbstractBusPart bus) {
                bus.setName(payload.name());
                menu.setBusNameLocal(bus.name());
            }
        });
    }

    private static void handleNetworkServers(final NetworkServersPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ComputerTerminalMenu menu) {
                menu.setNetworkServers(payload.servers());
            } else {
                // The Network Interactor desktop app (no container menu of its own) consumes the same list.
                dev.jstech.computers.client.os.NetworkInteractorApp.acceptServers(
                        payload.servers());
            }
        });
    }

    public static NetworkServersPayload collectComputers(final ServerLevel level, final NetworkUuid net) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<NetworkServersPayload.ServerEntry> rows = new ArrayList<>();
        final MainframeBlockEntity mf = resolveMainframe(level, net);
        if (mf != null && mf.nodeUuid() != null && mf.localStorageCapacity() > 0L) {
            rows.add(new NetworkServersPayload.ServerEntry(
                    mf.nodeUuid().asString(), "Mainframe", mf.localStore().free()));
        }
        for (final ServerNode server : system.serversOf(net)) {
            if (rows.size() >= NetworkServersPayload.MAX) {
                break;
            }
            final NodeUuid node = server.nodeUuid();
            final long free = system.locationOf(node)
                    .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                            instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                            ? rack.getServerStorage(loc.slot()).free() : 0L)
                    .orElse(0L);
            rows.add(new NetworkServersPayload.ServerEntry(node.asString(), serverLabel(level, node), free));
        }
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(net)) {
            if (rows.size() >= NetworkServersPayload.MAX) {
                break;
            }
            if (level.getBlockEntity(BlockPos.of(pc.pos()))
                    instanceof PersonalComputerBlockEntity pcBe && pcBe.localStorageCapacity() > 0L) {
                rows.add(new NetworkServersPayload.ServerEntry(
                        pc.nodeUuid().asString(), pcLabel(pcBe, pc.nodeUuid()), pcBe.localStore().free()));
            }
        }
        return new NetworkServersPayload(rows);
    }

    private static NetworkServersPayload collectServers(final ServerLevel level, final NetworkUuid net) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<NetworkServersPayload.ServerEntry> rows = new ArrayList<>();
        for (final ServerNode server : system.serversOf(net)) {
            if (rows.size() >= NetworkServersPayload.MAX) {
                break;
            }
            final NodeUuid node = server.nodeUuid();
            final long free = system.locationOf(node)
                    .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                            instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                            ? rack.getServerStorage(loc.slot()).free() : 0L)
                    .orElse(0L);
            rows.add(new NetworkServersPayload.ServerEntry(node.asString(), serverLabel(level, node), free));
        }
        return new NetworkServersPayload(rows);
    }

    public static void dispatchNetworkServers(final ServerPlayer player, final NetworkUuid net,
                                              final ServerLevel level) {
        PacketDistributor.sendToPlayer(player, collectServers(level, net));
    }

    private static void handleRequestNetworkManager(final RequestNetworkManagerPayload payload,
                                                    final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mf) {
                final NetworkUuid net = mf.networkUuid();
                final String netId = net != null ? ShortId.of(net.asString()) : "";
                PacketDistributor.sendToPlayer(player,
                        new NetworkManagerPayload(payload.hostPos(), netId, collectNodes(level, mf),
                                collectHardware(level, mf), collectStatistics(level, mf)));
            }
        });
    }

    /** The last hour's Operation statistics of a Mainframe, by type, for the Stats tab. */
    static NetworkManagerPayload.Statistics collectStatistics(final ServerLevel level, final MainframeBlockEntity mf) {
        final long now = level.getGameTime();
        final List<NetworkManagerPayload.TypeStat> types = new ArrayList<>();
        for (final var summary : mf.statistics().summaries(now)) {
            if (types.size() >= NetworkManagerPayload.MAX_STAT_TYPES) {
                break;
            }
            types.add(new NetworkManagerPayload.TypeStat((byte) summary.type(), summary.count(),
                    summary.shortfallPercent(), summary.averageWait(), summary.averageRun(), summary.moved()));
        }
        return new NetworkManagerPayload.Statistics(types, mf.statistics().peakConcurrentLastDay(now),
                mf.statistics().movedLastHour(now));
    }

    private static void handleNetworkManager(final NetworkManagerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() ->
                dev.jstech.computers.client.os.NetworkManagerApp.accept(payload));
    }

    private static List<NetworkNodeInfo> collectNodes(final ServerLevel level, final MainframeBlockEntity mf) {
        final UnitFormatter fmt = UnitFormatter.forCurrentLocale();
        final List<NetworkNodeInfo> nodes = new ArrayList<>();
        final NetworkUuid net = mf.networkUuid();

        nodes.add(computerNodeInfo(NetworkNodeInfo.KIND_MAINFRAME, mf, mf.nodeUuid().asString(),
                fmt.compact(mf.capacity(), Unit.IT_PER_TICK)));

        if (net != null) {
            final NetworkSystem system = NetworkSystem.get(level);
            for (final ServerNode server : system.serversOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(serverNodeInfo(level, system, server, fmt));
            }
            for (final SubframeNode subframe : system.subframesOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(new NetworkNodeInfo(NetworkNodeInfo.KIND_SUBFRAME,
                        ShortId.of(subframe.nodeUuid().asString()), "",
                        fmt.compact(subframe.contributedCapacity(), Unit.IT_PER_TICK), true,
                        0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, ""));
            }
            for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                /*
                 * A Cluster Management Computer takes a PC's place on the network (same layout, same role
                 * in the topology), but the overview names it for what it is.
                 */
                final int kind = level.getBlockEntity(BlockPos.of(pc.pos()))
                        instanceof dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity
                        ? NetworkNodeInfo.KIND_CLUSTER_MANAGEMENT : NetworkNodeInfo.KIND_PC;
                nodes.add(resolveComputerNode(level, kind, pc.nodeUuid().asString(),
                        pc.pos(), fmt.compact(pc.capacity(), Unit.IT_PER_TICK)));
            }
            for (final NetworkSystem.CraftingComputerNode cc : system.craftingComputersOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(resolveComputerNode(level, NetworkNodeInfo.KIND_CRAFTING, cc.nodeUuid().asString(),
                        cc.pos(), fmt.compact(cc.capacity(), Unit.IT_PER_TICK)));
            }
            for (final NetworkSystem.SupercomputerNode sc : system.supercomputersOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                /*
                 * A supercomputer is a whole cluster bridged by an HBW interface (its pos is that interface,
                 * not a single computer). It is on the network whenever its uplink is; it is online (able
                 * to take crafts) only with at least one rated node.
                 */
                final String scName = level.getBlockEntity(BlockPos.of(sc.pos()))
                        instanceof dev.jstech.computers.blockentity.HbwInterfaceBlockEntity hub
                        ? hub.customName() : "";
                nodes.add(new NetworkNodeInfo(NetworkNodeInfo.KIND_SUPERCOMPUTER,
                        ShortId.of(sc.nodeUuid().asString()), scName, sc.parallelCrafts() + " crafts",
                        sc.parallelCrafts() > 0, 0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, ""));
            }
        }
        return nodes;
    }

    /** Builds an enriched node row from a resolved computer block entity (name, specs, OS, storage share). */
    private static NetworkNodeInfo computerNodeInfo(final int kind,
            final dev.jstech.computers.os.IOsHost c,
            final String uuid, final String detail) {
        final int share = dev.jstech.computers.item.DiskItem.publicPermille(c.systemDisk());
        /*
         * Total capacity is only summed for the Mainframe; a generic computer reports its free space, which is
         * the "available storage" the tooltip shows, with total left as 0 (unknown).
         */
        return new NetworkNodeInfo(kind, ShortId.of(uuid), c.customName(), detail, c.isRunning(),
                c.maxCpuMhz(), c.totalVramMb(), c.systemDiskFreeMb(), 0L,
                share, osLabelOf(c.installedOsId()));
    }

    /** Resolves the computer at {@code posLong}; falls back to a bare row if it is not loaded as a computer. */
    private static NetworkNodeInfo resolveComputerNode(final ServerLevel level, final int kind, final String uuid,
                                                       final long posLong, final String detail) {
        if (level.getBlockEntity(BlockPos.of(posLong))
                instanceof dev.jstech.computers.os.IOsHost c) {
            return computerNodeInfo(kind, c, uuid, detail);
        }
        return new NetworkNodeInfo(kind, ShortId.of(uuid), "", detail, false,
                0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, "");
    }

    /** A server lives as a disk in a rack, so it carries a name and storage but no processor/OS of its own. */
    private static NetworkNodeInfo serverNodeInfo(final ServerLevel level, final NetworkSystem system,
                                                  final ServerNode server, final UnitFormatter fmt) {
        final long total = server.storageItems();
        final long free = system.locationOf(server.nodeUuid())
                .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                        instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                        ? rack.getServerStorage(loc.slot()).free() : 0L)
                .orElse(0L);
        return new NetworkNodeInfo(NetworkNodeInfo.KIND_SERVER, ShortId.of(server.nodeUuid().asString()),
                serverLabel(level, server.nodeUuid()), String.format(java.util.Locale.ROOT, "%,d items", total), true,
                0, 0, free, total, NetworkNodeInfo.SHARE_UNKNOWN, "");
    }

    /** Network-wide hardware totals for the Network Manager's Hardware tab. */
    private static NetworkManagerPayload.Hardware collectHardware(final ServerLevel level,
                                                                  final MainframeBlockEntity mf) {
        long storage = mf.localStorageCapacity();
        final NetworkUuid net = mf.networkUuid();
        if (net != null) {
            final NetworkSystem system = NetworkSystem.get(level);
            for (final ServerNode server : system.serversOf(net)) {
                storage += server.storageItems();
            }
        }
        return new NetworkManagerPayload.Hardware(
                mf.orchestrationCapacity(), mf.pooledQueues(), mf.computerRamBuffer(), storage);
    }

    private static void handleRequestStorageInsights(final RequestStorageInsightsPayload payload,
                                                     final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level) {
                final var host = niHost(player, level, payload.host(), payload.monitorPos());
                if (host != null && host.networkUuid() != null) {
                    PacketDistributor.sendToPlayer(player, collectStorageInsights(level, host.networkUuid()));
                }
            }
        });
    }

    private static void handleStorageInsights(final StorageInsightsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() ->
                dev.jstech.computers.client.os.StorageInsightsApp.accept(payload));
    }

    /** Builds the Storage Insights dashboard: totals, the biggest and smallest types, and per-server usage. */
    private static StorageInsightsPayload collectStorageInsights(final ServerLevel level, final NetworkUuid net) {
        final Map<StorageKey, Long> totals =
                dev.jstech.computers.operation.NetworkStorage.of(level, net).query();
        long totalItems = 0;
        for (final long v : totals.values()) {
            totalItems += v;
        }
        final List<Map.Entry<StorageKey, Long>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        final List<NetworkItemEntry> top = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < StorageInsightsPayload.MAX_TOP; i++) {
            top.add(new NetworkItemEntry(sorted.get(i).getKey(), sorted.get(i).getValue()));
        }
        final List<NetworkItemEntry> low = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0 && low.size() < StorageInsightsPayload.MAX_LOW; i--) {
            if (sorted.get(i).getValue() > 0) {
                low.add(new NetworkItemEntry(sorted.get(i).getKey(), sorted.get(i).getValue()));
            }
        }
        final NetworkSystem system = NetworkSystem.get(level);
        final List<NetworkItemEntry.StorageShare> servers = new ArrayList<>();
        int serverCount = 0;
        for (final ServerNode server : system.serversOf(net)) {
            serverCount++;
            if (servers.size() >= StorageInsightsPayload.MAX_SERVERS) {
                continue;
            }
            final long used = system.locationOf(server.nodeUuid())
                    .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                            instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                            ? rack.getServerStorage(loc.slot()).used() : 0L)
                    .orElse(0L);
            servers.add(new NetworkItemEntry.StorageShare(serverLabel(level, server.nodeUuid()), used));
        }
        return new StorageInsightsPayload(totalItems, totals.size(), serverCount, top, low, servers);
    }
}
