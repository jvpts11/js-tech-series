/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.material.VariantSettings;
import dev.jstech.core.multipart.MicroblockMaterials;
import dev.jstech.core.variant.FoundMaterials;
import dev.jstech.core.variant.Variant;
import dev.jstech.core.variant.VariantBlock;
import dev.jstech.core.variant.Variants;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestVariants;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Variants for other mods' materials: the test mod declares a frame of every wood, a tile of every stone and a casing
 * of every metal, and the Core makes them by themselves for every material the common tags name, the game's own and
 * other mods' (Mekanism's osmium, the test mod's marble), each copying its material's block, named after it, dropping
 * itself, in its tool's tag and its kind's, and cut into microblocks like any whole block.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class VariantGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private VariantGameTests() {
    }

    @GameTest(template = ARENA)
    public static void frames_areMadeForEveryWood(final GameTestHelper helper) {
        final Optional<Variant> oak = Variants.of(TestVariants.FRAMES, ResourceLocation.withDefaultNamespace("oak"));
        helper.assertTrue(oak.isPresent(), "the oak has a frame; found " + FoundMaterials.woods());
        helper.assertTrue(oak.get().id().equals(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                "oak_test_frame")), "named after the oak; got " + oak.get().id());
        final String name = ((VariantBlock) oak.get().block()).displayName().english();
        helper.assertTrue(name.equals("Oak Test Frame"), "called by the oak's own name; got " + name);
        final Block block = oak.get().block();
        helper.assertTrue(block.defaultDestroyTime() == Blocks.OAK_PLANKS.defaultDestroyTime()
                && block.defaultBlockState().getSoundType() == Blocks.OAK_PLANKS.defaultBlockState().getSoundType(),
                "as hard as the planks and sounding as they do");
        helper.assertTrue(TestVariants.FRAMES.variants().size() >= 11, "every one of the game's woods has one; got "
                + TestVariants.FRAMES.variants().size());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void casingsAndTiles_areMadeForTheMetalsAndStonesOfEveryMod(final GameTestHelper helper) {
        final Optional<Variant> iron = Variants.of(TestVariants.CASINGS, ResourceLocation.fromNamespaceAndPath("c",
                "iron"));
        helper.assertTrue(iron.isPresent() && iron.get().material().block() == Blocks.IRON_BLOCK,
                "the iron has a casing made from the game's block of iron");
        final Optional<Variant> osmium = Variants.of(TestVariants.CASINGS, ResourceLocation.fromNamespaceAndPath("c",
                "osmium"));
        helper.assertTrue(osmium.isPresent() && osmium.get().material().modid().equals("mekanism"),
                "another mod's metal has one too, found by its common tags; got " + osmium);
        final Optional<Variant> marble = Variants.of(TestVariants.TILES, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "test_marble"));
        helper.assertTrue(marble.isPresent() && marble.get().id().getPath().equals("jstests_test_marble_test_tile"),
                "a stone of another mod has a tile, named after its mod and itself; got " + marble);
        final Optional<Variant> granite = Variants.of(TestVariants.TILES, ResourceLocation.withDefaultNamespace(
                "granite"));
        helper.assertTrue(granite.isPresent(), "and so has the game's granite");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void variant_dropsItselfIsTaggedAndIsCut(final GameTestHelper helper) {
        final Variant oak = Variants.of(TestVariants.FRAMES, ResourceLocation.withDefaultNamespace("oak"))
                .orElseThrow();
        final BlockState state = oak.block().defaultBlockState();
        helper.setBlock(AT, state);
        final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(AT), null, null,
                new ItemStack(Items.IRON_AXE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(oak.item()), "it drops itself; got " + drops);
        helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), "it is broken with an axe, as wood is");
        helper.assertTrue(state.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "test_frames"))), "and is in its kind's tag");
        helper.assertTrue(MicroblockMaterials.cuttable(oak.block()), "microblocks are cut from it");
        helper.assertTrue(TestVariants.CASINGS.items().contains(Variants.of(TestVariants.CASINGS,
                ResourceLocation.fromNamespaceAndPath("c", "iron")).orElseThrow().item()), "its kind lists its item");
        helper.assertTrue(ConfigFiles.atStartup(VariantSettings.FILE).get(VariantSettings.VARIANTS),
                "variants are made unless a setting says not");
        helper.succeed();
    }
}
