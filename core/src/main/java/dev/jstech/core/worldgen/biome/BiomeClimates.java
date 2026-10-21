/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.time.GameCalendar;
import dev.jstech.core.time.Season;
import dev.jstech.core.time.WorldCalendar;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The climates of biomes in real values: the data map a mod or a data pack gives a biome its own climate in, every
 * other biome's read from the game's own temperature and downfall for it, and how warm a place is now, by its biome,
 * its height and the day of the year. Where a biome has a climate of its own, heat meets that air, warmer in summer,
 * colder in winter and on the mountain tops; a biome with none keeps its dimension's air, as it always had.
 */
public final class BiomeClimates {

    /** Each biome's own climate. */
    public static final DataMapType<Biome, BiomeClimate> CLIMATES = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "climate"), Registries.BIOME, BiomeClimate.CODEC)
            .synced(BiomeClimate.CODEC, false).build();

    private BiomeClimates() {
    }

    /** Hands the mod's bus the data map, and heat the biomes' climates, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(CLIMATES));
        HeatClimate.source(BiomeClimates::air);
    }

    /** The climate {@code biome} was given, or none. */
    public static Optional<BiomeClimate> declared(final Holder<Biome> biome) {
        return Optional.ofNullable(biome.getData(CLIMATES));
    }

    /** The climate of {@code biome}: the one it was given, or the one the game's own values for it give it. */
    public static BiomeClimate of(final Holder<Biome> biome) {
        return declared(biome).orElseGet(() -> BiomeClimate.fromGame(biome.value().getBaseTemperature(),
                biome.value().getModifiedClimateSettings().downfall()));
    }

    /**
     * How warm it is at {@code pos} now, in degrees Celsius: its biome's climate, at its height, on today's date; for a
     * block aboard a moving structure, of where the structure is in the world.
     */
    public static double celsius(final Level level, final BlockPos pos) {
        final BlockPos at = MovingStructures.inWorld(level, pos);
        return of(level.getBiome(at)).celsius(at.getY(), dayOfYear(level), daysAYear());
    }

    /**
     * How warm it is at {@code pos} as the world is made, in kelvin: its biome's own climate over the whole year, or
     * its dimension's air where the biome has none. Asks nothing of chunks outside {@code level}, so it is safe while
     * the world is generated.
     */
    public static double kelvinAsMade(final WorldGenLevel level, final BlockPos pos) {
        return declared(level.getBiome(pos)).map(climate -> HeatMath.kelvin(climate.celsius(pos.getY())))
                .orElseGet(() -> HeatMath.kelvin(DimensionRulesData.of(level.getLevel()).temperature()));
    }

    /* The air of a biome with a climate of its own, at its height and on today's date; none for any other biome. */
    private static HeatClimate.Air air(final Level level, final BlockPos pos) {
        // Heat asks often; while no biome has a climate of its own, the biome under it is never looked up.
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getDataMap(CLIMATES).isEmpty()) {
            return null;
        }
        final Optional<BiomeClimate> own = declared(level.getBiome(pos));
        if (own.isEmpty()) {
            return null;
        }
        return new HeatClimate.Air(HeatMath.kelvin(own.get().celsius(pos.getY(), dayOfYear(level), daysAYear())),
                DimensionRulesData.of(level).at(pos.getY() + 0.5).pressure());
    }

    private static double dayOfYear(final Level level) {
        final WorldCalendar.Moment now = WorldCalendar.now(level);
        final int before = switch (now.season()) {
            case SPRING -> 0;
            case SUMMER -> 1;
            case AUTUMN -> 2;
            case WINTER -> 3;
        };
        return before * (double) WorldCalendar.calendar().daysPerSeason() + now.dayOfSeason();
    }

    private static double daysAYear() {
        final GameCalendar calendar = WorldCalendar.calendar();
        return (double) calendar.daysPerSeason() * Season.perYear();
    }
}
