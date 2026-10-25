/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * The sheet of a part a test names, read from the hardware registry as the game reads it: a test that names a part
 * expects one, so a missing entry fails loudly, naming the item, rather than as a null further on.
 */
public final class PartSheets {

    private PartSheets() {
    }

    public static CpuSpec cpu(final ItemLike item) {
        return required(HardwareRegistry.cpu(stack(item)), item, "processor");
    }

    public static GpuSpec gpu(final ItemLike item) {
        return required(HardwareRegistry.gpu(stack(item)), item, "graphics card");
    }

    public static RamSpec ram(final ItemLike item) {
        return required(HardwareRegistry.ram(stack(item)), item, "memory module");
    }

    public static DiskSpec disk(final ItemLike item) {
        return required(HardwareRegistry.disk(stack(item)), item, "disk");
    }

    public static MotherboardSpec board(final ItemLike item) {
        return required(HardwareRegistry.board(stack(item)), item, "motherboard");
    }

    public static PsuSpec psu(final ItemLike item) {
        return required(HardwareRegistry.psu(stack(item)), item, "power supply");
    }

    public static IExpansionCardSpec card(final ItemLike item) {
        return required(HardwareRegistry.card(stack(item)), item, "expansion card");
    }

    private static ItemStack stack(final ItemLike item) {
        return new ItemStack(item);
    }

    private static <T> T required(final @Nullable T sheet, final ItemLike item, final String what) {
        if (sheet == null) {
            throw new IllegalStateException(BuiltInRegistries.ITEM.getKey(item.asItem()) + " is no " + what);
        }
        return sheet;
    }
}
