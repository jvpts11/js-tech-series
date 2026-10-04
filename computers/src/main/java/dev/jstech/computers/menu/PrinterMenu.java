/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.gui.layout.PrinterLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A printer's window: the paper tray, the three places its printed sheets come out to, the player's inventory, and
 * two buttons, Pause (Resume while paused) and Cancel job. What it shows of the queue and the job printing comes off
 * the block entity, which the server keeps synced.
 */
public class PrinterMenu extends CoreMenu {

    private final PrinterBlockEntity printer;

    public static final int BUTTON_PAUSE = 0;
    public static final int BUTTON_CANCEL = 1;
    public static final int PAPER_SLOT = 0;

    public PrinterMenu(final int containerId, final Inventory playerInventory, final PrinterBlockEntity printer) {
        super(ComputingMenus.PRINTER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(printer.getLevel(), printer.getBlockPos(), PrinterBlock.class));
        this.printer = printer;
        final GuiLayout layout = PrinterLayout.layout();
        final GuiLayout.SlotPosition paperAt = layout.slotAt("paper");
        final SlotGroup paper = slots(new SlotItemHandler(printer.paper(), 0, paperAt.x(), paperAt.y()));
        final Slot[] out = new Slot[PrinterLayout.OUT_SLOTS];
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
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(output, playerSlots.all());
        shiftClick(paper, playerSlots.all());
        shiftClick(playerSlots.all(), paper);
        button(BUTTON_PAUSE, player -> printer.togglePaused());
        button(BUTTON_CANCEL, player -> printer.cancelCurrent());
    }

    public static PrinterMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                          final RegistryFriendlyByteBuf buf) {
        return new PrinterMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, PrinterBlockEntity.class));
    }

    public PrinterBlockEntity printer() {
        return printer;
    }

    public BlockPos printerPos() {
        return printer.getBlockPos();
    }

    /** How many sheets the tray holds, as the menu sees it. */
    public int paperCount() {
        return slots.get(PAPER_SLOT).getItem().getCount();
    }
}
