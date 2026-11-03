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
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The workshop's crafting table, standing on the floor against the hull's wall where its part's model draws it: a
 * click opens the crafting grid, as the game's crafting table does.
 */
public final class ShipCraftingTableBlock extends ShipFixtureBlock {

    public ShipCraftingTableBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new Menu(id, inventory,
                    ContainerLevelAccess.create(level, pos), this), GameText.component(ShipFixtureTexts.CRAFTING)));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* The game's crafting grid, open while its user stays by this table rather than by the game's own. */
    private static final class Menu extends CraftingMenu {

        private final ContainerLevelAccess access;
        private final Block table;

        Menu(final int id, final Inventory inventory, final ContainerLevelAccess access, final Block table) {
            super(id, inventory, access);
            this.access = access;
            this.table = table;
        }

        @Override
        public boolean stillValid(final Player player) {
            return stillValid(this.access, player, this.table);
        }
    }
}
