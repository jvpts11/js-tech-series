/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of a Crafting Input Router. All behavior lives in {@link AbstractBusMenu}; this only binds the router's
 * menu type and the create/fromNetwork factories.
 */
public class CraftingRouterMenu extends AbstractBusMenu {

    public CraftingRouterMenu(final int containerId, final Inventory playerInventory, final CraftingRouterPart part,
                              final Level level, final Opening opening) {
        super(ComputingMenus.CRAFTING_ROUTER_MENU.get(), containerId, playerInventory, part, level, opening,
                BusLayout.Window.ROUTER);
    }

    public static CraftingRouterMenu create(final int containerId, final Inventory playerInventory,
                                            final CableBlockEntity cable, final Direction face) {
        return open(containerId, playerInventory, cable, face, CraftingRouterPart.class,
                era -> new CraftingRouterPart(), CraftingRouterMenu::new);
    }

    public static CraftingRouterMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        return openFromNetwork(containerId, playerInventory, buf, CraftingRouterPart.class,
                era -> new CraftingRouterPart(), CraftingRouterMenu::new);
    }
}
