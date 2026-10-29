/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The kinds of density function the Core adds to the game's, for any mod's terrain and any datapack:
 * {@code jscore:terrace}, {@code jscore:ridge}, {@code jscore:power}, {@code jscore:remap}, {@code jscore:smooth_min},
 * {@code jscore:smooth_max}, {@code jscore:distance}, {@code jscore:round_map} and {@code jscore:round_crater}, a round
 * world's map and its great craters, and {@code jscore:original}, the function a worldgen modifier changes. In code
 * a terrain's recipe builds them with the methods here, beside the game's {@code DensityFunctions}.
 *
 * <pre>{@code
 * Terrains.density(rl("mesas"), (noises, densities) -> CoreDensities.terrace(
 *         DensityFunctions.noise(noises.getOrThrow(MESA_NOISE)), 0.25, 0.2));
 * }</pre>
 */
public final class CoreDensities {

    private static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, JsCore.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<TerraceDensity>> TERRACE =
            TYPES.register("terrace", () -> TerraceDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RidgeDensity>> RIDGE =
            TYPES.register("ridge", () -> RidgeDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<PowerDensity>> POWER =
            TYPES.register("power", () -> PowerDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RemapDensity>> REMAP =
            TYPES.register("remap", () -> RemapDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<SmoothJoinDensity>> SMOOTH_MIN =
            TYPES.register("smooth_min", () -> SmoothJoinDensity.MIN_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<SmoothJoinDensity>> SMOOTH_MAX =
            TYPES.register("smooth_max", () -> SmoothJoinDensity.MAX_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DistanceDensity>> DISTANCE =
            TYPES.register("distance", () -> DistanceDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<OriginalDensity>> ORIGINAL =
            TYPES.register("original", () -> OriginalDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RoundMapDensity>> ROUND_MAP =
            TYPES.register("round_map", () -> RoundMapDensity.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RoundCraterDensity>>
            ROUND_CRATER = TYPES.register("round_crater", () -> RoundCraterDensity.DATA_CODEC);

    private CoreDensities() {
    }

    /** Registers the kinds, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    /** {@code input} in steps {@code step} high, rising over the last {@code smoothness} of each. */
    public static DensityFunction terrace(final DensityFunction input, final double step, final double smoothness) {
        return new TerraceDensity(input, step, smoothness);
    }

    /** {@code input} folded into ridges. */
    public static DensityFunction ridge(final DensityFunction input) {
        return new RidgeDensity(input);
    }

    /** {@code input} raised to {@code exponent}, keeping its sign. */
    public static DensityFunction power(final DensityFunction input, final double exponent) {
        return new PowerDensity(input, exponent);
    }

    /** {@code input} moved from {@code fromLow..fromHigh} to {@code toLow..toHigh}, held inside it. */
    public static DensityFunction remap(final DensityFunction input, final double fromLow, final double fromHigh,
                                        final double toLow, final double toHigh) {
        return new RemapDensity(input, fromLow, fromHigh, toLow, toHigh, true);
    }

    /** The smaller of {@code a} and {@code b}, joined with no crease over {@code smoothness}. */
    public static DensityFunction smoothMin(final DensityFunction a, final DensityFunction b,
                                            final double smoothness) {
        return new SmoothJoinDensity(false, a, b, smoothness);
    }

    /** The larger of {@code a} and {@code b}, joined with no crease over {@code smoothness}. */
    public static DensityFunction smoothMax(final DensityFunction a, final DensityFunction b,
                                            final double smoothness) {
        return new SmoothJoinDensity(true, a, b, smoothness);
    }

    /** How far a place is from {@code (x, z)}, across, in units of {@code radius}. */
    public static DensityFunction distance(final double x, final double z, final double radius) {
        return new DistanceDensity(x, z, radius);
    }

    /** The map {@code map} of the round world {@code world}, from {@code low} where black to {@code high} white. */
    public static DensityFunction roundMap(final ResourceLocation map, final ResourceLocation world, final double low,
                                           final double high) {
        return new RoundMapDensity(map, world, low, high);
    }

    /**
     * How far a great crater of the round world {@code world}, at {@code longitude} and {@code latitude} and
     * {@code radius} degrees across, moves the ground, in blocks: down {@code depth}, up {@code rim}, a {@code peak}.
     */
    public static DensityFunction roundCrater(final ResourceLocation world, final double longitude,
                                              final double latitude, final double radius, final double depth,
                                              final double rim, final double peak) {
        return new RoundCraterDensity(world, longitude, latitude, radius, depth, rim, peak);
    }
}
