/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * What has built up of each hazard in one living thing, saved with it, and how fast each built up in the last second,
 * which is not saved: the rate is weighed again a second after the thing is back.
 */
public final class HazardLevels {

    private final Map<ResourceLocation, Double> levels = new HashMap<>();
    private final Map<ResourceLocation, Double> rates = new HashMap<>();
    private final Map<ResourceLocation, Double> exposures = new HashMap<>();

    /** How it is saved: what has built up of each hazard, by the hazard's id. */
    public static final Codec<HazardLevels> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE)
            .xmap(HazardLevels::new, HazardLevels::levels);

    public HazardLevels() {
    }

    private HazardLevels(final Map<ResourceLocation, Double> saved) {
        this.levels.putAll(saved);
    }

    /** What has built up of the hazard {@code id}. */
    public synchronized double level(final ResourceLocation id) {
        return this.levels.getOrDefault(id, 0.0);
    }

    /** How fast the hazard {@code id} built up in the last second, what was worn having taken its share. */
    public synchronized double rate(final ResourceLocation id) {
        return this.rates.getOrDefault(id, 0.0);
    }

    /** Whether nothing has built up, of any hazard. */
    public synchronized boolean clear() {
        return this.levels.isEmpty();
    }

    /** Sets what has built up of {@code id}; nothing forgets it. */
    synchronized void set(final ResourceLocation id, final double level) {
        if (level <= 0) {
            this.levels.remove(id);
        } else {
            this.levels.put(id, level);
        }
    }

    /** How fast the hazard {@code id} came at the body in the last second, before what was worn took its share. */
    public synchronized double exposure(final ResourceLocation id) {
        return this.exposures.getOrDefault(id, 0.0);
    }

    /** Sets how fast {@code id} built up in the last second, and how fast it came before what was worn. */
    synchronized void rate(final ResourceLocation id, final double rate, final double exposure) {
        if (rate <= 0) {
            this.rates.remove(id);
        } else {
            this.rates.put(id, rate);
        }
        if (exposure <= 0) {
            this.exposures.remove(id);
        } else {
            this.exposures.put(id, exposure);
        }
    }

    /** What has built up, by hazard, as it is saved. */
    public synchronized Map<ResourceLocation, Double> levels() {
        return Map.copyOf(this.levels);
    }
}
