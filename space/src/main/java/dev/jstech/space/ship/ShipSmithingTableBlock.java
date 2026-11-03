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
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The workshop's smithing table, standing on the floor against the hull's wall where its part's model draws it: a
 * click opens the smithing slots, as the game's smithing table does.
 */
public final class ShipSmithingTableBlock extends ShipFixtureBlock {

    public ShipSmithingTableBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new Menu(id, inventory,
                    ContainerLevelAccess.create(level, pos), this), GameText.component(ShipFixtureTexts.SMITHING)));
            player.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* The game's smithing slots, open while their user stays by this table rather than by the game's own. */
    private static final class Menu extends SmithingMenu {

        private final Block table;

        Menu(final int id, final Inventory inventory, final ContainerLevelAccess access, final Block table) {
            super(id, inventory, access);
            this.table = table;
        }

        @Override
        protected boolean isValidBlock(final BlockState state) {
            return state.is(this.table);
        }
    }
}
