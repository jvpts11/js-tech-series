/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.InputBusPart;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/**
 * The window of a Crafting Input Bus. All behavior lives in {@link AbstractBusMenu}; this only binds the input menu
 * type and the create/fromNetwork factories.
 */
public class InputBusMenu extends AbstractBusMenu {

    public InputBusMenu(final int containerId, final Inventory playerInventory, final InputBusPart part,
                        final Level level, final Opening opening) {
        super(ComputingMenus.INPUT_BUS_MENU.get(), containerId, playerInventory, part, level, opening, true);
    }

    public static InputBusMenu create(final int containerId, final Inventory playerInventory,
                                      final CableBlockEntity cable, final Direction face) {
        // A part gone from the face leaves a window on a stand-in, which its validity closes at once.
        final InputBusPart part = cable.getPart(face) instanceof InputBusPart real ? real : new InputBusPart();
        return new InputBusMenu(containerId, playerInventory, part, cable.getLevel(), Opening.of(cable, face, part));
    }

    public static InputBusMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                           final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = playerInventory.player.level();
        final InputBusPart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof InputBusPart real ? real : new InputBusPart();
        return new InputBusMenu(containerId, playerInventory, part, level, opening);
    }
}
