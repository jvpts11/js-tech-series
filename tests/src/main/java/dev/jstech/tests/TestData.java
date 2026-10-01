/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.data.DataRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Content the tests read from the test mod's own datapack: a registry of shades filled by datapacks, sent to every
 * player as they join, and notes read from JSON files again on every reload and sent to every player, one of whose
 * files does not read.
 */
public final class TestData {

    /** A named colour. */
    public record Shade(String name, int colour) {

        public static final Codec<Shade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Shade::name),
                Codec.INT.fieldOf("colour").forGetter(Shade::colour)
        ).apply(instance, Shade::new));
    }

    /** How many times the notes were read. */
    public static final AtomicInteger NOTE_READS = new AtomicInteger();

    /** Shades, a registry datapacks fill. */
    public static final ResourceKey<Registry<Shade>> SHADES = TestSounds.CONTENT.datapackRegistry("shades",
            Shade.CODEC);

    /** Notes, read from {@code data/<namespace>/jstests_notes/} on every reload and sent to the players. */
    public static final DataRegistry<Shade> NOTES = DataRegistry.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "notes"), "jstests_notes", Shade.CODEC)
            .synced().onReload(read -> NOTE_READS.incrementAndGet()).register();

    private TestData() {
    }

    /** Declares the registries, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
