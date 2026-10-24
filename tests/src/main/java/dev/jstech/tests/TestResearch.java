/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.research.ResearchDataItem;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Research the way a mod adds its own, to prove the Core's: a kind of data with its item, a discovery that asks for
 * four of it, one that comes after it, one that asks for a kind no mod declares and so is not there, one whose cost a
 * data pack changes, and the test mod's own setting turning its research off.
 */
@TextHolder
public final class TestResearch {

    public static final TextKey SAMPLES_NAME = TextKey.of("jstests.data.samples", "Test samples");
    public static final TextKey NOWHERE_NAME = TextKey.of("jstests.data.nowhere", "Test data of nowhere");
    public static final TextKey FIELD = TextKey.of("jstests.research.field", "Test field");
    public static final TextKey FIRST_NAME = TextKey.of("jstests.discovery.first", "Test first discovery");
    public static final TextKey FIRST_WHEN = TextKey.of("jstests.discovery.first.when", "1901, the test lab");
    public static final TextKey SECOND_NAME = TextKey.of("jstests.discovery.second", "Test second discovery");
    public static final TextKey ABSENT_NAME = TextKey.of("jstests.discovery.absent", "Test absent discovery");
    public static final TextKey OVERRIDDEN_NAME = TextKey.of("jstests.discovery.overridden",
            "Test overridden discovery");

    /** A kind of data, with its item. */
    public static final DataKind SAMPLES = DataKinds.declare(new DataKind(id("test_samples"), SAMPLES_NAME));
    public static final ItemEntry<ResearchDataItem> SAMPLE_DATA = TestSounds.CONTENT.item("test_sample_data",
                    properties -> new ResearchDataItem(properties, SAMPLES))
            .named("Test Sample Data").look(IItemLook.parent("minecraft:item/paper")).tab(TestItems.ITEMS)
            .register();
    /** A kind no mod declares, so what asks for it is not there to research. */
    public static final DataKind NOWHERE = new DataKind(id("test_nowhere"), NOWHERE_NAME);
    /** A hundred points and four samples, each covering a quarter of the way. */
    public static final Discovery FIRST = Discovery.builder(id("test_first"), FIRST_NAME).when(FIRST_WHEN).year(1901)
            .field(FIELD).costs(100).needs(SAMPLES, 4).declare();
    /** Fifty points, once the first is made. */
    public static final Discovery SECOND = Discovery.builder(id("test_second"), SECOND_NAME).costs(50).after(FIRST)
            .declare();
    public static final Discovery ABSENT = Discovery.builder(id("test_absent"), ABSENT_NAME).costs(10)
            .needs(NOWHERE, 1).declare();
    /**
     * Declared at a thousand points; the test mod's data pack says seven. Its namespace is not the mod's, so the data
     * run writes no file of it over the pack's.
     */
    public static final Discovery OVERRIDDEN = Discovery.builder(
            ResourceLocation.fromNamespaceAndPath("jstests_pack", "overridden"), OVERRIDDEN_NAME).costs(1_000)
            .declare();

    /* Whether the test mod's research locks what its discoveries open; a test turns it off and on again. */
    private static volatile boolean required = true;

    private TestResearch() {
    }

    /** Declares it all, and the test mod's gate, before the game has loaded. */
    public static void declare() {
        Discoveries.gate(JsTests.MODID, () -> required);
    }

    /** Turns the test mod's research on or off, as its setting would. */
    public static void require(final boolean on) {
        required = on;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
