/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.unify.Unification;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Unification with the test mod's copy of the Core's tin ingot, filed under {@code c:ingots/tin}: the Core's ingot
 * stands for it, so the recipe that makes the copy makes the Core's ingot, the recipe that asks for the copy takes the
 * Core's, and the loot table that drops the copy drops the Core's.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class UnificationGameTests {

    private static final String ARENA = "empty";
    private static final String TIN_TAG = "c:ingots/tin";
    private static final int MADE = 2;

    private UnificationGameTests() {
    }

    @GameTest(template = ARENA)
    public static void unification_letsTheCoresIngotStandForTheCopy(final GameTestHelper helper) {
        final Item tin = tinIngot();
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(tin).toString().equals(Unification.current().chosen()
                .get(TIN_TAG)), "the Core's ingot stands for the tag; the plan: " + Unification.current().chosen()
                .get(TIN_TAG));
        helper.assertTrue(Unification.unified(new ItemStack(TestItems.FOREIGN_TIN.get())).is(tin),
                "the copy comes out as the Core's ingot");
        helper.assertTrue(Unification.unified(new ItemStack(tin)).is(tin), "the Core's ingot stays itself");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unification_makesTheStandingItemAndTakesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Item tin = tinIngot();
        final ItemStack made = recipe(helper, "unify/makes_copy").getResultItem(level.registryAccess());
        helper.assertTrue(made.is(tin) && made.getCount() == MADE, "the recipe makes the Core's ingot, as many; got "
                + made);
        final Recipe<?> takes = recipe(helper, "unify/takes_copy");
        helper.assertTrue(takes.getIngredients().getFirst().test(new ItemStack(tin)),
                "the recipe that asked for the copy takes the Core's ingot");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unifiedDrops_dropTheStandingItem(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "unify/copy")));
        final LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        final List<ItemStack> loot = table.getRandomItems(params);
        helper.assertTrue(loot.size() == 1 && loot.getFirst().is(tinIngot()),
                "the table that drops the copy drops the Core's ingot; got " + loot);
        helper.succeed();
    }

    private static Item tinIngot() {
        return MaterialUses.item(SeriesMaterials.TIN, Forms.INGOT).get();
    }

    private static Recipe<?> recipe(final GameTestHelper helper, final String path) {
        return helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path)).orElseThrow().value();
    }
}
