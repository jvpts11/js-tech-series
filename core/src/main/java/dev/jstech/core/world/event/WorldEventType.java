/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.Season;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * A kind of event the world goes through, by its schedule or by chance: a meteor shower, a solar storm, an earthquake.
 * It happens in its dimensions, in its seasons, either over the whole dimension or round a place in its biomes near a
 * player; those carrying its instrument are warned ahead, everyone it covers is told as it begins, and its effect
 * plays round them while it lasts. What it does besides is its mod's, heard on the game's bus
 * ({@link WorldEventStage}).
 *
 * <pre>{@code
 * { "name": "mymod.event.solar_storm", "english": "Solar storm", "dimensions": ["minecraft:overworld"],
 *   "chance_per_day": 0.05, "warning": 6000, "instrument": "mymod:magnetometers", "duration": 12000,
 *   "effect": "mymod:aurora" }
 * }</pre>
 *
 * @param name         its name's translation key
 * @param english      its name in English
 * @param dimensions   where it happens; empty for every dimension
 * @param biomes       for one round a place, the biomes its place lies in; empty for any
 * @param seasons      the seasons it happens in; empty for all
 * @param chancePerDay how likely it is to happen on a day, 0 to 1, when it is by chance
 * @param everyDays    for one on a schedule, every how many days it happens; 0 for one by chance
 * @param atHour       for one on a schedule, the hour of the day it begins at
 * @param warning      how long before it begins those carrying its instrument are warned, in ticks
 * @param instrument   what warns of it, carried anywhere in the inventory; empty for no warning
 * @param duration     how long it lasts, in ticks
 * @param radius       how far round its place it reaches, in blocks; 0 for the whole dimension
 * @param announced    whether everyone it covers is told as it begins
 * @param effect       the effect played round each player it covers while it lasts, if any
 */
public record WorldEventType(String name, String english, List<ResourceKey<Level>> dimensions,
                             Optional<TagKey<Biome>> biomes, List<Season> seasons, double chancePerDay,
                             int everyDays, int atHour, int warning, Optional<TagKey<Item>> instrument,
                             int duration, int radius, boolean announced, Optional<ResourceLocation> effect) {

    private static final int HOURS = 23;
    private static final int LONGEST = 24_000 * 365;
    private static final int FARTHEST = 30_000_000;
    public static final Codec<WorldEventType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(WorldEventType::name),
            Codec.STRING.optionalFieldOf("english", "").forGetter(WorldEventType::english),
            ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimensions", List.of())
                    .forGetter(WorldEventType::dimensions),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(WorldEventType::biomes),
            StableCodecs.byName(Season.class).listOf().optionalFieldOf("seasons", List.of())
                    .forGetter(WorldEventType::seasons),
            Codec.doubleRange(0, 1).optionalFieldOf("chance_per_day", 0.0).forGetter(WorldEventType::chancePerDay),
            Codec.intRange(0, LONGEST).optionalFieldOf("every_days", 0).forGetter(WorldEventType::everyDays),
            Codec.intRange(0, HOURS).optionalFieldOf("at_hour", 0).forGetter(WorldEventType::atHour),
            Codec.intRange(0, LONGEST).optionalFieldOf("warning", 0).forGetter(WorldEventType::warning),
            TagKey.codec(Registries.ITEM).optionalFieldOf("instrument").forGetter(WorldEventType::instrument),
            Codec.intRange(1, LONGEST).fieldOf("duration").forGetter(WorldEventType::duration),
            Codec.intRange(0, FARTHEST).optionalFieldOf("radius", 0).forGetter(WorldEventType::radius),
            Codec.BOOL.optionalFieldOf("announced", true).forGetter(WorldEventType::announced),
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(WorldEventType::effect))
            .apply(instance, WorldEventType::new));

    public WorldEventType {
        Objects.requireNonNull(name, "name");
        dimensions = List.copyOf(dimensions);
        seasons = List.copyOf(seasons);
    }

    /** Its name, as a text to show. */
    public TextKey title() {
        return new TextKey(this.name, this.english.isEmpty() ? this.name : this.english);
    }

    /** Whether it happens in {@code dimension}. */
    public boolean happensIn(final ResourceKey<Level> dimension) {
        return this.dimensions.isEmpty() || this.dimensions.contains(dimension);
    }

    /** Whether it happens round a place rather than over the whole dimension. */
    public boolean local() {
        return this.radius > 0;
    }
}
