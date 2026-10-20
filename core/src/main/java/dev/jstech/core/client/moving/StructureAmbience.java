/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.StructureFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * The little life of a moving structure's blocks round the player: a torch's flame, a furnace's smoke, water dripping,
 * a portal's shimmer. The game gives the blocks round the player a chance each tick to show something, and a
 * structure's blocks are out in the yard; this gives the same chance to the blocks of the structures near the player,
 * round where the player is on each, and what they give off is put where they are in the world.
 */
final class StructureAmbience {

    /* How many blocks are given their chance a tick, and how far from the player they are picked. */
    private static final int TRIES = 384;
    private static final int REACH = 16;
    /* Only the structures within this many blocks of the player show this. */
    private static final double NEAR = 48;
    private static final RandomSource RANDOM = RandomSource.create();

    private StructureAmbience() {
    }

    static void tick(final Minecraft game) {
        final LocalPlayer player = game.player;
        final Level level = game.level;
        if (player == null || level == null || game.isPaused()) {
            return;
        }
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (final ClientStructure structure : ClientMovingStructures.all()) {
            if (!structure.ready() || structure.worldBox().distanceToSqr(player.position()) > NEAR * NEAR) {
                continue;
            }
            final StructureFrame frame = structure.frame();
            final int cx = (int) Math.floor(frame.yardX(player.getX(), player.getY(), player.getZ()));
            final int cy = (int) Math.floor(frame.yardY(player.getX(), player.getY(), player.getZ()));
            final int cz = (int) Math.floor(frame.yardZ(player.getX(), player.getY(), player.getZ()));
            for (int i = 0; i < TRIES; i++) {
                at.set(cx + RANDOM.nextInt(REACH) - RANDOM.nextInt(REACH),
                        cy + RANDOM.nextInt(REACH) - RANDOM.nextInt(REACH),
                        cz + RANDOM.nextInt(REACH) - RANDOM.nextInt(REACH));
                if (!structure.bounds().contains(at.getX(), at.getY(), at.getZ())) {
                    continue;
                }
                final BlockState state = level.getBlockState(at);
                if (!state.isAir()) {
                    state.getBlock().animateTick(state, level, at, RANDOM);
                }
                final FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty()) {
                    fluid.animateTick(level, at, RANDOM);
                }
            }
        }
    }
}
