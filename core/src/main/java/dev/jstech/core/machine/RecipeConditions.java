/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The temperature and the pressure a recipe needs inside its machine: it runs only between its least and its most of
 * each. A recipe that names none runs at any.
 *
 * @param leastKelvin the coldest it runs at, in kelvin
 * @param mostKelvin  the hottest it runs at, in kelvin
 * @param leastKpa    the lowest pressure it runs at, in kilopascals
 * @param mostKpa     the highest pressure it runs at, in kilopascals
 */
public record RecipeConditions(double leastKelvin, double mostKelvin, double leastKpa, double mostKpa) {

    /** No condition: any temperature, any pressure. */
    public static final RecipeConditions ANY = new RecipeConditions(0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE);

    public static final Codec<RecipeConditions> CODEC = RecordCodecBuilder.<RecipeConditions>create(instance ->
            instance.group(
                    Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("min_temperature", 0.0)
                            .forGetter(RecipeConditions::leastKelvin),
                    Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("max_temperature", Double.MAX_VALUE)
                            .forGetter(RecipeConditions::mostKelvin),
                    Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("min_pressure", 0.0)
                            .forGetter(RecipeConditions::leastKpa),
                    Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("max_pressure", Double.MAX_VALUE)
                            .forGetter(RecipeConditions::mostKpa))
            .apply(instance, RecipeConditions::new)).validate(conditions ->
            conditions.leastKelvin() <= conditions.mostKelvin() && conditions.leastKpa() <= conditions.mostKpa()
                    ? DataResult.success(conditions)
                    : DataResult.error(() -> "a recipe's least temperature and pressure are below their most"));
    public static final StreamCodec<ByteBuf, RecipeConditions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RecipeConditions::leastKelvin,
            ByteBufCodecs.DOUBLE, RecipeConditions::mostKelvin,
            ByteBufCodecs.DOUBLE, RecipeConditions::leastKpa,
            ByteBufCodecs.DOUBLE, RecipeConditions::mostKpa,
            RecipeConditions::new);

    /** Whether a machine at {@code kelvin} and {@code kpa} runs the recipe. */
    public boolean holds(final double kelvin, final double kpa) {
        return kelvin >= this.leastKelvin && kelvin <= this.mostKelvin && kpa >= this.leastKpa
                && kpa <= this.mostKpa;
    }

    /** Whether it asks anything at all. */
    public boolean any() {
        return this.equals(ANY);
    }
}
