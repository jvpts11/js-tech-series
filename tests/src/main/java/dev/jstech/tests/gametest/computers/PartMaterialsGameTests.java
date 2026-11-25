/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The small parts that take a material of their own, which makes them what they are, so a bigger part's lot never
 * makes one of them; and the Vintage Lithography Station's mercury lamp, made with a Mercury Arc Tube sealed at the
 * Display Line instead of a bucket of mercury.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PartMaterialsGameTests {

    private static final String ARENA = "empty";
    private static final int ARC_TUBE_MERCURY = 100;

    private PartMaterialsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void parts_eachTakeTheirOwnMaterial(final GameTestHelper helper) {
        final Item rod = MaterialUses.item(SeriesAlloys.STEEL, Forms.ROD).get();
        final Item tin = MaterialUses.item(Elements.of(Element.TIN), Forms.NUGGET).get();
        takes(helper, CoolingItems.VINTAGE_STOCK_COOLER.get(), rod, "the Vintage stock cooler its clip");
        takes(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get(), rod, "the hot-swap drive bay its rails");
        takes(helper, ChainParts.COPPER_COIL.get(), MaterialUses.item(SeriesCompounds.PVC, Forms.SHEET).get(),
                "the copper coil its bobbin");
        takes(helper, ChainParts.INDUCTOR.get(), tin, "the inductor its tinned leads");
        takes(helper, MaterialUses.item(SeriesCompounds.GLASS, Forms.LENS).get(),
                MaterialUses.item(Elements.of(Element.ALUMINIUM), Forms.SHEET).get(), "the glass lens its ring");
        takes(helper, ChainParts.CERAMIC_PACKAGE.get(), tin, "the ceramic package its tinned lead frame");
        takes(helper, item("vaultis_array_pci"), ChainParts.MEMORY_CHIPS.get(1).get(),
                "the Vaultis Array PCI its read cache");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mercuryLamp_isMadeWithAnArcTubeSealedAtTheDisplayLine(final GameTestHelper helper) {
        final Item tube = ChainParts.MERCURY_ARC_TUBE.get();
        final Fluid mercury = MaterialLiquids.fluid(Elements.of(Element.MERCURY)).get();
        final List<RecipeHolder<?>> sealed = recipesMaking(helper, tube);
        helper.assertTrue(sealed.size() == 1, "the tube has one recipe: " + sealed.stream().map(RecipeHolder::id)
                .toList());
        final ProcessingRecipe arc = (ProcessingRecipe) sealed.getFirst().value();
        helper.assertTrue(has(arc, Items.QUARTZ)
                && has(arc, MaterialUses.item(Elements.of(Element.TUNGSTEN), Forms.NUGGET).get())
                && arc.fluidInputs().stream().anyMatch(fluid -> fluid.amount() == ARC_TUBE_MERCURY
                && fluid.test(new FluidStack(mercury, ARC_TUBE_MERCURY))),
                "the tube is quartz, a tungsten nugget and 100 mB of mercury");
        final Item lamp = ChainMachineBlocks.byId("lithography_mercury_lamp").get().asItem();
        final List<CraftingRecipe> made = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)
                .stream().map(RecipeHolder::value).filter(recipe -> recipe.getResultItem(helper.getLevel()
                        .registryAccess()).is(lamp)).toList();
        helper.assertTrue(made.size() == 1 && made.getFirst().getIngredients().stream().anyMatch(ingredient ->
                ingredient.test(new ItemStack(tube))) && made.getFirst().getIngredients().stream().noneMatch(
                ingredient -> ingredient.test(new ItemStack(mercury.getBucket()))),
                "the lamp takes the tube, and no bucket of mercury");
        helper.succeed();
    }

    /* Asserts {@code part}'s one recipe takes {@code material}. */
    private static void takes(final GameTestHelper helper, final Item part, final Item material, final String what) {
        final List<RecipeHolder<?>> made = recipesMaking(helper, part);
        helper.assertTrue(made.size() == 1 && has((ProcessingRecipe) made.getFirst().value(), material), what + ": "
                + BuiltInRegistries.ITEM.getKey(part) + " takes " + BuiltInRegistries.ITEM.getKey(material)
                + ", made by " + made.stream().map(RecipeHolder::id).toList());
    }

    /* Whether one of {@code recipe}'s inputs is {@code item}, whatever components it asks of it. */
    private static boolean has(final ProcessingRecipe recipe, final Item item) {
        return recipe.inputs().stream().anyMatch(input -> Arrays.stream(input.getItems())
                .anyMatch(stack -> stack.is(item)));
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }

    /* The game's processing recipes that make {@code item}, the test mod's own fixtures left out. */
    private static List<RecipeHolder<?>> recipesMaking(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(holder -> !holder.id().getNamespace().equals(JsTests.MODID)
                        && holder.value() instanceof ProcessingRecipe recipe
                        && recipe.outputs().stream().anyMatch(out -> out.is(item))).toList();
    }
}
