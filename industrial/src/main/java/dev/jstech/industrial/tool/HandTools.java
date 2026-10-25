/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.metallurgy.FormRoute;
import java.util.List;

/**
 * The hand tools of the start, so the first plates, wire and dust never wait on a machine. How long each lasts is an
 * estimate, to be tuned in playtesting.
 */
public final class HandTools {

    /** The Forge Hammer's uses. */
    public static final int HAMMER_USES = 128;
    /** The Draw Plate's uses. */
    public static final int DRAW_PLATE_USES = 128;
    /** The Mortar's uses. */
    public static final int MORTAR_USES = 64;
    /** How hard a blow of the Forge Hammer harms, and how many it gives a second: a heavy head, swung slowly. */
    public static final double HAMMER_DAMAGE = 7;
    public static final double HAMMER_SPEED = 0.9;

    public static final ItemEntry<HandTool> FORGE_HAMMER = IndustrialModule.CONTENT.item("forge_hammer",
                    properties -> new HandTool(properties, FormRoute.ByHand.FORGE_HAMMER))
            .named("Forge Hammer").look(IItemLook.HELD).durability(HAMMER_USES)
            .strikes(HAMMER_DAMAGE, HAMMER_SPEED).tab(IndustrialModule.TOOLS).register();
    public static final ItemEntry<HandTool> DRAW_PLATE = IndustrialModule.CONTENT.item("draw_plate",
                    properties -> new HandTool(properties, FormRoute.ByHand.DRAW_PLATE))
            .named("Draw Plate").look(IItemLook.HELD).durability(DRAW_PLATE_USES).tab(IndustrialModule.TOOLS)
            .register();
    public static final ItemEntry<HandTool> MORTAR = IndustrialModule.CONTENT.item("mortar",
                    properties -> new HandTool(properties, FormRoute.ByHand.MORTAR))
            .named("Mortar").look(IItemLook.HELD).durability(MORTAR_USES).tab(IndustrialModule.TOOLS).register();

    private HandTools() {
    }

    /** Declares the tools, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every hand tool, in the order the tab shows them. */
    public static List<ItemEntry<HandTool>> all() {
        return List.of(FORGE_HAMMER, DRAW_PLATE, MORTAR);
    }
}
