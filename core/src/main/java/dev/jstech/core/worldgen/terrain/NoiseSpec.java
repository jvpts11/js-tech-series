/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * A noise declared in code ({@link Terrains#noise}): its first octave, how large a feature of it is as a power of two
 * ({@code -7} about 128 blocks across), and how much each octave from there counts, each finer than the last.
 *
 * @param id          what its file is named
 * @param firstOctave its broadest octave
 * @param amplitudes  how much each octave counts, from the broadest
 */
public record NoiseSpec(ResourceLocation id, int firstOctave, List<Double> amplitudes) {

    public NoiseSpec {
        amplitudes = List.copyOf(amplitudes);
        if (amplitudes.isEmpty()) {
            throw new IllegalArgumentException("the noise " + id + " has at least one octave");
        }
    }

    /** The key it is registered under, for a density function or a surface rule to sample it by. */
    public ResourceKey<NormalNoise.NoiseParameters> key() {
        return ResourceKey.create(Registries.NOISE, this.id);
    }

    /** It as the game registers it. */
    public NormalNoise.NoiseParameters parameters() {
        return new NormalNoise.NoiseParameters(this.firstOctave, this.amplitudes);
    }
}
