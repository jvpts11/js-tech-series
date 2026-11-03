/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.GameText;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.state.BlockState;

/** What a starship's barrel holds: three rows of nine places, as a barrel does. */
public final class ShipBarrelEntity extends ShipContainerEntity {

    private static final int PLACES = 27;

    public ShipBarrelEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.SHIP_BARREL_ENTITY.get(), pos, state, PLACES);
    }

    @Override
    protected Component getDefaultName() {
        return GameText.component(ShipFixtureTexts.BARREL);
    }

    @Override
    protected AbstractContainerMenu createMenu(final int id, final Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }
}
