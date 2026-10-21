/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.FoundRocks;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialFormBlock;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.VariantSettings;
import dev.jstech.core.worldgen.vein.OreTable;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Ores in other mods' rocks: the test mod's marble, in the common tag of stones and declared by no mod as a rock, is
 * found by its tag as the game starts, and every material with an ore in the game's Overworld rocks has one in it,
 * as hard as the marble and a little harder, named after it, filed with the material's other ores, dropping what they
 * drop, and laid by the veins where they cross the marble.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FoundRockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(1, 1, 1);
    private static final ResourceLocation MARBLE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_marble");
    private static final float ORE_HARDER = 1.5f;

    private FoundRockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void marble_isFoundByTheCommonTagOfStones(final GameTestHelper helper) {
        helper.assertTrue(FoundRocks.inTags().get(MARBLE) == FoundRocks.Realm.OVERWORLD,
                "the marble is in the tags of the Overworld's stones");
        final Optional<HostRock> rock = HostRocks.byId(MARBLE);
        helper.assertTrue(rock.isPresent() && rock.get().found(), "and is a rock the Core found, declared by no mod");
        helper.assertFalse(HostRocks.STONE.found(), "the game's own rocks are declared");
        helper.assertTrue(ConfigFiles.atStartup(VariantSettings.FILE).get(VariantSettings.ORES_IN_OTHER_ROCKS),
                "ores in other mods' rocks are on unless a setting says not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void marble_holdsTheOresOfTheMaterials(final GameTestHelper helper) {
        final HostRock rock = HostRocks.byId(MARBLE).orElseThrow();
        final Block ore = MaterialUses.oreBlock(SeriesMaterials.TIN, rock).get();
        helper.assertTrue(ore instanceof MaterialFormBlock, "tin has an ore in the marble; got " + ore);
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(ore).equals(ResourceLocation.fromNamespaceAndPath("jscore",
                "jstests_test_marble_tin_ore")), "named after the marble's own id");
        helper.assertTrue(ore.defaultDestroyTime() == TestBlocks.MARBLE_HARDNESS + ORE_HARDER,
                "as hard as the marble and a little harder; got " + ore.defaultDestroyTime());
        final String name = ((MaterialFormBlock) ore).displayName().english();
        helper.assertTrue(name.equals("Tin Ore in Test Marble"), "named after the marble; got " + name);
        helper.assertTrue(ore.asItem() != Items.AIR, "with its item");
        helper.assertTrue(MaterialUses.registered().stream().anyMatch(thing -> thing.rock() == rock),
                "among the things the Core registered, so its art and its data are made");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void marblesOre_isFiledDropsAndIsLaidByTheVeins(final GameTestHelper helper) {
        final HostRock rock = HostRocks.byId(MARBLE).orElseThrow();
        final Block ore = MaterialUses.oreBlock(SeriesMaterials.TIN, rock).get();
        final BlockState state = ore.defaultBlockState();
        helper.assertTrue(state.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c",
                "ores/tin"))), "filed with tin's other ores");
        final BlockPos at = helper.absolutePos(AT);
        helper.setBlock(AT, state);
        final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), at, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(MaterialUses.item(SeriesMaterials.TIN,
                Forms.RAW).get()), "it drops raw tin, as tin's ores do; got " + drops);
        helper.assertTrue(OreTable.rockOf(TestBlocks.MARBLE.get()) == rock, "the veins know the marble as a rock");
        helper.assertTrue(OreTable.oreOf(SeriesMaterials.TIN.id(), rock) == ore, "and lay tin's ore in it");
        helper.succeed();
    }
}
