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
import dev.jstech.core.client.recipeview.IRecipeViewer;
import dev.jstech.core.client.recipeview.MultiblockRecipeViews;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.multipart.MicroblockRecipes;
import dev.jstech.core.multipart.MicroblockShowcase;
import dev.jstech.core.progression.KnowledgeShapedRecipe;
import dev.jstech.core.progression.KnowledgeShapelessRecipe;
import dev.jstech.core.recipeview.RecipeViewKind;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Core's bridge to JEI, which finds it by its annotation and only when JEI is installed: every kind of recipe from
 * the Core's kit gets a category ({@link ViewKinds}), every mod's processing machines among them, with the blocks that
 * work them beside it and, for a machine, the "+" that moves a recipe into the open machine; every multiblock that can
 * be shown before it is built has its preview and materials; the
 * microblocks' rules are in the crafting category, one entry each going through every block; and every screen that
 * names areas to keep clear has the ingredient list kept off them. A mod that declares its machines, its
 * multiblocks and its screens the Core's way writes nothing for JEI of its own.
 */
@JeiPlugin
public final class CoreJeiPlugin implements IModPlugin {

    /* JEI as the Core's screens ask it, added to the viewers running once JEI is. */
    private static final IRecipeViewer VIEWER = new JeiViewer();

    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registration) {
        final IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (final RecipeViewKind<?> kind : ViewKinds.all()) {
            registration.addRecipeCategories(category(kind, gui));
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
        // The crafting recipes that ask for knowledge, with a padlock while the player's team lacks it.
        crafting.addExtension(KnowledgeShapedRecipe.class, new KnowledgeCraftingExtension<>());
        crafting.addExtension(KnowledgeShapelessRecipe.class, new KnowledgeCraftingExtension<>());
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (final RecipeViewKind<?> kind : ViewKinds.all()) {
                addRecipes(registration, kind, level);
            }
        }
        registration.addRecipes(MultiblockCategory.TYPE, MultiblockRecipeViews.entries());
    }

    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        for (final RecipeViewKind<?> kind : ViewKinds.all()) {
            for (final ItemStack station : kind.workstations()) {
                registration.addRecipeCatalyst(station, DeclaredCategory.typeOf(kind));
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
                    DeclaredCategory.holderType(kind.id()));
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
        RecipeViewers.add(VIEWER);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** How many recipes JEI shows under that kind of machine, or nothing while JEI is not running. */
    public static OptionalInt recipesShown(final ProcessingKind kind) {
        return recipesShown(kind.id());
    }

    /** How many recipes JEI shows under the kind of recipe whose id is {@code id}, or nothing while it is not on. */
    public static OptionalInt recipesShown(final ResourceLocation id) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return OptionalInt.empty();
        }
        final Optional<RecipeType<?>> type = running.getJeiHelpers().getRecipeType(id);
        if (type.isEmpty()) {
            return OptionalInt.of(0);
        }
        final long count = running.getRecipeManager().createRecipeLookup(type.get()).get().count();
        return OptionalInt.of((int) Math.min(count, Integer.MAX_VALUE));
    }

    /** Opens JEI's recipe screen on the recipes of that kind of machine; false while JEI is not running. */
    public static boolean showRecipes(final ProcessingKind kind) {
        return showRecipes(kind.id());
    }

    /** Opens JEI's recipe screen on the kind of recipe whose id is {@code id}; false while JEI has none of it. */
    public static boolean showRecipes(final ResourceLocation id) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        final Optional<RecipeType<?>> type = running.getJeiHelpers().getRecipeType(id);
        type.ifPresent(found -> running.getRecipesGui().showTypes(List.of(found)));
        return type.isPresent();
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

    /** Opens JEI on the recipes of the processing machines of {@code kind}; whether JEI was there to open. */
    public static boolean showRecipesOf(final ProcessingKind kind) {
        return showRecipes(kind.id());
    }

    /** Opens JEI's recipe screen on what {@code stack} goes into; false before JEI is running. */
    public static boolean showUsesOf(final ItemStack stack) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        running.getRecipesGui().show(running.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack));
        return true;
    }

    /** Narrows JEI's ingredient list to what the words find, as typing them in its search does (client tests do). */
    public static boolean search(final String words) {
        final IJeiRuntime running = runtime;
        if (running == null) {
            return false;
        }
        running.getIngredientFilter().setFilterText(words);
        return true;
    }

    /* A kind's category, each page as large as the kind says for the recipes the player's game was sent. */
    private static <T> DeclaredCategory<T> category(final RecipeViewKind<T> kind, final IGuiHelper gui) {
        final ClientLevel level = Minecraft.getInstance().level;
        final List<T> recipes = level == null ? List.of() : kind.recipes(level);
        return new DeclaredCategory<>(kind, gui, kind.size(recipes));
    }

    private static <T> void addRecipes(final IRecipeRegistration registration, final RecipeViewKind<T> kind,
                                       final ClientLevel level) {
        registration.addRecipes(DeclaredCategory.typeOf(kind), kind.recipes(level));
    }

    /* JEI as the Core's screens ask it: its recipe screen first, then its ingredient list and its bookmarks. */
    private static final class JeiViewer implements IRecipeViewer {

        @Override
        public boolean showRecipesOf(final ItemStack stack) {
            return CoreJeiPlugin.showRecipesOf(stack);
        }

        @Override
        public boolean showUsesOf(final ItemStack stack) {
            return CoreJeiPlugin.showUsesOf(stack);
        }

        @Override
        public ItemStack hovered() {
            final IJeiRuntime running = runtime;
            if (running == null) {
                return ItemStack.EMPTY;
            }
            final Optional<ItemStack> inRecipe = running.getRecipesGui().getIngredientUnderMouse(
                    VanillaTypes.ITEM_STACK);
            if (inRecipe.isPresent()) {
                return inRecipe.get();
            }
            final ItemStack listed = running.getIngredientListOverlay().getIngredientUnderMouse(
                    VanillaTypes.ITEM_STACK);
            if (listed != null) {
                return listed;
            }
            final ItemStack bookmarked = running.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            return bookmarked == null ? ItemStack.EMPTY : bookmarked;
        }
    }
}
