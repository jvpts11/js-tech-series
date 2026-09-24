/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.cluster;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.client.os.ClusterManagerApp;
import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.datacenter.LoadBalancer;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ClusterManagerActionPayload;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload;
import dev.jstech.computers.operation.payload.ClusterMoveOutPayload;
import dev.jstech.computers.operation.payload.ClusterRenamePayload;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.RequestClusterManagerPayload;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.cluster.ClusterManagerStateBuilder.buildClusterManagerState;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import org.jetbrains.annotations.Nullable;

/**
 * The Cluster Manager's payloads: its actions on clusters, renaming them and moving a node out.
 */
public final class ClusterManagerPayloads {

    private ClusterManagerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        // The Cluster Manager: the Cluster Management Computer's program asks, acts, and gets a state back.
        ComputerAccess.accept(registrar, RequestClusterManagerPayload.TYPE, RequestClusterManagerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestClusterManagerPayload::hostPos),
                ClusterManagerPayloads::handleRequestClusterManager);
        ComputerAccess.accept(registrar, ClusterManagerActionPayload.TYPE, ClusterManagerActionPayload.STREAM_CODEC,
                ComputerAccess.machine(ClusterManagerActionPayload::hostPos), ClusterManagerPayloads::handleClusterManagerAction);
        ComputerAccess.accept(registrar, ClusterMoveOutPayload.TYPE, ClusterMoveOutPayload.STREAM_CODEC,
                ComputerAccess.machine(ClusterMoveOutPayload::hostPos), ClusterManagerPayloads::handleClusterMoveOut);
        ComputerAccess.accept(registrar, ClusterRenamePayload.TYPE, ClusterRenamePayload.STREAM_CODEC,
                ComputerAccess.machine(ClusterRenamePayload::hostPos), ClusterManagerPayloads::handleClusterRename);
        registrar.playToClient(ClusterManagerStatePayload.TYPE, ClusterManagerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ClusterManagerPayloads::handleClusterManagerState));
    }

    static List<ServerStore> sectionStores(final ServerLevel level, final List<NodeUuid> servers) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<ServerStore> stores = new ArrayList<>();
        for (final NodeUuid node : servers) {
            system.locationOf(node).ifPresent(loc -> {
                if (level.getBlockEntity(BlockPos.of(loc.rackPos())) instanceof ServerRackBlockEntity rack) {
                    stores.add(rack.getServerStorage(loc.slot()));
                }
            });
        }
        return stores;
    }

    private static void handleRequestClusterManager(final RequestClusterManagerPayload payload,
                                                    final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof ClusterManagementComputerBlockEntity cmc)) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                buildClusterManagerState(cmc, level, payload.selKind(), payload.selIndex(), Text.EMPTY));
    }

    private static void handleClusterManagerState(final ClusterManagerStatePayload payload, final Player player) {
        ClusterManagerApp.accept(payload);
    }

    private static void handleClusterManagerAction(final ClusterManagerActionPayload payload, final ServerPlayer player,
                                                   final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof ClusterManagementComputerBlockEntity cmc)) {
            return;
        }
        final var ref = clusterRef(cmc, payload.kind(), payload.index());
        Text status = Text.EMPTY;
        if (ref == null && payload.action() != ClusterManagerActionPayload.ACTION_REFRESH
                && payload.action() != ClusterManagerActionPayload.ACTION_CANCEL_JOB) {
            status = ClusterManagerStateBuilder.SELECT_FIRST.text();
        } else {
            final var node = new ClusterManagementComputerBlockEntity
                    .NodeRef(BlockPos.of(payload.rackPos()), payload.row());
            // Running a cluster is sending the whole of it one order: power it all on, or install on it all.
            if (payload.action() == ClusterManagerActionPayload.ACTION_POWER_ALL_ON
                    || payload.action() == ClusterManagerActionPayload.ACTION_INSTALL_SYSTEM_ALL
                    || payload.action() == ClusterManagerActionPayload.ACTION_INSTALL_PROGRAM_ALL) {
                JscEvents.award(player, JscEvents.CLUSTER_RUN);
            }
            status = switch (payload.action()) {
                case ClusterManagerActionPayload.ACTION_INSTALL_SYSTEM_ALL -> cmc.startJob(ref,
                        ClusterManagementComputerBlockEntity.JobKind.SYSTEM);
                case ClusterManagerActionPayload.ACTION_INSTALL_PROGRAM_ALL -> cmc.startJob(ref,
                        ClusterManagementComputerBlockEntity.JobKind.PROGRAM);
                case ClusterManagerActionPayload.ACTION_INSTALL_SYSTEM_NODE -> cmc.startJob(ref,
                        ClusterManagementComputerBlockEntity.JobKind.SYSTEM,
                        List.of(node));
                case ClusterManagerActionPayload.ACTION_INSTALL_PROGRAM_NODE -> cmc.startJob(ref,
                        ClusterManagementComputerBlockEntity.JobKind.PROGRAM,
                        List.of(node));
                case ClusterManagerActionPayload.ACTION_POWER_ALL_ON ->
                        ClusterManagerStateBuilder.BAYS_ON.with(cmc.powerAll(ref, true));
                case ClusterManagerActionPayload.ACTION_POWER_ALL_OFF ->
                        ClusterManagerStateBuilder.BAYS_OFF.with(cmc.powerAll(ref, false));
                case ClusterManagerActionPayload.ACTION_TOGGLE_NODE -> cmc.toggleNode(node.rack(), node.row())
                        ? Text.EMPTY : ClusterManagerStateBuilder.NOT_A_NODE.text();
                case ClusterManagerActionPayload.ACTION_CANCEL_JOB ->
                        cmc.cancelJob() ? ClusterManagerStateBuilder.JOB_CANCELLED.text() : Text.EMPTY;
                case ClusterManagerActionPayload.ACTION_CYCLE_BALANCE -> {
                    if (ref.face() != null && level.getBlockEntity(ref.anchor()) instanceof ServerRouterBlockEntity router) {
                        router.cycleLoadBalanceMode(ref.face());
                    }
                    yield Text.EMPTY;
                }
                case ClusterManagerActionPayload.ACTION_DEPOSIT, ClusterManagerActionPayload.ACTION_DEPOSIT_ONE ->
                        depositIntoSection(player, cmc, ref,
                                payload.action() == ClusterManagerActionPayload.ACTION_DEPOSIT_ONE);
                default -> Text.EMPTY;
            };
        }
        PacketDistributor.sendToPlayer(player, buildClusterManagerState(cmc, level, payload.kind(), payload.index(), status));
    }

    private static void handleClusterRename(final ClusterRenamePayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof ClusterManagementComputerBlockEntity cmc)) {
            return;
        }
        final var ref = clusterRef(cmc, payload.kind(), payload.index());
        Text status = ClusterManagerStateBuilder.SELECT_FIRST.text();
        if (ref != null) {
            final String typed = payload.name().strip().replaceAll("\\p{Cntrl}", "");
            final String name = typed.length() > ClusterRenamePayload.MAX_NAME
                    ? typed.substring(0, ClusterRenamePayload.MAX_NAME) : typed;
            if (ref.face() != null && level.getBlockEntity(ref.anchor()) instanceof ServerRouterBlockEntity router) {
                router.setSectionName(ref.face(), name);
                status = name.isEmpty() ? ClusterManagerStateBuilder.SECTION_CLEARED.text()
                        : ClusterManagerStateBuilder.SECTION_RENAMED.with(name);
            } else if (ref.face() == null && cmc.supercomputerAt(ref.anchor())
                    instanceof HbwInterfaceBlockEntity hub) {
                hub.setCustomName(name);
                status = name.isEmpty() ? ClusterManagerStateBuilder.SUPERCOMPUTER_CLEARED.text()
                        : ClusterManagerStateBuilder.SUPERCOMPUTER_RENAMED.with(name);
            }
        }
        PacketDistributor.sendToPlayer(player, buildClusterManagerState(cmc, level, payload.kind(), payload.index(), status));
    }

    private static void handleClusterMoveOut(final ClusterMoveOutPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        if (payload.quantity() <= 0L
                || !(level.getBlockEntity(payload.hostPos())
                        instanceof ClusterManagementComputerBlockEntity cmc)) {
            return;
        }
        final var ref = clusterRef(cmc, ClusterManagerStatePayload.KIND_DATACENTER, payload.index());
        final NetworkUuid net = cmc.networkUuid();
        if (ref == null || net == null
                || !(level.getBlockEntity(BlockPos.of(payload.destPos())) instanceof IComputerTerminalHost dest)
                || !net.equals(dest.networkUuid())) {
            return; // the destination must be on this machine's own network
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        final Set<NodeUuid> sources = new HashSet<>();
        final var section = cmc.sectionAt(ref.anchor(), ref.face());
        if (mainframe == null || section == null) {
            return;
        }
        sources.addAll(section.section().servers());
        if (sources.isEmpty()) {
            return;
        }
        final var op = mainframe.submitNetworkMove(payload.key(), payload.quantity(), dest.localStorage(),
                cmc.originLabel(MoveLabels.CLUSTER_MANAGER), sources);
        if (op != null) {
            op.onSettle(() -> PacketDistributor.sendToPlayer(player, buildClusterManagerState(cmc, level,
                    ClusterManagerStatePayload.KIND_DATACENTER, payload.index(), Text.EMPTY)));
        }
        PacketDistributor.sendToPlayer(player, buildClusterManagerState(cmc, level,
                ClusterManagerStatePayload.KIND_DATACENTER, payload.index(), ClusterManagerStateBuilder.MOVING.text()));
    }

    /** The cluster a (kind, index) pair names in the state's own order, or null. */
    @Nullable
    private static ClusterManagementComputerBlockEntity.ClusterRef clusterRef(
            final ClusterManagementComputerBlockEntity cmc,
            final int kind, final int index) {
        if (kind == ClusterManagerStatePayload.KIND_SUPERCOMPUTER) {
            final var hubs = cmc.supercomputers();
            return index >= 0 && index < hubs.size()
                    ? new ClusterManagementComputerBlockEntity.ClusterRef(
                            RackChassis.RackType.SUPERCOMPUTER,
                            hubs.get(index).getBlockPos(), null)
                    : null;
        }
        if (kind == ClusterManagerStatePayload.KIND_DATACENTER) {
            final var sections = cmc.datacenterSections();
            return index >= 0 && index < sections.size()
                    ? new ClusterManagementComputerBlockEntity.ClusterRef(
                            RackChassis.RackType.SERVER,
                            sections.get(index).routerPos(), sections.get(index).face())
                    : null;
        }
        return null;
    }

    /** Puts the stack on the player's cursor into the section's servers, spread by its balance mode. */
    private static Text depositIntoSection(
            final ServerPlayer player,
            final ClusterManagementComputerBlockEntity cmc,
            final ClusterManagementComputerBlockEntity.ClusterRef ref,
            final boolean single) {
        final ItemStack cursor = player.containerMenu.getCarried();
        final var section = cmc.sectionAt(ref.anchor(), ref.face());
        if (cursor.isEmpty() || section == null || !(player.level() instanceof ServerLevel level)) {
            return Text.EMPTY;
        }
        final List<ServerStore> stores = sectionStores(level, section.section().servers());
        if (stores.isEmpty()) {
            return ClusterManagerStateBuilder.NO_SERVERS.text();
        }
        final ServerRouterBlockEntity router =
                level.getBlockEntity(ref.anchor()) instanceof ServerRouterBlockEntity r ? r : null;
        final LoadBalanceMode mode = router != null
                ? router.loadBalanceMode(ref.face())
                : LoadBalanceMode.ROUND_ROBIN;
        final StorageKey key = StorageKey.of(cursor);
        final long want = single ? 1L : cursor.getCount();
        /*
         * The rotation lives on the router, so a run of single-item deposits really does move down the row
         * of servers instead of piling onto the first one every time.
         */
        final int start = router != null && ref.face() != null ? router.nextBalanceStart(ref.face()) : 0;
        final long stored = LoadBalancer.insert(stores, key, want, mode, start);
        if (stored > 0L) {
            cursor.shrink((int) stored);
            player.containerMenu.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
            player.containerMenu.broadcastChanges();
        }
        return stored > 0L ? ClusterManagerStateBuilder.DEPOSITED.with(stored)
                : ClusterManagerStateBuilder.NO_ROOM.text();
    }
}
