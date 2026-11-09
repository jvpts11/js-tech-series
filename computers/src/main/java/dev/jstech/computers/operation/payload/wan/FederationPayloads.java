/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.InteractorFederation;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.NetworkInteractorPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.terminal.MoveDestinations;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.wan.FederatedOperation;
import dev.jstech.computers.wan.FederationPath;
import dev.jstech.computers.wan.WanFederation;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * The Federated tab's payloads (computers/docs/design/network.md): what the networks a computer's network is federated
 * with hold, and taking from, sending to and crafting on one of them. Every action is an Operation that crosses, which
 * the far network's own switches allow or refuse; this only starts it.
 */
public final class FederationPayloads {

    /* The most a single request takes or crafts, so a spoofed packet cannot ask for more than an int's worth. */
    private static final long MOST = Integer.MAX_VALUE;

    private FederationPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, FederationRequestPayload.TYPE, FederationRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(FederationRequestPayload::hostPos), FederationPayloads::handleRequest);
        registrar.playToClient(FederationStatePayload.TYPE, FederationStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(FederationPayloads::handleState));
    }

    /**
     * What the Federated tab shows to the computer at {@code hostPos}: every network its network reaches, and what
     * {@code shown} holds and can craft, or the nearest's when {@code shown} names none of them.
     */
    public static FederationStatePayload state(final ServerLevel level, final IComputerTerminalHost host,
                                               final BlockPos hostPos, final String shown) {
        final NetworkUuid home = host.networkUuid();
        if (home == null) {
            return FederationStatePayload.none(hostPos);
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, home);
        final List<OperationRecord> live = mainframe == null ? List.of() : mainframe.activeOperationRecords();
        final List<FederationStatePayload.Peer> peers = new ArrayList<>();
        for (final FederationPath path : WanFederation.of(level).reachable(home, level.getGameTime())) {
            if (peers.size() >= FederationStatePayload.MOST_PEERS) {
                break;
            }
            peers.add(peer(level, path, live));
        }
        final FederationPath picked = pathTo(level, home, shown);
        if (picked == null) {
            return new FederationStatePayload(hostPos, peers, "", List.of(), List.of());
        }
        return new FederationStatePayload(hostPos, peers, picked.target().asString(),
                picked.allows(WanRight.SEE) ? items(level, picked.target()) : List.of(),
                picked.allows(WanRight.CRAFT) ? craftable(level, picked.target()) : List.of());
    }

    /* Acts when asked to, then answers with the tab's state, the network acted on shown. */
    private static void handleRequest(final FederationRequestPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        final IComputerTerminalHost host = niHost(player, level, payload.hostPos(), payload.monitorPos());
        if (host == null) {
            return;
        }
        if (payload.action() != FederationRequestPayload.ASK) {
            act(payload, player, level, host);
        }
        PacketDistributor.sendToPlayer(player, state(level, host, payload.hostPos(), payload.network()));
    }

    /* Starts the Operation the request asks for, on the asking network's Mainframe. */
    private static void act(final FederationRequestPayload payload, final ServerPlayer player, final ServerLevel level,
                            final IComputerTerminalHost host) {
        final NetworkUuid home = host.networkUuid();
        final MainframeBlockEntity mainframe = home == null ? null : resolveMainframe(level, home);
        // Only the network asked for: one no longer reached is acted on nowhere, never on another.
        final FederationPath path = mainframe == null ? null : NetworkUuid.parse(payload.network())
                .flatMap(target -> WanFederation.of(level).path(home, target, level.getGameTime())).orElse(null);
        if (path == null) {
            return;
        }
        final long amount = Math.max(1L, Math.min(payload.amount(), MOST));
        switch (payload.action()) {
            case FederationRequestPayload.TAKE -> {
                final MoveDestinations.Dest dest = MoveDestinations.resolveDest(host, level, home,
                        TerminalSelectPayload.DEST_LOCAL, "");
                if (dest != null && payload.key().isPresent()) {
                    mainframe.submitFederated(FederatedOperation.take(level, path, payload.key().get(), amount,
                            dest.handler()), OperationPriority.DEFAULT);
                }
            }
            case FederationRequestPayload.SEND -> send(player, level, mainframe, path);
            case FederationRequestPayload.CRAFT -> payload.key().ifPresent(key -> mainframe.submitFederated(
                    FederatedOperation.craft(level, path, key, amount), OperationPriority.DEFAULT));
            default -> { }
        }
    }

    /*
     * Sends the stack on the player's cursor, which leaves the cursor at once; what the far network does not take
     * comes back to the player. Nothing leaves the cursor when the far network does not let this one send.
     */
    private static void send(final ServerPlayer player, final ServerLevel level, final MainframeBlockEntity mainframe,
                             final FederationPath path) {
        final DataHandoff.ISource cursor = DataHandoff.cursor(player);
        final ItemStack stack = cursor.get();
        if (stack.isEmpty() || !path.allows(WanRight.SEND)) {
            return;
        }
        final StorageKey key = StorageKey.of(stack);
        final int count = stack.getCount();
        cursor.set(ItemStack.EMPTY);
        player.containerMenu.broadcastChanges();
        if (mainframe.submitFederated(FederatedOperation.send(level, path, key, count, player),
                OperationPriority.DEFAULT) == null) {
            DataHandoff.returnToPlayer(player, key.stack(count));
        }
    }

    /* The way to the network named {@code network}, or to the nearest when it names none this one reaches. */
    @Nullable
    private static FederationPath pathTo(final ServerLevel level, final NetworkUuid home, final String network) {
        final List<FederationPath> paths = WanFederation.of(level).reachable(home, level.getGameTime());
        for (final FederationPath path : paths) {
            if (path.target().asString().equals(network)) {
                return path;
            }
        }
        return paths.isEmpty() ? null : paths.getFirst();
    }

    private static FederationStatePayload.Peer peer(final ServerLevel level, final FederationPath path,
                                                    final List<OperationRecord> live) {
        final NetworkUuid target = path.target();
        final String name = WanFederation.farName(level, path);
        final NetworkStorage storage = NetworkStorage.of(level, target);
        int crossing = 0;
        for (final OperationRecord record : live) {
            if (FederatedOperation.KIND.equals(record.kind()) && record.machine().equals(name)) {
                crossing++;
            }
        }
        return new FederationStatePayload.Peer(target.asString(), name, path.out().getFirst().port(),
                path.out().getFirst().line().serializedName(), path.speed(), path.grant().bits(), path.hops(),
                storage.query().size(), NetworkSystem.get(level).serversOf(target).size(),
                CraftingPayloads.buildCraftCatalog(level, target).size(), crossing, storage.usedMb(),
                storage.capacityMb());
    }

    /* What {@code network} holds, as the grid lists it. */
    private static List<NetworkItemEntry> items(final ServerLevel level, final NetworkUuid network) {
        final List<NetworkItemEntry> out = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> entry : NetworkStorage.of(level, network).query().entrySet()) {
            if (out.size() >= NetworkInteractorPayload.MAX_ENTRIES) {
                break;
            }
            out.add(new NetworkItemEntry(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    /* What {@code network} can craft, by the data each recipe gives. */
    private static List<StorageKey> craftable(final ServerLevel level, final NetworkUuid network) {
        final List<StorageKey> out = new ArrayList<>();
        for (final var entry : CraftingPayloads.buildCraftCatalog(level, network)) {
            if (out.size() >= FederationStatePayload.MOST_CRAFTABLE) {
                break;
            }
            out.add(StorageKey.of(entry.result()));
        }
        return out;
    }

    /* Hands the state to the Federated tab of the Network Interactor, and to the terminal's when that is open. */
    private static void handleState(final FederationStatePayload payload, final Player player) {
        InteractorFederation.accept(payload);
        if (player.containerMenu instanceof ComputerTerminalMenu menu && menu.hostPos().equals(payload.hostPos())) {
            menu.setFederation(payload);
        }
    }
}
