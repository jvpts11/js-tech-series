/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.weather.WeatherKind;
import dev.jstech.core.weather.Weathers;
import dev.jstech.core.world.event.WorldEvents;
import net.minecraft.resources.ResourceLocation;

/**
 * The sky kit's test content: two weathers, each brought by a world event of its own round a place and started by the
 * tests only (they never come by chance): a green acid rain that harms and poisons whoever stands out in it, closes the
 * view in and blows three times as hard; and a storm of nothing but lightning. The dimensions they are tried in, a land
 * with the whole sky and a land locked to its star, are {@link TestWorldGen#SKY_LAND} and
 * {@link TestWorldGen#LOCKED_LAND}, their rules and sky in the test mod's data.
 */
@TextHolder
public final class TestSkies {

    public static final TextKey ACID_RAIN_NAME = TextKey.of("jstests.world_event.acid_rain", "Test acid rain");
    public static final TextKey SPARK_STORM_NAME = TextKey.of("jstests.world_event.spark_storm", "Test spark storm");
    /** How far round its place each weather reaches, in blocks. */
    public static final int REACH = 10;
    /** Acid rain's colour and the colour of its fog. */
    public static final int ACID = 0x9CE05A;
    public static final int ACID_FOG = 0x7C9A5A;
    public static final ResourceLocation ACID_RAIN = WorldEvents.event(id("test_acid_rain"), ACID_RAIN_NAME)
            .radius(REACH).lasts(6_000).quiet().declare();
    public static final ResourceLocation ACID_RAIN_WEATHER = Weathers.weather(ACID_RAIN)
            .falls(WeatherKind.Fall.RAIN, ACID, 0.8).fog(ACID_FOG, 0.5).wind(3)
            .harms(ResourceLocation.withDefaultNamespace("generic"), 1F, 1)
            .gives(ResourceLocation.withDefaultNamespace("poison"), 0, 4).declare();
    public static final ResourceLocation SPARK_STORM = WorldEvents.event(id("test_spark_storm"), SPARK_STORM_NAME)
            .radius(REACH).lasts(6_000).quiet().declare();
    public static final ResourceLocation SPARK_STORM_WEATHER = Weathers.weather(SPARK_STORM).lightning(60)
            .declare();

    private TestSkies() {
    }

    /** Declares them, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
