/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The tools the series' mods share, so a mod used alone has them: the Basic Wrench, which sets the ends of a line, in
 * the common wrench tag every mod's machines know.
 */
public final class SharedTools {

    /** The common tag of wrenches, by which other mods' blocks know one. */
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    /** The Basic Wrench. */
    public static final ItemEntry<BasicWrenchItem> WRENCH = CoreItems.CONTENT.item("basic_wrench",
                    properties -> new BasicWrenchItem(properties.stacksTo(1))).named("Basic Wrench")
            .tag(WRENCHES).tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    static {
        // Every mod of the series sets its lines' ends with it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, WRENCH::get);
    }

    private SharedTools() {
    }

    /** Declares the tools, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }
}
