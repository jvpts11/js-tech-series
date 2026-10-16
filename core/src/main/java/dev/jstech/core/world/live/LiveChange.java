/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.live;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.time.Season;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * A change the surface of the world goes through by itself, as the climate and the weather say: ice melting where it
 * is warm, basins filling with water in the rain, grass drying to dirt in the heat. Each round, a column of each chunk
 * near the players is looked at, and a change whose block and conditions all hold there happens as often as its
 * chance says.
 *
 * <pre>{@code
 * { "test": {"predicate_type": "minecraft:block_match", "block": "minecraft:air"},
 *   "becomes": {"Name": "minecraft:water", "Properties": {"level": "0"}}, "at": "above",
 *   "on": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:dirt"}, "chance": 0.05, "rain": true,
 *   "sky": true, "basin": true }
 * }</pre>
 *
 * @param test       the block that changes
 * @param becomes    what it changes into
 * @param at         whether the block is the column's topmost, or the one just above it
 * @param on         what the block under it must be, if anything
 * @param chance     how often it changes each time its column is looked at, 0 to 1
 * @param minKelvin  the coldest the place may be, if it matters
 * @param maxKelvin  the hottest
 * @param rain       whether it must be raining there, or must not; empty when the weather does not matter
 * @param sky        whether it must lie under the open sky
 * @param basin      whether it must lie in a basin: each block beside it solid, or already what it becomes
 * @param seasons    the seasons it happens in; empty for all
 * @param dimensions the dimensions it happens in; empty for all
 */
public record LiveChange(RuleTest test, BlockState becomes, Where at, Optional<RuleTest> on, double chance,
                         Optional<Double> minKelvin, Optional<Double> maxKelvin, Optional<Boolean> rain, boolean sky,
                         boolean basin, List<Season> seasons, List<ResourceKey<Level>> dimensions) {

    public static final Codec<LiveChange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RuleTest.CODEC.fieldOf("test").forGetter(LiveChange::test),
            BlockState.CODEC.fieldOf("becomes").forGetter(LiveChange::becomes),
            StableCodecs.byName(Where.class).optionalFieldOf("at", Where.TOP).forGetter(LiveChange::at),
            RuleTest.CODEC.optionalFieldOf("on").forGetter(LiveChange::on),
            Codec.doubleRange(0, 1).fieldOf("chance").forGetter(LiveChange::chance),
            Codec.DOUBLE.optionalFieldOf("min_kelvin").forGetter(LiveChange::minKelvin),
            Codec.DOUBLE.optionalFieldOf("max_kelvin").forGetter(LiveChange::maxKelvin),
            Codec.BOOL.optionalFieldOf("rain").forGetter(LiveChange::rain),
            Codec.BOOL.optionalFieldOf("sky", false).forGetter(LiveChange::sky),
            Codec.BOOL.optionalFieldOf("basin", false).forGetter(LiveChange::basin),
            StableCodecs.byName(Season.class).listOf().optionalFieldOf("seasons", List.of())
                    .forGetter(LiveChange::seasons),
            ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimensions", List.of())
                    .forGetter(LiveChange::dimensions))
            .apply(instance, LiveChange::new));

    public LiveChange {
        seasons = List.copyOf(seasons);
        dimensions = List.copyOf(dimensions);
    }

    /** Which block of a column a change looks at. */
    public enum Where implements IStableName {

        /** The column's topmost block, the first that blocks movement from above. */
        TOP("top"),
        /** The block just above it, often air: where water gathers or snow lies. */
        ABOVE("above");

        private final String serializedName;

        Where(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
