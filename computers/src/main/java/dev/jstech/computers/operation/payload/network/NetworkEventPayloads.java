/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.operation.payload.RequestNetworkEventsPayload;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Network Manager's event log: what happened to the machines of a Mainframe's network, through the filters the
 * window has set. Only a player at that Mainframe's own screens asks.
 */
public final class NetworkEventPayloads {

    private NetworkEventPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestNetworkEventsPayload.TYPE, RequestNetworkEventsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNetworkEventsPayload::hostPos), NetworkEventPayloads::handleRequest);
        registrar.playToClient(NetworkEventsPayload.TYPE, NetworkEventsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkEventPayloads::handleEvents));
    }

    /**
     * What the event log of the network {@code network} shows through {@code request}'s filters at the world's game
     * time {@code now}: every event they keep, newest first, and the machines the log names.
     */
    public static NetworkEventsPayload collect(final NetworkEventLog log, final BlockPos hostPos,
                                               final NetworkUuid network, final RequestNetworkEventsPayload request,
                                               final long now) {
        final long revision = log.revision(network);
        if (request.known() == revision) {
            return NetworkEventsPayload.unchanged(hostPos, revision);
        }
        final List<NetworkEventsPayload.Row> rows = new ArrayList<>();
        for (final NetworkEvent event : log.newest(network, now, keeps(request, now), NetworkEventLog.MOST)) {
            rows.add(new NetworkEventsPayload.Row(event.time(), event.dayTime(), (byte) event.level().id(),
                    NetworkEventsPayload.clip(event.machineName()), event.message()));
        }
        final List<NetworkEventsPayload.Machine> machines = new ArrayList<>();
        for (final NetworkEvent named : log.machines(network, now)) {
            if (machines.size() >= NetworkEventsPayload.MAX_MACHINES) {
                break;
            }
            machines.add(new NetworkEventsPayload.Machine(named.machine().asString(),
                    NetworkEventsPayload.clip(named.machineName())));
        }
        return new NetworkEventsPayload(hostPos, revision, false, rows, machines);
    }

    private static void handleRequest(final RequestNetworkEventsPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mainframe)) {
            return;
        }
        final NetworkUuid network = mainframe.networkUuid();
        if (network == null) {
            PacketDistributor.sendToPlayer(player, new NetworkEventsPayload(payload.hostPos(), 0L, false, List.of(),
                    List.of()));
            return;
        }
        PacketDistributor.sendToPlayer(player, collect(NetworkEventLog.of(level), payload.hostPos(), network, payload,
                level.getGameTime()));
    }

    private static void handleEvents(final NetworkEventsPayload payload, final Player player) {
        NetworkManagerApp.acceptEvents(payload);
    }

    /* Whether an event passes the request's level, machine and period. */
    private static Predicate<NetworkEvent> keeps(final RequestNetworkEventsPayload request, final long now) {
        final EventLevel level = request.level() == RequestNetworkEventsPayload.ANY_LEVEL ? null
                : EventLevel.byId(request.level());
        final String machine = request.machine();
        final long since = RequestNetworkEventsPayload.ticksOf(request.period());
        return event -> (level == null || event.level() == level)
                && (machine.equals(RequestNetworkEventsPayload.ANY_MACHINE)
                || machine.equals(RequestNetworkEventsPayload.WHOLE_NETWORK) && event.machine() == null
                || event.machine() != null && event.machine().asString().equals(machine))
                && (since == Long.MAX_VALUE || now - event.time() <= since);
    }
}
