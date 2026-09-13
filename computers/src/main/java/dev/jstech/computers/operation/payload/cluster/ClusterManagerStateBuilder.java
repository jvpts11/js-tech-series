/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.cluster;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.cluster.ClusterManagerPayloads.sectionStores;
import static dev.jstech.computers.operation.payload.machine.MachineLabels.osLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;

/**
 * Builds the state the Cluster Manager shows: the clusters, their racks and nodes, and what each node is doing.
 */
public final class ClusterManagerStateBuilder {

    private ClusterManagerStateBuilder() {
    }

    /** Everything the Cluster Manager shows, for one machine and one selected cluster. */
    public static ClusterManagerStatePayload buildClusterManagerState(
            final dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity cmc,
            final ServerLevel level, final int selKind, final int selIndex, final String status) {
        final var card = cmc.clusterCard();
        final var systemDisc = cmc.medium(dev.jstech.computers.os.media.MediaKind.OS_INSTALL);
        final var program = cmc.medium(dev.jstech.computers.os.media.MediaKind.PROGRAM_INSTALL);
        final var head = new ClusterManagerStatePayload.Head(card != null, card == null ? 0 : card.reach().id(),
                cmc.parallelLanes(), systemDisc == null ? "" : systemDisc.label(), program == null ? "" : program.label(),
                status.isEmpty() ? cmc.lastJobSummary() : status);
        final List<ClusterManagerStatePayload.WireCluster> clusters = new ArrayList<>();
        final var hubs = cmc.supercomputers();
        for (int i = 0; i < hubs.size() && clusters.size() < ClusterManagerStatePayload.MAX_CLUSTERS; i++) {
            final var hub = hubs.get(i);
            final int nodes = hub.clusterNodes().size();
            clusters.add(new ClusterManagerStatePayload.WireCluster(ClusterManagerStatePayload.KIND_SUPERCOMPUTER, i,
                    dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity
                            .supercomputerName(hub, i),
                    hub.clusterOnline(), nodes, hub.craftSlotsInUse(), hub.parallelCrafts(), 0,
                    nodes + " nodes · " + hub.craftSlotsInUse() + "/" + hub.parallelCrafts(),
                    cmc.reaches(dev.jstech.computers.rack.RackChassis.RackType.SUPERCOMPUTER)));
        }
        final var sections = cmc.datacenterSections();
        for (int i = 0; i < sections.size() && clusters.size() < ClusterManagerStatePayload.MAX_CLUSTERS; i++) {
            final var ref = sections.get(i);
            long used = 0L;
            long total = 0L;
            for (final ServerStore store : sectionStores(level, ref.section().servers())) {
                used += store.usedWeight();
                total += store.capacityWeight();
            }
            final int mode = level.getBlockEntity(ref.routerPos()) instanceof ServerRouterBlockEntity router
                    ? router.loadBalanceMode(ref.face()).id()
                    : dev.jstech.computers.datacenter.LoadBalanceMode.ROUND_ROBIN.id();
            clusters.add(new ClusterManagerStatePayload.WireCluster(ClusterManagerStatePayload.KIND_DATACENTER, i,
                    ref.label(), ref.section().serverCount() > 0, ref.section().serverCount(), used, total, mode,
                    ref.section().rackCount() + " racks · " + ref.section().serverCount() + " srv",
                    cmc.reaches(dev.jstech.computers.rack.RackChassis.RackType.SERVER)));
        }
        // The selected cluster in detail.
        ClusterManagerStatePayload.Detail detail = ClusterManagerStatePayload.Detail.none();
        final List<NetworkItemEntry> items = new ArrayList<>();
        final List<ClusterManagerStatePayload.WireDest> dests = new ArrayList<>();
        if (selKind == ClusterManagerStatePayload.KIND_SUPERCOMPUTER && selIndex >= 0 && selIndex < hubs.size()) {
            final var hub = hubs.get(selIndex);
            final List<ClusterManagerStatePayload.WireNode> nodes = new ArrayList<>();
            final java.util.Map<BlockPos, Integer> rackIndex = new java.util.LinkedHashMap<>();
            final var slots = hub.clusterSlots();
            int i = 0;
            for (final var node : hub.clusterNodes()) {
                if (level.getBlockEntity(node.rack()) instanceof ServerRackBlockEntity rack && nodes.size() < ClusterManagerStatePayload.MAX_NODES) {
                    final int rIdx = rackIndex.computeIfAbsent(node.rack(), r -> rackIndex.size() + 1);
                    final ItemStack server = rack.getServers().getStackInSlot(node.row());
                    final var host = rack.unitHost(node.row());
                    final var phi = dev.jstech.computers.blockentity.HbwInterfaceBlockEntity.installedPhi(server);
                    final int slotIndex = i < slots.size() ? i : -1;
                    final int code = slotIndex >= 0 ? slots.get(slotIndex).code()
                            : dev.jstech.computers.blockentity.HbwInterfaceBlockEntity.SLOT_EMPTY;
                    nodes.add(new ClusterManagerStatePayload.WireNode(node.rack().asLong(), rIdx, node.row(),
                            dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity.nodeName(rack, node.row()),
                            osLabel(host), programsLabel(host),
                            phi == null ? -1 : dev.jstech.computers.blockentity.HbwInterfaceBlockEntity.modelIndex(phi.spec()),
                            rack.bayPowerOn(node.row()), slotIndex, code, 0L, 0L,
                            nodeState(cmc, rack, node.row(), server, host, slotIndex, code)));
                }
                i++;
            }
            final List<ClusterManagerStatePayload.WireCraft> queue = new ArrayList<>();
            final NetworkUuid net = hub.networkUuid();
            final MainframeBlockEntity mainframe = net == null ? null : resolveMainframe(level, net);
            if (mainframe != null) {
                final java.util.Map<java.util.UUID, Integer> held = hub.heldSlots();
                final List<ClusterManagerStatePayload.WireCraft> waiting = new ArrayList<>();
                for (final var operation : mainframe.liveOperations()) {
                    if (!(operation instanceof dev.jstech.computers.crafting.NetworkCraftOperation craft)
                            || craft.isDone()) {
                        continue;
                    }
                    final OperationRecord record = craft.liveRecord();
                    final String label = record.key().displayName().getString() + " x" + record.requested();
                    final int held0 = held.getOrDefault(craft.operationId(), 0);
                    if (held0 > 0) {
                        queue.add(new ClusterManagerStatePayload.WireCraft(label, craft.requesterLabel(), held0, false));
                    } else if (record.status() == OperationRecord.STATUS_WAITING && craft.usesSupercomputer(hub.getBlockPos())) {
                        waiting.add(new ClusterManagerStatePayload.WireCraft(label, craft.requesterLabel(), 0, true));
                    }
                }
                queue.addAll(waiting);
            }
            final BlockPos hp = hub.getBlockPos();
            detail = new ClusterManagerStatePayload.Detail(selKind, selIndex,
                    dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity
                            .supercomputerName(hub, selIndex),
                    "HBW Interface at " + hp.getX() + ", " + hp.getY() + ", " + hp.getZ() + " · " + rackIndex.size()
                            + " rack" + (rackIndex.size() == 1 ? "" : "s") + " · " + hub.craftSlotsInUse() + "/"
                            + hub.parallelCrafts() + " crafts",
                    hub.clusterOnline(), 0, nodes,
                    queue.size() > ClusterManagerStatePayload.MAX_QUEUE ? queue.subList(0, ClusterManagerStatePayload.MAX_QUEUE) : queue);
        } else if (selKind == ClusterManagerStatePayload.KIND_DATACENTER && selIndex >= 0 && selIndex < sections.size()) {
            final var ref = sections.get(selIndex);
            final NetworkSystem system = NetworkSystem.get(level);
            final List<ClusterManagerStatePayload.WireNode> nodes = new ArrayList<>();
            final java.util.Map<BlockPos, Integer> rackIndex = new java.util.LinkedHashMap<>();
            /*
             * Every seated server in the section's cabinets, switched on or off: a bay the manager powered
             * off has left the network, and must still be listed so the manager can power it back on.
             */
            for (final long rackLong : ref.section().rackPositions()) {
                final BlockPos rackPos = BlockPos.of(rackLong);
                if (!(level.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack)) {
                    continue;
                }
                final int rIdx = rackIndex.computeIfAbsent(rackPos, r -> rackIndex.size() + 1);
                for (final int row : rack.computerSlots()) {
                    if (nodes.size() >= ClusterManagerStatePayload.MAX_NODES) {
                        break;
                    }
                    final var host = rack.unitHost(row);
                    final ServerStore store = rack.getServerStorage(row);
                    nodes.add(new ClusterManagerStatePayload.WireNode(rackPos.asLong(), rIdx, row,
                            dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity.nodeName(rack, row),
                            osLabel(host), programsLabel(host), -1, rack.bayPowerOn(row), -1, 0,
                            store == null ? 0L : store.usedWeight(), store == null ? 0L : store.capacityWeight(),
                            nodeState(cmc, rack, row, rack.getServers().getStackInSlot(row), host, -1, -1)));
                }
            }
            final int mode = level.getBlockEntity(ref.routerPos()) instanceof ServerRouterBlockEntity router
                    ? router.loadBalanceMode(ref.face()).id()
                    : dev.jstech.computers.datacenter.LoadBalanceMode.ROUND_ROBIN.id();
            final BlockPos rp = ref.routerPos();
            detail = new ClusterManagerStatePayload.Detail(selKind, selIndex, ref.label(),
                    ref.section().rackCount() + " rack" + (ref.section().rackCount() == 1 ? "" : "s") + " · "
                            + ref.section().serverCount() + " servers @ " + rp.getX() + ", " + rp.getY() + ", " + rp.getZ(),
                    ref.section().serverCount() > 0, mode, nodes, List.of());
            // The section's inventory, and where a move-out can go: the network's computers with local storage.
            final java.util.Map<StorageKey, Long> totals = dev.jstech.computers.operation.NetworkStorage
                    .ofServers(level, ref.section().servers()).query();
            totals.entrySet().stream().limit(ClusterManagerStatePayload.MAX_ITEMS)
                    .forEach(e -> items.add(new NetworkItemEntry(e.getKey(), e.getValue())));
            final NetworkUuid net = cmc.networkUuid();
            if (net != null) {
                for (final var pc : system.personalComputersOf(net)) {
                    if (level.getBlockEntity(BlockPos.of(pc.pos())) instanceof IComputerTerminalHost pcHost
                            && pcHost.localStorageCapacity() > 0L && dests.size() < ClusterManagerStatePayload.MAX_DESTS) {
                        final String name = level.getBlockEntity(BlockPos.of(pc.pos()))
                                instanceof dev.jstech.computers.os.IOsHost os && !os.customName().isEmpty()
                                ? os.customName() : "PC-" + pc.nodeUuid().asString().substring(0, 4);
                        dests.add(new ClusterManagerStatePayload.WireDest(pc.pos(), name));
                    }
                }
                system.mainframePositionOf(net).ifPresent(mfPos -> {
                    if (level.getBlockEntity(BlockPos.of(mfPos)) instanceof IComputerTerminalHost host
                            && host.localStorageCapacity() > 0L) {
                        dests.add(new ClusterManagerStatePayload.WireDest(mfPos, "Mainframe"));
                    }
                });
            }
        }
        // The job in flight, if any.
        ClusterManagerStatePayload.WireJob job = ClusterManagerStatePayload.WireJob.none(cmc.lastJobSummary());
        final var running = cmc.job();
        if (running != null) {
            final List<ClusterManagerStatePayload.WireLane> lanes = new ArrayList<>();
            for (final var lane : running.lanes()) {
                if (lanes.size() < ClusterManagerStatePayload.MAX_LANES) {
                    lanes.add(new ClusterManagerStatePayload.WireLane(lane.name(), lane.permille()));
                }
            }
            final var ref = running.cluster();
            int clusterIndex = -1;
            final int clusterKind = ref.kind() == dev.jstech.computers.rack.RackChassis.RackType.SUPERCOMPUTER
                    ? ClusterManagerStatePayload.KIND_SUPERCOMPUTER : ClusterManagerStatePayload.KIND_DATACENTER;
            if (clusterKind == ClusterManagerStatePayload.KIND_SUPERCOMPUTER) {
                for (int i = 0; i < hubs.size(); i++) {
                    if (hubs.get(i).getBlockPos().equals(ref.anchor())) {
                        clusterIndex = i;
                    }
                }
            } else {
                for (int i = 0; i < sections.size(); i++) {
                    if (sections.get(i).routerPos().equals(ref.anchor()) && sections.get(i).face() == ref.face()) {
                        clusterIndex = i;
                    }
                }
            }
            job = new ClusterManagerStatePayload.WireJob(true, running.kind().id(), running.medium().label(),
                    clusterKind, clusterIndex, running.done(), running.skipped(), running.queued(), running.total(),
                    running.elapsedTicks(), running.cancelled(), lanes, cmc.lastJobSummary());
        }
        return new ClusterManagerStatePayload(head, clusters, detail, job, items, dests);
    }

