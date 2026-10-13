/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.gui.layout.DockLayout;
import dev.jstech.computers.os.media.DockStationBlock;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Dock Station's window: its three trays and its USB port as slots a disk or the stick is put in and taken from,
 * an Eject for each, and the player's inventory. What it shows of each disk, its letter above all, comes off the
 * block entity, which the server keeps synced.
 */
public class DockStationMenu extends CoreMenu {

    private final DockStationBlockEntity dock;

    /** The buttons: Eject for each tray, top to bottom, then for the port. */
    public static final int BUTTON_EJECT_USB = DockStationBlockEntity.USB;

    public DockStationMenu(final int containerId, final Inventory playerInventory, final DockStationBlockEntity dock) {
        super(ComputingMenus.DOCK_STATION_MENU.get(), containerId, playerInventory,
                MenuValidity.block(dock.getLevel(), dock.getBlockPos(), DockStationBlock.class));
        this.dock = dock;
        final GuiLayout layout = DockLayout.layout();
        final Slot[] holders = new Slot[DockLayout.ROWS];
        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
            final GuiLayout.SlotPosition at = layout.slotAt("bay" + bay);
            holders[bay] = new SlotItemHandler(dock.bays(), bay, at.x(), at.y());
        }
        final GuiLayout.SlotPosition usbAt = layout.slotAt("bay" + DockStationBlockEntity.USB);
        holders[DockStationBlockEntity.USB] = new SlotItemHandler(dock.mediaSlot(), 0, usbAt.x(), usbAt.y());
        final SlotGroup trays = slots(holders);
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(trays, playerSlots.all());
        shiftClick(playerSlots.all(), trays);
        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
            final int tray = bay;
            button(tray, player -> give(player, dock.ejectDisk(tray)));
        }
        button(BUTTON_EJECT_USB, player -> give(player, dock.ejectMedia()));
    }

    public static DockStationMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                              final RegistryFriendlyByteBuf buf) {
        return new DockStationMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, DockStationBlockEntity.class));
    }

    public DockStationBlockEntity dock() {
        return dock;
    }

    public BlockPos dockPos() {
        return dock.getBlockPos();
    }

    /** What a tray or the port holds, as the menu sees it. */
    public ItemStack held(final int row) {
        return slots.get(row).getItem();
    }

    private void give(final Player player, final ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            final BlockPos pos = dock.getBlockPos();
            Containers.dropItemStack(player.level(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
    }
}
