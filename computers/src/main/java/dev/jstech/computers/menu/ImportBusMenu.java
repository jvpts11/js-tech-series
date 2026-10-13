/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of an Import Bus. All behavior lives in {@link AbstractBusMenu}; this only binds the import menu type and
 * the create/fromNetwork factories.
 */
public class ImportBusMenu extends AbstractBusMenu {

    public ImportBusMenu(final int containerId, final Inventory playerInventory, final ImportBusPart part,
                         final Level level, final Opening opening) {
        super(ComputingMenus.IMPORT_BUS_MENU.get(), containerId, playerInventory, part, level, opening,
                BusLayout.Window.MOVER);
    }

    public static ImportBusMenu create(final int containerId, final Inventory playerInventory,
                                       final CableBlockEntity cable, final Direction face) {
        return open(containerId, playerInventory, cable, face, ImportBusPart.class,
                ImportBusPart::new, ImportBusMenu::new);
    }

    public static ImportBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        return openFromNetwork(containerId, playerInventory, buf, ImportBusPart.class,
                ImportBusPart::new, ImportBusMenu::new);
    }
}
