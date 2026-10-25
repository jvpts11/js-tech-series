/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a storage card in a server's expansion slot holds: the Vaultis Array joins the server's drives into one volume,
 * in the RAID mode set in the server's firmware and kept on the card itself, so the card carries its array's settings
 * wherever it goes; the Vaultis Cache cuts the read latency of the server's drives.
 */
public final class StorageCards {

    private StorageCards() {
    }

    /** The kind of storage card in {@code stack}, or null when it holds none. */
    public static @Nullable ExpansionCardKind kindOf(final ItemStack stack) {
        final StorageCardSpec card = spec(stack);
        return card == null ? null : card.kind();
    }

    /** How much of its drives' read latency the card in {@code stack} takes away, 0 for anything but a cache. */
    public static int readCutPercent(final ItemStack stack) {
        final StorageCardSpec card = spec(stack);
        return card == null ? 0 : card.readCutPercent();
    }

    /** The RAID mode set on the controller in {@code stack}, {@link RaidMode#NONE} when unset or not a controller. */
    public static RaidMode raidMode(final ItemStack stack) {
        if (kindOf(stack) != ExpansionCardKind.RAID_CONTROLLER) {
            return RaidMode.NONE;
        }
        // An unset or unknown mode degrades to independent volumes.
        return RaidMode.byName(stack.get(ComputingComponents.RAID_MODE.get()));
    }

    /** Writes a RAID mode onto the controller in {@code stack}; nothing for any other item. */
    public static void setRaidMode(final ItemStack stack, final RaidMode mode) {
        if (kindOf(stack) == ExpansionCardKind.RAID_CONTROLLER) {
            stack.set(ComputingComponents.RAID_MODE.get(), mode.serializedName());
        }
    }

    private static @Nullable StorageCardSpec spec(final ItemStack stack) {
        return HardwareRegistry.card(stack) instanceof StorageCardSpec card ? card : null;
    }
}
