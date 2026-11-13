/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.guide;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.DimensionClock;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.orbit.SolarSystemOrbits;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.math.BigDecimal;
import net.minecraft.resources.ResourceLocation;

/**
 * The figures the Mission Handbook's world sheets read, {@code {{jsspace:moon_gravity}}} and the like, each from where
 * the game keeps it, so a sheet never tells another figure than the world has: a world's size from its declaration;
 * its gravity, air, cold, day and year from its dimension's rules, as this game knows them; the usual trip there and
 * the burn down to its ground from the flight's own figures.
 */
public final class WorldFacts {

    /* Ticks a second, seconds a minute and an hour, the share a percent is, the pascals of a kilopascal. */
    private static final int TICKS = 20;
    private static final int MINUTE = 60;
    private static final int HOUR = 3_600;
    private static final int PERCENT = 100;
    private static final int PASCALS = 1_000;
    private static final long TICKS_A_MINUTE = (long) TICKS * MINUTE;

    private WorldFacts() {
    }

    /** Declares the figures, once, from the mod's constructor. */
    public static void declare() {
        world("moon", SolarSystemWorlds.MOON);
        world("mars", SolarSystemWorlds.MARS);
        trip("moon", SolarSystemOrbits.MOON_TRIP_TICKS, Rocketry.MOON_ORBIT_TO_SURFACE);
        trip("mars", SolarSystemOrbits.MARS_TRIP_TICKS, Rocketry.MARS_LANDING);
        GuideFacts.value(id("mars_carbon_dioxide"), () -> share(SolarSystemWorlds.MARS, AirMix.CARBON_DIOXIDE));
        GuideFacts.value(id("mars_nitrogen"), () -> share(SolarSystemWorlds.MARS, AirMix.NITROGEN));
        GuideFacts.value(id("mars_argon"), () -> share(SolarSystemWorlds.MARS, AirMix.ARGON));
        GuideFacts.value(id("mars_year"), () -> amount(rules(SolarSystemWorlds.MARS).clock()
                .map(DimensionClock::year).orElse(0), ""));
    }

    /* The figures of the world {@code world}, under {@code name}: its diameter, gravity, air, cold and day. */
    private static void world(final String name, final ResourceLocation world) {
        GuideFacts.value(id(name + "_diameter"), () -> amount(Math.round(SpaceWorlds.anywhere(world)
                .map(SpaceWorld::diameter).orElse(0.0)), "km"));
        GuideFacts.value(id(name + "_gravity"), () -> new GuideBlock.GuideValue.Literal(
                BigDecimal.valueOf(rules(world).gravity()).stripTrailingZeros().toPlainString() + " g"));
        GuideFacts.value(id(name + "_pressure"), () -> amount(Math.round(rules(world).pressure() * PASCALS), "Pa"));
        GuideFacts.value(id(name + "_temperature"), () -> amount(Math.round(rules(world).temperature()), "°C"));
        GuideFacts.value(id(name + "_day"), () -> new GuideBlock.GuideValue.Literal(rules(world).clock()
                .map(clock -> played(clock.day())).orElse("")));
    }

    /* The usual trip to the world {@code name} and the burn from its orbit to its ground. */
    private static void trip(final String name, final long ticks, final double landing) {
        GuideFacts.value(id(name + "_trip"), () -> amount(Math.max(1, ticks / TICKS_A_MINUTE), "min"));
        GuideFacts.value(id(name + "_landing_delta_v"), () -> amount(Math.round(landing), "m/s"));
    }

    /* The share of {@code gas} in the air of the world {@code world}, in percent. */
    private static GuideBlock.GuideValue share(final ResourceLocation world, final ResourceLocation gas) {
        return amount(Math.round(rules(world).air().map(air -> air.share(gas)).orElse(0.0) * PERCENT), "%");
    }

    /* The rules of the world {@code world}'s dimension as this game knows them, a pack's world over the declared. */
    private static DimensionRules rules(final ResourceLocation world) {
        return SpaceWorlds.anywhere(world).map(found -> DimensionRulesData.known(found.dimension()))
                .orElse(DimensionRules.OVERWORLD);
    }

    /* {@code ticks} of play, as hours and minutes, or minutes and seconds under an hour. */
    static String played(final long ticks) {
        final long seconds = ticks / TICKS;
        if (seconds >= HOUR) {
            // Rounded whole first, so a day of 1 h 59 min 40 s reads 2 h 0 min, never 1 h 60 min.
            final long minutes = (seconds + MINUTE / 2) / MINUTE;
            return minutes / MINUTE + " h " + minutes % MINUTE + " min";
        }
        return seconds / MINUTE + " min " + seconds % MINUTE + " s";
    }

    private static GuideBlock.GuideValue amount(final long value, final String unit) {
        return new GuideBlock.GuideValue.Amount(value, unit);
    }

    private static String id(final String path) {
        return JsSpace.MODID + ":" + path;
    }
}
