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
 * Turns on and on about an axis of the world through the structure's pivot, until told otherwise: a windmill, a
 * carousel, a radar dish sweeping the sky.
 *
 * @param axisX the axis, east part; it need not be of unit length
 * @param axisY the axis, up part
 * @param axisZ the axis, south part
 * @param rate  how fast it turns, in degrees a second; below zero it turns the other way
 */
public record SpinMotion(double axisX, double axisY, double axisZ, double rate) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "spin");
    public static final MapCodec<SpinMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("axis_x").forGetter(SpinMotion::axisX),
            Codec.DOUBLE.fieldOf("axis_y").forGetter(SpinMotion::axisY),
            Codec.DOUBLE.fieldOf("axis_z").forGetter(SpinMotion::axisZ),
            Codec.DOUBLE.fieldOf("rate").forGetter(SpinMotion::rate)
    ).apply(instance, SpinMotion::new));
    /* Ticks in a second. */
    private static final double TICKS = 20;

    /** @throws IllegalArgumentException when the axis has no length */
    public SpinMotion {
        if (axisX * axisX + axisY * axisY + axisZ * axisZ < 1.0e-12) {
            throw new IllegalArgumentException("a spin has an axis");
        }
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        return new Step(MotionMath.spin(now, axisX, axisY, axisZ, Math.toRadians(rate) / TICKS), this);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
