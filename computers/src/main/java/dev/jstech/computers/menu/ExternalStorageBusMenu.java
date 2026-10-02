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
import dev.jstech.core.tier.HardwareEra;
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
        // A part gone from the face leaves a window on a stand-in, which its validity closes at once.
        final ExternalStorageBusPart part = cable.getPart(face) instanceof ExternalStorageBusPart real ? real
                : new ExternalStorageBusPart(HardwareEra.STANDARD);
        return new ExternalStorageBusMenu(containerId, playerInventory, part, cable.getLevel(),
                Opening.of(cable, face, part));
    }

    public static ExternalStorageBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                     final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = playerInventory.player.level();
        final ExternalStorageBusPart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof ExternalStorageBusPart real ? real
                : new ExternalStorageBusPart(opening.era());
        return new ExternalStorageBusMenu(containerId, playerInventory, part, level, opening);
    }
}
