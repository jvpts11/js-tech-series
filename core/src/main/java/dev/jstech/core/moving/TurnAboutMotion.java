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
 * Turns by an angle about a hinge of the world and stops: a drawbridge rising on its hinges, a door on its post, a
 * hatch. The hinge is an axis through a place, and the structure swings round it as a whole.
 *
 * @param hingeX the place the hinge passes through, east
 * @param hingeY the place the hinge passes through, up
 * @param hingeZ the place the hinge passes through, south
 * @param axisX  the hinge's direction, east part; it need not be of unit length
 * @param axisY  the hinge's direction, up part
 * @param axisZ  the hinge's direction, south part
 * @param left   the angle still to turn, in degrees; below zero it turns the other way
 * @param rate   how fast it turns, in degrees a second
 */
public record TurnAboutMotion(double hingeX, double hingeY, double hingeZ, double axisX, double axisY, double axisZ,
                              double left, double rate) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "turn_about");
    public static final MapCodec<TurnAboutMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("hinge_x").forGetter(TurnAboutMotion::hingeX),
            Codec.DOUBLE.fieldOf("hinge_y").forGetter(TurnAboutMotion::hingeY),
            Codec.DOUBLE.fieldOf("hinge_z").forGetter(TurnAboutMotion::hingeZ),
            Codec.DOUBLE.fieldOf("axis_x").forGetter(TurnAboutMotion::axisX),
            Codec.DOUBLE.fieldOf("axis_y").forGetter(TurnAboutMotion::axisY),
            Codec.DOUBLE.fieldOf("axis_z").forGetter(TurnAboutMotion::axisZ),
            Codec.DOUBLE.fieldOf("left").forGetter(TurnAboutMotion::left),
            Codec.DOUBLE.fieldOf("rate").forGetter(TurnAboutMotion::rate)
    ).apply(instance, TurnAboutMotion::new));
    /* Ticks in a second. */
    private static final double TICKS = 20;

    /** @throws IllegalArgumentException when the hinge has no direction, or the rate is not above zero */
    public TurnAboutMotion {
        if (axisX * axisX + axisY * axisY + axisZ * axisZ < 1.0e-12 || !(rate > 0)) {
            throw new IllegalArgumentException("a hinge has a direction, and turns at a rate: " + rate);
        }
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final double most = rate / TICKS;
        final double turn = Math.abs(left) <= most ? left : Math.copySign(most, left);
        final StructurePose next = MotionMath.about(now, hingeX, hingeY, hingeZ, axisX, axisY, axisZ,
                Math.toRadians(turn));
        final double still = left - turn;
        return new Step(next, Math.abs(still) < 1.0e-9 ? null
                : new TurnAboutMotion(hingeX, hingeY, hingeZ, axisX, axisY, axisZ, still, rate));
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
