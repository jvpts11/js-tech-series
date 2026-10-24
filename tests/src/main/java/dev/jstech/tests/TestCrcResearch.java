/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.research.Discovery;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.research.IndustrialResearch;
import net.minecraft.resources.ResourceLocation;

/**
 * Discoveries for J's Industrial's Computational Research Centre to work in the tests: a short one that asks for two
 * sample data, each covering half the way, and one so long no test ends it, to keep a CRC working.
 */
@TextHolder
public final class TestCrcResearch {

    public static final TextKey SHORT_NAME = TextKey.of("jstests.discovery.crc_short", "Test short CRC discovery");
    public static final TextKey LONG_NAME = TextKey.of("jstests.discovery.crc_long", "Test long CRC discovery");

    /** Forty points and two sample data. */
    public static final Discovery SHORT = Discovery.builder(id("test_crc_short"), SHORT_NAME).costs(40)
            .needs(IndustrialResearch.SAMPLE, 2).declare();
    /** A billion points and no data. */
    public static final Discovery LONG = Discovery.builder(id("test_crc_long"), LONG_NAME).costs(1_000_000_000L)
            .declare();

    private TestCrcResearch() {
    }

    /** Declares them, before the game has loaded. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
