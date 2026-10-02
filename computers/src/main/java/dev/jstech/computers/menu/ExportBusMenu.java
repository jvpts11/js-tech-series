/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.tier.HardwareEra;
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
        super(ComputingMenus.EXPORT_BUS_MENU.get(), containerId, playerInventory, part, level, opening, false);
    }

    public static ExportBusMenu create(final int containerId, final Inventory playerInventory,
                                       final CableBlockEntity cable, final Direction face) {
        // A part gone from the face leaves a window on a stand-in, which its validity closes at once.
        final ExportBusPart part = cable.getPart(face) instanceof ExportBusPart real ? real
                : new ExportBusPart(HardwareEra.STANDARD);
        return new ExportBusMenu(containerId, playerInventory, part, cable.getLevel(), Opening.of(cable, face, part));
    }

    public static ExportBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                            final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = playerInventory.player.level();
        final ExportBusPart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof ExportBusPart real ? real
                : new ExportBusPart(opening.era());
        return new ExportBusMenu(containerId, playerInventory, part, level, opening);
    }
}
