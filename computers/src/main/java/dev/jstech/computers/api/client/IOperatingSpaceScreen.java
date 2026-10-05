/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.ApiStatus;

/**
 * Makes the screen that draws an operating space.
 *
 * <p>The split is deliberate: the menu is the mod's, because the items in it are the server's to move and
 * every space needs the same ones, and everything a player sees and types is the space's. So a space is
 * handed an opening already wired to the machine and draws whatever it likes over its menu, up to and
 * including an interface nothing here imagined.
 */
@FunctionalInterface
public interface IOperatingSpaceScreen {

    /**
     * The screen for one opening of one machine, built on {@link IOperatingSpace#menu()}.
     *
     * @param space     the opening: the machine, its monitor, its era and its menu
     * @param inventory the player's inventory, as any container screen takes it
     * @param title     the name of the block the monitor is showing
     */
    @ApiStatus.Experimental
    AbstractContainerScreen<?> open(IOperatingSpace space, Inventory inventory, Component title);
}
