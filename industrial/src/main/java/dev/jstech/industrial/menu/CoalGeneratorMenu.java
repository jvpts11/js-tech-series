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
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.gui.layout.CoalGeneratorLayout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Coal Generator's menu: its fuel slot and the player's inventory, with the burn time and the stored energy.
 * Shift-clicking the fuel sends it to the player; shift-clicking in the player's inventory sends it to the fuel slot.
 */
public class CoalGeneratorMenu extends CoreMenu {

    private final CoalGeneratorBlockEntity generator;

    public CoalGeneratorMenu(final int containerId, final Inventory inventory,
                             final CoalGeneratorBlockEntity generator) {
        super(IndustrialModule.COAL_GENERATOR_MENU.get(), containerId, inventory, MenuValidity.blockEntity(generator));
        this.generator = generator;
        final GuiLayout layout = CoalGeneratorLayout.layout();
        final SlotGroup fuel = slots(slot(generator.getInventory(), CoalGeneratorBlockEntity.FUEL_SLOT,
                layout.slotAt("fuel")));
        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(fuel, player.all());
        shiftClick(player.all(), fuel);
        data(generator.fields().menuData());
    }

    public CoalGeneratorMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, CoalGeneratorBlockEntity.class));
    }

    /** The generator the menu is open on; on the client its menu values are the server's. */
    public CoalGeneratorBlockEntity generator() {
        return generator;
    }
}
