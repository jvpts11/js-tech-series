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
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.core.world.field.ChunkField;
import dev.jstech.core.world.field.ChunkFields;
import dev.jstech.core.world.live.LiveChanges;
import dev.jstech.core.worldgen.retro.Retrogen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

/**
 * The world changing, declared the way a mod declares it, to prove the Core's kit: generation laid into old chunks
 * (a beacon of lodestone, only in the dimensions the tests make for it, and the test land's patches of moss), surface
 * changes on blocks no world holds by itself (blue ice thawing, water gathering in a basin floored with waxed copper),
 * a storm and a quake the tests start, and two fields over the chunks, one moved only by hand and one that spreads by
 * itself.
 */
@TextHolder
public final class TestWorldChanges {

    public static final TextKey STORM_NAME = TextKey.of("jstests.world_event.storm", "Test storm");
    public static final TextKey QUAKE_NAME = TextKey.of("jstests.world_event.quake", "Test quake");
    public static final TextKey GALE_NAME = TextKey.of("jstests.world_event.gale", "Test gale");
    /** The dimensions the old-chunk generation of lodestone acts in: those made for its test. */
    public static final String RETRO_DIMENSIONS = "retro_";
    /** A lodestone on top of the middle of each old chunk, in the dimensions made for its test. */
    public static final ResourceLocation BEACON = Retrogen.declare(id("test_beacon"), (level, chunk, random) -> {
        if (level.dimension().location().getPath().startsWith(RETRO_DIMENSIONS)) {
            final int x = chunk.getPos().getMiddleBlockX();
            final int z = chunk.getPos().getMiddleBlockZ();
            level.setBlockAndUpdate(new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z),
                    Blocks.LODESTONE.defaultBlockState());
        }
    });
    /** The test land's patches of moss laid into its old chunks, as generation would lay them. */
    public static final ResourceLocation OLD_MOSS = Retrogen.feature(id("test_old_moss"),
            ResourceKey.create(Registries.PLACED_FEATURE, TestWorldGen.MOSS.id()));
    /** Blue ice under the open sky thaws to water wherever it is above freezing. */
    public static final ResourceLocation THAW = LiveChanges.change(id("test_thaw"))
            .test(new BlockMatchTest(Blocks.BLUE_ICE)).becomes(Blocks.WATER.defaultBlockState()).warmerThan(273.15)
            .sky().declare();
    /** Water gathers in a basin one block deep floored with waxed copper. */
    public static final ResourceLocation BASIN = LiveChanges.change(id("test_basin"))
            .test(new BlockMatchTest(Blocks.AIR)).becomes(Blocks.WATER.defaultBlockState()).above()
            .on(new BlockMatchTest(Blocks.WAXED_COPPER_BLOCK)).basin().declare();
    /** What warns of the test storm. */
    public static final TagKey<Item> STORM_INSTRUMENTS = TagKey.create(Registries.ITEM, id("storm_instruments"));
    /** A storm over the whole overworld that never comes by itself, warned of ten seconds ahead, lasting three. */
    public static final ResourceLocation STORM = WorldEvents.event(id("test_storm"), STORM_NAME).in(Level.OVERWORLD)
            .warning(200).instrument(STORM_INSTRUMENTS).lasts(60).declare();
    /** A gale over the overworld that never comes by itself, which an operator's commands start and stop. */
    public static final ResourceLocation GALE = WorldEvents.event(id("test_gale"), GALE_NAME).in(Level.OVERWORLD)
            .lasts(200).quiet().declare();
    /** How far the test quake reaches round its place. */
    public static final int QUAKE_RADIUS = 16;
    /** A quake round a place of the overworld, that never comes by itself and tells no one. */
    public static final ResourceLocation QUAKE = WorldEvents.event(id("test_quake"), QUAKE_NAME).in(Level.OVERWORLD)
            .radius(QUAKE_RADIUS).lasts(200).quiet().declare();
    /** A field moved only by hand: a fifth spreads and a tenth fades each step. */
    public static final ChunkField STILL = ChunkFields.field(id("test_still_field")).spread(0.2).decay(0.1)
            .every(0).synced().declare();
    /** A field that spreads by itself every five ticks, fading a twentieth a step so it never lingers. */
    public static final ChunkField DRIFT = ChunkFields.field(id("test_drift_field")).spread(0.4).decay(0.05)
            .every(5).declare();

    private TestWorldChanges() {
    }

    /** Declares them, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
