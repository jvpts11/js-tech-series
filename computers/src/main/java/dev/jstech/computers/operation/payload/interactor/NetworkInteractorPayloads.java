/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.interactor;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.NetworkInteractorPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NiCraftPayload;
import dev.jstech.computers.operation.payload.NiDepositPayload;
import dev.jstech.computers.operation.payload.NiGridClickPayload;
import dev.jstech.computers.operation.payload.NiSelectPayload;
import dev.jstech.computers.operation.payload.NiShiftInsertPayload;
import dev.jstech.computers.operation.payload.RequestNetworkInteractorPayload;
import dev.jstech.computers.operation.payload.RequestNiServersPayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.terminal.MoveDestinations.Dest;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.storage.DataContainers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.jstech.computers.operation.payload.crafting.CraftingPayloads.buildCraftCatalog;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkPayloads.collectComputers;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.gone;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.resolveDest;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.sourcesWithout;
import static dev.jstech.computers.operation.payload.terminal.MoveDestinations.toNodes;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.inventoryRoomFor;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The Network Interactor's payloads: its grid of items, depositing, inserting and crafting through it, and the
 * servers it moves items to.
 */
public final class NetworkInteractorPayloads {

    private NetworkInteractorPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestNetworkInteractorPayload.TYPE,
                RequestNetworkInteractorPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNetworkInteractorPayload::host),
                NetworkInteractorPayloads::handleRequestNetworkInteractor);
        registrar.playToClient(NetworkInteractorPayload.TYPE, NetworkInteractorPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkInteractorPayloads::handleNetworkInteractor));
        ComputerAccess.accept(registrar, NiGridClickPayload.TYPE, NiGridClickPayload.STREAM_CODEC,
                ComputerAccess.machine(NiGridClickPayload::host), NetworkInteractorPayloads::handleNiGridClick);
        ComputerAccess.accept(registrar, NiDepositPayload.TYPE, NiDepositPayload.STREAM_CODEC,
                ComputerAccess.machine(NiDepositPayload::host), NetworkInteractorPayloads::handleNiDeposit);
        ComputerAccess.accept(registrar, NiShiftInsertPayload.TYPE, NiShiftInsertPayload.STREAM_CODEC,
                ComputerAccess.machine(NiShiftInsertPayload::host), NetworkInteractorPayloads::handleNiShiftInsert);
        ComputerAccess.accept(registrar, RequestNiServersPayload.TYPE, RequestNiServersPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNiServersPayload::host), NetworkInteractorPayloads::handleRequestNiServers);
        ComputerAccess.accept(registrar, NiSelectPayload.TYPE, NiSelectPayload.STREAM_CODEC,
                ComputerAccess.machine(NiSelectPayload::host), NetworkInteractorPayloads::handleNiSelect);
        ComputerAccess.accept(registrar, NiCraftPayload.TYPE, NiCraftPayload.STREAM_CODEC,
                ComputerAccess.machine(NiCraftPayload::host), NetworkInteractorPayloads::handleNiCraft);
    }

    /** Sends the network's computers (Mainframe/Servers/PCs) to the open Network Interactor's advanced popup. */
    private static void handleRequestNiServers(final RequestNiServersPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, collectComputers(level, host.networkUuid()));
    }

    /**
     * The Network Interactor's advanced request: pull from chosen source Servers into a chosen destination.
     * Reuses the same dispatch as the MC-NET terminal SELECT, and only the host resolution (niHost) differs.
     */
    private static void handleNiSelect(final NiSelectPayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        if (payload.quantity() <= 0L) {
            return;
        }
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        // Empty destKey lands in this computer's own storage (a plain SELECT); a key targets a Server/PC (a MOVE).
        final boolean toComputer = !payload.destKey().isEmpty();
        final Dest dest = resolveDest(host, level, net,
                toComputer ? TerminalSelectPayload.DEST_SERVER : TerminalSelectPayload.DEST_LOCAL,
                payload.destKey());
        if (dest == null) {
            return;
        }
        Set<NodeUuid> sources = payload.serverKeys().isEmpty() ? null : toNodes(payload.serverKeys());
        if (dest.move() && dest.target() != null) {
            sources = sourcesWithout(level, net, sources, dest.target());
            if (sources.isEmpty()) {
                return; // the only chosen source was the destination, nothing to move
            }
        }
        final long qty = Math.min(payload.quantity(), Integer.MAX_VALUE);
        final var op = dest.move()
                ? mainframe.submitNetworkMove(payload.key(), qty, dest.handler(), dest.label(), sources)
                : mainframe.submitNetworkSelect(payload.key(), qty, dest.handler(), dest.label(), sources);
        if (op != null) {
            op.setPriority(payload.priority());
            if (!dest.move()) {
                op.abortWhen(gone(host)); // the pull lands in this computer: stop once it is gone
            }
        }
        if (op != null && host instanceof dev.jstech.computers.os
                .IOsHost computer) {
            op.onSettle(() -> sendNetworkInteractor(player, level, computer));
        }
    }

    private static void handleRequestNetworkInteractor(final RequestNetworkInteractorPayload payload,
                                                       final ServerPlayer player, final ServerLevel level) {
        /*
         * Proximity + monitor-link gated, like the mutating handlers, since the snapshot leaks the whole
         * network's contents, so a player must be at a monitor actually linked to this host.
         */
        if (niHost(player, level, payload.host(), payload.monitorPos()) != null
                && level.getBlockEntity(payload.host()) instanceof IOsHost computer) {
            sendNetworkInteractor(player, level, computer);
        }
    }

    /** Builds and sends a fresh Network Interactor snapshot (network grid, local grid, status, craft catalog). */
    public static void sendNetworkInteractor(final ServerPlayer player, final ServerLevel level,
            final IOsHost computer) {
        final NetworkUuid network = computer.networkUuid();
        // The whole network's items (Network Storage tab).
        final List<NetworkItemEntry> networkItems = new ArrayList<>();
        long usedItems = 0L;
        if (network != null) {
            final NetworkSystem system =
                    NetworkSystem.get(level);
            final NetworkStorage storage =
                    NetworkStorage.of(level, network);
            final Map<StorageKey, Long> totals = storage.query();
            for (final var e : totals.entrySet()) {
                if (networkItems.size() >= NetworkInteractorPayload.MAX_ENTRIES) {
                    break;
                }
                // Where this type lives, for the details panel: one share per server/storage that holds it.
                final List<NetworkItemEntry.StorageShare> shares = new ArrayList<>();
                for (final var s : storage.breakdown(e.getKey()).entrySet()) {
                    if (shares.size() >= NetworkItemEntry.MAX_SHARES) {
                        break;
                    }
                    shares.add(new NetworkItemEntry.StorageShare(serverLabel(system, s.getKey()), s.getValue()));
                }
                networkItems.add(new NetworkItemEntry(e.getKey(), e.getValue(), shares));
                usedItems += e.getValue();
            }
        }
        // This computer's own disks (Local Storage tab).
        final List<NetworkItemEntry> localItems = new ArrayList<>();
        int serverCount = 0;
        if (computer instanceof IComputerTerminalHost host) {
            for (final var e : host.localStore().view().entrySet()) {
                if (localItems.size() >= NetworkInteractorPayload.MAX_ENTRIES) {
                    break;
                }
                localItems.add(new NetworkItemEntry(e.getKey(), e.getValue()));
            }
            serverCount = host.networkServerCount();
        }
        final boolean online = network != null && resolveMainframe(level, network) != null;
        final List<CraftCatalogPayload.Entry> crafts = buildCraftCatalog(level, network);
        final List<String> favourites = computer.console() == null ? List.of()
                : computer.console().settings().favourites();
        final NetworkStorage room = network == null ? null
                : NetworkStorage.of(level, network);
        PacketDistributor.sendToPlayer(player, new NetworkInteractorPayload(
                networkItems, localItems, online, usedItems, serverCount, crafts, favourites,
                room == null ? 0L : room.capacity(),
                room == null ? 0L : room.usedMb(), room == null ? 0L : room.capacityMb()));
    }

    /** A human label for a storage node in the details panel's per-server breakdown, such as a server's rack position
     *  and slot, or a generic label for a published Personal Computer (which has no rack location). */
    private static String serverLabel(final NetworkSystem system,
                                      final NodeUuid node) {
        return system.locationOf(node)
                .map(loc -> {
                    final BlockPos p = BlockPos.of(loc.rackPos());
                    return "Server " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " #" + (loc.slot() + 1);
                })
                .orElse("Published PC");
    }

    private static void handleNiShiftInsert(final NiShiftInsertPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null) {
            return;
        }
        final int slot = payload.slot();
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return;
        }
        // The whole stack as items, the way a chest takes a shift-click; a bucket goes in as a bucket.
        final DataHandoff.ISource source = DataHandoff.inventory(player, slot);
        final int amount = source.get().getCount();
        final IOsHost computer =
                host instanceof IOsHost c ? c : null;
        final Runnable refresh = () -> {
            if (computer != null) {
                sendNetworkInteractor(player, level, computer);
            }
        };
        if (payload.target() == NiShiftInsertPayload.TARGET_STORAGE) {
            if (DataHandoff.intoLocalStore(host.localStore(), player, source, amount, false)
                    == DataHandoff.Outcome.DEPOSITED) {
                refresh.run();
            }
            return;
        }
        // Network: push the stack into the network over ticks, returning any overflow to the player.
        if (host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        DataHandoff.intoNetwork(mainframe, level, host.networkUuid(), player, source, amount, false,
                host.originLabel(MoveLabels.INTERACTOR), refresh);
    }

    private static void handleNiGridClick(final NiGridClickPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null || payload.amount() <= 0L) {
            return;
        }
        // Clamp the client-supplied amount so a spoofed packet cannot ask the dispatcher for Long.MAX.
        final long safeAmount = Math.min(payload.amount(), Integer.MAX_VALUE);
        final StorageKey key = payload.key();
        if (payload.mode() == NiGridClickPayload.MODE_NET_TO_LOCAL) {
            // Pull from the network into this computer's local storage, exactly like the terminal SELECT.
            final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
            if (mainframe == null) {
                return;
            }
            final var op = mainframe.submitNetworkSelect(key, safeAmount, host.localStorage(),
                    host.originLabel(MoveLabels.INTERACTOR));
            if (op != null) {
                op.setPriority(payload.priority());
                op.abortWhen(gone(host));
            }
            if (op != null && host instanceof dev.jstech.computers.os
                    .IOsHost computer) {
                op.onSettle(() -> sendNetworkInteractor(player, level, computer));
            }
        } else if (payload.mode() == NiGridClickPayload.MODE_LOCAL_TO_NET) {
            // Upload from this computer's local storage into the network (Storage popup "TO NETWORK").
            final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
            if (mainframe == null) {
                return;
            }
            final long taken = host.localStore().extract(key,
                    Math.min(safeAmount, host.localStore().count(key)));
            if (taken <= 0L) {
                return;
            }
            final var op = mainframe.submitNetworkInsert(key, taken, host.originLabel(MoveLabels.INTERACTOR));
            if (op == null) {
                host.localStore().insert(key, taken); // no live dispatcher: put it straight back
                return;
            }
            op.setPriority(payload.priority());
            op.onSettle(() -> {
                final long leftover = op.leftover();
                if (leftover > 0L) {
                    host.localStore().insert(key, leftover);
                }
                if (host instanceof dev.jstech.computers.os
                        .IOsHost computer) {
                    sendNetworkInteractor(player, level, computer);
                }
            });
        } else if (!key.isItem()) {
            return; // a fluid or chemical cannot be held in the inventory
        } else {
            // Withdraw from local storage into the player's inventory (terminal Storage-tab withdraw).
            final int maxStack = Math.max(1, key.stack(1).getMaxStackSize());
            final long want = Math.min(safeAmount, host.localStore().count(key));
            final long toWithdraw = Math.min(want, inventoryRoomFor(player, key, maxStack));
            if (toWithdraw > 0L) {
                long remaining = host.localStore().extract(key, toWithdraw);
                while (remaining > 0L) {
                    final int batch = (int) Math.min(remaining, maxStack);
                    final ItemStack out = key.stack(batch);
                    player.getInventory().add(out);
                    final int placed = batch - out.getCount();
                    remaining -= placed;
                    if (placed <= 0) {
                        break;
                    }
                }
                if (remaining > 0L) {
                    host.localStore().insert(key, remaining);
                }
            }
            if (host instanceof dev.jstech.computers.os
                    .IOsHost computer) {
                sendNetworkInteractor(player, level, computer);
            }
        }
    }

    /**
     * Deposits the player's held cursor stack into the network (Network tab) or the host's local
     * storage (Storage tab), the desktop equivalent of the MC-NET terminal's deposit. A left-click
     * pushes the whole stack; a right-click pushes one. Whatever does not fit is returned.
     */
    private static void handleNiDeposit(final NiDepositPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null) {
            return;
        }
        /*
         * Left click deposits the whole stack as items; a right-click hands over ONE: one item, or what a
         * held container holds, and a held empty container over a fluid or chemical entry fills from it.
         */
        final DataHandoff.ISource source = DataHandoff.cursor(player);
        final boolean one = !payload.whole();
        final int amount = one ? 1 : source.get().getCount();
        final boolean fill = one && payload.entry().isPresent()
                && DataContainers.canTake(source.get(), payload.entry().get());
        final IOsHost computer =
                host instanceof IOsHost c ? c : null;
        final Runnable refresh = () -> {
            if (computer != null) {
                sendNetworkInteractor(player, level, computer);
            }
        };
        if (payload.target() == NiDepositPayload.TARGET_STORAGE) {
            final DataHandoff.Outcome outcome = fill
                    ? DataHandoff.fillFromLocalStore(host.localStore(), player, source, payload.entry().get())
                    : DataHandoff.intoLocalStore(host.localStore(), player, source, amount, one);
            if (outcome == DataHandoff.Outcome.DEPOSITED || outcome == DataHandoff.Outcome.FILLED) {
                refresh.run();
            }
            return;
        }
        // Network: the handoff runs over ticks and the view refreshes when it settles.
        if (host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        if (fill) {
            DataHandoff.fillFromNetwork(mainframe, level, host.networkUuid(), player, source,
                    payload.entry().get(), host.originLabel(MoveLabels.INTERACTOR), refresh);
        } else {
            DataHandoff.intoNetwork(mainframe, level, host.networkUuid(), player, source, amount, one,
                    host.originLabel(MoveLabels.INTERACTOR), refresh);
        }
    }

    private static void handleNiCraft(final NiCraftPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null || payload.amount() <= 0L
                || payload.result().isEmpty()) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        // Clamp the client-supplied quantity so a spoofed packet cannot ask the dispatcher for Long.MAX.
        final long safeAmount = Math.max(1L, Math.min(payload.amount(), Integer.MAX_VALUE));
        final IOsHost computer =
                host instanceof dev.jstech.computers.os
                        .IOsHost c ? c : null;
        final Runnable refreshNi = () -> {
            if (computer != null) {
                sendNetworkInteractor(player, level, computer);
            }
        };
        /*
         * The shared entry point runs a machine or multi-stage recipe directly, else plans a recursive
         * craft; refreshNi resends the Network Interactor now and again when the operation settles.
         */
        mainframe.submitCraftRequest(StorageKey.of(payload.result()), safeAmount, true,
                host.originLabel(MoveLabels.INTERACTOR), refreshNi);
        refreshNi.run();
    }

    private static void handleNetworkInteractor(final NetworkInteractorPayload payload, final Player player) {
        NetworkInteractorApp.accept(payload);
    }
}
