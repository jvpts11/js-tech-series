/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The light of a Rocket Hangar's bay, as the Vehicle Assembly Building's floodlights light its high bay through: the
 * game's own invisible light blocks laid in a grid through the whole bay wherever nothing stands, so every place of it
 * is lit, from the floor the crew walks on to the top of the tallest rocket, and no creature of the dark is born in it.
 * A lamp on a wall lights no more than a few blocks of a bay thirty wide and a hundred high. A block laid where a light
 * stands takes its place, a crawler leaves them behind as it rolls out, the lights come back where the bay is empty
 * again, and they go when the hangar comes apart.
 */
final class HangarLights {

    /* How far apart the lights stand, each way, so the darkest place between them is still lit; the first's place. */
    private static final int EVERY = 5;
    private static final int FIRST = 2;

    private HangarLights() {
    }

    /** Lays the bay's lights wherever nothing stands in their places, and the place is loaded. */
    static void light(final Level level, final HangarBay bay) {
        each(bay, at -> {
            if (level.isLoaded(at) && level.getBlockState(at).isAir()) {
                level.setBlock(at.immutable(), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    /** Takes the bay's lights away, those still in their places. */
    static void darken(final Level level, final HangarBay bay) {
        each(bay, at -> {
            if (level.isLoaded(at) && level.getBlockState(at).is(Blocks.LIGHT)) {
                level.setBlock(at.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    /* Every place of the bay's grid of lights. */
    private static void each(final HangarBay bay, final Consumer<BlockPos> at) {
        final BlockPos.MutableBlockPos place = new BlockPos.MutableBlockPos();
        for (int x = bay.low().getX() + FIRST; x <= bay.high().getX(); x += EVERY) {
            for (int y = bay.low().getY() + FIRST; y <= bay.high().getY(); y += EVERY) {
                for (int z = bay.low().getZ() + FIRST; z <= bay.high().getZ(); z += EVERY) {
                    at.accept(place.set(x, y, z));
                }
            }
        }
    }
}
