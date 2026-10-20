/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import net.minecraft.resources.ResourceLocation;

/**
 * Turns in place until the structure's forward, its own south, points along a direction of the world, its up kept as
 * near the world's up as that allows: an antenna following a satellite, a telescope, a turret. A mod following
 * something that moves gives it a new direction as often as it likes.
 *
 * @param directionX the direction to face, east part; it need not be of unit length
 * @param directionY the direction to face, up part
 * @param directionZ the direction to face, south part
 * @param rate       the most it turns, in degrees a second; 0 turns it at once
 */
public record FaceMotion(double directionX, double directionY, double directionZ, double rate)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "face");
    public static final MapCodec<FaceMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("direction_x").forGetter(FaceMotion::directionX),
            Codec.DOUBLE.fieldOf("direction_y").forGetter(FaceMotion::directionY),
            Codec.DOUBLE.fieldOf("direction_z").forGetter(FaceMotion::directionZ),
            Codec.DOUBLE.fieldOf("rate").forGetter(FaceMotion::rate)
    ).apply(instance, FaceMotion::new));
    /* Ticks in a second. */
    private static final double TICKS = 20;

    /** @throws IllegalArgumentException when the direction has no length, or the rate is below zero */
    public FaceMotion {
        if (directionX * directionX + directionY * directionY + directionZ * directionZ < 1.0e-12 || rate < 0) {
            throw new IllegalArgumentException("a structure faces a direction at a rate: " + rate);
        }
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final StructurePose aimed = MotionMath.facing(now, directionX, directionY, directionZ);
        final double mostTurn = rate == 0 ? Math.PI : Math.toRadians(rate) / TICKS;
        final StructurePose next = MotionMath.towards(now, aimed, 0, mostTurn);
        return new Step(next, next.angleTo(aimed) < MotionMath.TURNED ? null : this);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
