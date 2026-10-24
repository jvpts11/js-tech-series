/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * What a discovery costs: its research, in points, and how many of each kind of data, each covering an equal share of
 * the way. A mod declares it with its discovery; a data pack's file of the same id, under
 * {@code data/<namespace>/jstech/research/}, says otherwise:
 *
 * <pre>{@code
 * {"points": 36000, "data": {"myaddon:crystal": 10}}
 * }</pre>
 *
 * @param points the research it costs, at least one point
 * @param data   how many of each kind of data it asks for, by the kind's id, at least one of each named
 */
public record ResearchCost(long points, Map<ResourceLocation, Integer> data) {

    /** A cost as its file holds it; one of no points, or of none of a kind it names, is refused. */
    public static final Codec<ResearchCost> CODEC = RecordCodecBuilder.<RawCost>create(instance -> instance.group(
                    Codec.LONG.fieldOf("points").forGetter(RawCost::points),
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("data", Map.of())
                            .forGetter(RawCost::data))
            .apply(instance, RawCost::new))
            .flatXmap(ResearchCost::made, cost -> DataResult.success(new RawCost(cost.points(), cost.data())));

    public ResearchCost {
        if (points < 1) {
            throw new IllegalArgumentException("a discovery costs at least a point; got " + points);
        }
        for (final Map.Entry<ResourceLocation, Integer> kind : data.entrySet()) {
            if (kind.getValue() < 1) {
                throw new IllegalArgumentException("a discovery asks for at least one of " + kind.getKey()
                        + " it names; got " + kind.getValue());
            }
        }
        data = Map.copyOf(new TreeMap<>(data));
    }

    /** A cost of {@code points} and no data. */
    public static ResearchCost of(final long points) {
        return new ResearchCost(points, Map.of());
    }

    /** The same, asking for {@code count} of {@code kind} besides. */
    public ResearchCost with(final DataKind kind, final int count) {
        final Map<ResourceLocation, Integer> more = new TreeMap<>(this.data);
        more.merge(kind.id(), count, Integer::sum);
        return new ResearchCost(this.points, more);
    }

    /** How many of {@code kind} it asks for; none for a kind it does not name. */
    public int data(final ResourceLocation kind) {
        return this.data.getOrDefault(kind, 0);
    }

    /* The cost a file says, or why it cannot be one. */
    private static DataResult<ResearchCost> made(final RawCost raw) {
        try {
            return DataResult.success(new ResearchCost(raw.points(), raw.data()));
        } catch (final IllegalArgumentException wrong) {
            return DataResult.error(wrong::getMessage);
        }
    }

    /* A cost as read, before it is checked. */
    private record RawCost(long points, Map<ResourceLocation, Integer> data) {
    }
}
