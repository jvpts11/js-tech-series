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
 * Goes to a pose and stops there: a lift to its floor, a door sliding open, a gantry to its place. The structure moves
 * no faster than {@code speed} and turns no faster than {@code turnRate}, and arrives exactly.
 *
 * @param target   the pose to reach
 * @param speed    the most it moves, in blocks a second
 * @param turnRate the most it turns, in degrees a second; 0 turns it at once
 */
public record TowardMotion(StructurePose target, double speed, double turnRate) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "toward");
    public static final MapCodec<TowardMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StructureCodecs.POSE.fieldOf("target").forGetter(TowardMotion::target),
            Codec.DOUBLE.fieldOf("speed").forGetter(TowardMotion::speed),
            Codec.DOUBLE.fieldOf("turn_rate").forGetter(TowardMotion::turnRate)
    ).apply(instance, TowardMotion::new));
    /* Ticks in a second. */
    private static final double TICKS = 20;

    /** @throws IllegalArgumentException when the speed is not above zero, or the rate below it */
    public TowardMotion {
        if (!(speed > 0) || turnRate < 0) {
            throw new IllegalArgumentException("a structure goes somewhere at a speed: " + speed + ", " + turnRate);
        }
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final double mostTurn = turnRate == 0 ? Math.PI : Math.toRadians(turnRate) / TICKS;
        final StructurePose next = MotionMath.towards(now, target, speed / TICKS, mostTurn);
        return new Step(next, MotionMath.arrived(next, target) ? null : this);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
