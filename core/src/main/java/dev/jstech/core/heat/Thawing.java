/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What becomes of ice warmed where it lies, by the air it meets. Water stays liquid only in air thicker than its triple
 * point, 0.6117 kPa: ice warmed in such air melts, and ice warmed in a vacuum, or in air as thin as Mars', turns
 * straight to vapour and is gone. Ice is warmed by air above freezing on one of its faces, a sealed room's or a warm
 * world's, or by a light as bright as the one that melts the game's ice beside a torch. A block of ice meets the air
 * on its open faces, the warmest of them, so ice lying in a sealed room meets the room's air, though it walls it.
 */
public final class Thawing {

    /** The pressure, in kilopascals, below which water cannot be a liquid: ice turns straight to vapour. */
    public static final double WATER_TRIPLE_POINT_KPA = 0.6117;
    /** Water's freezing point, in kelvin. */
    public static final double FREEZING_KELVIN = 273.15;

    /* A block light brighter than this warms ice, as the game's own ice melts beside a torch. */
    private static final int WARMING_LIGHT = 11;

    private Thawing() {
    }

    /** Whether ice at {@code pos} is warmed: by air above freezing on one of its faces, or by a bright light. */
    public static boolean warmed(final Level level, final BlockPos pos) {
        return airMet(level, pos).kelvin() > FREEZING_KELVIN || lightMet(level, pos) > WARMING_LIGHT;
    }

    /** Whether ice melting at {@code pos} becomes water: the air it meets is thicker than water's triple point. */
    public static boolean meltsToWater(final Level level, final BlockPos pos) {
        return airMet(level, pos).kiloPascals() >= WATER_TRIPLE_POINT_KPA;
    }

    /**
     * The air ice at {@code pos} meets: the warmest of the airs on its open faces, and its own place's when no face
     * is open.
     */
    public static HeatClimate.Air airMet(final Level level, final BlockPos pos) {
        HeatClimate.Air met = null;
        for (final Direction side : Direction.values()) {
            final BlockPos beside = pos.relative(side);
            if (open(level, beside)) {
                final HeatClimate.Air air = HeatClimate.air(level, beside);
                if (met == null || air.kelvin() > met.kelvin()) {
                    met = air;
                }
            }
        }
        return met != null ? met : HeatClimate.air(level, pos);
    }

    /* The brightest block light on the open faces of {@code pos}, and in it. */
    private static int lightMet(final Level level, final BlockPos pos) {
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        for (final Direction side : Direction.values()) {
            final BlockPos beside = pos.relative(side);
            if (open(level, beside)) {
                light = Math.max(light, level.getBrightness(LightLayer.BLOCK, beside));
            }
        }
        return light;
    }

    /* Whether the place {@code pos} holds air: nothing solid fills it. */
    private static boolean open(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.isSolidRender(level, pos);
    }
}
