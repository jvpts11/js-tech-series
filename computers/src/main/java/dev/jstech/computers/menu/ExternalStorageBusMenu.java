/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of an External Storage Bus. All behavior lives in {@link AbstractBusMenu}; this only binds the external
 * storage menu type and the create/fromNetwork factories.
 */
public class ExternalStorageBusMenu extends AbstractBusMenu {

    public ExternalStorageBusMenu(final int containerId, final Inventory playerInventory,
                                  final ExternalStorageBusPart part, final Level level, final Opening opening) {
        super(ComputingMenus.EXTERNAL_STORAGE_BUS_MENU.get(), containerId, playerInventory, part, level, opening,
                BusLayout.Window.EXTERNAL);
    }

    public static ExternalStorageBusMenu create(final int containerId, final Inventory playerInventory,
                                                final CableBlockEntity cable, final Direction face) {
        return open(containerId, playerInventory, cable, face, ExternalStorageBusPart.class,
                ExternalStorageBusPart::new, ExternalStorageBusMenu::new);
    }

    public static ExternalStorageBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                     final RegistryFriendlyByteBuf buf) {
        return openFromNetwork(containerId, playerInventory, buf, ExternalStorageBusPart.class,
                ExternalStorageBusPart::new, ExternalStorageBusMenu::new);
    }
}
