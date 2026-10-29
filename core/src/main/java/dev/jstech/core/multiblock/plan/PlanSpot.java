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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * A place of a plan: where it is from the plan's origin, the block the plan wants there, the part of the plan it
 * belongs to, which block of that part's formed model it merges as, and the blocks of other parts' models reaching
 * into it.
 *
 * @param at      where it is, from the plan's origin
 * @param block   the block wanted there
 * @param part    the part it belongs to, by its number in the plan
 * @param place   the block of its part's formed model it merges as; none for a part merged whole
 * @param reaches the blocks of the other parts' models reaching into it
 */
public record PlanSpot(BlockPos at, ResourceLocation block, int part, Optional<FormedPlace> place,
                       List<PlanReach> reaches) {

    public static final Codec<PlanSpot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("at").forGetter(PlanSpot::at),
            ResourceLocation.CODEC.fieldOf("block").forGetter(PlanSpot::block),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("part").forGetter(PlanSpot::part),
            FormedPlace.CODEC.optionalFieldOf("place").forGetter(PlanSpot::place),
            PlanReach.CODEC.listOf().optionalFieldOf("reaches", List.of()).forGetter(PlanSpot::reaches)
    ).apply(instance, PlanSpot::new));

    public PlanSpot {
        at = at.immutable();
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(place, "place");
        reaches = List.copyOf(reaches);
    }
}
