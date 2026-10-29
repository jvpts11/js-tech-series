/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.Objects;

/**
 * A block of the formed model of another part of a plan, whose faces reach into a place of the plan: the place takes
 * that block's shape too while that part is formed, as an engine's blocks take the skirt rising round them from the
 * ring under it.
 *
 * @param part  the part whose model reaches in, by its number in the plan
 * @param place the block of that model the place is
 */
public record PlanReach(int part, FormedPlace place) {

    public static final Codec<PlanReach> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("part").forGetter(PlanReach::part),
            FormedPlace.CODEC.fieldOf("place").forGetter(PlanReach::place)
    ).apply(instance, PlanReach::new));

    public PlanReach {
        Objects.requireNonNull(place, "place");
    }
}
