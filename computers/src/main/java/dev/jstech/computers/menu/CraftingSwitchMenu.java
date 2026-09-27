/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.CraftingSwitchBlock;
import dev.jstech.computers.gui.layout.CraftingSwitchLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * Menu for the Crafting Switch. It holds no item slots of its own; the face config (name + active) is read
 * from the block entity, which the server keeps in sync via its update tag, and edited back through
 * {@code SetCraftingSwitchFacePayload}. Only the player inventory is wired here, from the shared layout.
 */
public class CraftingSwitchMenu extends CoreMenu {

    private final BlockPos switchPos;

    public CraftingSwitchMenu(final int containerId, final Inventory playerInventory,
                              final Level level, final BlockPos switchPos) {
        super(ComputingMenus.CRAFTING_SWITCH_MENU.get(), containerId, playerInventory,
                MenuValidity.block(level, switchPos, CraftingSwitchBlock.class));
        this.switchPos = switchPos;
        final PlayerSlots player = playerInventory(playerInventory, CraftingSwitchLayout.layout().playerInventoryAt());
        // No item slots beyond the player's own: shift-click just moves between its main grid and hotbar.
        shiftClick(player.main(), player.hotbar());
        shiftClick(player.hotbar(), player.main());
    }

    public static CraftingSwitchMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        final BlockPos pos = buf.readBlockPos();
        return new CraftingSwitchMenu(containerId, playerInventory, playerInventory.player.level(), pos);
    }

    public BlockPos switchPos() {
        return switchPos;
    }
}
