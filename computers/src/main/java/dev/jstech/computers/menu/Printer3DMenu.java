/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.Printer3DBlock;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.gui.layout.Printer3DLayout;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A 3D printer's window (computers/docs/design/teracoin.md, "The 3D Printer"): its output's three places, which only
 * give, the inventory, and the keys PAUSE and CANCEL.
 */
public class Printer3DMenu extends CoreMenu {

    private final Printer3DBlockEntity printer;

    public static final int BUTTON_PAUSE = 0;
    public static final int BUTTON_CANCEL = 1;

    public Printer3DMenu(final int containerId, final Inventory playerInventory, final Printer3DBlockEntity printer) {
        super(ComputingMenus.PRINTER_3D_MENU.get(), containerId, playerInventory,
                MenuValidity.block(printer.getLevel(), printer.getBlockPos(), Printer3DBlock.class));
        this.printer = printer;
        final GuiLayout layout = Printer3DLayout.layout();
        final Slot[] out = new Slot[Printer3DLayout.OUT_SLOTS];
        for (int i = 0; i < out.length; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("out" + i);
            out[i] = new SlotItemHandler(printer.output(), i, at.x(), at.y()) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return false;
                }
            };
        }
        final SlotGroup output = slots(out);
        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(output, player.all());
        button(BUTTON_PAUSE, p -> printer.togglePaused());
        button(BUTTON_CANCEL, p -> printer.cancelCurrent());
    }

    public static Printer3DMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        return new Printer3DMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, Printer3DBlockEntity.class));
    }

    public Printer3DBlockEntity printer() {
        return printer;
    }
}
