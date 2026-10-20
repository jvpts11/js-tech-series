/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sleeping aboard a moving structure: a bed of a structure is out in the yard, and the game measures, lays down,
 * keeps and stands up whoever sleeps in a bed by the bed's own place. A bed aboard is measured where it is in the
 * world, for whether the player reaches it and whether monsters are near; its sleeper lies in it there and is carried
 * with it as the structure goes ({@code LivingEntityMixin}), and stands up beside it there; one who sleeps in it wakes
 * there, the bed their place to come back to, as long as its structure is loaded. Anyone found out in the yard, where
 * no one stands, is put back at their place to come back to.
 */
public final class StructureSleeping {

    /* How far from a bed a player may lie down in it, across and up, and how far round it a monster keeps them up. */
    private static final double REACH_ACROSS = 3.0;
    private static final double REACH_UP = 2.0;
    private static final double MONSTERS_ACROSS = 8.0;
    private static final double MONSTERS_UP = 5.0;
    /* The height above a bed's block a sleeper lies at, as the game's own. */
    private static final double LIES = 0.6875;
    private static final double HALF = 0.5;

    private StructureSleeping() {
    }

    /** Hands the game's bus what sleeping aboard answers, once, as the kit starts. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(CanPlayerSleepEvent.class, StructureSleeping::onSleep);
        NeoForge.EVENT_BUS.addListener(PlayerRespawnPositionEvent.class, StructureSleeping::onRespawn);
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, StructureSleeping::afterTick);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, StructureSleeping::afterPlayer);
    }

    /** {@code sleeper} was laid in its bed: one aboard lies where the bed is in the world. */
    public static void laidDown(final LivingEntity sleeper) {
        toWorld(sleeper);
    }

    /** {@code sleeper} stood up from its bed: from one aboard, beside it where it is in the world. */
    public static void stoodUp(final LivingEntity sleeper) {
        toWorld(sleeper);
    }

    /* Puts {@code entity}, at a place of the yard, where that place is in the world. */
    private static void toWorld(final LivingEntity entity) {
        if (!YardLayout.isYard(entity.getX()) || !LevelMovingStructures.anyLoaded()) {
            return;
        }
        final Vec3 world = MovingStructures.inWorld(entity.level(), entity.position());
        if (!YardLayout.isYard(world.x)) {
            entity.setPos(world.x, world.y, world.z);
        }
    }

    /* A sleeper in a bed aboard stands on the structure, so it is carried with it, on either side of the game. */
    private static void afterTick(final EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity sleeper) || !sleeper.isSleeping()
                || !LevelMovingStructures.anyLoaded()) {
            return;
        }
        final BlockPos bed = sleeper.getSleepingPos().orElse(null);
        if (bed == null || !YardLayout.isYardBlock(bed.getX())) {
            return;
        }
        final IStructureBody body = MovingStructures.at(sleeper.level(), bed);
        if (body == null || !body.ready()) {
            return;
        }
        StructureFooting.stood(sleeper, body, bed);
        final StructureFrame frame = body.frame();
        final double x = bed.getX() + HALF;
        final double y = bed.getY() + LIES;
        final double z = bed.getZ() + HALF;
        final double wx = frame.worldX(x, y, z);
        final double wy = frame.worldY(x, y, z);
        final double wz = frame.worldZ(x, y, z);
        if (Math.abs(sleeper.getX() - wx) + Math.abs(sleeper.getY() - wy) + Math.abs(sleeper.getZ() - wz) > 1.0e-6) {
            sleeper.setPos(wx, wy, wz);
        }
    }

    /*
     * A player is laying down in a bed aboard: reached and kept safe from monsters as the bed stands in the world,
     * not by its place out in the yard.
     */
    private static void onSleep(final CanPlayerSleepEvent event) {
        final BlockPos bed = event.getPos();
        if (!YardLayout.isYardBlock(bed.getX())) {
            return;
        }
        final ServerPlayer player = event.getEntity();
        final Level level = event.getLevel();
        final IStructureBody body = MovingStructures.at(level, bed);
        if (body == null) {
            event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            return;
        }
        final BlockState state = level.getBlockState(bed);
        final Direction way = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        final Vec3 head = bottomCentre(body.frame(), bed);
        final Vec3 foot = bottomCentre(body.frame(), bed.relative(way.getOpposite()));
        if (event.getProblem() == Player.BedSleepingProblem.TOO_FAR_AWAY && (reaches(player, head)
                || reaches(player, foot))) {
            event.setProblem(null);
        }
        if (event.getProblem() == null && !player.isCreative()) {
            final AABB round = new AABB(head, head).inflate(MONSTERS_ACROSS, MONSTERS_UP, MONSTERS_ACROSS);
            if (!level.getEntitiesOfClass(Monster.class, round, monster -> monster.isPreventingPlayerRest(player))
                    .isEmpty()) {
                event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
            }
        }
    }

    /*
     * A player comes back to a bed aboard: where the bed is in the world, its structure loaded; else to the world's
     * place to come back to, never out in the yard.
     */
    private static void onRespawn(final PlayerRespawnPositionEvent event) {
        final DimensionTransition going = event.getDimensionTransition();
        if (!YardLayout.isYard(going.pos().x)) {
            return;
        }
        final ServerLevel level = going.newLevel();
        final Vec3 world = MovingStructures.inWorld(level, going.pos());
        final Vec3 to = YardLayout.isYard(world.x) ? Vec3.atBottomCenterOf(level.getSharedSpawnPos()) : world;
        event.setDimensionTransition(new DimensionTransition(level, to, going.speed(), going.yRot(), going.xRot(),
                going.missingRespawnBlock(), going.postDimensionTransition()));
    }

    /* Anyone found out in the yard, where no one stands, not asleep in a bed aboard, is put back where they belong. */
    private static void afterPlayer(final PlayerTickEvent.Post event) {
        // A mod's stand-in for a player, using a block aboard for a machine, stands where its mod put it.
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || !YardLayout.isYard(player.getX()) || player.isSleeping() || player.isSpectator()) {
            return;
        }
        final Vec3 world = MovingStructures.inWorld(player.level(), player.position());
        final Vec3 to = YardLayout.isYard(world.x) ? Vec3.atBottomCenterOf(player.serverLevel().getSharedSpawnPos())
                : world;
        player.teleportTo(player.serverLevel(), to.x, to.y, to.z, player.getYRot(), player.getXRot());
    }

    /* Whether {@code player} is within reach of a bed's half whose bottom's middle is at {@code at}. */
    private static boolean reaches(final Player player, final Vec3 at) {
        return Math.abs(player.getX() - at.x) <= REACH_ACROSS && Math.abs(player.getY() - at.y) <= REACH_UP
                && Math.abs(player.getZ() - at.z) <= REACH_ACROSS;
    }

    /* Where the middle of the bottom of the yard's block {@code pos} is in the world. */
    private static Vec3 bottomCentre(final StructureFrame frame, final BlockPos pos) {
        final double x = pos.getX() + HALF;
        final double y = pos.getY();
        final double z = pos.getZ() + HALF;
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }
}
