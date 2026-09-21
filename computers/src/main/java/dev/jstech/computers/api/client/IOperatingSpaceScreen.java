/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.computers.menu.ComputerTerminalMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Makes the screen that draws an operating space.
 *
 * <p>The split is deliberate: the menu is the mod's, because the items in it are the server's to move and
 * every space needs the same ones, and everything a player sees and types is the space's. So a space is
 * handed a menu that is already wired to the machine and draws whatever it likes over it, up to and
 * including an interface nothing here imagined.
 *
 * <p>What the menu gives a space to work with: the machine and monitor it belongs to, the hardware era for
 * the skin ({@link ComputerTerminalMenu#hardwareEra()}), the network's items and servers, the operations
 * log, and the player's own inventory as real slots.
 */
@FunctionalInterface
public interface IOperatingSpaceScreen {

    /**
     * The screen for one opening of one machine.
     *
     * @param menu      the machine's terminal menu, already holding the player's inventory slots
     * @param inventory the player's inventory, as any container screen takes it
     * @param title     the name of the block the monitor is showing
     */
    AbstractContainerScreen<ComputerTerminalMenu> open(ComputerTerminalMenu menu, Inventory inventory,
                                                       Component title);
}
