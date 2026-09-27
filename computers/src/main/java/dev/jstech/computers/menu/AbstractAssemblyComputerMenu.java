/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.core.menu.CoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Predicate;

/**
 * A menu that assembles one computer out of hardware parts, backed by one block entity in the world: the
 * Personal Computer, the Crafting Computer and the Cluster Management Computer share this base so a payload
 * that renames whichever of the three is open can be admitted by one gate instead of three.
 */
public abstract class AbstractAssemblyComputerMenu extends CoreMenu {

    protected AbstractAssemblyComputerMenu(final MenuType<?> type, final int containerId, final Inventory inventory,
                                           final Predicate<Player> validity) {
        super(type, containerId, inventory, validity);
    }

    /** Where the block entity this menu assembles stands. */
    public abstract BlockPos computerPos();
}
