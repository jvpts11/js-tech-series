/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.ScannerBlock;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.gui.layout.ScannerLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A scanner's window: the glass, the player's inventory, and two buttons, Scan and Stop. What it shows of the page
 * being read and where the file goes comes off the block entity, which the server keeps synced.
 */
public class ScannerMenu extends CoreMenu {

    private final ScannerBlockEntity scanner;

    public static final int BUTTON_SCAN = 0;
    public static final int BUTTON_STOP = 1;

    public ScannerMenu(final int containerId, final Inventory playerInventory, final ScannerBlockEntity scanner) {
        super(ComputingMenus.SCANNER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(scanner.getLevel(), scanner.getBlockPos(), ScannerBlock.class));
        this.scanner = scanner;
        final GuiLayout layout = ScannerLayout.layout();
        final GuiLayout.SlotPosition glassAt = layout.slotAt("glass");
        final SlotGroup glass = slots(new SlotItemHandler(scanner.glass(), 0, glassAt.x(), glassAt.y()));
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(glass, playerSlots.all());
        shiftClick(playerSlots.all(), glass);
        button(BUTTON_SCAN, player -> scanner.scan());
        button(BUTTON_STOP, player -> scanner.stop());
        // The window says where the file goes as it is when it opens, not as it was when the cable was laid.
        if (scanner.getLevel() instanceof ServerLevel server) {
            scanner.refreshTarget(server);
        }
    }

    public static ScannerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                          final RegistryFriendlyByteBuf buf) {
        return new ScannerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, ScannerBlockEntity.class));
    }

    public ScannerBlockEntity scanner() {
        return scanner;
    }
}
