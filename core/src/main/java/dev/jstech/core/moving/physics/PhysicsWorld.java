/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.effect.EffectPhysics;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.Pull;
import dev.jstech.core.util.Loaded;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * What the world is like for a body moving through it, at a place: the pull on it, the air's density, the liquid it
 * is in and its surface, and the wind. The pull is the laws' of the place where they say one, else the dimension's
 * at that height; the air's density is the dimension's pressure and temperature at that height, none in a vacuum.
 * Nothing of a chunk not loaded is read: there, it is as if there were nothing.
 */
public final class PhysicsWorld {

    /** The pull at the overworld's ground, in metres a second a second. */
    public static final double EARTH_GRAVITY = 9.81;
    /* Kelvin of a degree Celsius at nought. */
    private static final double ZERO_CELSIUS = 273.15;
    /* A centre pulls at most four times as hard within its radius. */
    private static final double CENTRE_MOST = 4.0;
    /* Closer to a point than this, its pull has no way to go. */
    private static final double NEAR = 1.0e-6;

    private PhysicsWorld() {
    }

    /** The pull at the world's place {@code at}, in metres a second a second, into {@code into}. */
    public static Vector3d gravity(final Level level, final Vector3d at, final Vector3d into) {
        final Laws laws = Physics.lawsAt(level, new Vec3(at.x, at.y, at.z));
        final Optional<Pull> pull = laws.gravity();
        if (pull.isEmpty()) {
            final DimensionRules rules = DimensionRulesData.of(level);
            return into.set(0, -EARTH_GRAVITY * rules.gravityAt(at.y), 0);
        }
        return switch (pull.get()) {
            case Pull.Scaled scaled -> into.set(0, -EARTH_GRAVITY * scaled.g(), 0);
            case Pull.Towards towards -> {
                into.set(towards.x(), towards.y(), towards.z());
                yield into.lengthSquared() < NEAR ? into.set(0, 0, 0)
                        : into.normalize(EARTH_GRAVITY * towards.g());
            }
            case Pull.Centre centre -> {
                into.set(centre.x() - at.x, centre.y() - at.y, centre.z() - at.z);
                final double distance = into.length();
                if (distance < NEAR) {
                    yield into.set(0, 0, 0);
                }
                final double share = Math.min(CENTRE_MOST, centre.radius() * centre.radius() / (distance * distance));
                yield into.normalize(EARTH_GRAVITY * centre.g() * share);
            }
        };
    }

    /** The density of the air at the height {@code y}, in kilograms a cubic metre; none in a vacuum. */
    public static double airDensity(final Level level, final double y) {
        final Climate climate = DimensionRulesData.of(level).at(y);
        return EffectPhysics.airDensity(climate.pressure(), climate.temperature() + ZERO_CELSIUS);
    }

    /** The air's temperature at the height {@code y}, in kelvin. */
    public static double airTemperature(final Level level, final double y) {
        return DimensionRulesData.of(level).at(y).temperature() + ZERO_CELSIUS;
    }

    /** The air's pressure at the height {@code y}, in kilopascals. */
    public static double airPressure(final Level level, final double y) {
        return DimensionRulesData.of(level).at(y).pressure();
    }

    /**
     * How much of a block of side 1 standing at the world's place {@code at} is under a liquid, from 0 to 1, and the
     * liquid's density: what floats it. Gases do not float anything here; a balloon floats by its own.
     */
    public static Liquid liquidAt(final Level level, final Vector3d at) {
        final BlockPos pos = BlockPos.containing(at.x, at.y, at.z);
        if (!Loaded.at(level, pos)) {
            return Liquid.NONE;
        }
        final FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty()) {
            // The bottom of the block may still be in the liquid of the block below.
            final BlockPos below = pos.below();
            final double into = 1 - (at.y + 0.5 - pos.getY());
            if (into <= 0 || !Loaded.at(level, below)) {
                return Liquid.NONE;
            }
            final FluidState under = level.getFluidState(below);
            if (under.isEmpty()) {
                return Liquid.NONE;
            }
            final double surface = below.getY() + under.getHeight(level, below);
            return liquid(under, surface - (at.y - 0.5));
        }
        final boolean full = !level.getFluidState(pos.above()).isEmpty();
        final double surface = full ? pos.getY() + 1 : pos.getY() + fluid.getHeight(level, pos);
        return liquid(fluid, surface - (at.y - 0.5));
    }

    /** The wind at the world's place {@code at}, in metres a second, into {@code into}. */
    public static Vector3d wind(final Level level, final Vector3d at, final Vector3d into) {
        final Wind wind = Winds.at(level, new Vec3(at.x, at.y, at.z));
        return into.set(wind.x(), 0, wind.z());
    }

    private static Liquid liquid(final FluidState fluid, final double depth) {
        final int density = fluid.getFluidType().getDensity();
        if (density <= 0) {
            return Liquid.NONE;
        }
        return new Liquid(Math.max(0, Math.min(1, depth)), density);
    }

    /**
     * How much of a block is in a liquid, and how dense the liquid is.
     *
     * @param share   how much of the block is under it, from 0 to 1
     * @param density its density, in kilograms a cubic metre
     */
    public record Liquid(double share, double density) {

        /** No liquid. */
        public static final Liquid NONE = new Liquid(0, 0);
    }
}
