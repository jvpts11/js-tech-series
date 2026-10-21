/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Loot changed as data: the test mod's data pack adds flint to a table of a stick, rotten flesh and raw iron, takes the
 * flesh away, swaps the raw iron for an ingot, adds an emerald only when the tool is shears, and would add a diamond
 * were a mod that is not installed there, which the file's condition keeps out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LootChangeGameTests {

    private static final String ARENA = "empty";
    private static final int FLINT = 2;

    private LootChangeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void lootChanges_addTakeAwayAndSwapDrops(final GameTestHelper helper) {
        final List<ItemStack> loot = roll(helper, new ItemStack(Items.STICK));
        helper.assertTrue(count(loot, Items.STICK) == 1, "what is not changed stays; got " + loot);
        helper.assertTrue(count(loot, Items.FLINT) == FLINT, "the flint is added, as many as the file says; got "
                + loot);
        helper.assertTrue(count(loot, Items.ROTTEN_FLESH) == 0, "the flesh is taken away; got " + loot);
        helper.assertTrue(count(loot, Items.RAW_IRON) == 0 && count(loot, Items.IRON_INGOT) == 1,
                "the raw iron is swapped for an ingot; got " + loot);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lootChanges_holdTheirConditions(final GameTestHelper helper) {
        helper.assertTrue(count(roll(helper, new ItemStack(Items.STICK)), Items.EMERALD) == 0,
                "no emerald with a stick for a tool");
        helper.assertTrue(count(roll(helper, new ItemStack(Items.SHEARS)), Items.EMERALD) == 1,
                "an emerald with shears, the tool the change asks for");
        helper.assertTrue(count(roll(helper, new ItemStack(Items.SHEARS)), Items.DIAMOND) == 0,
                "and no diamond: the change's file asks for a mod that is not installed");
        helper.succeed();
    }

    private static List<ItemStack> roll(final GameTestHelper helper, final ItemStack tool) {
        final ServerLevel level = helper.getLevel();
        final LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "loot_changes")));
        final LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        return table.getRandomItems(params);
    }

    private static int count(final List<ItemStack> loot, final Item item) {
        int out = 0;
        for (final ItemStack stack : loot) {
            if (stack.is(item)) {
                out += stack.getCount();
            }
        }
        return out;
    }
}
