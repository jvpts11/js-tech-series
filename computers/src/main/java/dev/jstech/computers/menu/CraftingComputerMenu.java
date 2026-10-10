/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.gui.layout.CraftingComputerLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu for the Crafting Computer's assembly surface: motherboard, PSU, CPU, RAM, PCIe (where the Crafting Card goes)
 * and disks (each restricted to its component category and clamped to the count the installed motherboard offers)
 * plus the player inventory.
 */
public class CraftingComputerMenu extends AbstractAssemblyComputerMenu {

    private final CraftingComputerBlockEntity blockEntity;

    public CraftingComputerMenu(final int containerId, final Inventory playerInventory,
                                final CraftingComputerBlockEntity be) {
        super(ComputingMenus.CRAFTING_COMPUTER_MENU.get(), containerId, playerInventory, be,
                CraftingComputerLayout.layout(), "pcie");
        this.blockEntity = be;
    }

    public static CraftingComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                   final RegistryFriendlyByteBuf buf) {
        return new CraftingComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, CraftingComputerBlockEntity.class));
    }

    public int craftFactorX100() {
        return blockEntity.assemblyCraftFactorX100();
    }

    public long craftThroughput() {
        return blockEntity.assemblyCraftThroughput();
    }

    public int craftThreads() {
        return blockEntity.assemblyCraftThreads();
    }

    public int romUsed() {
        return blockEntity.assemblyRomUsed();
    }

    /** How many bench recipes the cards' ROM keeps at most together. */
    public int romLimit() {
        return blockEntity.assemblyRomSize();
    }

    /** How many Crafting Interfaces the cards drive together while the computer runs. */
    public int interfaces() {
        return blockEntity.assemblyInterfaces();
    }
}