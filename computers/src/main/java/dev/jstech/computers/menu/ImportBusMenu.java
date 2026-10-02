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
import dev.jstech.core.tier.HardwareEra;
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
        // A part gone from the face leaves a window on a stand-in, which its validity closes at once.
        final ImportBusPart part = cable.getPart(face) instanceof ImportBusPart real ? real
                : new ImportBusPart(HardwareEra.STANDARD);
        return new ImportBusMenu(containerId, playerInventory, part, cable.getLevel(), Opening.of(cable, face, part));
    }

    public static ImportBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = playerInventory.player.level();
        final ImportBusPart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof ImportBusPart real ? real
                : new ImportBusPart(opening.era());
        return new ImportBusMenu(containerId, playerInventory, part, level, opening);
    }
}
