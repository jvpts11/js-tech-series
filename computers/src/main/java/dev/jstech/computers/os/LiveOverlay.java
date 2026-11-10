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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The disk in memory a live medium's desktop runs off (Linux Mint's, computers/docs/design/operating-systems.md): the
 * system as its installer would lay it down, the medium's Install icon on it, and nothing of it on any disk of the
 * machine. Whatever the session writes is written here, and goes when the machine stops, as a live session's does.
 */
public final class LiveOverlay {

    /** The installer a live desktop offers on its wallpaper, the medium's own program. */
    public static final ResourceLocation INSTALLER =
            ResourceLocation.fromNamespaceAndPath("jsc", "live_installer");

    private LiveOverlay() {
    }

    /**
     * The disk the live desktop of {@code system} runs off on a machine of {@code era}: the largest disk of that era,
     * standing for the memory the session lives in, with the system on it; empty when no disk of that era holds it.
     */
    public static ItemStack build(final OsDef system, final Era era) {
        final ItemStack[] held = {largestDisk(era)};
        if (held[0].isEmpty()
                || !OsDisks.installOs(1, slot -> held[0], (stack, slot) -> held[0] = stack, system.id(), 0)) {
            return ItemStack.EMPTY;
        }
        final ComputerConsoleState console = new ComputerConsoleState();
        final CompoundTag saved = held[0].get(ComputingComponents.DISK_CONSOLE.get());
        if (saved != null) {
            console.load(saved);
        }
        console.install(INSTALLER.toString());
        final CompoundTag written = new CompoundTag();
        console.save(written);
        held[0].set(ComputingComponents.DISK_CONSOLE.get(), written);
        return held[0];
    }

    /* The disk of that era that holds the most, which a session in memory is given to work in. */
    private static ItemStack largestDisk(final Era era) {
        Item best = null;
        long most = -1L;
        for (final Item item : HardwareRegistry.parts()) {
            final DiskSpec spec = HardwareRegistry.disk(item);
            if (spec != null && spec.era().equals(era) && spec.capacityItems() > most) {
                best = item;
                most = spec.capacityItems();
            }
        }
        return best == null ? ItemStack.EMPTY : new ItemStack(best);
    }
}
