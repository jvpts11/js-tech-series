/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.api.SuitModules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * The suit modules of the skeleton, those the Moon and Mars call for: more air in the tanks, a vest against the sky's
 * radiation, as the AstroRad vest flew on Artemis I, and lamps on the helmet to light the dark. The rest come with the
 * worlds that call for them.
 */
public final class SuitModuleItems {

    /** The tab's row of what keeps a crew alive. */
    public static final ContentTab.Section LIFE_SUPPORT = SpaceContent.TAB.section();

    /** A second oxygen bottle for the pack: half as much air again. */
    public static final ItemEntry<SuitModuleItem> EXTRA_OXYGEN = SpaceContent.CONTENT.item("extra_oxygen",
            SuitModuleItem::new).named("Extra Oxygen").tab(LIFE_SUPPORT).register();
    /** A vest of polyethylene, rich in hydrogen, worn under the suit: it stops half the sky's radiation. */
    public static final ItemEntry<SuitModuleItem> RADIATION_VEST = SpaceContent.CONTENT.item("radiation_vest",
            SuitModuleItem::new).named("Radiation Vest").tab(LIFE_SUPPORT).register();
    /** Two lamps on the helmet's sides, lighting the way ahead of whoever wears it. */
    public static final ItemEntry<SuitModuleItem> SUIT_LAMPS = SpaceContent.CONTENT.item("suit_lamps",
            SuitModuleItem::new).named("Suit Lamps").tab(LIFE_SUPPORT).register();

    /* How much each gives: a share more air, the share of radiation stopped, the light given. */
    private static final double MORE_AIR = 0.5;
    private static final double STOPPED = 0.5;
    private static final double LIGHT = 14;

    private SuitModuleItems() {
    }

    /** Declares the modules, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        module("extra_oxygen", SuitModule.Kind.AIR, EquipmentSlot.CHEST, MORE_AIR);
        module("radiation_vest", SuitModule.Kind.RADIATION, EquipmentSlot.CHEST, STOPPED);
        module("suit_lamps", SuitModule.Kind.LIGHT, EquipmentSlot.HEAD, LIGHT);
    }

    private static void module(final String id, final SuitModule.Kind kind, final EquipmentSlot piece,
                               final double amount) {
        SuitModules.declare(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id),
                new SuitModule("item." + JsSpace.MODID + "." + id, kind, piece, amount));
    }
}
