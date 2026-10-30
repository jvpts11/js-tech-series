/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A landing as set on the LAND page, for the descent to fly: the world it comes down on and the spot marked there,
 * round which the ellipse of its guidance lies.
 *
 * @param world the world it comes down on, by its id among the worlds
 * @param x     the spot marked, west to east
 * @param z     the spot marked, north to south
 */
public record PlannedLanding(ResourceLocation world, int x, int z) {

    public static final Codec<PlannedLanding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("world").forGetter(PlannedLanding::world),
            Codec.INT.fieldOf("x").forGetter(PlannedLanding::x),
            Codec.INT.fieldOf("z").forGetter(PlannedLanding::z)
    ).apply(instance, PlannedLanding::new));

    public PlannedLanding {
        Objects.requireNonNull(world, "world");
    }
}
