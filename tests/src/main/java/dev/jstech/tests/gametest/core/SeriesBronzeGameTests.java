/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.SeriesBronze;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's bronze and tin: three copper ingots and one tin ingot make four of bronze at the Crafting Table, in any
 * places; raw tin smelts in the furnace and tin's ore in the blast furnace, as the game's raw iron and iron ore do;
 * and J's Computers and J's Industrial leave tin to the Core rather than smelting it a second time.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SeriesBronzeGameTests {

    private static final String ARENA = "empty";
    /* The furnace's and the blast furnace's slots: what smelts, the fuel, what comes out. */
    private static final int INPUT = 0;
    private static final int FUEL = 1;
    private static final int OUTPUT = 2;
    private static final int SMELTING = 400;
    /* The series' mods, whose smelting of tin is the Core's alone. */
    private static final List<String> SERIES = List.of(JsCore.MODID, "jsc", "jsindustrial", "jsspace");

    private SeriesBronzeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bronze_isThreeCopperIngotsAndOneTinIngotInAnyPlaces(final GameTestHelper helper) {
        final ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        final ItemStack tin = new ItemStack(SeriesBronze.TIN_INGOT.get());
        final ItemStack none = ItemStack.EMPTY;
        final ItemStack made = crafted(helper, List.of(copper, none, copper, none, tin, none, none, copper, none));
        helper.assertTrue(made.is(SeriesBronze.BRONZE_INGOT.get()) && made.getCount() == SeriesBronze.ALLOYED,
                "three copper and one tin make four bronze ingots: " + made);
        helper.assertTrue(crafted(helper, List.of(copper, copper, none, tin, tin, none, none, none, none)).isEmpty(),
                "two copper and two tin make nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = SMELTING)
    public static void rawTin_smeltsInTheFurnace(final GameTestHelper helper) {
        smelts(helper, Blocks.FURNACE, new ItemStack(MaterialUses.item(SeriesMaterials.TIN, Forms.RAW).get()));
    }

    @GameTest(template = ARENA, timeoutTicks = SMELTING)
    public static void tinOre_smeltsInTheBlastFurnace(final GameTestHelper helper) {
        smelts(helper, Blocks.BLAST_FURNACE,
                new ItemStack(MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.STONE).get()));
    }

    /** Of the series' mods only the Core smelts raw tin and tin's ore; another mod's recipe for tin may stand. */
    @GameTest(template = ARENA)
    public static void tin_isSmeltedByTheCoreAloneOfTheSeries(final GameTestHelper helper) {
        for (final ItemStack tin : List.of(new ItemStack(MaterialUses.item(SeriesMaterials.TIN, Forms.RAW).get()),
                new ItemStack(MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.STONE).get()))) {
            final List<ResourceLocation> series = helper.getLevel().getRecipeManager()
                    .getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(tin), helper.getLevel()).stream()
                    .map(recipe -> recipe.id()).filter(id -> SERIES.contains(id.getNamespace())).toList();
            helper.assertTrue(series.size() == 1 && series.get(0).getNamespace().equals(JsCore.MODID),
                    "one recipe of the series smelts " + tin + ", the Core's: " + series);
        }
        helper.succeed();
    }

    private static ItemStack crafted(final GameTestHelper helper, final List<ItemStack> grid) {
        final CraftingInput input = CraftingInput.of(3, 3, grid);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /* Lights {@code furnace} with coal under {@code what} and waits for a tin ingot in its output. */
    private static void smelts(final GameTestHelper helper, final Block furnace, final ItemStack what) {
        final BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, furnace);
        final AbstractFurnaceBlockEntity oven = (AbstractFurnaceBlockEntity) helper.getBlockEntity(at);
        oven.setItem(INPUT, what);
        oven.setItem(FUEL, new ItemStack(Items.COAL));
        // Any tin ingot: another installed mod may smelt its own tin's raw lumps too, and the oven takes one recipe.
        helper.succeedWhen(() -> helper.assertTrue(oven.getItem(OUTPUT).is(Forms.INGOT.itemTag(SeriesMaterials.TIN)),
                "the oven gives a tin ingot: " + oven.getItem(OUTPUT)));
    }
}
