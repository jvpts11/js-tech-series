/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import net.minecraft.world.item.ItemStack;

/**
 * What a mod adds to items' tooltips through the Core ({@link ItemTooltips#add}): lines, bars and pictures, some
 * shown always, some only with Shift held (what the item does) or with Ctrl held (its numbers). It is asked for
 * every item whose tooltip is shown, and adds nothing to the items it has nothing to say of.
 */
@FunctionalInterface
public interface ITooltipSection {

    /** Adds to the tooltip of {@code stack} what the mod says of it. */
    void describe(ItemStack stack, ItemTooltip tooltip);
}
