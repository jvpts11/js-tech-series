/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.client.recipeview.IKeepsViewersClear;
import dev.jstech.core.client.recipeview.MultiblockRecipeViews;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.multipart.MicroblockRecipes;
import dev.jstech.core.multipart.MicroblockShowcase;
import java.util.List;
import java.util.OptionalInt;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.IExtendableCraftingRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's bridge to JEI, which finds it by its annotation and only when JEI is installed: every mod's processing
 * machines get a category of their recipes, with the machines that work them beside it and the "+" that moves a recipe
 * into the open machine; every multiblock that can be shown before it is built has its preview and materials; the
 * microblocks' rules are in the crafting category, one entry each going through every block; and every screen that
 * names areas to keep clear has the ingredient list kept off them. A mod that declares its machines, its
 * multiblocks and its screens the Core's way writes nothing for JEI of its own.
 */
@JeiPlugin
public final class CoreJeiPlugin implements IModPlugin {

    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registration) {
        final IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            final List<ProcessingRecipe> recipes = recipesOf(kind).stream().map(RecipeHolder::value).toList();
            registration.addRecipeCategories(new ProcessingCategory(kind, gui,
                    ProcessingRecipeViews.largest(recipes)));
        }
        registration.addRecipeCategories(new MultiblockCategory(gui));
    }

    /* The microblocks' rules in the crafting category, each one entry going through every block. */
    @Override
    public void registerVanillaCategoryExtensions(final IVanillaCategoryExtensionRegistration registration) {
        MicroblockShowcase.forget();
        final IExtendableCraftingRecipeCategory crafting = registration.getCraftingCategory();
        crafting.addExtension(MicroblockRecipes.SawCut.class,
                new MicroblockCraftingExtension<>(MicroblockShowcase.Rule.CUT));
        crafting.addExtension(MicroblockRecipes.Join.class,
                new MicroblockCraftingExtension<>(MicroblockShowcase.Rule.JOIN));
        crafting.addExtension(MicroblockRecipes.Facade.class,
                new MicroblockCraftingExtension<>(MicroblockShowcase.Rule.FACADE));
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            registration.addRecipes(ProcessingCategory.typeOf(kind), recipesOf(kind));
        }
        registration.addRecipes(MultiblockCategory.TYPE, MultiblockRecipeViews.entries());
    }

    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            for (final ItemStack machine : ProcessingRecipeViews.machineStacks(kind)) {
                registration.addRecipeCatalyst(machine, ProcessingCategory.typeOf(kind));
            }
        }
        for (final ItemStack tool : MultiblockShows.tools()) {
            registration.addRecipeCatalyst(tool, MultiblockCategory.TYPE);
        }
    }

    /** The "+" that moves a recipe into the open machine, for every kind of machine. */
    @Override
    public void registerRecipeTransferHandlers(final IRecipeTransferRegistration registration) {
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            registration.addRecipeTransferHandler(new MachineTransferHandler(kind, registration.getTransferHelper()),
                    ProcessingCategory.typeOf(kind));
        }
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class,
                new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(final AbstractContainerScreen<?> screen) {
                        return screen instanceof IKeepsViewersClear keeps ? keeps.areasKeptClear() : List.of();
                    }
                });
    }

    @Override
    public void onRuntimeAvailable(final IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** How many recipes JEI shows under that kind of machine, or nothing while JEI is not running. */
    public static OptionalInt recipesShown(final ProcessingKind kind) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return OptionalInt.empty();
        }
        final long count = running.getRecipeManager().createRecipeLookup(ProcessingCategory.typeOf(kind)).get()
                .count();
        return OptionalInt.of((int) Math.min(count, Integer.MAX_VALUE));
    }

    /** Opens JEI's recipe screen on the recipes of that kind of machine; false while JEI is not running. */
    public static boolean showRecipes(final ProcessingKind kind) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        running.getRecipesGui().showTypes(List.of(ProcessingCategory.typeOf(kind)));
        return true;
    }

    /** How many multiblocks JEI shows, or nothing while JEI is not running. */
    public static OptionalInt multiblocksShown() {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return OptionalInt.empty();
        }
        final long count = running.getRecipeManager().createRecipeLookup(MultiblockCategory.TYPE).get().count();
        return OptionalInt.of((int) Math.min(count, Integer.MAX_VALUE));
    }

    /** Opens JEI's recipe screen on the multiblocks; false while JEI is not running. */
    public static boolean showMultiblocks() {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        running.getRecipesGui().showTypes(List.of(MultiblockCategory.TYPE));
        return true;
    }

    /** Opens JEI's recipe screen on what makes {@code stack}; false before JEI is running. */
    public static boolean showRecipesOf(final ItemStack stack) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        running.getRecipesGui().show(running.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        return true;
    }

    /** The recipes of {@code kind} the player's game was sent, or none before it joined a world. */
    private static List<RecipeHolder<ProcessingRecipe>> recipesOf(final ProcessingKind kind) {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(kind.type());
    }
}
