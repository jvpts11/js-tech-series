/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.terminal;

import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.peripheral.IPeripheralOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
         * monitor cannot drive a foreign computer by sending that computer's position as the host.
         */
        if (!(host instanceof IPeripheralOwner owner)
                || !owner.linkedEndpoints().contains(monitorPos.asLong())) {
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
