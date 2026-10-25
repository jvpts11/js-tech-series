/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Disks with a system already on them, as a computer comes from the shop: put in a machine of the system's era, it
 * comes up at once, the machine named "My Computer". They are the creative tabs' shortcut past the installer, one for
 * every system, each on the biggest disk of the system's own era it fits on.
 */
@TextHolder
public final class ReadyDisks {

    /** The name the machine goes by until its owner gives it one. */
    public static final TextKey MY_COMPUTER = TextKey.of("jsc.ready_disk.my_computer", "My Computer");

    private ReadyDisks() {
    }

    /** A disk of {@code era} with the system {@code osId} on it, or an empty stack when no disk of the era fits it. */
    public static ItemStack withSystem(final ResourceLocation osId, final Era era) {
        final Item disk = biggestOf(era);
        if (disk == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack[] held = {new ItemStack(disk)};
        if (!OsDisks.installOs(1, slot -> held[0], (stack, slot) -> held[0] = stack, osId, 0)) {
            return ItemStack.EMPTY;
        }
        // The machine's name rides on its system disk with the rest of what the system keeps.
        final ComputerConsoleState console = new ComputerConsoleState();
        console.setComputerName(MY_COMPUTER.text().english());
        final CompoundTag saved = new CompoundTag();
        console.save(saved);
        held[0].set(ComputingComponents.DISK_CONSOLE.get(), saved);
        return held[0];
    }

    /* The disk of that era that holds the most, which leaves the most room once the system is on it. */
    @Nullable
    private static Item biggestOf(final Era era) {
        Item best = null;
        long most = 0L;
        for (final Item item : BuiltInRegistries.ITEM) {
            final DiskSpec disk = HardwareRegistry.disk(item);
            if (disk != null && disk.era() == era && (best == null || disk.capacityItems() > most)) {
                best = item;
                most = disk.capacityItems();
            }
        }
        return best;
    }
}