    /**
     * What one machine in a cluster is doing, in the order it matters to the player: a machine that is not
     * assembled cannot be powered, one with no power cannot be written to, and a supercomputer node without
     * a working coprocessor holds a slot without contributing to a single craft. Without this the manager
     * showed the disk and the system but never whether the machine was actually up.
     */
    private static int nodeState(
            final dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity cmc,
            final ServerRackBlockEntity rack, final int row, final ItemStack server,
            final dev.jstech.computers.os.IOsHost host, final int slotIndex, final int code) {
        if (dev.jstech.computers.item.ServerItem.build(server) == null) {
            return ClusterManagerStatePayload.STATE_INCOMPLETE;
        }
        if (!rack.bayPowerOn(row)) {
            return ClusterManagerStatePayload.STATE_BAY_OFF;
        }
        final var job = cmc.job();
        if (job != null) {
            for (final var lane : job.lanes()) {
                if (lane.node().row() == row && lane.node().rack().equals(rack.getBlockPos())) {
                    return ClusterManagerStatePayload.STATE_INSTALLING;
                }
            }
        }
        if (code >= 0) {   // a supercomputer node: its cluster slot decides whether it counts for anything
            if (slotIndex < 0) {
                return ClusterManagerStatePayload.STATE_UNSLOTTED;
            }
            if (code == dev.jstech.computers.blockentity.HbwInterfaceBlockEntity.SLOT_EMPTY) {
                return ClusterManagerStatePayload.STATE_NO_COPROCESSOR;
            }
            if (code == dev.jstech.computers.blockentity.HbwInterfaceBlockEntity.SLOT_UNDER_RATED) {
                return ClusterManagerStatePayload.STATE_UNDER_RATED;
            }
        }
        return host.installedOsId() == null ? ClusterManagerStatePayload.STATE_NO_SYSTEM
                : ClusterManagerStatePayload.STATE_ONLINE;
    }

    private static String programsLabel(final dev.jstech.computers.os.IOsHost host) {
        if (host.console() == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder();
        for (final String id : host.console().installed()) {
            final net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            final dev.jstech.computers.os.ProgramSpec spec = rl == null ? null : OsRegistry.getProgram(rl);
            if (spec == null || spec.preinstalled()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(spec.displayName());
            if (out.length() > 140) {
                out.append(", ...");
                break;
            }
        }
        return out.toString();
    }
}
