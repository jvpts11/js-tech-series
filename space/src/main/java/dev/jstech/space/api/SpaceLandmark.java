/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A landmark of a world: a landform of its own with its real name, lying once, at its place on the world's sphere, as
 * large as it is drawn there, so what is seen from orbit is what is found on the ground. The world's generator lays
 * it.
 *
 * <pre>{@code
 * {"name": "jsspace.landmark.tycho", "world": "jsspace:moon", "kind": "crater",
 *  "longitude": -11.4, "latitude": -43.3, "radius": 2.6}
 * }</pre>
 *
 * @param name      the key of its name in the language files
 * @param world     the world it lies on ({@link SpaceWorlds})
 * @param kind      what it is
 * @param longitude where it lies, east of the world's middle, in degrees
 * @param latitude  north of its equator, in degrees
 * @param radius    how far it reaches from its middle, in degrees of the world
 */
public record SpaceLandmark(String name, ResourceLocation world, Kind kind, double longitude, double latitude,
                            double radius) {

    /** As a data pack's file writes one. */
    public static final Codec<SpaceLandmark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SpaceLandmark::name),
            ResourceLocation.CODEC.fieldOf("world").forGetter(SpaceLandmark::world),
            Kind.CODEC.fieldOf("kind").forGetter(SpaceLandmark::kind),
            Codec.doubleRange(-180, 180).fieldOf("longitude").forGetter(SpaceLandmark::longitude),
            Codec.doubleRange(-90, 90).fieldOf("latitude").forGetter(SpaceLandmark::latitude),
            Codec.doubleRange(1e-4, 90).fieldOf("radius").forGetter(SpaceLandmark::radius)
    ).apply(instance, SpaceLandmark::new));

    public SpaceLandmark {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(kind, "kind");
    }

    /** What a landmark is, which says how its world's generator lays it. */
    public enum Kind implements IStableName {
        /** A great crater: a bowl under a raised rim, with a peak in its middle. */
        CRATER("crater");

        public static final Codec<Kind> CODEC = StableCodecs.byName(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
