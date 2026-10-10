/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gui.layout.PersonalComputerLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu for the Personal Computer: a single hardware-assembly surface (motherboard, PSU, CPU, RAM, GPU, disks, each
 * restricted to its component category and clamped to the count the installed motherboard offers) plus the player
 * inventory.
 */
public class PersonalComputerMenu extends AbstractAssemblyComputerMenu {

    private final PersonalComputerBlockEntity blockEntity;

    public PersonalComputerMenu(final int containerId, final Inventory playerInventory,
                                final PersonalComputerBlockEntity be) {
        super(ComputingMenus.PERSONAL_COMPUTER_MENU.get(), containerId, playerInventory, be,
                PersonalComputerLayout.layout(), "gpu");
        this.blockEntity = be;
    }

    public static PersonalComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                   final RegistryFriendlyByteBuf buf) {
        return new PersonalComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, PersonalComputerBlockEntity.class));
    }

    public int networkServerCount() {
        return blockEntity.assemblyServerCount();
    }
}