/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.client.os.StorageInsightsApp;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.CraftingInterfaceMenu;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.BusEditPayload;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftingInterfaceEditPayload;
import dev.jstech.computers.operation.payload.CraftingInterfaceStatePayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkManagerPayload;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.computers.operation.payload.RenameServerRouterPayload;
import dev.jstech.computers.operation.payload.RequestNetworkManagerPayload;
import dev.jstech.computers.operation.payload.RequestStorageInsightsPayload;
import dev.jstech.computers.operation.payload.SetBusNamePayload;
import dev.jstech.computers.operation.payload.StorageInsightsPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.network.SubframeNode;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

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
                ClientPayloadHandlers.onMainThread(NetworkPayloads::handleNetworkManager));
        ComputerAccess.accept(registrar, RequestStorageInsightsPayload.TYPE, RequestStorageInsightsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestStorageInsightsPayload::host),
                NetworkPayloads::handleRequestStorageInsights);
        registrar.playToClient(StorageInsightsPayload.TYPE, StorageInsightsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkPayloads::handleStorageInsights));
        registrar.playToClient(NetworkServersPayload.TYPE, NetworkServersPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkPayloads::handleNetworkServers));
        ComputerAccess.onMenu(registrar, RenameServerRouterPayload.TYPE, RenameServerRouterPayload.STREAM_CODEC,
                ServerRouterMenu.class, ServerRouterMenu::routerPos, RenameServerRouterPayload::routerPos,
                NetworkPayloads::handleRenameServerRouter);
        ComputerAccess.onMenu(registrar, SetBusNamePayload.TYPE, SetBusNamePayload.STREAM_CODEC,
                AbstractBusMenu.class, AbstractBusMenu::cablePos, SetBusNamePayload::cablePos,
                NetworkPayloads::handleSetBusName);
        ComputerAccess.onMenu(registrar, BusEditPayload.TYPE, BusEditPayload.STREAM_CODEC, AbstractBusMenu.class,
                (payload, menu, player, level) -> menu.edit(player, payload));
        registrar.playToClient(BusStatePayload.TYPE, BusStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkPayloads::handleBusState));
        ComputerAccess.onMenu(registrar, CraftingInterfaceEditPayload.TYPE, CraftingInterfaceEditPayload.STREAM_CODEC,
                CraftingInterfaceMenu.class, (payload, menu, player, level) -> menu.edit(player, payload));
        registrar.playToClient(CraftingInterfaceStatePayload.TYPE, CraftingInterfaceStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkPayloads::handleInterfaceState));
    }

    /* The state of the bus whose window the player has open, if it is still that window. */
    private static void handleBusState(final BusStatePayload payload, final Player player) {
        if (player.containerMenu instanceof AbstractBusMenu menu && menu.containerId == payload.containerId()) {
            menu.accept(payload);
        }
    }

    /* The state of the Crafting Interface whose window the player has open, if it is still that window. */
    private static void handleInterfaceState(final CraftingInterfaceStatePayload payload, final Player player) {
        if (player.containerMenu instanceof CraftingInterfaceMenu menu && menu.containerId == payload.containerId()) {
            menu.accept(payload.view());
        }
    }

    private static void handleRenameServerRouter(final RenameServerRouterPayload payload, final ServerRouterMenu menu,
                                                 final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(menu.routerPos()) instanceof ServerRouterBlockEntity router) {
            router.setCustomName(payload.name());
        }
    }

    /*
     * The gate already proved the sender has an AbstractBusMenu open on this cable position; the face still has
     * to be checked here since one cable can carry a bus on more than one face, and only the menu's own face (not
     * whatever the payload claims) may be trusted to pick which mounted part to rename.
     */
    private static void handleSetBusName(final SetBusNamePayload payload, final AbstractBusMenu menu,
                                         final ServerPlayer player, final ServerLevel level) {
        if (menu.face().get3DDataValue() == payload.face()
                && level.getBlockEntity(menu.cablePos()) instanceof CableBlockEntity cable
                && cable.getPart(menu.face()) instanceof AbstractBusPart bus) {
            bus.setName(payload.name());
            menu.setBusNameLocal(bus.name());
        }
    }

    private static void handleNetworkServers(final NetworkServersPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setNetworkServers(payload.servers());
        } else {
            // The Network Interactor desktop app (no container menu of its own) consumes the same list.
            NetworkInteractorApp.acceptServers(
                    payload.servers());
        }
    }

    public static NetworkServersPayload collectComputers(final ServerLevel level, final NetworkUuid net) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<NetworkServersPayload.ServerEntry> rows = new ArrayList<>();
        final MainframeBlockEntity mf = resolveMainframe(level, net);
        if (mf != null && mf.nodeUuid() != null && mf.localStorageCapacity() > 0L) {
            rows.add(new NetworkServersPayload.ServerEntry(
                    mf.nodeUuid().asString(), "Mainframe", mf.localStore().free()));
        }
        addServerRows(level, net, rows);
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(net)) {
            if (rows.size() >= NetworkServersPayload.MAX) {
                break;
            }
            if (Loaded.blockEntity(level, BlockPos.of(pc.pos()))
                    instanceof PersonalComputerBlockEntity pcBe && pcBe.localStorageCapacity() > 0L) {
                rows.add(new NetworkServersPayload.ServerEntry(
                        pc.nodeUuid().asString(), pcLabel(pcBe, pc.nodeUuid()), pcBe.localStore().free()));
            }
        }
        return new NetworkServersPayload(rows);
    }

    private static NetworkServersPayload collectServers(final ServerLevel level, final NetworkUuid net) {
        final List<NetworkServersPayload.ServerEntry> rows = new ArrayList<>();
        addServerRows(level, net, rows);
        return new NetworkServersPayload(rows);
    }

    /* One row per server of the network, with its free room, until the list is full. */
    private static void addServerRows(final ServerLevel level, final NetworkUuid net,
                                      final List<NetworkServersPayload.ServerEntry> rows) {
        final NetworkSystem system = NetworkSystem.get(level);
        for (final ServerNode server : system.serversOf(net)) {
            if (rows.size() >= NetworkServersPayload.MAX) {
                break;
            }
            final NodeUuid node = server.nodeUuid();
            final ServerStore store = storeOf(level, system, node);
            rows.add(new NetworkServersPayload.ServerEntry(node.asString(), serverLabel(level, node),
                    store == null ? 0L : store.free()));
        }
    }

    /* The storage of the server {@code node}, or null when its cabinet is unknown or not loaded. */
    @Nullable
    private static ServerStore storeOf(final ServerLevel level, final NetworkSystem system, final NodeUuid node) {
        return system.locationOf(node)
                .map(loc -> Loaded.blockEntity(level, BlockPos.of(loc.rackPos()))
                        instanceof ServerRackBlockEntity rack ? rack.getServerStorage(loc.slot()) : null)
                .orElse(null);
    }

    public static void dispatchNetworkServers(final ServerPlayer player, final NetworkUuid net,
                                              final ServerLevel level) {
        PacketDistributor.sendToPlayer(player, collectServers(level, net));
    }

    private static void handleRequestNetworkManager(final RequestNetworkManagerPayload payload,
                                                    final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mf) {
            PacketDistributor.sendToPlayer(player, collectNetworkManager(level, mf));
        }
    }

    /** What the Network Manager of {@code mf} shows: its network's nodes, their links, the totals and the hour. */
    public static NetworkManagerPayload collectNetworkManager(final ServerLevel level, final MainframeBlockEntity mf) {
        final NetworkUuid net = mf.networkUuid();
        final String netId = net != null ? ShortId.of(net.asString()) : "";
        final List<NetworkNodeInfo> nodes = collectNodes(level, mf);
        return new NetworkManagerPayload(mf.getBlockPos(), netId, nodes, collectHardware(level, mf, nodes),
                collectStatistics(level, mf));
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

    private static void handleNetworkManager(final NetworkManagerPayload payload, final Player player) {
        NetworkManagerApp.accept(payload);
    }

    private static List<NetworkNodeInfo> collectNodes(final ServerLevel level, final MainframeBlockEntity mf) {
        final UnitFormatter fmt = UnitFormatter.forCurrentLocale();
        final List<NetworkNodeInfo> nodes = new ArrayList<>();
        final NetworkUuid net = mf.networkUuid();
        final NodeLinks links = net == null ? null : new NodeLinks(level, net);

        nodes.add(computerNodeInfo(NetworkNodeInfo.KIND_MAINFRAME, mf, mf.nodeUuid().asString(),
                fmt.compact(mf.capacity(), Unit.IT_PER_TICK), linkOf(level, links, mf.getBlockPos())));

        if (net != null) {
            final NetworkSystem system = NetworkSystem.get(level);
            for (final ServerNode server : system.serversOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(serverNodeInfo(level, system, server, fmt, links));
            }
            for (final SubframeNode subframe : system.subframesOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(new NetworkNodeInfo(NetworkNodeInfo.KIND_SUBFRAME,
                        ShortId.of(subframe.nodeUuid().asString()), "",
                        fmt.compact(subframe.contributedCapacity(), Unit.IT_PER_TICK), true,
                        0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, "", NodeLink.NONE));
            }
            for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                /*
                 * A Cluster Management Computer takes a PC's place on the network (same layout, same role
                 * in the topology), but the overview names it for what it is.
                 */
                final int kind = Loaded.blockEntity(level, BlockPos.of(pc.pos()))
                        instanceof ClusterManagementComputerBlockEntity
                        ? NetworkNodeInfo.KIND_CLUSTER_MANAGEMENT : NetworkNodeInfo.KIND_PC;
                nodes.add(resolveComputerNode(level, kind, pc.nodeUuid().asString(),
                        pc.pos(), fmt.compact(pc.capacity(), Unit.IT_PER_TICK), links));
            }
            for (final NetworkSystem.CraftingComputerNode cc : system.craftingComputersOf(net)) {
                if (nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                    break;
                }
                nodes.add(resolveComputerNode(level, NetworkNodeInfo.KIND_CRAFTING, cc.nodeUuid().asString(),
                        cc.pos(), fmt.compact(cc.capacity(), Unit.IT_PER_TICK), links));
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
                final String scName = Loaded.blockEntity(level, BlockPos.of(sc.pos()))
                        instanceof HbwInterfaceBlockEntity hub
                        ? hub.customName() : "";
                nodes.add(new NetworkNodeInfo(NetworkNodeInfo.KIND_SUPERCOMPUTER,
                        ShortId.of(sc.nodeUuid().asString()), scName, sc.parallelCrafts() + " crafts",
                        sc.parallelCrafts() > 0, 0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, "", NodeLink.NONE));
            }
            lostNodes(level, links, nodes);
        }
        return nodes;
    }

    /*
     * The machines joined to the network that lost their link, after the ones on it: a computer as itself, a cabinet as
     * each server seated in it. They are on no list of the network's, so they come from the cables around them.
     */
    private static void lostNodes(final ServerLevel level, final NodeLinks links, final List<NetworkNodeInfo> nodes) {
        for (final Map.Entry<BlockPos, NodeLink> lost : links.lost().entrySet()) {
            final BlockEntity entity = Loaded.blockEntity(level, lost.getKey());
            if (entity instanceof ServerRackBlockEntity rack) {
                for (int slot = 0; slot < rack.getServers().getSlots(); slot++) {
                    final ItemStack stack = rack.getServers().getStackInSlot(slot);
                    if (!(stack.getItem() instanceof ServerItem) || nodes.size() >= NetworkManagerPayload.MAX_NODES) {
                        continue;
                    }
                    final UUID node = ServerItem.nodeUuid(stack);
                    final String custom = ServerItem.customName(stack);
                    final String name = !custom.isEmpty() || node == null ? custom
                            : serverLabel(level, new NodeUuid(node));
                    nodes.add(new NetworkNodeInfo(NetworkNodeInfo.KIND_SERVER,
                            node == null ? "" : ShortId.of(node.toString()), name, "", false, 0, 0, 0L, 0L,
                            NetworkNodeInfo.SHARE_UNKNOWN, "", lost.getValue()));
                }
            } else if (entity instanceof AbstractComputerBlockEntity computer
                    && nodes.size() < NetworkManagerPayload.MAX_NODES) {
                final int kind = computer instanceof ClusterManagementComputerBlockEntity
                        ? NetworkNodeInfo.KIND_CLUSTER_MANAGEMENT
                        : computer instanceof CraftingComputerBlockEntity ? NetworkNodeInfo.KIND_CRAFTING
                        : NetworkNodeInfo.KIND_PC;
                nodes.add(computerNodeInfo(kind, computer, computer.nodeUuid().asString(), "", lost.getValue()));
            }
        }
    }

    /* The link of the machine at {@code pos}: the cables it is plugged into, and whether it holds a card. */
    private static NodeLink linkOf(final ServerLevel level, @Nullable final NodeLinks links, final BlockPos pos) {
        if (links == null) {
            return NodeLink.NONE;
        }
        final Set<Long> cables = Loaded.blockEntity(level, pos) instanceof AbstractComputerBlockEntity computer
                ? computer.networkCables(level)
                : NetworkSystem.get(level).connectivity().bridgedBy(pos.asLong());
        return links.of(cables, links.optical(pos));
    }

    /** Builds an enriched node row from a resolved computer block entity (name, specs, OS, storage share). */
    private static NetworkNodeInfo computerNodeInfo(final int kind,
            final IOsHost c,
            final String uuid, final String detail, final NodeLink link) {
        final int share = DiskItem.publicPermille(c.systemDisk());
        /*
         * Total capacity is only summed for the Mainframe; a generic computer reports its free space, which is
         * the "available storage" the tooltip shows, with total left as 0 (unknown).
         */
        return new NetworkNodeInfo(kind, ShortId.of(uuid), c.customName(), detail, c.isRunning(),
                c.maxCpuMhz(), c.totalVramMb(), c.systemDiskFreeMb(), 0L,
                share, osLabelOf(c.installedOsId()), link);
    }

    /** Resolves the computer at {@code posLong}; falls back to a bare row if it is not loaded as a computer. */
    private static NetworkNodeInfo resolveComputerNode(final ServerLevel level, final int kind, final String uuid,
                                                       final long posLong, final String detail,
                                                       @Nullable final NodeLinks links) {
        final BlockPos pos = BlockPos.of(posLong);
        if (Loaded.blockEntity(level, pos) instanceof IOsHost c) {
            return computerNodeInfo(kind, c, uuid, detail, linkOf(level, links, pos));
        }
        return new NetworkNodeInfo(kind, ShortId.of(uuid), "", detail, false,
                0, 0, 0L, 0L, NetworkNodeInfo.SHARE_UNKNOWN, "", NodeLink.NONE);
    }

    /** A server lives as a disk in a rack, so it carries a name and storage but no processor/OS of its own. */
    private static NetworkNodeInfo serverNodeInfo(final ServerLevel level, final NetworkSystem system,
                                                  final ServerNode server, final UnitFormatter fmt,
                                                  @Nullable final NodeLinks links) {
        final long total = server.storageItems();
        final Optional<NetworkSystem.ServerLocation> location = system.locationOf(server.nodeUuid());
        final ServerStore store = storeOf(level, system, server.nodeUuid());
        final long free = store == null ? 0L : store.free();
        final NodeLink link = location.map(loc -> linkOf(level, links, BlockPos.of(loc.rackPos())))
                .orElse(NodeLink.NONE);
        return new NetworkNodeInfo(NetworkNodeInfo.KIND_SERVER, ShortId.of(server.nodeUuid().asString()),
                serverLabel(level, server.nodeUuid()), String.format(Locale.ROOT, "%,d items", total), true,
                0, 0, free, total, NetworkNodeInfo.SHARE_UNKNOWN, "", link);
    }

    /**
     * Network-wide hardware totals for the Network Manager's Hardware tab, and its links: the optical ones up and
     * down, the Mainframe's own, and the slowest a node on the network is plugged into.
     */
    private static NetworkManagerPayload.Hardware collectHardware(final ServerLevel level,
                                                                  final MainframeBlockEntity mf,
                                                                  final List<NetworkNodeInfo> nodes) {
        long storage = mf.localStorageCapacity();
        final NetworkUuid net = mf.networkUuid();
        if (net != null) {
            final NetworkSystem system = NetworkSystem.get(level);
            for (final ServerNode server : system.serversOf(net)) {
                storage += server.storageItems();
            }
        }
        int opticalUp = 0;
        int opticalDown = 0;
        NetworkNodeInfo slowest = null;
        for (final NetworkNodeInfo node : nodes) {
            final NodeLink link = node.link();
            if (link.optical()) {
                if (link.up()) {
                    opticalUp++;
                } else {
                    opticalDown++;
                }
            }
            final DataLink cable = link.dataLink();
            if (link.up() && cable != null && (slowest == null
                    || cable.throughput() < slowest.link().dataLink().throughput())) {
                slowest = node;
            }
        }
        return new NetworkManagerPayload.Hardware(mf.orchestrationCapacity(), mf.pooledQueues(),
                mf.computerRamBuffer(), storage, opticalUp, opticalDown, nodes.getFirst().link().link(),
                slowest == null ? "" : slowest.link().link(), slowest == null ? "" : slowest.displayName());
    }

    private static void handleRequestStorageInsights(final RequestStorageInsightsPayload payload,
                                                     final ServerPlayer player, final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host != null && host.networkUuid() != null) {
            PacketDistributor.sendToPlayer(player, collectStorageInsights(level, host.networkUuid()));
        }
    }

    private static void handleStorageInsights(final StorageInsightsPayload payload, final Player player) {
        StorageInsightsApp.accept(payload);
    }

    /** Builds the Storage Insights dashboard: totals, the biggest and smallest types, and per-server usage. */
    private static StorageInsightsPayload collectStorageInsights(final ServerLevel level, final NetworkUuid net) {
        final Map<StorageKey, Long> totals =
                NetworkStorage.of(level, net).query();
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
            final ServerStore store = storeOf(level, system, server.nodeUuid());
            final long used = store == null ? 0L : store.used();
            servers.add(new NetworkItemEntry.StorageShare(Text.literal(serverLabel(level, server.nodeUuid())), used));
        }
        return new StorageInsightsPayload(totalItems, totals.size(), serverCount, top, low, servers);
    }
}
