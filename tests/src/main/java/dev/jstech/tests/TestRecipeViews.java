/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.recipeview.RecipeViews;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Kinds of recipe declared for the recipe viewers that are not a machine's, to prove the Core's kit shows any kind in
 * JEI, EMI and REI alike: the game's own stonecutting recipes, each in its holder, and a list of fuels that are no
 * recipe at all, each a value the test mod lists.
 */
@TextHolder
public final class TestRecipeViews {

    public static final TextKey STONECUTTING_TITLE = TextKey.of("jstests.recipe_view.stonecutting",
            "Test Stonecutting");
    public static final TextKey FUELS_TITLE = TextKey.of("jstests.recipe_view.fuels", "Test Fuels");
    public static final TextKey BURNS = TextKey.of("jstests.recipe_view.burns", "Burns %s ticks");

    /** The slot of a page's input, the arrow beside it, its output, and its line. */
    public static final int INPUT_X = 0;
    public static final int ARROW_X = 22;
    public static final int OUTPUT_X = 50;
    public static final int LINE_X = 24;
    public static final int LINE_Y = 5;
    public static final int WIDTH = 120;
    public static final int HEIGHT = 32;
    /* The viewers' own dark grey, opaque. */
    private static final int TEXT_COLOUR = 0x404040 | (0xFF << 24);

    /** The game's stonecutting, each recipe its input, an arrow and its output. */
    public static final RecipeViewKind<RecipeHolder<StonecutterRecipe>> STONECUTTING = RecipeViews
            .recipes(JsTests.MODID, "test_stonecutting", STONECUTTING_TITLE, () -> RecipeType.STONECUTTING)
            .workstations(() -> List.of(new ItemStack(Items.STONECUTTER)))
            .size(WIDTH, HEIGHT)
            .layout((holder, page) -> page.input(INPUT_X, 0, holder.value().getIngredients().getFirst())
                    .arrow(ARROW_X, 0, 0)
                    .output(OUTPUT_X, 0, holder.value().getResultItem(RegistryAccess.EMPTY)))
            .declare();

    /** A few fuels, each its item and how long it burns. */
    public static final RecipeViewKind<Fuel> FUELS = RecipeViews.of(JsTests.MODID, "test_fuels", FUELS_TITLE,
                    Fuel.class)
            .recipes(level -> List.of(new Fuel(new ItemStack(Items.COAL), 1600),
                    new Fuel(new ItemStack(Items.BLAZE_ROD), 2400), new Fuel(new ItemStack(Items.STICK), 100)))
            .icon(() -> new ItemStack(Items.FURNACE))
            .size(WIDTH, HEIGHT)
            .layout((fuel, page) -> page.input(INPUT_X, 0, List.of(fuel.item()))
                    .line(LINE_X, LINE_Y, WIDTH - LINE_X, GameText.component(BURNS.with(fuel.ticks())),
                            TEXT_COLOUR))
            .declare();

    private TestRecipeViews() {
    }

    /** Declares the kinds, before the game registers its things. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * A fuel, as the test kind of fuels lists it.
     *
     * @param item  the fuel
     * @param ticks how long it burns
     */
    public record Fuel(ItemStack item, int ticks) {
    }
}
