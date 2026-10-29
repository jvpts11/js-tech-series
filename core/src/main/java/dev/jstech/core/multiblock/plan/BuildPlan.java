/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * A plan to build by: what goes where, part by part, laid out from an origin, as a hangar keeps the rocket it builds
 * or a dock the ship. Each part forms by itself once every block of it is in place, with no terminal of its own, and
 * comes apart when one goes ({@link PlannedBuild}); a part that merges takes its formed model's shape, place by place,
 * and the shapes of the models of other parts that reach into it. For any mod: the parts and their models are the
 * mod's.
 *
 * @param parts how many parts it has
 * @param spots its places, each with its part
 */
public record BuildPlan(int parts, List<PlanSpot> spots) {

    /* As it is written, before it is checked. */
    private static final Codec<Raw> RAW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("parts").forGetter(Raw::parts),
            PlanSpot.CODEC.listOf().fieldOf("spots").forGetter(Raw::spots)
    ).apply(instance, Raw::new));
    public static final Codec<BuildPlan> CODEC = RAW.comapFlatMap(BuildPlan::checked,
            plan -> new Raw(plan.parts(), plan.spots()));
    /** A plan of nothing. */
    public static final BuildPlan EMPTY = new BuildPlan(0, List.of());

    public BuildPlan {
        spots = List.copyOf(spots);
        for (final PlanSpot spot : spots) {
            if (spot.part() >= parts || spot.reaches().stream().anyMatch(reach -> reach.part() >= parts)) {
                throw new IllegalArgumentException("a plan of " + parts + " parts has a place of part "
                        + spot.part() + " or reached by a part past them, at " + spot.at());
            }
        }
    }

    /** The places of each part, by their numbers in {@link #spots}, part by part. */
    public List<List<Integer>> byPart() {
        final List<List<Integer>> out = new ArrayList<>();
        for (int part = 0; part < this.parts; part++) {
            out.add(new ArrayList<>());
        }
        for (int i = 0; i < this.spots.size(); i++) {
            out.get(this.spots.get(i).part()).add(i);
        }
        return out.stream().map(List::copyOf).toList();
    }

    /* A plan as written, when every place's part is one of its parts. */
    private static DataResult<BuildPlan> checked(final Raw raw) {
        try {
            return DataResult.success(new BuildPlan(raw.parts(), raw.spots()));
        } catch (final IllegalArgumentException wrong) {
            return DataResult.error(wrong::getMessage);
        }
    }

    private record Raw(int parts, List<PlanSpot> spots) {
    }
}
