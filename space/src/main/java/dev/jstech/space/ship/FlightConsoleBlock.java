/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.room.RoomLiving;
import dev.jstech.space.life.OxygenBottleItem;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.Vec3;

/**
 * The cockpit's flight console, which keeps the small ship's air reserve ({@link AirReserveEntity}) and tops up the
 * pressurised cabin from it; an oxygen bottle used on it pours into the reserve. Used with an empty hand in a ship
 * built whole in its dock, it starts the ship: the ship leaves the dock's hold, a moving structure from then on.
 */
public final class FlightConsoleBlock extends ShipFixtureBlock implements EntityBlock {

    /* How far from a dock's terminal a ship's console may be for the dock to take the ship back, in blocks. */
    private static final double DOCK_REACH = 32;

    public FlightConsoleBlock(final Properties properties) {
        super(properties);
    }

    /*
     * Starts the ship it is the console of, while the ship stands whole in the dock that built it; aboard a ship come
     * into a dock's bay, has the dock take it back onto the grid, to change it there.
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (YardLayout.isYardBlock(pos.getX())) {
            if (MovingStructures.at(server, pos) instanceof MovingStructure ship) {
                final Vec3 here = Vec3.atCenterOf(MovingStructures.inWorld(server, pos));
                for (final ShipyardTerminalBlockEntity dock : ShipyardTerminalBlockEntity.near(server, here,
                        DOCK_REACH)) {
                    if (dock.seat(ship)) {
                        return InteractionResult.CONSUME;
                    }
                }
            }
            return InteractionResult.PASS;
        }
        final BlockPos dock = LevelStructures.of(server).terminalHolding(pos);
        if (dock != null && server.getBlockEntity(dock) instanceof ShipyardTerminalBlockEntity terminal
                && terminal.release(player.getUUID()).isPresent()) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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
