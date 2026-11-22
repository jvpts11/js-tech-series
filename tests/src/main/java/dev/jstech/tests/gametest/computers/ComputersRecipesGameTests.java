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
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Computers' recipes (computers/docs/design/recipes.md): every recipe file loads, no two recipes a machine or a
 * bench could run take the same things, every bench recipe makes one thing, the Crafting Computer is made of an
 * empty Personal Computer and its card, never of one with parts inside, the raw ore of the furnace's metals melts and
 * each Lithography Station's light is made at the bench of the era before.
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

    /*
     * A qRusset sold in several colours is made in its first and dyed into the others at a crafting table: the G3's
     * Bondi Blue with each flavour's dye, the Advanced qRusset's blue with each colour's. One with parts inside is not
     * dyed, so no recipe eats a computer's parts.
     */
    @GameTest(template = ARENA)
    public static void qrusset_isDyedIntoEachOfItsColoursEmpty(final GameTestHelper helper) {
        final List<Item> flavourDyes = List.of(Items.BLUE_DYE, Items.RED_DYE, Items.LIME_DYE, Items.PURPLE_DYE,
                Items.ORANGE_DYE);
        final List<Item> colourDyes = List.of(Items.GREEN_DYE, Items.PINK_DYE, Items.LIGHT_GRAY_DYE,
                Items.YELLOW_DYE, Items.ORANGE_DYE, Items.PURPLE_DYE);
        final List<String> missing = new ArrayList<>();
        final Item bondi = ComputingModule.QRUSSET_G3.getFirst().get().asItem();
        for (int i = 1; i < ComputingModule.QRUSSET_G3.size(); i++) {
            dyed(helper, new ItemStack(bondi), flavourDyes.get(i - 1), ComputingModule.QRUSSET_G3.get(i).get()
                    .asItem(), missing);
        }
        final Item blue = ComputingModule.QRUSSET_COLOURS.getFirst().get().asItem();
        for (int i = 1; i < ComputingModule.QRUSSET_COLOURS.size(); i++) {
            dyed(helper, new ItemStack(blue), colourDyes.get(i - 1), ComputingModule.QRUSSET_COLOURS.get(i).get()
                    .asItem(), missing);
        }
        helper.assertTrue(missing.isEmpty(), "each colour is its first dyed: none for " + missing);
        final ItemStack built = new ItemStack(bondi);
        built.set(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.fromItems(
                List.of(new ItemStack(ChainParts.FERRITE.get()))));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1,
                List.of(built, new ItemStack(Items.BLUE_DYE))), helper.getLevel()).isEmpty(), "one with parts"
                + " inside is not dyed");
        helper.succeed();
    }

    /*
     * The metals of the machines' first step exist before any machine: the raw ore of each metal the furnace melts
     * gives its ingot in the furnace and the blast furnace, the Crusher not needed.
     */
    @GameTest(template = ARENA)
    public static void rawOre_ofTheFurnacesMetalsSmeltsIntoItsIngot(final GameTestHelper helper) {
        final List<String> missing = new ArrayList<>();
        for (final Element element : List.of(Element.TIN, Element.LEAD, Element.ZINC, Element.SILVER, Element.NICKEL,
                Element.COBALT, Element.MANGANESE)) {
            final ItemStack raw = new ItemStack(MaterialUses.item(Elements.of(element), Forms.RAW).get());
            final Item ingot = MaterialUses.item(Elements.of(element), Forms.INGOT).get();
            for (final RecipeType<? extends AbstractCookingRecipe> furnace : List.of(RecipeType.SMELTING,
                    RecipeType.BLASTING)) {
                final SingleRecipeInput input = new SingleRecipeInput(raw);
                final boolean melts = helper.getLevel().getRecipeManager().getRecipeFor(furnace, input,
                        helper.getLevel()).map(recipe -> recipe.value().assemble(input, helper.getLevel()
                        .registryAccess()).is(ingot)).orElse(false);
                if (!melts) {
                    missing.add(element.name().toLowerCase() + " in " + BuiltInRegistries.RECIPE_TYPE.getKey(furnace)
                            .getPath());
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "each raw ore melts into its ingot: not " + missing);
        helper.succeed();
    }

    /*
     * Each Lithography Station's light is made at the bench of the era before: the era's own bench takes chips only
     * its own station makes, so a light made there could never be had.
     */
    @GameTest(template = ARENA)
    public static void lights_areMadeAtTheBenchOfTheEraBefore(final GameTestHelper helper) {
        final Map<String, ProcessingKind> before = Map.of("lithography_krf_laser", ChainBenches.VINTAGE_PARTS,
                "lithography_arf_laser", ChainBenches.LEGACY_PARTS,
                "lithography_euv_source", ChainBenches.STANDARD_PARTS);
        final List<String> wrong = new ArrayList<>();
        before.forEach((light, bench) -> {
            final Item made = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    light));
            final boolean there = helper.getLevel().getRecipeManager().getRecipes().stream()
                    .anyMatch(holder -> holder.value() instanceof ProcessingRecipe recipe && recipe.kind() == bench
                            && recipe.outputs().stream().anyMatch(output -> output.is(made)));
            if (!there) {
                wrong.add(light);
            }
        });
        helper.assertTrue(wrong.isEmpty(), "each light is made at the bench of the era before: not " + wrong);
        helper.succeed();
    }

    /* Notes in {@code missing} when {@code base} and {@code dye} at a crafting table make no {@code into}. */
    private static void dyed(final GameTestHelper helper, final ItemStack base, final Item dye, final Item into,
                             final List<String> missing) {
        final CraftingInput input = CraftingInput.of(2, 1, List.of(base, new ItemStack(dye)));
        final boolean makes = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input,
                helper.getLevel()).map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess())
                .is(into)).orElse(false);
        if (!makes) {
            missing.add(BuiltInRegistries.ITEM.getKey(into).getPath());
        }
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
