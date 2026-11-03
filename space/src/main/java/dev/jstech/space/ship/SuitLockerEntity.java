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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

/** What a suit locker holds: a row of nine places, for the suit's four pieces and what goes with them. */
public final class SuitLockerEntity extends ShipContainerEntity {

    private static final int PLACES = 9;

    public SuitLockerEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.SUIT_LOCKER_ENTITY.get(), pos, state, PLACES);
    }

    @Override
    protected Component getDefaultName() {
        return GameText.component(ShipFixtureTexts.SUIT_LOCKER);
    }

    @Override
    protected AbstractContainerMenu createMenu(final int id, final Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }
}
