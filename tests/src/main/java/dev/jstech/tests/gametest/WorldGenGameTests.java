/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Core's world generation: a vein and a structure declared in code reach the game's registries through the files
 * the generator wrote, and the vein, placed into stone, lays its ore there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldGenGameTests {

    private static final String ARENA = "empty";

    private WorldGenGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declarations_reachTheRegistries(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(TestWorldGen.ORE.id()),
                "the vein's feature is registered");
        helper.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE).containsKey(TestWorldGen.ORE.id()),
                "and its placement");
        helper.assertTrue(registries.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                .containsKey(TestWorldGen.ORE.id()), "and the biome modifier that adds it");
        helper.assertTrue(registries.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(TestWorldGen.CAMP.id()),
                "the structure's pool is registered");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE).containsKey(TestWorldGen.CAMP.id()),
                "and the structure");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE_SET).containsKey(TestWorldGen.CAMP.id()),
                "and its set");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ore_laysItsVeinIntoStone(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos centre = new BlockPos(3, 4, 3);
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 7; y++) {
                for (int z = 0; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        final Registry<ConfiguredFeature<?, ?>> features = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE);
        final ConfiguredFeature<?, ?> vein = features.get(TestWorldGen.ORE.id());
        helper.assertTrue(vein != null, "the vein's feature is registered");
        vein.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42L),
                helper.absolutePos(centre));
        int laid = 0;
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 7; y++) {
                for (int z = 0; z <= 6; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.AMETHYST_BLOCK)) {
                        laid++;
                    }
                }
            }
        }
        helper.assertTrue(laid > 0, "the vein laid its ore into the stone; it laid " + laid);
        helper.succeed();
    }
}
