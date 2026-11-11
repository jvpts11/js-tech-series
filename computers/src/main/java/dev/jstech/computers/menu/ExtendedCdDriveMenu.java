/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.gui.layout.ExtendedCdDriveLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * An Extended CD Drive's window: its reading tray, its seven writing trays, the player's inventory, and Copy and
 * Eject. How far each copy is comes off the block entity, which the server keeps synced.
 */
public class ExtendedCdDriveMenu extends CoreMenu {

    private final ExtendedCdDriveBlockEntity tower;

    public static final int BUTTON_COPY = 0;
    public static final int BUTTON_EJECT = 1;

    public ExtendedCdDriveMenu(final int containerId, final Inventory playerInventory,
                               final ExtendedCdDriveBlockEntity tower) {
        super(ComputingMenus.EXTENDED_CD_DRIVE_MENU.get(), containerId, playerInventory,
                MenuValidity.block(tower.getLevel(), tower.getBlockPos(), ExtendedCdDriveBlock.class));
        this.tower = tower;
        final GuiLayout layout = ExtendedCdDriveLayout.layout();
        final GuiLayout.SlotPosition readAt = layout.slotAt("read");
        final SlotGroup read = slots(new SlotItemHandler(tower.readTray(), 0, readAt.x(), readAt.y()));
        final Slot[] trays = new Slot[ExtendedCdDriveBlockEntity.TRAYS];
        for (int i = 0; i < trays.length; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("tray" + i);
            trays[i] = new SlotItemHandler(tower.writeTrays(), i, at.x(), at.y());
        }
        final SlotGroup write = slots(trays);
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(read, playerSlots.all());
        shiftClick(write, playerSlots.all());
        shiftClick(playerSlots.all(), read, write);
        button(BUTTON_COPY, player -> tower.copy());
        button(BUTTON_EJECT, tower::eject);
    }

    public static ExtendedCdDriveMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                  final RegistryFriendlyByteBuf buf) {
        return new ExtendedCdDriveMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, ExtendedCdDriveBlockEntity.class));
    }

    public ExtendedCdDriveBlockEntity tower() {
        return tower;
    }
}
