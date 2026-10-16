/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.MarkerLayer;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.worldgen.place.Landmarks;
import dev.jstech.core.worldgen.structure.KnowledgeSealBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Places of the test mod, to prove the Core's places and maps: a landmark far out in the overworld, a layer of map
 * markers of its own, a lore its teams can learn and a seal that opens only to a team that knows it. The seal's block
 * state and model are written by hand, of the game's own textures.
 */
@TextHolder
public final class TestPlaces {

    public static final TextKey SPIRE_NAME = TextKey.of("jstests.landmark.spire", "Test Spire");
    public static final TextKey RUINS_NAME = TextKey.of("jstests.map.layer.ruins", "Test ruins");
    public static final TextKey LORE_NAME = TextKey.of("jstests.knowledge.lore", "Test Lore");
    /** Where the spire stands, far from where the tests and the players play. */
    public static final int SPIRE_X = 20_000;
    public static final int SPIRE_Y = 80;
    public static final int SPIRE_Z = -20_000;
    /** How near the spire a player comes to discover it, in blocks. */
    public static final int SPIRE_RADIUS = 32;
    /** A spire in the overworld, far out. */
    public static final ResourceLocation SPIRE = Landmarks.landmark(id("test_spire"), SPIRE_NAME)
            .at(SPIRE_X, SPIRE_Y, SPIRE_Z).radius(SPIRE_RADIUS).declare();
    /** The test mod's own layer of map markers, of points, in brown. */
    public static final MarkerLayer RUINS = MapMarkers.layer(id("test_ruins"), RUINS_NAME, 0xC08040, false);
    /** What a team learns to open the seal. */
    public static final Knowledge LORE = Knowledges.declare(Knowledge.of(id("test_lore"), LORE_NAME));
    /** A seal over a chamber, opened only by a team that knows what its block entity asks. */
    public static final BlockEntry<KnowledgeSealBlock> SEAL = TestSounds.CONTENT.block("test_seal",
            properties -> new KnowledgeSealBlock(properties.strength(-1F, 3_600_000F), () -> TestPlaces.SEAL_ENTITY
                    .get())).named("Test seal").look(IBlockLook.cubeAll("test_seal")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnowledgeSealBlock.Entity>> SEAL_ENTITY =
            TestSounds.CONTENT.blockEntity("test_seal", (pos, state) -> new KnowledgeSealBlock.Entity(
                    TestPlaces.SEAL_ENTITY.get(), pos, state), SEAL);

    private TestPlaces() {
    }

    /** Declares the places, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
