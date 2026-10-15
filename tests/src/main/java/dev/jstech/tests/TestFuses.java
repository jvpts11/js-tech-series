/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.energy.IFuse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Two fuses of the test mod's own, to prove the Core's protection slot: one that breaks a line one class above its
 * port, and one that breaks any; each blows into a piece of flint.
 */
public final class TestFuses {

    /** A fuse that breaks one class above its port. */
    public static final ItemEntry<FuseItem> ONE_CLASS = TestSounds.CONTENT.item("rewirable_fuse",
            properties -> new FuseItem(properties, 1)).named("Test Rewirable Fuse").look(IItemLook.HANDMADE)
            .tab(TestItems.ITEMS).register();
    /** A fuse that breaks any overvoltage. */
    public static final ItemEntry<FuseItem> ANY_CLASS = TestSounds.CONTENT.item("hrc_fuse",
            properties -> new FuseItem(properties, IFuse.ANY)).named("Test HRC Fuse").look(IItemLook.HANDMADE)
            .tab(TestItems.ITEMS).register();

    private TestFuses() {
    }

    /** Declares the fuses, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** A fuse that breaks so many classes above its port and blows into flint. */
    public static final class FuseItem extends Item implements IFuse {

        private final int breaks;

        public FuseItem(final Properties properties, final int breaks) {
            super(properties);
            this.breaks = breaks;
        }

        @Override
        public int breaks(final ItemStack fuse) {
            return this.breaks;
        }

        @Override
        public ItemStack blown(final ItemStack fuse) {
            return new ItemStack(Items.FLINT);
        }
    }
}
