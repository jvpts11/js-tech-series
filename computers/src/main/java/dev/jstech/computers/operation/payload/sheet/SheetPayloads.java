/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.sheet;

import dev.jstech.computers.client.os.ExceedApp;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.RequestSheetFactsPayload;
import dev.jstech.computers.operation.payload.SheetFactsPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads behind a sheet's asking cells.
 *
 * <p>A sheet names the few things it mentions and gets their counts back, rather than being handed the
 * whole network. That keeps the answer small whatever the network is holding, and it is why a sheet can
 * be worked out the moment the answer arrives instead of being recomputed on a tick.
 */
public final class SheetPayloads {

    private SheetPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestSheetFactsPayload.TYPE, RequestSheetFactsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestSheetFactsPayload::hostPos), SheetPayloads::handleRequest);
        registrar.playToClient(SheetFactsPayload.TYPE, SheetFactsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SheetPayloads::handleFacts));
    }

    private static void handleRequest(final RequestSheetFactsPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        final IComputerTerminalHost host =
                niHost(player, level, payload.hostPos(), payload.monitorPos());
        final NetworkUuid net = host == null ? null : host.networkUuid();
        final List<String> names = payload.items();
        final List<Long> counts = new ArrayList<>(names.size());
        if (net == null) {
            /*
             * A machine on no network knows nothing rather than holding nothing, so every count comes back
             * as unknown and the cells say so. Answering zero would be a lie a player could act on.
             */
            for (int i = 0; i < names.size(); i++) {
                counts.add(-1L);
            }
            PacketDistributor.sendToPlayer(player, new SheetFactsPayload(names, counts, -1L, -1L));
            return;
        }
        /*
         * The whole index is read once and turned into a lookup, rather than walked per name: a sheet with
         * twenty asking cells would otherwise walk a network of ten thousand types twenty times over.
         */
        final Map<StorageKey, Long> totals = NetworkStorage.of(level, net).query();
        final Map<String, Long> byName = new HashMap<>(totals.size());
        for (final Map.Entry<StorageKey, Long> entry : totals.entrySet()) {
            byName.merge(entry.getKey().id(), entry.getValue(), Long::sum);
        }
        for (final String name : names) {
            final Long held = byName.get(normalise(name));
            counts.add(held == null ? 0L : held);
        }
        long free = 0L;
        final NetworkSystem system = NetworkSystem.get(level);
        int servers = 0;
        for (final var server : system.serversOf(net)) {
            servers++;
            free += server.storageItems();
        }
        long used = 0L;
        for (final long value : totals.values()) {
            used += value;
        }
        PacketDistributor.sendToPlayer(player,
                new SheetFactsPayload(names, counts, Math.max(0L, free - used), servers));
    }

    /**
     * A name as the index writes it.
     *
     * <p>A player types {@code iron_ingot} and the index holds {@code minecraft:iron_ingot}, so a name
     * with no namespace is read as vanilla's, which is what every other place in the mod does.
     */
    private static String normalise(final String name) {
        return name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    private static void handleFacts(final SheetFactsPayload payload, final Player player) {
        ExceedApp.accept(payload);
    }
}
