/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.terminal;

import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.peripheral.IPeripheralOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the computer a terminal payload is about from the monitor and host positions it names.
 */
public final class TerminalHosts {

    private TerminalHosts() {
    }

    static IComputerTerminalHost openTerminal(final ServerPlayer player, final BlockPos monitorPos,
                                              final BlockPos hostPos) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu
                && menu.monitorPos().equals(monitorPos)
                && menu.hostPos().equals(hostPos)
                && player.level().getBlockEntity(hostPos) instanceof IComputerTerminalHost host) {
            return host;
        }
        return null;
    }

    /**
     * The computer a payload gated onto {@code menu} is about, resolved fresh from where the menu says it is: the
     * gate already proved the menu is open and stands on the block the payload names, so nothing here re-checks that.
     * The payload's own {@code monitorPos} is still checked against the menu's, so a payload that names a foreign
     * monitor while riding a real terminal's gate is refused rather than acted on.
     */
    @Nullable
    static IComputerTerminalHost hostOf(final ComputerTerminalMenu menu, final BlockPos monitorPos,
                                        final ServerLevel level) {
        if (!menu.monitorPos().equals(monitorPos)) {
            return null;
        }
        return level.getBlockEntity(menu.hostPos()) instanceof IComputerTerminalHost host ? host : null;
    }

    /**
     * The item source a deposit payload names: the cursor when {@code slotIndex} is one of the two cursor markers,
     * otherwise the menu slot at that index, or {@code null} when the index is outside the menu. Every slot in a
     * terminal menu belongs to the player's inventory, so any in-range slot is a legitimate source.
     */
    @Nullable
    static DataHandoff.ISource sourceOf(final ComputerTerminalMenu menu, final ServerPlayer player,
                                        final int slotIndex, final int cursor, final int cursorOne) {
        if (slotIndex == cursor || slotIndex == cursorOne) {
            return DataHandoff.cursor(player);
        }
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return null;
        }
        return DataHandoff.slot(menu.getSlot(slotIndex), player);
    }

    /**
     * Resolves the computer for a craft request that may come from the MC-NET terminal (its container menu) OR
     * the desktop Network Interactor (no menu, authenticated by proximity to a linked monitor). Tries the
     * terminal first, then the NI host, so the shared craft flow works from both.
     */
    public static IComputerTerminalHost craftHost(final ServerPlayer player, final ServerLevel level,
                                                  final BlockPos monitorPos, final BlockPos hostPos) {
        final IComputerTerminalHost terminal = openTerminal(player, monitorPos, hostPos);
        if (terminal != null) {
            return terminal;
        }
        return niHost(player, level, hostPos, monitorPos);
    }

    /**
     * Resolves the host computer for a desktop Network Interactor action, validating the player is within
     * reach of the monitor (the desktop is a client-only Screen with no server menu to authenticate against).
     */
    public static IComputerTerminalHost niHost(
            final ServerPlayer player, final ServerLevel level, final BlockPos hostPos, final BlockPos monitorPos) {
        if (player.distanceToSqr(Vec3.atCenterOf(monitorPos)) > 64.0) {
            return null;
        }
        if (!(level.getBlockEntity(hostPos)
                instanceof IComputerTerminalHost host)) {
            return null;
        }
        /*
         * Anti-spoof: the monitor must actually be a linked peripheral of this host, so a player near any
         * monitor cannot drive a foreign computer by sending that computer's position as the host; and one the host
         * disabled shows it nothing.
         */
        if (!(host instanceof IPeripheralOwner owner)
                || !owner.enabledEndpoints().contains(monitorPos.asLong())) {
            return null;
        }
        return host;
    }

    public static long inventoryRoomFor(final ServerPlayer player, final StorageKey key, final int maxStack) {
        final ItemStack probe = key.stack(1);
        final Inventory inv = player.getInventory();
        long room = 0L;
        for (int i = 0; i < inv.items.size(); i++) {
            final ItemStack slot = inv.items.get(i);
            if (slot.isEmpty()) {
                room += maxStack;
            } else if (ItemStack.isSameItemSameComponents(slot, probe)) {
                room += Math.max(0, maxStack - slot.getCount());
            }
        }
        return room;
    }
}
