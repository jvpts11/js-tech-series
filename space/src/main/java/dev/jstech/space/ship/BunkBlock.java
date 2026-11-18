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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The quarters' bunk: a bed, so a click makes it its user's respawn point and, at night, lays them down in it to pass
 * the night as the game's beds do, docked, landed or in flight; as long as it stands in a sealed cabin whose air is
 * breathable: where there is none it refuses, and a bunk whose cabin has lost its air wakes no one. The player wakes
 * standing beside it.
 */
public final class BunkBlock extends ShipFixtureBlock {

    /* How far round a bunk a monster keeps its sleeper up, across and up and down, as round the game's beds. */
    private static final double MONSTERS_ACROSS = 8;
    private static final double MONSTERS_UP = 5;
    /* The way of its model's north end, across and along, as the model stands unturned: where its pillow is. */
    private static final int[] NORTH_END = {0, -1};

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

    /* A bunk is a bed: the game lays its sleeper in it, keeps them there and stands them up beside it. */
    @Override
    public boolean isBed(final BlockState state, final BlockGetter level, final BlockPos pos,
                         final LivingEntity sleeper) {
        return true;
    }

    /* Its pillow's way: its model's north end, as its part stands; north while its part is not formed. */
    @Override
    public Direction getBedDirection(final BlockState state, final LevelReader level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IFixtureEntity fixture && !fixture.places().isEmpty()) {
            final int[] way = fixture.places().getFirst().turn().rotation().map(turn -> turn.transform(0, -1))
                    .orElse(NORTH_END);
            return Direction.fromDelta(way[0], 0, way[1]);
        }
        return Direction.NORTH;
    }

    /* A bunk keeps no mark of who lies in it; a click finds them by where they sleep. */
    @Override
    public void setBedOccupied(final BlockState state, final Level level, final BlockPos pos,
                               final LivingEntity sleeper, final boolean occupied) {
    }

    /*
     * Formed, it is the air over the bunk its part's model draws: the mattress lies on the floor, in the block under
     * it, and is aimed at, outlined and clicked there as the model's piece "bunk", which works this block. Whole while
     * its part is not formed.
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getShape(state, level, pos, context);
    }

    /*
     * A click makes the bunk its user's place to wake and, as a bed does, lays them down in it: at night or in a
     * storm, no monster near and no one in it already, wherever its ship is, in flight too. Its pillow end is where
     * they lie, whichever of its blocks was clicked.
     */
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
        final BlockPos head = head(server, state, pos);
        if (sleeper.isSleeping() || !sleeper.isAlive()) {
            return InteractionResult.CONSUME;
        }
        final Component why = whyNot(server, head, sleeper);
        if (why != null) {
            player.displayClientMessage(why, true);
        } else {
            sleeper.startSleepInBed(head).ifLeft(refused -> {
                if (refused.getMessage() != null) {
                    player.displayClientMessage(refused.getMessage(), true);
                }
            });
        }
        return InteractionResult.CONSUME;
    }

    /* The block of the bunk at {@code pos} its pillow lies on: the one beyond it its pillow's way, if a bunk too. */
    private BlockPos head(final ServerLevel level, final BlockState state, final BlockPos pos) {
        final BlockPos beyond = pos.relative(getBedDirection(state, level, pos));
        return level.getBlockState(beyond).is(this) ? beyond : pos;
    }

    /*
     * Why {@code sleeper} cannot lie down in the bunk whose pillow is at {@code head}, as the game asks of a bed, or
     * none: the game leaves a bed of a mod to it. It is no matter what world it is in, so it sleeps in space, nor how
     * far the clicked bunk stands, as a click reaches it.
     */
    private static @Nullable Component whyNot(final ServerLevel level, final BlockPos head,
                                              final ServerPlayer sleeper) {
        if (level.isDay()) {
            return Player.BedSleepingProblem.NOT_POSSIBLE_NOW.getMessage();
        }
        for (final ServerPlayer other : level.players()) {
            if (other.isSleeping() && head.equals(other.getSleepingPos().orElse(null))) {
                return GameText.component(ShipFixtureTexts.OCCUPIED);
            }
        }
        // A bunk aboard stands out in the yard, where no monster is: the Core asks for those round it in the world.
        if (!sleeper.isCreative() && !level.getEntitiesOfClass(Monster.class, new AABB(head).inflate(
                MONSTERS_ACROSS, MONSTERS_UP, MONSTERS_ACROSS), monster -> monster.isPreventingPlayerRest(sleeper))
                .isEmpty()) {
            return Player.BedSleepingProblem.NOT_SAFE.getMessage();
        }
        return null;
    }
}
