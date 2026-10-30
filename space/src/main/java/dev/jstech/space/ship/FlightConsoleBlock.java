/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.room.RoomLiving;
import dev.jstech.space.life.OxygenBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cockpit's flight console, which keeps the small ship's air reserve ({@link AirReserveEntity}) and tops up the
 * pressurised cabin from it; an oxygen bottle used on it pours into the reserve.
 */
public final class FlightConsoleBlock extends ShipFixtureBlock implements EntityBlock {

    public FlightConsoleBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AirReserveEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide() || type != ShipKitBlocks.AIR_RESERVE_ENTITY.get()) {
            return null;
        }
        return (at, pos, now, entity) -> ((AirReserveEntity) entity).tick();
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!(stack.getItem() instanceof OxygenBottleItem) || OxygenBottleItem.air(stack) <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AirReserveEntity reserve) {
            final double went = reserve.pour(OxygenBottleItem.air(stack) * RoomLiving.PLAYER_BREATH);
            OxygenBottleItem.setAir(stack, OxygenBottleItem.air(stack)
                    - (int) Math.round(went / RoomLiving.PLAYER_BREATH));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
