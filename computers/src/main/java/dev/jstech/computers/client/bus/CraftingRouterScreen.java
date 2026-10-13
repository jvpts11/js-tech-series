/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.menu.CraftingRouterMenu;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Crafting Input Router's window. All of it lives in {@link AbstractBusScreen}; this only names it. */
public class CraftingRouterScreen extends AbstractBusScreen<CraftingRouterMenu> {

    public CraftingRouterScreen(final CraftingRouterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected TextKey windowTitle() {
        return BusTexts.ROUTER_TITLE;
    }
}
