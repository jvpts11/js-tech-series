/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.SpaceContent;

/**
 * What keeps a suit going between airlocks: the oxygen bottle that refills its tanks, and the patch that mends a hole.
 * They follow the suits and their modules in the tab's row of what keeps a crew alive.
 */
@TextHolder
public final class SuitSupplies {

    public static final TextKey AIR_IN_THE_BOTTLE = TextKey.of("jsspace.oxygen_bottle.air", "Air: %s");
    /** Half an hour of breathing, poured into a suit's tanks. */
    public static final ItemEntry<OxygenBottleItem> OXYGEN_BOTTLE = SpaceContent.CONTENT.item("oxygen_bottle",
            OxygenBottleItem::new).named("Oxygen Bottle").tab(LunarSuit.LIFE_SUPPORT).register();
    /** A patch that mends a hole in a suit. */
    public static final ItemEntry<SuitPatchItem> SUIT_PATCH = SpaceContent.CONTENT.item("suit_patch",
            SuitPatchItem::new).named("Suit Patch").tab(LunarSuit.LIFE_SUPPORT).register();

    private SuitSupplies() {
    }

    /** Declares the items, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }
}
