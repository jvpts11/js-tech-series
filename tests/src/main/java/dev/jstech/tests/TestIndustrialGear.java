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
import dev.jstech.core.jstechseries.SeriesModules;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.module.ModuleDef;
import dev.jstech.core.module.ModuleHostDef;
import dev.jstech.core.module.Modules;
import dev.jstech.industrial.tool.IndustrialModules;
import dev.jstech.industrial.tool.NoEnchanting;
import dev.jstech.industrial.tool.ToolKinds;
import java.util.List;
import net.minecraft.world.item.Item;

/**
 * J's Industrial's modules and the exo-suit's contract, on items of the test mod's own: an open frame's chest of the
 * T3 and a sealed suit's chest of the T5, a sealed helmet; a module any armour takes, at most two in one piece; a
 * head's filter; a space module, which only a sealed suit takes; and a sword of the mod's sword kind, never enchanted,
 * a tool of the T2.
 */
public final class TestIndustrialGear {

    /** At most this many plates in one piece. */
    public static final int MOST_PLATES = 2;

    public static final ItemEntry<Item> OPEN_CHEST = item("test_open_chest", "Test open frame chest");
    public static final ItemEntry<Item> SEALED_CHEST = item("test_sealed_chest", "Test sealed suit chest");
    public static final ItemEntry<Item> SEALED_HELMET = item("test_sealed_helmet", "Test sealed suit helmet");
    public static final ItemEntry<Item> PLATE = item("test_armour_plate", "Test armour plate module");
    public static final ItemEntry<Item> FILTER = item("test_filter_module", "Test filter module");
    public static final ItemEntry<Item> SPACE = item("test_space_module", "Test space module");
    public static final ItemEntry<Item> SWORD = TestSounds.CONTENT.item("test_industrial_sword", Item::new)
            .named("Test industrial sword").look(IItemLook.HANDMADE).tag(ToolKinds.SWORDS.items())
            .tag(NoEnchanting.TAG).tab(TestItems.ITEMS).register();

    public static final ModuleHostDef OPEN_CHEST_DEF = IndustrialModules.armour(OPEN_CHEST.getId(), IndustrialTier.T3,
            SeriesModules.CHEST, false);
    public static final ModuleHostDef SEALED_CHEST_DEF = IndustrialModules.armour(SEALED_CHEST.getId(),
            IndustrialTier.T5, SeriesModules.CHEST, true);
    public static final ModuleHostDef SEALED_HELMET_DEF = IndustrialModules.armour(SEALED_HELMET.getId(),
            IndustrialTier.T5, SeriesModules.HEAD, true);
    public static final ModuleHostDef SWORD_DEF = IndustrialModules.tool(SWORD.getId(), IndustrialTier.T2);
    public static final ModuleDef PLATE_DEF = Modules.declare(PLATE.getId(),
            IndustrialModules.module(SeriesModules.ARMOUR, 0, List.of()).upTo(MOST_PLATES));
    public static final ModuleDef FILTER_DEF = Modules.declare(FILTER.getId(),
            IndustrialModules.module(SeriesModules.ARMOUR, 0, List.of()).needing(SeriesModules.HEAD));
    public static final ModuleDef SPACE_DEF = Modules.declare(SPACE.getId(),
            IndustrialModules.module(SeriesModules.ARMOUR, 0, List.of()).needing(SeriesModules.SEALED));

    private TestIndustrialGear() {
    }

    /** Declares the items, hosts and modules, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ItemEntry<Item> item(final String id, final String english) {
        return TestSounds.CONTENT.item(id, Item::new).named(english).look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
                .register();
    }
}
