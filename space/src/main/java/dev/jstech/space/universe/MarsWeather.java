/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.weather.WeatherKind;
import dev.jstech.core.weather.Weathers;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.space.JsSpace;
import dev.jstech.space.worldgen.MarsWorld;
import net.minecraft.resources.ResourceLocation;

/**
 * Mars' dust storms, as the real planet has them: regional storms a few kilometres across now and then, lasting some
 * hours, and now and then a global one that covers the whole planet for a day and a half, as the storm of 2018 that
 * ended Opportunity. Either fills the air with blown dust, closes the view in to a brown haze that swallows the
 * horizon and hides most of the sky, the Sun a dim disc behind it. How often and how long are estimates, to be set by
 * playing.
 */
@TextHolder
public final class MarsWeather {

    public static final TextKey DUST_STORM_NAME = TextKey.of("jsspace.world_event.dust_storm", "Dust storm");
    public static final TextKey GLOBAL_DUST_STORM_NAME = TextKey.of("jsspace.world_event.global_dust_storm",
            "Global dust storm");

    /* The regional storms: how likely on a day, how many blocks round their place, how long, in ticks. */
    private static final double REGIONAL_CHANCE = 0.35;
    private static final int REGIONAL_REACH = 3_000;
    private static final int REGIONAL_LASTS = 6_200;
    /* The global one: about once in Mars' year of thirteen days, for a day and a half of its days. */
    private static final double GLOBAL_CHANCE = 1.0 / 13.0;
    private static final int GLOBAL_LASTS = 37_000;
    /* The dust blown along, the brown haze it makes, how far it closes the view in, how much sky it hides. */
    private static final int DUST = 0xB07A50;
    private static final double DUST_THICK = 0.6;
    private static final int HAZE = 0x7C5840;
    private static final double REGIONAL_SIGHT = 0.7;
    private static final double GLOBAL_SIGHT = 0.8;
    private static final double REGIONAL_VEIL = 0.75;
    private static final double GLOBAL_VEIL = 0.85;
    private static final double WIND = 4.0;

    /** A regional dust storm, a few kilometres round its place. */
    public static final ResourceLocation DUST_STORM = WorldEvents.event(id("dust_storm"), DUST_STORM_NAME)
            .in(MarsWorld.world().dimension()).chance(REGIONAL_CHANCE).radius(REGIONAL_REACH).lasts(REGIONAL_LASTS)
            .declare();
    public static final ResourceLocation DUST_STORM_WEATHER = Weathers.weather(DUST_STORM)
            .falls(WeatherKind.Fall.DUST, DUST, DUST_THICK).fog(HAZE, REGIONAL_SIGHT).veils(REGIONAL_VEIL).wind(WIND)
            .declare();
    /** A global dust storm, over the whole planet. */
    public static final ResourceLocation GLOBAL_DUST_STORM = WorldEvents.event(id("global_dust_storm"),
                    GLOBAL_DUST_STORM_NAME)
            .in(MarsWorld.world().dimension()).chance(GLOBAL_CHANCE).lasts(GLOBAL_LASTS).declare();
    public static final ResourceLocation GLOBAL_DUST_STORM_WEATHER = Weathers.weather(GLOBAL_DUST_STORM)
            .falls(WeatherKind.Fall.DUST, DUST, DUST_THICK).fog(HAZE, GLOBAL_SIGHT).veils(GLOBAL_VEIL).wind(WIND)
            .declare();

    private MarsWeather() {
    }

    /** Declares the storms, once, from the mod's constructor, after Mars' ground. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
