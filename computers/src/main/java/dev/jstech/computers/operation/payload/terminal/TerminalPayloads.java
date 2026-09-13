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
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkSnapshotPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.RequestServerBreakdownPayload;
import dev.jstech.computers.operation.payload.ServerBreakdownPayload;
import dev.jstech.computers.operation.payload.TerminalDropPayload;
import dev.jstech.computers.operation.payload.TerminalInsertPayload;
import dev.jstech.computers.operation.payload.TerminalMaintenancePayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.terminal.MoveDestinations.Dest;
import dev.jstech.computers.storage.DataContainers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;
import static dev.jstech.computers.operation.payload.network.NetworkPayloads.collectComputers;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.gone;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.resolveDest;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.sourcesWithout;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.toNodes;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.openTerminal;

/**
 * The network terminal's payloads: selecting, inserting and dropping items, storage maintenance, the server
 * breakdown and the snapshot of what the network holds.
 */
public final class TerminalPayloads {

    private TerminalPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(NetworkSnapshotPayload.TYPE, NetworkSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(TerminalPayloads::handleSnapshot));
        ComputerAccess.accept(registrar, TerminalSelectPayload.TYPE, TerminalSelectPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalSelectPayload::hostPos), TerminalPayloads::handleTerminalSelect);
        ComputerAccess.accept(registrar, TerminalInsertPayload.TYPE, TerminalInsertPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalInsertPayload::hostPos), TerminalPayloads::handleTerminalInsert);
        ComputerAccess.accept(registrar, RequestServerBreakdownPayload.TYPE, RequestServerBreakdownPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestServerBreakdownPayload::hostPos), TerminalPayloads::handleRequestBreakdown);
        registrar.playToClient(ServerBreakdownPayload.TYPE, ServerBreakdownPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(TerminalPayloads::handleServerBreakdown));
        ComputerAccess.accept(registrar, TerminalMaintenancePayload.TYPE, TerminalMaintenancePayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalMaintenancePayload::hostPos), TerminalPayloads::handleTerminalMaintenance);
        ComputerAccess.accept(registrar, TerminalDropPayload.TYPE, TerminalDropPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalDropPayload::hostPos), TerminalPayloads::handleTerminalDrop);
    }

    // Network-operation dispatch: the ONLY way storage is touched. Every request

    private static void returnToPlayer(final ServerPlayer player, final ItemStack stack) {
        DataHandoff.returnToPlayer(player, stack);
    }

    public static void dispatchQuery(final ServerPlayer player, final PersonalComputerBlockEntity pc) {
        dispatch(player, pc, (level, net, mf) -> mf.submitOperation(
                new dev.jstech.computers.operation.NetworkQueryOperationTask(level, net, player),
                dev.jstech.core.operation.OperationPriority.MEDIUM));
    }

    @FunctionalInterface
    private interface IOperationSubmit {
        boolean submit(ServerLevel level, NetworkUuid net, MainframeBlockEntity mainframe);
    }

    private static boolean dispatch(final ServerPlayer player, final PersonalComputerBlockEntity pc,
                                    final IOperationSubmit submit) {
        final NetworkUuid net = pc.networkUuid();
        if (net == null || !(pc.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        return mainframe != null && submit.submit(level, net, mainframe);
    }

    public static boolean networkHasActiveOps(final ServerLevel level, final NetworkUuid network) {
        final MainframeBlockEntity mainframe = resolveMainframe(level, network);
        return mainframe != null && mainframe.hasActiveOperations();
    }

    private static void handleTerminalMaintenance(final TerminalMaintenancePayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || !host.isMainframeHost() || host.networkUuid() == null) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        final dev.jstech.computers.operation.NetworkIndex index = mainframe.networkIndex();
        byte opType;
        long count;
        ItemStack icon;
        String message;
        switch (payload.action()) {
            case TerminalMaintenancePayload.ACTION_ANALYZE -> {
                index.analyzeIncremental(level, net);
                opType = OperationRecord.TYPE_ANALYZE;
                count = index.catalogSize();
                icon = labelledIcon(Items.SPYGLASS, "index");
                message = "ANALYZE complete - " + count + " types reconciled";
            }
            case TerminalMaintenancePayload.ACTION_REINDEX -> {
                /*
                 * The disks are read now; the catalog is built off the tick and swapped in later, when
                 * the run is logged and the grid refreshed.
                 */
                final ItemStack reindexIcon = labelledIcon(Items.COMPASS, "index");
                mainframe.reindexAsync(() -> {
                    mainframe.recordOperation(OperationRecord.TYPE_REINDEX, reindexIcon, index.catalogSize(),
                            index.catalogSize(), OperationRecord.STATUS_COMPLETED, java.util.List.of());
                    player.displayClientMessage(Component.literal("REINDEX complete - catalog rebuilt from disks"),
                            true);
                    dispatchTerminalQuery(player, net, level);
                });
                player.displayClientMessage(Component.literal("REINDEX started - rebuilding the catalog from disks"),
                        true);
                return;
            }
            case TerminalMaintenancePayload.ACTION_VACUUM -> {
                final int freed = index.vacuum(level, net);
                opType = OperationRecord.TYPE_VACUUM;
                count = freed;
                icon = labelledIcon(Items.HOPPER, "ghost rows");
                message = "VACUUM freed " + freed + (freed == 1 ? " ghost entry" : " ghost entries");
            }
            default -> {
                return;
            }
        }
        // Index maintenance is instantaneous; log it COMPLETED so the Operations tab records that it ran.
        mainframe.recordOperation(opType, icon, count, count,
                OperationRecord.STATUS_COMPLETED, java.util.List.of());
        player.displayClientMessage(Component.literal(message), true);
        dispatchTerminalQuery(player, net, level); // the catalog may have changed, so refresh the grid
    }

    private static void handleTerminalDrop(final TerminalDropPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || !host.isMainframeHost() || host.networkUuid() == null) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        final dev.jstech.computers.operation.NetworkIndex index = mainframe.networkIndex();
        long destroyed = 0L;
        String label;
        StorageKey recordKey;
        switch (payload.scope()) {
            case TerminalDropPayload.SCOPE_NETWORK -> {
                destroyed = index.dropAll(level, net);
                label = "the network";
                recordKey = StorageKey.of(labelledIcon(Items.TNT, "network"));
            }
            case TerminalDropPayload.SCOPE_SERVER -> {
                if (payload.serverKey().isEmpty()) {
                    return;
                }
                final NodeUuid node;
                try {
                    node = NodeUuid.fromString(payload.serverKey());
                } catch (final IllegalArgumentException malformed) {
                    return;
                }
                destroyed = index.dropServer(level, node);
                label = "a server";
                recordKey = StorageKey.of(labelledIcon(Items.TNT, serverLabel(level, node)));
            }
            case TerminalDropPayload.SCOPE_TYPES -> {
                for (final StorageKey key : payload.types()) {
                    destroyed += index.dropType(level, net, key, null);
                }
                final int n = payload.types().size();
                label = n + (n == 1 ? " type" : " types");
                // A single-type DROP shows that data's real icon; many types collapse to a tagged marker.
                recordKey = n == 1 ? payload.types().get(0) : StorageKey.of(labelledIcon(Items.TNT, n + " types"));
            }
            default -> {
                return;
            }
        }
        mainframe.recordOperation(new OperationRecord(OperationRecord.TYPE_DROP, recordKey,
                destroyed, destroyed, OperationRecord.STATUS_COMPLETED, java.util.List.of()));
        player.displayClientMessage(Component.literal(
                "DROP destroyed " + destroyed + " from " + label), true);
        dispatchTerminalQuery(player, net, level);
    }

    private static ItemStack labelledIcon(final net.minecraft.world.item.Item item, final String label) {
        final ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        return stack;
    }

    private static void handleSnapshot(final NetworkSnapshotPayload payload, final Player player) {
        if (player.containerMenu
                instanceof dev.jstech.computers.menu.ComputerTerminalMenu terminal) {
            terminal.setNetworkItems(payload.items());
        }
    }

    public static void dispatchTerminalQuery(final ServerPlayer player, final NetworkUuid net,
                                             final ServerLevel level) {
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe != null) {
            mainframe.submitOperation(
                    new dev.jstech.computers.operation.NetworkQueryOperationTask(level, net, player),
                    dev.jstech.core.operation.OperationPriority.MEDIUM);
        }
    }

    private static void handleTerminalSelect(final TerminalSelectPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        // Resolve where the pulled items land: the computer's own local storage (simple/auto) or
        final Dest dest = resolveDest(host, level, net, payload.destKind(), payload.destServer());
        if (dest == null) {
            return;
        }
        Set<NodeUuid> sources = payload.serverKeys().isEmpty() ? null : toNodes(payload.serverKeys());
        /*
         * A MOVE must never pull from its own destination Server: extracting and re-inserting into
         * the same store would churn items in place. Drop the target from the sources.
         */
        if (dest.move() && dest.target() != null) {
            sources = sourcesWithout(level, net, sources, dest.target());
            if (sources.isEmpty()) {
                return; // the only chosen source was the destination, nothing to move
            }
        }
        final StorageKey key = payload.key();
        final var op = dest.move()
                ? mainframe.submitNetworkMove(key, payload.quantity(), dest.handler(), dest.label(), sources)
                : mainframe.submitNetworkSelect(key, payload.quantity(), dest.handler(), dest.label(), sources);
        if (op != null) {
            if (!dest.move()) {
                op.abortWhen(gone(host)); // the pull lands in this computer: stop once it is gone
            }
            op.onSettle(() -> sendSnapshot(player, level, net));
        }
    }

    private static void handleTerminalInsert(final TerminalInsertPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || host.networkUuid() == null
                || !(player.containerMenu instanceof ComputerTerminalMenu menu)) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        final int idx = payload.slotIndex();
        final boolean fromCursor = idx == TerminalInsertPayload.CURSOR || idx == TerminalInsertPayload.CURSOR_ONE;
        // A slot source must be a player-inventory slot, never a storage slot.
        if (!fromCursor && (idx < menu.storageSlotCount() || idx >= menu.slots.size())) {
            return;
        }
        final DataHandoff.ISource source = fromCursor
                ? DataHandoff.cursor(player) : DataHandoff.slot(menu.getSlot(idx), player);
        /*
         * A right-click hands over ONE: one item, or what a held container holds, and a held empty
         * container over a fluid or chemical entry fills from it instead. Left click and shift-click
         * deposit the stack as items, the way a chest takes them.
         */
        final boolean one = idx == TerminalInsertPayload.CURSOR_ONE;
        final Runnable refresh = () -> sendSnapshot(player, level, net);
        if (one && payload.entry().isPresent() && DataContainers.canTake(source.get(), payload.entry().get())) {
            DataHandoff.fillFromNetwork(mainframe, level, net, player, source, payload.entry().get(),
                    host.originLabel(MoveLabels.TERMINAL), refresh);
            return;
        }
        DataHandoff.intoNetwork(mainframe, level, net, player, source, one ? 1 : source.get().getCount(),
                one, host.originLabel(MoveLabels.TERMINAL), refresh);
    }

    private static void handleRequestBreakdown(final RequestServerBreakdownPayload payload,
                                               final ServerPlayer player, final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host != null && host.networkUuid() != null) {
            PacketDistributor.sendToPlayer(player, collectBreakdown(level, host.networkUuid(), payload.key()));
            /*
             * The advanced-mode destination picker needs every computer that can hold items (the
             * Mainframe's local storage and every Server), not just those holding the clicked item.
             */
            PacketDistributor.sendToPlayer(player, collectComputers(level, host.networkUuid()));
        }
    }

    private static void handleServerBreakdown(final ServerBreakdownPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setServerBreakdown(payload.servers());
        }
    }

    private static ServerBreakdownPayload collectBreakdown(final ServerLevel level, final NetworkUuid net,
                                                           final StorageKey key) {
        final Map<NodeUuid, Long> perServer = dev.jstech.computers.operation.NetworkStorage
                .of(level, net).breakdown(key);
        final List<ServerBreakdownPayload.ServerHolding> rows = new ArrayList<>();
        for (final Map.Entry<NodeUuid, Long> e : perServer.entrySet()) {
            if (rows.size() >= ServerBreakdownPayload.MAX) {
                break;
            }
            rows.add(new ServerBreakdownPayload.ServerHolding(
                    e.getKey().asString(), serverLabel(level, e.getKey()), e.getValue()));
        }
        return new ServerBreakdownPayload(rows);
    }

    public static void sendSnapshot(final ServerPlayer player, final ServerLevel level, final NetworkUuid network) {
        if (player == null || player.isRemoved()) {
            return; // no one to send to (e.g. the requester logged out before the Operation settled)
        }
        final Map<StorageKey, Long> totals = network == null
                ? Map.of()
                : dev.jstech.computers.operation.NetworkStorage.of(level, network).query();
        final List<NetworkItemEntry> entries = new ArrayList<>(Math.min(totals.size(),
                NetworkSnapshotPayload.MAX_ENTRIES));
        /*
         * Bounded by the wire cap so encoding never overflows the StreamCodec. The entry carries the
         * full stack (components and all), so the terminal shows the enchanted item, not a bare one.
         */
        totals.entrySet().stream().limit(NetworkSnapshotPayload.MAX_ENTRIES)
                .forEach(e -> entries.add(new NetworkItemEntry(e.getKey(), e.getValue())));
        PacketDistributor.sendToPlayer(player, new NetworkSnapshotPayload(entries));
    }
}
