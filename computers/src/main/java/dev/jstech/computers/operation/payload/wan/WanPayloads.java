/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.client.os.BorderLinkApp;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.hardware.WanCardSpec;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.wan.BorderLink;
import dev.jstech.computers.wan.GatewayLinks;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanLink;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Nouvell BorderLink's payloads: its panel asks for the gateway's ports and sets their switches.
 */
public final class WanPayloads {

    private WanPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, BorderLinkRequestPayload.TYPE, BorderLinkRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(BorderLinkRequestPayload::hostPos), WanPayloads::handleRequest);
        registrar.playToClient(BorderLinkStatePayload.TYPE, BorderLinkStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(WanPayloads::handleState));
        FederationPayloads.register(registrar);
    }

    /** What BorderLink shows of {@code gateway}, in {@code level}. */
    public static BorderLinkStatePayload state(final WanGatewayBlockEntity gateway, final ServerLevel level) {
        final List<BorderLinkStatePayload.Port> ports = new ArrayList<>(WanGatewayStructure.PORTS);
        final List<WanCardSpec> cards = gateway.wanCards();
        final GatewayLinks links = gateway.links();
        for (int port = 1; port <= WanGatewayStructure.PORTS; port++) {
            final WanLink link = links.link(port);
            final String line = port > cards.size() ? ""
                    : (link != null ? link.line() : cards.get(port - 1).line()).serializedName();
            final NetworkUuid far = links.farNetwork(port);
            ports.add(new BorderLinkStatePayload.Port((byte) links.state(port), (byte) links.shownMode(port).id(),
                    line, links.farName(port), far == null ? "" : ShortId.of(far.asString()),
                    gateway.portSettings(port).bits()));
        }
        return new BorderLinkStatePayload(gateway.getBlockPos(), gateway.displayName(),
                BorderLink.version(gateway.cabinetEra()), ports, log(gateway, level));
    }

    /* The newest events the gateway wrote into its network's log, as BorderLink's log shows them. */
    private static List<NetworkEventsPayload.Row> log(final WanGatewayBlockEntity gateway, final ServerLevel level) {
        final NetworkUuid network = gateway.networkUuid();
        final NodeUuid node = gateway.nodeUuid();
        final List<NetworkEventsPayload.Row> rows = new ArrayList<>();
        if (network == null || node == null) {
            return rows;
        }
        for (final NetworkEvent event : NetworkEventLog.of(level).newest(network, level.getGameTime(),
                event -> node.equals(event.machine()), BorderLinkStatePayload.LOG_ROWS)) {
            rows.add(new NetworkEventsPayload.Row(event.time(), event.dayTime(), (byte) event.level().id(),
                    NetworkEventsPayload.clip(event.machineName()), event.message()));
        }
        return rows;
    }

    /* Sets a port's switches when asked to, on a gateway that runs BorderLink, and answers with the state. */
    private static void handleRequest(final BorderLinkRequestPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof WanGatewayBlockEntity gateway)) {
            return;
        }
        if (payload.port() >= 1 && payload.port() <= WanGatewayStructure.PORTS && BorderLink.installed(gateway)) {
            gateway.setPortSettings(payload.port(), PortSettings.of(payload.bits()));
        }
        PacketDistributor.sendToPlayer(player, state(gateway, level));
    }

    /* Hands the state to BorderLink's window on a desktop, and to its operating space when that is what is open. */
    private static void handleState(final BorderLinkStatePayload payload, final Player player) {
        BorderLinkApp.accept(payload);
        if (player.containerMenu instanceof ComputerTerminalMenu menu && menu.hostPos().equals(payload.hostPos())) {
            menu.setBorderLink(payload);
        }
    }
}
