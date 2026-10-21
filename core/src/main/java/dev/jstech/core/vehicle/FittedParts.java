/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The parts a vehicle has in its places, each with how worn it is, from 1 new to nought broken: what is saved with it
 * and sent to every player seeing it, so its look and its instruments follow its parts.
 */
public final class FittedParts {

    private final Map<String, Fitted> byPlace = new LinkedHashMap<>();
    private int version;

    private static final String PART = "part";
    private static final String HEALTH = "health";

    /** The part in {@code place}, or none. */
    public Optional<Fitted> at(final String place) {
        return Optional.ofNullable(byPlace.get(place));
    }

    /** Every part, by its place. */
    public Map<String, Fitted> all() {
        return Collections.unmodifiableMap(byPlace);
    }

    /** Goes up whenever a part comes, goes or wears, so what is worked out of them is worked out again. */
    public int version() {
        return version;
    }

    /** Puts {@code part}, worn to {@code health}, in {@code place}. */
    public void fit(final String place, final ResourceLocation part, final float health) {
        byPlace.put(Objects.requireNonNull(place, "place"), new Fitted(part, Math.clamp(health, 0, 1)));
        version++;
    }

    /** Takes the part out of {@code place}; what was there, or none. */
    public Optional<Fitted> take(final String place) {
        final Fitted taken = byPlace.remove(place);
        if (taken != null) {
            version++;
        }
        return Optional.ofNullable(taken);
    }

    /** Wears the part in {@code place} by {@code by}, down to nought; how worn it is now, or nought for none there. */
    public float wear(final String place, final float by) {
        final Fitted fitted = byPlace.get(place);
        if (fitted == null || by <= 0) {
            return fitted == null ? 0 : fitted.health();
        }
        final float now = Math.max(0, fitted.health() - by);
        if (now != fitted.health()) {
            byPlace.put(place, new Fitted(fitted.part(), now));
            version++;
        }
        return now;
    }

    /** Mends the part in {@code place} by {@code by}, up to new; how worn it is now. */
    public float mend(final String place, final float by) {
        final Fitted fitted = byPlace.get(place);
        if (fitted == null) {
            return 0;
        }
        final float now = Math.min(1, fitted.health() + by);
        byPlace.put(place, new Fitted(fitted.part(), now));
        version++;
        return now;
    }

    /** The parts as they are saved and sent. */
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<String, Fitted> each : byPlace.entrySet()) {
            final CompoundTag part = new CompoundTag();
            part.putString(PART, each.getValue().part().toString());
            part.putFloat(HEALTH, each.getValue().health());
            tag.put(each.getKey(), part);
        }
        return tag;
    }

    /** Takes the parts {@code tag} saved, in place of those it has. */
    public void load(final CompoundTag tag) {
        byPlace.clear();
        for (final String place : tag.getAllKeys()) {
            final CompoundTag part = tag.getCompound(place);
            final ResourceLocation id = ResourceLocation.tryParse(part.getString(PART));
            if (id != null) {
                byPlace.put(place, new Fitted(id, Math.clamp(part.getFloat(HEALTH), 0, 1)));
            }
        }
        version++;
    }

    /**
     * A part in its place.
     *
     * @param part   the part, by id
     * @param health how worn it is, 1 new and nought broken
     */
    public record Fitted(ResourceLocation part, float health) {

        /** Whether it still works at all. */
        public boolean works() {
            return health > 0;
        }
    }
}
