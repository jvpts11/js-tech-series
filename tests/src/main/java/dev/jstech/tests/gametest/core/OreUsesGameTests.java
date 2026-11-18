/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.tests.JsTests;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every ore of the series, and the raw form it breaks into, is good for something: a recipe takes it, so a recipe
 * viewer shows the player what to do with what they mined.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OreUsesGameTests {

    private static final String ARENA = "empty";
    /* The series' mods, whose ores are checked. */
    private static final Set<String> SERIES = Set.of("jscore", "jsc", "jsindustrial", "jsspace");
    /* The mods whose recipes make J's Computers' chain, followed from the ores. */
    private static final Set<String> CHAIN = Set.of("jscore", "jsc");
    /*
     * The wafers of light, which the Lithography Station takes by its own steps, through the laser diodes' mask or the
     * LEDs', rather than by a recipe; until the recipe viewers show those steps.
     */
    private static final Set<String> WORKED_BY_STEPS = Set.of("algainp_wafer", "gallium_arsenide_wafer",
            "gallium_nitride_wafer");
    /*
     * The Moon's and Mars' own minerals: what J's Space's own recipes will need, not yet built (its minerals page,
     * "To build"); mined and kept until then.
     */
    private static final List<String> AWAITING_SPACE = List.of("ilmenite", "armalcolite", "tranquillityite",
            "merrillite", "native_iron", "olivine", "jarosite", "gypsum");

    private OreUsesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void ores_everyOreAndItsRawFormHaveAUse(final GameTestHelper helper) {
        final List<RecipeHolder<?>> recipes = List.copyOf(helper.getLevel().getRecipeManager().getRecipes());
        final List<String> unused = new ArrayList<>();
        int checked = 0;
        for (final Item item : BuiltInRegistries.ITEM) {
            final String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            final String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            final ItemStack stack = new ItemStack(item);
            if (!SERIES.contains(namespace) || !stack.is(Tags.Items.ORES) && !stack.is(Tags.Items.RAW_MATERIALS)
                    || AWAITING_SPACE.stream().anyMatch(path::contains)) {
                continue;
            }
            checked++;
            if (recipes.stream().noneMatch(recipe -> takes(recipe, stack))) {
                unused.add(namespace + ":" + path);
            }
        }
        helper.assertTrue(checked > 0, "the series has ores");
        helper.assertTrue(unused.isEmpty(), unused.size() + " of " + checked + " ores and raw forms no recipe takes: "
                + unused);
        helper.succeed();
    }

    /*
     * What J's Computers' chain makes of the Overworld's ores is good for something all the way along: every form of a
     * material its recipes make from an ore, its raw form, or what was made of them (crushed ore, dust, ingot, crystal,
     * a byproduct's dust) is taken by a recipe in turn, so no mined chain ends in a thing nothing uses.
     */
    @GameTest(template = ARENA)
    public static void ores_everyFormTheChainMakesOfAnOreHasAUse(final GameTestHelper helper) {
        final List<RecipeHolder<?>> recipes = List.copyOf(helper.getLevel().getRecipeManager().getRecipes());
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final Set<Item> forms = new HashSet<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            forms.add(BuiltInRegistries.ITEM.get(thing.id()));
        }
        final Deque<Item> next = new ArrayDeque<>();
        final Set<Item> seen = new HashSet<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            final ItemStack stack = new ItemStack(item);
            final String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (real(item) && (stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS))
                    && AWAITING_SPACE.stream().noneMatch(path::contains) && seen.add(item)) {
                next.add(item);
            }
        }
        final Set<String> unused = new TreeSet<>();
        while (!next.isEmpty()) {
            final Item item = next.poll();
            final ItemStack stack = new ItemStack(item);
            boolean used = false;
            for (final RecipeHolder<?> recipe : recipes) {
                if (!takes(recipe, stack)) {
                    continue;
                }
                used = true;
                if (!CHAIN.contains(recipe.id().getNamespace())) {
                    continue;
                }
                for (final ItemStack made : made(recipe, registries)) {
                    if (real(made.getItem()) && forms.contains(made.getItem()) && seen.add(made.getItem())) {
                        next.add(made.getItem());
                    }
                }
            }
            if (!used && !WORKED_BY_STEPS.contains(BuiltInRegistries.ITEM.getKey(item).getPath())) {
                unused.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }
        helper.assertTrue(unused.isEmpty(), unused.size() + " of " + seen.size() + " things made of the ores no recipe"
                + " takes: " + unused);
        helper.succeed();
    }

    /* Everything {@code recipe} makes: its result, and a machine's every output and byproduct. */
    private static List<ItemStack> made(final RecipeHolder<?> recipe, final RegistryAccess registries) {
        final List<ItemStack> out = new ArrayList<>();
        if (recipe.value() instanceof ProcessingRecipe processing) {
            out.addAll(processing.outputs());
            processing.byproducts().forEach(byproduct -> out.add(byproduct.stack()));
        } else {
            out.add(recipe.value().getResultItem(registries));
        }
        out.removeIf(ItemStack::isEmpty);
        return out;
    }

    /* Whether {@code item} is a real thing of the series' mods, not one of the tests'. */
    private static boolean real(final Item item) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return SERIES.contains(id.getNamespace()) && !id.getPath().contains("jstests") && !id.getPath()
                .contains("testite");
    }

    /* Whether {@code recipe} takes {@code stack} as one of its ingredients. */
    private static boolean takes(final RecipeHolder<?> recipe, final ItemStack stack) {
        for (final Ingredient ingredient : recipe.value().getIngredients()) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }
}
