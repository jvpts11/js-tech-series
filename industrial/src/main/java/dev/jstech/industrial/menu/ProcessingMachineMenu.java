/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.menu;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.industrial.gui.layout.ProcessingMachineLayout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * The menu of a processing machine (the Compressor, the Electric Furnace, the Macerator): its input slot, its output
 * slot the player only takes from, and the player's inventory, with the machine's progress and stored energy.
 * Shift-clicking the input or the output sends the stack to the player; shift-clicking in the player's inventory
 * sends it to the input.
 */
public class ProcessingMachineMenu extends CoreMenu {

    private final ProcessingMachineBlockEntity machine;

    public ProcessingMachineMenu(final MenuType<?> type, final int containerId, final Inventory inventory,
                                 final ProcessingMachineBlockEntity machine) {
        super(type, containerId, inventory, MenuValidity.blockEntity(machine));
        this.machine = machine;
        final GuiLayout layout = ProcessingMachineLayout.layout();
        final SlotGroup input = slots(slot(machine.getInventory(), 0, layout.slotAt("input")));
        final SlotGroup output = slots(outputSlot(machine.getInventory(), machine.layout().firstOutput(),
                layout.slotAt("output")));
        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(input, player.all());
        shiftClick(output, player.all());
        shiftClick(player.all(), input);
        data(machine.fields().menuData());
    }

    /** The menu the client opens, on the machine at the position the server wrote. */
    public static ProcessingMachineMenu fromNetwork(final MenuType<?> type, final int containerId,
                                                   final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        return new ProcessingMachineMenu(type, containerId, inventory,
                MenuOpening.blockEntity(inventory, buf, ProcessingMachineBlockEntity.class));
    }

    /** The machine the menu is open on; on the client its menu values are the server's. */
    public ProcessingMachineBlockEntity machine() {
        return machine;
    }
}
