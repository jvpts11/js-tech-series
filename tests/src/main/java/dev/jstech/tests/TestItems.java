/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.Codec;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The items the tests hold, declared through the Core as any mod's items are: one that holds everything an item can
 * hold (three modes, energy, a fluid and four stacks) and starts with a component of the test mod's own, and one with
 * modes only. They wear the game's own bundle, so the test mod draws nothing.
 */
@TextHolder
public final class TestItems {

    public static final TextKey MODE_SCAN = TextKey.of("jstests.mode.scan", "Scan");
    public static final TextKey MODE_MARK = TextKey.of("jstests.mode.mark", "Mark");
    public static final TextKey MODE_CLEAR = TextKey.of("jstests.mode.clear", "Clear");
    public static final ItemMode SCAN = ItemMode.of("scan", MODE_SCAN);
    public static final ItemMode MARK = ItemMode.of("mark", MODE_MARK);
    public static final ItemMode CLEAR = ItemMode.of("clear", MODE_CLEAR);
    public static final int ENERGY = 10_000;
    public static final int ENERGY_IN = 500;
    public static final int ENERGY_OUT = 400;
    public static final int FLUID = 4_000;
    public static final int SLOTS = 4;
    public static final int MARK_VALUE = 7;

    /** A number of the test mod's own, which the full tool starts with. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MARK_COMPONENT =
            TestSounds.CONTENT.component("mark", Codec.INT, ByteBufCodecs.VAR_INT);

    public static final ContentTab TAB = TestSounds.CONTENT.tab("tests", "Test Items", () -> TestItems.FULL);
    private static final ContentTab.Section ITEMS = TAB.section();

    /** Everything an item can hold. */
    public static final ItemEntry<Item> FULL = TestSounds.CONTENT.item("full_tool", Item::new)
            .named("Full Test Tool").look(IItemLook.HANDMADE).tab(ITEMS)
            .modes(SCAN, MARK, CLEAR).holdsEnergy(ENERGY, ENERGY_IN, ENERGY_OUT).holdsFluid(FLUID).holdsItems(SLOTS)
            .component(MARK_COMPONENT, MARK_VALUE)
            .register();

    /** Modes and nothing else, so it still stacks. */
    public static final ItemEntry<Item> MODES_ONLY = TestSounds.CONTENT.item("modes_tool", Item::new)
            .named("Modes Test Tool").look(IItemLook.HANDMADE).tab(ITEMS)
            .modes(SCAN, MARK)
            .register();

    private TestItems() {
    }

    /** Declares the items, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
