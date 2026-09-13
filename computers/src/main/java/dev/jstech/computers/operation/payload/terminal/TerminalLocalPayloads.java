/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.terminal;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.LocalStorageSnapshotPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.TerminalDiskPrivacyPayload;
import dev.jstech.computers.operation.payload.TerminalLocalDepositPayload;
import dev.jstech.computers.operation.payload.TerminalLocalUploadPayload;
import dev.jstech.computers.operation.payload.TerminalLocalWithdrawPayload;
import dev.jstech.computers.storage.DataContainers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.inventoryRoomFor;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.openTerminal;
import static dev.jstech.computers.operation.payload.terminal.TerminalPayloads.sendSnapshot;

/**
 * The payloads that move items between a terminal's local storage, the player's inventory and the network.
 */
public final class TerminalLocalPayloads {

    private TerminalLocalPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(LocalStorageSnapshotPayload.TYPE, LocalStorageSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(TerminalLocalPayloads::handleLocalSnapshot));
        ComputerAccess.accept(registrar, TerminalLocalWithdrawPayload.TYPE, TerminalLocalWithdrawPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalLocalWithdrawPayload::hostPos), TerminalLocalPayloads::handleLocalWithdraw);
        ComputerAccess.accept(registrar, TerminalDiskPrivacyPayload.TYPE, TerminalDiskPrivacyPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalDiskPrivacyPayload::hostPos), TerminalLocalPayloads::handleDiskPrivacy);
        ComputerAccess.accept(registrar, TerminalLocalDepositPayload.TYPE, TerminalLocalDepositPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalLocalDepositPayload::hostPos), TerminalLocalPayloads::handleLocalDeposit);
        ComputerAccess.accept(registrar, TerminalLocalUploadPayload.TYPE, TerminalLocalUploadPayload.STREAM_CODEC,
                ComputerAccess.machine(TerminalLocalUploadPayload::hostPos), TerminalLocalPayloads::handleLocalUpload);
    }

    private static void handleLocalUpload(final TerminalLocalUploadPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || host.networkUuid() == null || payload.quantity() <= 0L) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        final StorageKey key = payload.key();
        /*
         * Take the items out of local storage and carry them in the Operation; whatever the network
         * cannot hold is returned to local storage when it settles, so nothing is ever lost.
         */
        final long taken = host.localStore().extract(key,
                Math.min(payload.quantity(), host.localStore().count(key)));
        if (taken <= 0L) {
            return;
        }
        final var op = mainframe.submitNetworkInsert(key, taken, "local");
        if (op == null) {
            host.localStore().insert(key, taken); // no live dispatcher: put it straight back
            return;
        }
        op.onSettle(() -> {
            final long leftover = op.leftover();
            if (leftover > 0L) {
                host.localStore().insert(key, leftover);
            }
            dispatchLocalSnapshot(player, host);
            sendSnapshot(player, level, net);
        });
    }

    // Local storage (the Storage tab): disk-backed, component-preserving quantity view.

    private static void handleLocalSnapshot(final LocalStorageSnapshotPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setLocalItems(payload.items());
            menu.setDiskPrivacy(payload.disks());
        }
    }

    public static void dispatchLocalSnapshot(final ServerPlayer player, final IComputerTerminalHost host) {
        final Map<StorageKey, Long> view = host.localStore().view();
        final List<NetworkItemEntry> entries = new ArrayList<>(
                Math.min(view.size(), LocalStorageSnapshotPayload.MAX_ENTRIES));
        view.entrySet().stream().limit(LocalStorageSnapshotPayload.MAX_ENTRIES)
                .forEach(e -> entries.add(new NetworkItemEntry(e.getKey(), e.getValue())));
        /*
         * Per-disk privacy state for the Storage tab's slider; empty for a host with no slider, which
         * makes the Storage tab show the static "always public" badge instead of a control.
         */
        final List<LocalStorageSnapshotPayload.DiskInfo> disks = new ArrayList<>();
        if (host.storageHasSlider()) {
            final int count = Math.min(host.diskPrivacyDiskCount(), LocalStorageSnapshotPayload.MAX_DISKS);
            for (int i = 0; i < count; i++) {
                disks.add(new LocalStorageSnapshotPayload.DiskInfo(
                        host.diskPrivacyPermille(i), host.diskUsedWeight(i), host.diskCapacityWeight(i)));
            }
        }
        PacketDistributor.sendToPlayer(player, new LocalStorageSnapshotPayload(entries, disks));
    }

    private static void handleLocalWithdraw(final TerminalLocalWithdrawPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || payload.quantity() <= 0L) {
            return;
        }
        final StorageKey key = payload.key();
        if (!key.isItem()) {
            return; // a fluid or chemical cannot be held in the inventory, withdraw it via an Export Bus
        }
        final int maxStack = Math.max(1, key.stack(1).getMaxStackSize());
        /*
         * Take only as much as the player's inventory can actually hold, so a "withdraw all" on a
         * huge stack never extracts more than fits, since items must never be destroyed by overflow.
         */
        final long want = Math.min(payload.quantity(), host.localStore().count(key));
        final long toWithdraw = Math.min(want, inventoryRoomFor(player, key, maxStack));
        if (toWithdraw <= 0L) {
            return;
        }
        long remaining = host.localStore().extract(key, toWithdraw);
        while (remaining > 0L) {
            final int batch = (int) Math.min(remaining, maxStack);
            final ItemStack out = key.stack(batch);
            player.getInventory().add(out); // mutates out to whatever did not fit
            final int placed = batch - out.getCount();
            remaining -= placed;
            if (placed <= 0) {
                break; // inventory unexpectedly full, return the remainder below
            }
        }
        if (remaining > 0L) {
            host.localStore().insert(key, remaining); // belt-and-braces: never lose the remainder
        }
        dispatchLocalSnapshot(player, host);
    }

    private static void handleDiskPrivacy(final TerminalDiskPrivacyPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null) {
            return;
        }
        /*
         * A Server or the Mainframe is always fully public: it carries no slider, so a privacy
         * write to one is a stale or spoofed packet. Warn lightly and ignore it.
         */
        if (!host.storageHasSlider()
                || !(host instanceof PersonalComputerBlockEntity pc)) {
            JsComputers.LOGGER.warn("Ignoring disk-privacy write to a host without a storage slider at {}",
                    payload.hostPos());
            return;
        }
        if (payload.diskIndex() < 0 || payload.diskIndex() >= pc.diskPrivacyDiskCount()) {
            return;
        }
        // Whitelist + clamp: only a value inside the valid per-mille range is ever applied.
        final int permille = dev.jstech.computers.storage.DiskPrivacy
                .clampPermille(payload.permille());
        pc.setDiskPrivacy(payload.diskIndex(), permille); // a no-op + no counter bump if the slot has no disk
        // Refresh the owner's Storage tab so the readout reflects the authoritative value.
        dispatchLocalSnapshot(player, host);
    }

    private static void handleLocalDeposit(final TerminalLocalDepositPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        final IComputerTerminalHost host = openTerminal(player, payload.monitorPos(), payload.hostPos());
        if (host == null || !(player.containerMenu instanceof ComputerTerminalMenu menu)) {
            return;
        }
        final int idx = payload.slotIndex();
        final boolean fromCursor = idx == TerminalLocalDepositPayload.CURSOR
                || idx == TerminalLocalDepositPayload.CURSOR_ONE;
        if (!fromCursor && (idx < menu.storageSlotCount() || idx >= menu.slots.size())) {
            return; // a slot source must be a player-inventory menu slot
        }
        final DataHandoff.ISource source = fromCursor
                ? DataHandoff.cursor(player) : DataHandoff.slot(menu.getSlot(idx), player);
        /*
         * A right-click hands over ONE: one item, or what a held container holds, and a held empty
         * container over a fluid or chemical entry fills from the disks instead. Left click and
         * shift-click deposit the stack as items, the way a chest takes them.
         */
        final boolean one = idx == TerminalLocalDepositPayload.CURSOR_ONE;
        final DataHandoff.Outcome outcome;
        if (one && payload.entry().isPresent() && DataContainers.canTake(source.get(), payload.entry().get())) {
            outcome = DataHandoff.fillFromLocalStore(host.localStore(), player, source, payload.entry().get());
        } else {
            outcome = DataHandoff.intoLocalStore(host.localStore(), player, source,
                    one ? 1 : source.get().getCount(), one);
        }
        if (outcome == DataHandoff.Outcome.DEPOSITED || outcome == DataHandoff.Outcome.FILLED) {
            dispatchLocalSnapshot(player, host);
        }
    }
}
