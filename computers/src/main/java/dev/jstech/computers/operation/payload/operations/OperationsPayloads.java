/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.operations;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.ActiveOperationsPayload;
import dev.jstech.computers.operation.payload.CancelOperationPayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.OperationsLogPayload;
import dev.jstech.computers.operation.payload.RequestNiOperationsPayload;
import dev.jstech.computers.operation.payload.SetOperationPriorityPayload;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads of the Operations views: the log, the active Operations, changing the priority of an Operation and
 * cancelling one.
 */
public final class OperationsPayloads {

    private OperationsPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(OperationsLogPayload.TYPE, OperationsLogPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(OperationsPayloads::handleOpsLog));
        registrar.playToClient(ActiveOperationsPayload.TYPE, ActiveOperationsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(OperationsPayloads::handleActiveOps));
        ComputerAccess.accept(registrar, RequestNiOperationsPayload.TYPE, RequestNiOperationsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNiOperationsPayload::host), OperationsPayloads::handleRequestNiOperations);
        ComputerAccess.accept(registrar, SetOperationPriorityPayload.TYPE, SetOperationPriorityPayload.STREAM_CODEC,
                ComputerAccess.machine(SetOperationPriorityPayload::host), OperationsPayloads::handleSetOperationPriority);
        ComputerAccess.accept(registrar, CancelOperationPayload.TYPE, CancelOperationPayload.STREAM_CODEC,
                ComputerAccess.machine(CancelOperationPayload::host), OperationsPayloads::handleCancelOperation);
    }

    private static void handleSetOperationPriority(final SetOperationPriorityPayload payload,
                                                   final ServerPlayer player, final ServerLevel level) {
        /*
         * Any computer on the network may re-prioritise its Operations: the same proximity-to-a-linked-
         * monitor check the other desktop requests use, so a player cannot drive a foreign network.
         */
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        mainframe.setOperationPriority(payload.operationId(), payload.priority());
        dispatchActiveOperations(player, host.networkUuid(), level);
    }

    private static void handleCancelOperation(final CancelOperationPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        mainframe.cancelOperation(payload.operationId());
        // The cancelled Operation is logged on the Mainframe's next tick: refresh both views then.
        mainframe.runNextTick(() -> {
            dispatchActiveOperations(player, host.networkUuid(), level);
            dispatchTerminalOpsLog(player, host.networkUuid(), level);
        });
    }

    public static void dispatchTerminalOpsLog(final ServerPlayer player, final NetworkUuid net,
                                              final ServerLevel level) {
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        PacketDistributor.sendToPlayer(player, new OperationsLogPayload(
                mainframe != null ? mainframe.recentOperations() : List.of()));
    }

    private static void handleOpsLog(final OperationsLogPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setOperationsLog(payload.operations());
        } else {
            dev.jstech.computers.client.os.NetworkInteractorApp
                    .acceptOps(payload.operations());
            dev.jstech.computers.client.os.NetworkManagerApp
                    .acceptOpsLog(payload.operations());
        }
    }

    public static void dispatchActiveOperations(final ServerPlayer player, final NetworkUuid net,
                                                final ServerLevel level) {
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        final int[] slots = mainframe != null ? mainframe.supercomputerCraftSlots() : new int[] {0, 0};
        PacketDistributor.sendToPlayer(player, new ActiveOperationsPayload(
                mainframe != null ? mainframe.activeOperationRecords() : List.of(), slots[0], slots[1]));
    }

    private static void handleActiveOps(final ActiveOperationsPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setActiveOps(payload.operations());
        } else {
            dev.jstech.computers.client.os.NetworkInteractorApp
                    .acceptActiveOps(payload.operations(), payload.scSlotsUsed(), payload.scSlotsTotal());
            dev.jstech.computers.client.os.NetworkManagerApp
                    .acceptActiveOps(payload.operations(), payload.scSlotsUsed(), payload.scSlotsTotal());
        }
    }

    /** The NI's Operations tab asks for the network's recent + active Operations; replies with both logs. */
    private static void handleRequestNiOperations(final RequestNiOperationsPayload payload,
                                                  final ServerPlayer player, final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        dispatchTerminalOpsLog(player, host.networkUuid(), level);
        dispatchActiveOperations(player, host.networkUuid(), level);
    }
}
