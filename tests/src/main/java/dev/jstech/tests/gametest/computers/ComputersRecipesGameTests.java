/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Computers' recipes (computers/docs/design/recipes.md): every recipe file loads, no two recipes a machine or a
 * bench could run take the same things, every bench recipe makes one thing, and the Crafting Computer is made of an
 * empty Personal Computer and its card, never of one with parts inside.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputersRecipesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos BENCH = new BlockPos(3, 2, 3);
    private static final String FOLDER = "recipe/";
    private static final String JSON = ".json";
    private static final int SHOWN = 5;

    private ComputersRecipesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void recipes_everyFileLoads(final GameTestHelper helper) {
        final Set<ResourceLocation> files = helper.getLevel().getServer().getResourceManager()
                .listResources("recipe", id -> id.getNamespace().equals(JsComputers.MODID)
                        && id.getPath().endsWith(JSON)).keySet().stream()
                .map(id -> ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath()
                        .substring(FOLDER.length(), id.getPath().length() - JSON.length())))
                .collect(Collectors.toSet());
        final Set<ResourceLocation> loaded = new HashSet<>();
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            loaded.add(holder.id());
        }
        final List<ResourceLocation> missing = files.stream().filter(id -> !loaded.contains(id)).sorted().toList();
        helper.assertTrue(!files.isEmpty(), "the recipes are there to load");
        helper.assertTrue(missing.isEmpty(), "every recipe file loads: " + first(missing));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recipes_noTwoAMachineRunsTakeTheSame(final GameTestHelper helper) {
        final List<ProcessingKind> benches = ChainBenches.assembledAt(HardwareEra.ADVANCED);
        final Map<String, ResourceLocation> taken = new HashMap<>();
        final List<String> twice = new ArrayList<>();
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            if (holder.id().getNamespace().equals(JsComputers.MODID)
                    && holder.value() instanceof ProcessingRecipe recipe) {
                // A bench of the latest era runs every era's bench recipes, so they are one group.
                final String group = benches.contains(recipe.kind()) ? "bench" : recipe.kind().id().toString();
                final ResourceLocation before = taken.put(group + " " + takes(recipe), holder.id());
                if (before != null) {
                    twice.add(before + " and " + holder.id());
                }
            }
        }
        helper.assertTrue(twice.isEmpty(), "no two recipes take the same: " + first(twice));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void benches_eachRecipeMakesOneThing(final GameTestHelper helper) {
        final List<ProcessingKind> benches = ChainBenches.assembledAt(HardwareEra.ADVANCED);
        final List<ResourceLocation> more = new ArrayList<>();
        int recipes = 0;
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof ProcessingRecipe recipe && benches.contains(recipe.kind())
                    && holder.id().getNamespace().equals(JsComputers.MODID)) {
                recipes++;
                if (recipe.outputs().size() != 1 || !recipe.fluidOutputs().isEmpty()
                        || !recipe.matterOutputs().isEmpty() || !recipe.byproducts().isEmpty()) {
                    more.add(holder.id());
                }
            }
        }
        helper.assertTrue(recipes > 0, "the benches have their recipes");
        helper.assertTrue(more.isEmpty(), "a bench recipe makes one thing: " + first(more));
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void craftingComputer_isAnEmptyPersonalComputerAndItsCard(final GameTestHelper helper) {
        final WorkbenchBlockEntity empty = bench(helper, BENCH);
        empty.trays().insertItem(0, new ItemStack(ComputingModule.PERSONAL_COMPUTER.get()), false);
        empty.trays().insertItem(4, new ItemStack(ComputingModule.CRAFTING_CARD_PCIE3.get()), false);
        final WorkbenchBlockEntity full = bench(helper, BENCH.south(2));
        final ItemStack assembled = new ItemStack(ComputingModule.PERSONAL_COMPUTER.get());
        assembled.set(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.fromItems(
                List.of(new ItemStack(ChainParts.FERRITE.get()))));
        full.trays().insertItem(0, assembled, false);
        full.trays().insertItem(4, new ItemStack(ComputingModule.CRAFTING_CARD_PCIE3.get()), false);
        helper.succeedWhen(() -> {
            helper.assertTrue(empty.output().getStackInSlot(0).is(ComputingModule.CRAFTING_COMPUTER.get().asItem()),
                    "the empty computer and its card make the Crafting Computer");
            helper.assertTrue(full.output().getStackInSlot(0).isEmpty() && full.partUnderWay().isEmpty(),
                    "a computer with parts inside makes nothing");
        });
    }

    /* What {@code recipe} takes, the same text for two recipes that take the same things in any order. */
    private static String takes(final ProcessingRecipe recipe) {
        final List<String> things = new ArrayList<>();
        for (final SizedIngredient input : recipe.inputs()) {
            things.add(items(input.ingredient()) + " x" + input.count());
        }
        for (final SizedIngredient catalyst : recipe.catalysts()) {
            things.add("kept " + items(catalyst.ingredient()) + " x" + catalyst.count());
        }
        for (final SizedFluidIngredient fluid : recipe.fluidInputs()) {
            things.add(Arrays.stream(fluid.getFluids()).map(stack -> BuiltInRegistries.FLUID.getKey(stack.getFluid())
                    .toString()).sorted().collect(Collectors.joining("|")) + " " + fluid.amount());
        }
        for (final MatterStack matter : recipe.matterInputs()) {
            things.add(matter.material().id() + " " + matter.amount());
        }
        things.sort(null);
        return String.join(", ", things);
    }

    private static String items(final Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())
                .toString()).sorted().collect(Collectors.joining("|"));
    }

    private static String first(final List<?> list) {
        return list.stream().limit(SHOWN).map(Object::toString).collect(Collectors.joining("; "));
    }

    /* A Standard bench at {@code pos}, facing east. */
    private static WorkbenchBlockEntity bench(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ChainBenches.STANDARD.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        return TestWorldBuilder.forGameTest(helper).blockEntity(pos, WorkbenchBlockEntity.class);
    }
}
