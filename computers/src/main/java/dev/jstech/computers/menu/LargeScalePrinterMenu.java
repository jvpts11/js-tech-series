/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineTerminalBlock;
import dev.jstech.computers.gui.layout.LargeScalePrinterLayout;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Large Scale Printer's window: one place of its feeder, which paper goes into by hand, the player's inventory,
 * and three buttons, Pause (Resume while paused), Cancel job and the switch of where its books go. What it shows of
 * the queue and the book printing comes off the block entity, which the server keeps synced.
 */
public class LargeScalePrinterMenu extends CoreMenu {

    private final LargeScalePrinterBlockEntity printer;

    public static final int BUTTON_PAUSE = 0;
    public static final int BUTTON_CANCEL = 1;
    public static final int BUTTON_DESTINATION = 2;

    public LargeScalePrinterMenu(final int containerId, final Inventory playerInventory,
                                 final LargeScalePrinterBlockEntity printer) {
        super(ComputingMenus.LARGE_SCALE_PRINTER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(printer.getLevel(), printer.getBlockPos(), ChainMachineTerminalBlock.class));
        this.printer = printer;
        final GuiLayout layout = LargeScalePrinterLayout.layout();
        final GuiLayout.SlotPosition feederAt = layout.slotAt("feeder");
        final SlotGroup feeder = slots(new SlotItemHandler(printer.getInventory(), 0, feederAt.x(), feederAt.y()) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(Items.PAPER);
            }
        });
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(feeder, playerSlots.all());
        shiftClick(playerSlots.all(), feeder);
        button(BUTTON_PAUSE, player -> printer.togglePaused());
        button(BUTTON_CANCEL, player -> printer.cancelCurrent());
        button(BUTTON_DESTINATION, player -> printer.toggleDestination());
    }

    /** The window a Large Scale Printer's terminal opens; none for another machine's. */
    public static AbstractContainerMenu open(final int containerId, final Inventory playerInventory,
                                             final ChainMachineBlockEntity terminal) {
        return terminal instanceof LargeScalePrinterBlockEntity printer
                ? new LargeScalePrinterMenu(containerId, playerInventory, printer) : null;
    }

    public static LargeScalePrinterMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                    final RegistryFriendlyByteBuf buf) {
        return new LargeScalePrinterMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, LargeScalePrinterBlockEntity.class));
    }

    public LargeScalePrinterBlockEntity printer() {
        return printer;
    }
}
