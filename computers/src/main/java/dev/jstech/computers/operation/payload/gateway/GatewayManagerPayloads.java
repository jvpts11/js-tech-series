/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.gateway;

import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.GatewayManagerActionPayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.payload.RequestGatewayManagerPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Gateway Manager's payloads: the Gateways a computer hosts and the actions taken on one.
 */
public final class GatewayManagerPayloads {

    private GatewayManagerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        // The Gateway Manager: the host computer's program asks about its Gateways, acts on one, and gets a state back.
        ComputerAccess.accept(registrar, RequestGatewayManagerPayload.TYPE, RequestGatewayManagerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestGatewayManagerPayload::hostPos),
                GatewayManagerPayloads::handleRequestGatewayManager);
        ComputerAccess.accept(registrar, GatewayManagerActionPayload.TYPE, GatewayManagerActionPayload.STREAM_CODEC,
                ComputerAccess.machine(GatewayManagerActionPayload::hostPos), GatewayManagerPayloads::handleGatewayManagerAction);
        registrar.playToClient(GatewayManagerStatePayload.TYPE, GatewayManagerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(GatewayManagerPayloads::handleGatewayManagerState));
    }

    /** Routes the Cluster Manager state to the open window. */
    private static void handleRequestGatewayManager(final RequestGatewayManagerPayload payload,
                                                    final ServerPlayer player, final ServerLevel level) {
        final var host = level.getBlockEntity(payload.hostPos());
        if (host == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                dev.jstech.computers.gateway.GatewayManager.state(level, host, payload.selected(), ""));
    }

    private static void handleGatewayManagerAction(final GatewayManagerActionPayload payload,
                                                   final ServerPlayer player, final ServerLevel level) {
        final var host = level.getBlockEntity(payload.hostPos());
        if (host == null) {
            return;
        }
        final String status = dev.jstech.computers.gateway.GatewayManager.act(level, host, payload.gatewayPos(),
                payload.action(), payload.value(), payload.text());
        PacketDistributor.sendToPlayer(player,
                dev.jstech.computers.gateway.GatewayManager.state(level, host, payload.gatewayPos(), status));
    }

    private static void handleGatewayManagerState(final GatewayManagerStatePayload payload, final Player player) {
        dev.jstech.computers.client.os.GatewayManagerApp.accept(payload);
    }
}
