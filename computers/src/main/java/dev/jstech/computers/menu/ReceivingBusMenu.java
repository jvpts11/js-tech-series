/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of a Crafting Receiving Bus. All behavior lives in {@link AbstractBusMenu}; this only binds the receiving
 * menu type and the create/fromNetwork factories.
 */
public class ReceivingBusMenu extends AbstractBusMenu {

    public ReceivingBusMenu(final int containerId, final Inventory playerInventory, final ReceivingBusPart part,
                            final Level level, final Opening opening) {
        super(ComputingMenus.RECEIVING_BUS_MENU.get(), containerId, playerInventory, part, level, opening,
                BusLayout.Window.RECEIVING);
    }

    public static ReceivingBusMenu create(final int containerId, final Inventory playerInventory,
                                          final CableBlockEntity cable, final Direction face) {
        // A part gone from the face leaves a window on a stand-in, which its validity closes at once.
        final ReceivingBusPart part = cable.getPart(face) instanceof ReceivingBusPart real ? real
                : new ReceivingBusPart();
        return new ReceivingBusMenu(containerId, playerInventory, part, cable.getLevel(),
                Opening.of(cable, face, part));
    }

    public static ReceivingBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                               final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = playerInventory.player.level();
        final ReceivingBusPart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof ReceivingBusPart real ? real : new ReceivingBusPart();
        return new ReceivingBusMenu(containerId, playerInventory, part, level, opening);
    }
}
