/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.operation.payload.terminal.TerminalHosts;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A portable's network app on the server (computers/docs/design/wireless.md): a simpler Network Interactor on the
 * network of the access point that reaches its player. A request puts the items into the device's own storage, never
 * straight into the inventory; from there they are taken into the inventory or returned to the network. The
 * inventory is deposited row by row or all at once, and the network crafts and plans as it does for a computer.
 * Every answer is the app's whole view again, sent when the work has settled.
 */
public final class PortableNetwork {

    /* The rows of the inventory the Deposit lists, the hotbar's with them; never what is worn. */
    private static final int LAST_ROW_SLOT = Inventory.INVENTORY_SIZE;

    private PortableNetwork() {
    }

    /** Sends {@code player} what the network app of the device {@code device} shows now. */
    public static void view(final ServerPlayer player, final ServerLevel level, final UUID device,
                            final PortableState state) {
        PacketDistributor.sendToPlayer(player, viewOf(level, device, state));
    }

    /** What the network app of the device {@code device} shows now. */
    public static PortableNetworkPayload viewOf(final ServerLevel level, final UUID device,
                                                final PortableState state) {
        final NetworkUuid network = networkOf(level, device);
        final List<NetworkItemEntry> stock = new ArrayList<>();
        int types = 0;
        final MainframeBlockEntity mainframe = network == null ? null : NetworkLookup.resolveMainframe(level, network);
        if (network != null) {
            final Map<StorageKey, Long> totals = NetworkStorage.of(level, network).query();
            types = totals.size();
            for (final Map.Entry<StorageKey, Long> entry : totals.entrySet()) {
                if (stock.size() >= PortableNetworkPayload.MOST_STOCK) {
                    break;
                }
                stock.add(new NetworkItemEntry(entry.getKey(), entry.getValue()));
            }
        }
        final List<NetworkItemEntry> stored = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> entry : state.stored().entrySet()) {
            stored.add(new NetworkItemEntry(entry.getKey(), entry.getValue()));
        }
        return new PortableNetworkPayload(device, network == null ? "" : ShortId.of(network.asString()),
                mainframe != null, types, stock, stored, state.freeItems(), PortableStorage.items(state.model()),
                network == null ? List.of() : CraftingPayloads.buildCraftCatalog(level, network),
                mainframe == null ? List.of() : mainframe.activeOperationRecords());
    }

    /** Requests {@code amount} of {@code key} from the network into the device's own storage. */
    public static void request(final ServerPlayer player, final ServerLevel level, final UUID device,
                               final PortableState state, final StorageKey key, final long amount) {
        final MainframeBlockEntity mainframe = mainframeOf(level, device);
        final long wanted = Math.min(Math.min(amount, Integer.MAX_VALUE), state.freeItems());
        if (mainframe == null || wanted <= 0L || !key.isItem()) {
            return;
        }
        final PortableDevices devices = PortableDevices.get(level.getServer());
        final NetworkSelectOperation operation = mainframe.networkOperations().pull(key, wanted,
                new Sink(devices, device), label(device, state.model()));
        if (operation != null) {
            // A device erased while its items were on their way has nowhere left for them.
            operation.abortWhen(() -> devices.find(device) == null);
            operation.onSettle(() -> refresh(player, level, device));
            JscEvents.award(player, JscEvents.PORTABLE_REQUEST);
        }
    }

    /** Takes {@code amount} of {@code key} from the device's storage into the player's inventory, as it has room. */
    public static void take(final ServerPlayer player, final ServerLevel level, final UUID device,
                            final StorageKey key, final long amount) {
        if (!key.isItem()) {
            return;
        }
        final PortableDevices devices = PortableDevices.get(level.getServer());
        final int most = Math.max(1, key.stack(1).getMaxStackSize());
        final long room = TerminalHosts.inventoryRoomFor(player, key, most);
        long left = devices.extract(device, key, Math.min(Math.min(amount, Integer.MAX_VALUE), room));
        while (left > 0L) {
            final ItemStack out = key.stack((int) Math.min(left, most));
            final int batch = out.getCount();
            player.getInventory().add(out);
            final int placed = batch - out.getCount();
            left -= placed;
            if (placed <= 0) {
                break;
            }
        }
        if (left > 0L) {
            devices.insert(device, key, left);
        }
        refresh(player, level, device);
    }

    /** Returns {@code amount} of {@code key} from the device's storage to the network. */
    public static void giveBack(final ServerPlayer player, final ServerLevel level, final UUID device,
                                final PortableState state, final StorageKey key, final long amount) {
        final MainframeBlockEntity mainframe = mainframeOf(level, device);
        if (mainframe == null) {
            return;
        }
        final PortableDevices devices = PortableDevices.get(level.getServer());
        final long taken = devices.extract(device, key, Math.min(amount, Integer.MAX_VALUE));
        if (taken <= 0L) {
            return;
        }
        final NetworkInsertOperation operation = mainframe.networkOperations().push(key, taken,
                label(device, state.model()));
        if (operation == null) {
            devices.insert(device, key, taken);
            return;
        }
        operation.onSettle(() -> {
            // What the network could not take comes back to the device; nothing is lost on the way.
            if (operation.leftover() > 0L) {
                devices.insert(device, key, operation.leftover());
            }
            refresh(player, level, device);
        });
    }

    /** Deposits what is in the inventory's slot {@code slot} into the network, as the Interactor deposits a row. */
    public static void deposit(final ServerPlayer player, final ServerLevel level, final UUID device,
                               final PortableState state, final int slot) {
        final MainframeBlockEntity mainframe = mainframeOf(level, device);
        final NetworkUuid network = networkOf(level, device);
        if (mainframe == null || network == null || slot < 0 || slot >= LAST_ROW_SLOT
                || player.getInventory().getItem(slot).getItem() instanceof PortableItem) {
            return;
        }
        final DataHandoff.ISource source = DataHandoff.inventory(player, slot);
        DataHandoff.intoNetwork(mainframe, level, network, player, source, source.get().getCount(), false,
                label(device, state.model()), () -> refresh(player, level, device));
    }

    /** Deposits every row of the inventory the Deposit lists into the network, a row at a time; never a portable. */
    public static void depositAll(final ServerPlayer player, final ServerLevel level, final UUID device,
                                  final PortableState state) {
        for (int slot = 0; slot < LAST_ROW_SLOT; slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) {
                deposit(player, level, device, state, slot);
            }
        }
    }

    /** Crafts {@code amount} of {@code key} on the network, as the Interactor's craft does. */
    public static void craft(final ServerPlayer player, final ServerLevel level, final UUID device,
                             final PortableState state, final StorageKey key, final long amount) {
        final MainframeBlockEntity mainframe = mainframeOf(level, device);
        if (mainframe == null || amount <= 0L) {
            return;
        }
        final long safe = Math.min(amount, Integer.MAX_VALUE);
        mainframe.networkOperations().craft(CraftRequest.of(key, safe, true, label(device, state.model()),
                () -> refresh(player, level, device)));
        refresh(player, level, device);
    }

    /** Sends how the network would craft {@code amount} of {@code key}, by its first recipe. */
    public static void plan(final ServerPlayer player, final ServerLevel level, final UUID device,
                            final StorageKey key, final long amount) {
        final NetworkUuid network = networkOf(level, device);
        if (network != null && key.isItem()) {
            CraftingPayloads.sendPlan(player, level, network, key.stack(1), Math.max(1L, amount), -1, count -> 0);
        }
    }

    /* The network the device is joined to now, or null when no access point reaches it. */
    @Nullable
    private static NetworkUuid networkOf(final ServerLevel level, final UUID device) {
        final PortableLinks.Joined joined = PortableLinks.of(level, device);
        return joined == null ? null : joined.network();
    }

    @Nullable
    private static MainframeBlockEntity mainframeOf(final ServerLevel level, final UUID device) {
        final NetworkUuid network = networkOf(level, device);
        return network == null ? null : NetworkLookup.resolveMainframe(level, network);
    }

    /* Sends the view again, when the device is still kept. */
    private static void refresh(final ServerPlayer player, final ServerLevel level, final UUID device) {
        final PortableState state = PortableDevices.get(level.getServer()).find(device);
        if (state != null && !player.hasDisconnected()) {
            view(player, level, device, state);
        }
    }

    /* What the network's log and its Operations call the device: its model and short id, through its app. */
    private static String label(final UUID device, final PortableModel model) {
        return MoveLabels.via(model.serializedName() + "-" + ShortId.of(device.toString()), MoveLabels.NETWORK_APP);
    }

    /* The device's own storage, as the end of a request: it takes items, as many as its room holds. */
    private record Sink(PortableDevices devices, UUID device) implements IDataSink {

        @Override
        public long insert(final StorageKey key, final long amount, final boolean simulate) {
            final PortableState state = devices.find(device);
            if (state == null || amount <= 0L || !key.isItem()) {
                return 0L;
            }
            return simulate ? Math.min(amount, state.freeItems()) : devices.insert(device, key, amount);
        }
    }
}
