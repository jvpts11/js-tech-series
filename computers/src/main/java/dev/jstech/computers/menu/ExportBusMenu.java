/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of an Export Bus. All behavior lives in {@link AbstractBusMenu}; this only binds the export menu type and
 * the create/fromNetwork factories.
 */
public class ExportBusMenu extends AbstractBusMenu {

    public ExportBusMenu(final int containerId, final Inventory playerInventory, final ExportBusPart part,
                         final Level level, final Opening opening) {
        super(ComputingMenus.EXPORT_BUS_MENU.get(), containerId, playerInventory, part, level, opening,
                BusLayout.Window.MOVER);
    }

    public static ExportBusMenu create(final int containerId, final Inventory playerInventory,
                                       final CableBlockEntity cable, final Direction face) {
        return open(containerId, playerInventory, cable, face, ExportBusPart.class,
                ExportBusPart::new, ExportBusMenu::new);
    }

    public static ExportBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        return openFromNetwork(containerId, playerInventory, buf, ExportBusPart.class,
                ExportBusPart::new, ExportBusMenu::new);
    }
}
