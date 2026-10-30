/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The quarters' bunk: a bed to wake in, so a click makes it its user's respawn point, as long as it stands in a sealed
 * cabin whose air is breathable; where there is none it refuses, and a bunk whose cabin has lost its air wakes no one.
 * The player wakes standing beside it.
 */
public final class BunkBlock extends ShipFixtureBlock {

    public BunkBlock(final Properties properties) {
        super(properties);
    }

    /**
     * Whether the air beside the bunk at {@code pos} is breathable: the cabin's it opens onto, or, where it stands in
     * no room, the open air round it.
     */
    public static boolean breathable(final ServerLevel level, final BlockPos pos) {
        BlockPos open = null;
        for (final Direction side : Direction.values()) {
            final BlockPos beside = pos.relative(side);
            if (SealedRooms.roomAt(level, beside).isPresent()) {
                return SealedRooms.breathableAt(level, beside);
            }
            if (open == null && level.getBlockState(beside).getCollisionShape(level, beside).isEmpty()) {
                open = beside;
            }
        }
        return open != null && SealedRooms.breathableAt(level, open);
    }

    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(final BlockState state, final EntityType<?> type,
                                                                     final LevelReader level, final BlockPos pos,
                                                                     final float orientation) {
        if (!(level instanceof ServerLevel server) || !breathable(server, pos)) {
            return Optional.empty();
        }
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            final BlockPos beside = pos.relative(side);
            if (server.getBlockState(beside).getCollisionShape(server, beside).isEmpty()
                    && server.getBlockState(beside.above()).getCollisionShape(server, beside.above()).isEmpty()) {
                return Optional.of(new ServerPlayer.RespawnPosAngle(Vec3.atBottomCenterOf(beside), orientation));
            }
        }
        return Optional.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sleeper)) {
            return InteractionResult.SUCCESS;
        }
        if (!breathable(server, pos)) {
            player.displayClientMessage(GameText.component(ShipFixtureTexts.NO_AIR), true);
            return InteractionResult.CONSUME;
        }
        sleeper.setRespawnPosition(server.dimension(), pos, player.getYRot(), false, false);
        player.displayClientMessage(GameText.component(ShipFixtureTexts.RESPAWN_SET), true);
        return InteractionResult.CONSUME;
    }
}
