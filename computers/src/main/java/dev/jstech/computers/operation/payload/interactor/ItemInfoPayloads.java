/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.interactor;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.blockentity.DataCableBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.client.os.StorageInsightsApp;
import dev.jstech.computers.machine.ItemRecipes;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.ItemDetailPayload;
import dev.jstech.computers.operation.payload.ItemRecipesPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.RequestItemDetailPayload;
import dev.jstech.computers.operation.payload.RequestItemRecipesPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads that describe one item: how much of it the network holds and where, and the recipes that make or use
 * it.
 */
public final class ItemInfoPayloads {

    private ItemInfoPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestItemDetailPayload.TYPE, RequestItemDetailPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestItemDetailPayload::host), ItemInfoPayloads::handleRequestItemDetail);
        registrar.playToClient(ItemDetailPayload.TYPE, ItemDetailPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ItemInfoPayloads::handleItemDetail));
        ComputerAccess.accept(registrar, RequestItemRecipesPayload.TYPE, RequestItemRecipesPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestItemRecipesPayload::hostPos), ItemInfoPayloads::handleRequestItemRecipes);
        registrar.playToClient(ItemRecipesPayload.TYPE, ItemRecipesPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        NetworkInteractorApp.acceptItemRecipes(payload)));
    }

    private static void handleRequestItemDetail(final RequestItemDetailPayload payload, final ServerPlayer player,
                                                final ServerLevel level) {
        if (!payload.item().isEmpty()) {
            final var host = niHost(player, level, payload.host(), payload.monitorPos());
            if (host != null && host.networkUuid() != null) {
                PacketDistributor.sendToPlayer(player,
                        collectItemDetail(level, host.networkUuid(), StorageKey.of(payload.item())));
            }
        }
    }

    private static void handleItemDetail(final ItemDetailPayload payload, final Player player) {
        StorageInsightsApp.acceptDetail(payload);
    }

    /** One item's detail: the network total, where it is stored, what it makes, and which buses filter it. */
    private static ItemDetailPayload collectItemDetail(final ServerLevel level, final NetworkUuid network,
                                                       final StorageKey key) {
        final NetworkSystem system = NetworkSystem.get(level);
        final long total = NetworkStorage.of(level, network)
                .query().getOrDefault(key, 0L);

        // Where it is stored: per server that holds any.
        final List<NetworkItemEntry.StorageShare> stored = new ArrayList<>();
        for (final ServerNode server : system.serversOf(network)) {
            if (stored.size() >= ItemDetailPayload.MAX_STORED) {
                break;
            }
            final long held = system.locationOf(server.nodeUuid())
                    .map(loc -> level.getBlockEntity(BlockPos.of(loc.rackPos()))
                            instanceof ServerRackBlockEntity rack
                            ? rack.getServerStorage(loc.slot()).count(key) : 0L)
                    .orElse(0L);
            if (held > 0) {
                stored.add(new NetworkItemEntry.StorageShare(Text.literal(serverLabel(level, server.nodeUuid())), held));
            }
        }

        // What it makes: the products of any pattern that consumes it as an ingredient.
        final List<ItemStack> uses = new ArrayList<>();
        final Set<StorageKey> seen = new HashSet<>();
        final MainframeBlockEntity mf = resolveMainframe(level, network);
        if (mf != null) {
            for (final var pattern : mf.networkPatterns()) {
                if (uses.size() >= ItemDetailPayload.MAX_USES) {
                    break;
                }
                if (pattern.ingredientTotals().containsKey(key)) {
                    final StorageKey rk = StorageKey.of(pattern.result());
                    if (seen.add(rk)) {
                        uses.add(pattern.result().copy());
                    }
                }
            }
            for (final var proc : mf.networkProcessingPatterns()) {
                if (uses.size() >= ItemDetailPayload.MAX_USES) {
                    break;
                }
                final boolean consumes = proc.inputs().stream().anyMatch(in -> in.key().equals(key));
                final var out = proc.primaryOutput();
                if (consumes && out != null && seen.add(out.key())) {
                    uses.add(out.key().stack(1));
                }
            }
        }

        // Which buses filter it: walk the network's cable positions and read each bus's filter.
        final List<ItemDetailPayload.BusRef> buses = new ArrayList<>();
        for (final long posLong : system.connectivity().positionsOf(network)) {
            if (buses.size() >= ItemDetailPayload.MAX_BUSES) {
                break;
            }
            if (!(level.getBlockEntity(BlockPos.of(posLong))
                    instanceof DataCableBlockEntity cable)) {
                continue;
            }
            for (final Direction dir : Direction.values()) {
                if (cable.getPart(dir)
                        instanceof AbstractBusPart bus
                        && key.equals(bus.filterKey())) {
                    buses.add(new ItemDetailPayload.BusRef(bus.name(), bus.type().text()));
                }
            }
        }
        return new ItemDetailPayload(key.stack(1), total, stored, uses, buses);
    }

    /** Answers the details panel: what makes the item on this network, and what uses it. */
    private static void handleRequestItemRecipes(final RequestItemRecipesPayload payload, final ServerPlayer player,
                                                 final ServerLevel level) {
        final var host = niHost(player, level, payload.hostPos(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        PacketDistributor.sendToPlayer(player, new ItemRecipesPayload(payload.key(),
                ItemRecipes.madeBy(mainframe, payload.key(), ItemRecipesPayload.MAX_LINES),
                ItemRecipes.usedIn(mainframe, payload.key(), ItemRecipesPayload.MAX_LINES)));
    }
}
