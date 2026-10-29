/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * How a sleeper in a bed of a moving structure is drawn: the game lays a sleeper along their bed by the way the bed
 * looks, in the yard, and a structure turned has its beds turned with it, so the sleeper is turned as far again
 * ({@code LivingEntityRendererMixin}). One lying on a seat of a structure is drawn the same way, by their seat.
 */
public final class SleepersDrawn {

    /* How many times a sleeper aboard was turned with their bed as drawn, since the game started. */
    private static long turned;

    private SleepersDrawn() {
    }

    /**
     * How far the structure {@code sleeper}'s bed, or the seat they lie on, is on is turned where it is drawn now,
     * {@code partialTick} through the tick, as the game's yaw in degrees; nought for one asleep in a bed of the world,
     * or awake and not lying aboard.
     */
    public static float turnOf(final LivingEntity sleeper, final float partialTick) {
        BlockPos bed = sleeper.getSleepingPos().orElse(null);
        if (bed == null && sleeper.getVehicle() instanceof StructureSeatEntity seat && seat.lyingHead() != null) {
            bed = seat.seat();
        }
        if (bed == null || !YardLayout.isYardBlock(bed.getX())) {
            return 0F;
        }
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(bed.getX(),
                bed.getZ()));
        if (structure == null) {
            return 0F;
        }
        turned++;
        return (float) structure.drawnAt(partialTick).yawDegrees();
    }

    /** How many times a sleeper aboard was turned with their bed as drawn, since the game started. */
    public static long turned() {
        return turned;
    }
}
