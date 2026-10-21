/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import com.mojang.blaze3d.platform.Window;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.client.recipeview.IKeepsViewersClear;
import dev.jstech.core.client.recipeview.IRecipeViewer;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.client.recipeview.MultiblockRecipeViews;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.machine.Machines;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.multipart.MicroblockShowcase;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's bridge to EMI, which finds it by its annotation and only when EMI is installed: the same as the bridge to
 * JEI, a category for every mod's processing machines with the machines as its workstations, the fill button that
 * moves a recipe into the open machine, one for every multiblock that can be shown before it is built, the
 * microblocks' rules as one crafting recipe each going through every block, and the areas screens name kept clear of
 * EMI's lists.
 */
@EmiEntrypoint
public final class CoreEmiPlugin implements EmiPlugin {

    /* The id of the multiblocks' category, the same as JEI's. */
    private static final ResourceLocation MULTIBLOCKS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "multiblocks");
    /* EMI as the Core's screens ask it, added to the viewers running once EMI reads its plugins. */
    private static final IRecipeViewer VIEWER = new EmiViewer();

    @Override
    public void register(final EmiRegistry registry) {
        RecipeViewers.add(VIEWER);
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            final List<ItemStack> machines = ProcessingRecipeViews.machineStacks(kind);
            final EmiRenderable icon = machines.isEmpty() ? EmiStack.EMPTY : EmiStack.of(machines.getFirst());
            final EmiRecipeCategory category = new KindCategory(kind, icon);
            registry.addCategory(category);
            for (final ItemStack machine : machines) {
                registry.addWorkstation(category, EmiStack.of(machine));
            }
            for (final RecipeHolder<ProcessingRecipe> holder
                    : registry.getRecipeManager().getAllRecipesFor(kind.type())) {
                registry.addRecipe(new ProcessingEmiRecipe(category, holder));
            }
        }
        final List<ItemStack> tools = MultiblockShows.tools();
        final EmiRecipeCategory multiblocks = new NamedCategory(MULTIBLOCKS,
                tools.isEmpty() ? EmiStack.of(Items.BRICKS) : EmiStack.of(tools.getFirst()));
        registry.addCategory(multiblocks);
        for (final ItemStack tool : tools) {
            registry.addWorkstation(multiblocks, EmiStack.of(tool));
        }
        for (final MultiblockEntry entry : MultiblockRecipeViews.entries()) {
            registry.addRecipe(new MultiblockEmiRecipe(multiblocks, entry));
        }
        // The microblocks' rules, each one crafting recipe going through every block.
        MicroblockShowcase.forget();
        for (final MicroblockShowcase.Rule rule : MicroblockShowcase.Rule.values()) {
            if (!MicroblockShowcase.frames(rule).isEmpty()) {
                registry.addRecipe(new MicroblockEmiRecipe(rule));
            }
        }
        registry.addRecipeHandler(Machines.MENU.get(), new MachineRecipeHandler());
        registry.addGenericExclusionArea((screen, areas) -> {
            if (screen instanceof IKeepsViewersClear keeps) {
                for (final Rect2i area : keeps.areasKeptClear()) {
                    areas.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
    }

    /** How many recipes EMI shows under that kind of machine, or nothing before EMI has read them. */
    public static OptionalInt recipesShown(final ProcessingKind kind) {
        final EmiRecipeCategory category = categoryOf(kind);
        return category == null ? OptionalInt.empty()
                : OptionalInt.of(EmiApi.getRecipeManager().getRecipes(category).size());
    }

    /** Opens EMI's recipe screen on the recipes of that kind of machine; false before EMI has read them. */
    public static boolean showRecipes(final ProcessingKind kind) {
        final EmiRecipeCategory category = categoryOf(kind);
        if (category == null) {
            return false;
        }
        EmiApi.displayRecipeCategory(category);
        return true;
    }

    /** How many multiblocks EMI shows, or nothing before EMI has read them. */
    public static OptionalInt multiblocksShown() {
        final EmiRecipeCategory category = categoryOf(MULTIBLOCKS);
        return category == null ? OptionalInt.empty()
                : OptionalInt.of(EmiApi.getRecipeManager().getRecipes(category).size());
    }

    /** Opens EMI's recipe screen on the multiblocks; false before EMI has read them. */
    public static boolean showMultiblocks() {
        final EmiRecipeCategory category = categoryOf(MULTIBLOCKS);
        if (category == null) {
            return false;
        }
        EmiApi.displayRecipeCategory(category);
        return true;
    }

    /** Opens EMI's recipe screen on what makes {@code stack}. */
    public static boolean showRecipesOf(final ItemStack stack) {
        EmiApi.displayRecipes(EmiStack.of(stack));
        return true;
    }

    /** Opens EMI's recipe screen on what {@code stack} goes into. */
    public static boolean showUsesOf(final ItemStack stack) {
        EmiApi.displayUses(EmiStack.of(stack));
        return true;
    }

    /** Narrows EMI's list to what the words find, as typing them in its search does (client tests do). */
    public static boolean search(final String words) {
        EmiApi.setSearchText(words);
        return true;
    }

    /** Opens EMI on the recipes of the processing machines of {@code kind}; whether it has a category for them. */
    public static boolean showRecipesOf(final ProcessingKind kind) {
        final EmiRecipeCategory category = categoryOf(kind);
        if (category == null) {
            return false;
        }
        EmiApi.displayRecipeCategory(category);
        return true;
    }

    @Nullable
    private static EmiRecipeCategory categoryOf(final ProcessingKind kind) {
        return categoryOf(kind.id());
    }

    @Nullable
    private static EmiRecipeCategory categoryOf(final ResourceLocation id) {
        for (final EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            if (category.getId().equals(id)) {
                return category;
            }
        }
        return null;
    }

    /* EMI as the Core's screens ask it: what the pointer is over in its own lists and screens, never a game slot. */
    private static final class EmiViewer implements IRecipeViewer {

        @Override
        public boolean showRecipesOf(final ItemStack stack) {
            return CoreEmiPlugin.showRecipesOf(stack);
        }

        @Override
        public boolean showUsesOf(final ItemStack stack) {
            return CoreEmiPlugin.showUsesOf(stack);
        }

        /* Where the pointer is now, in the screen's scaled pixels, rather than where EMI last drew it. */
        @Override
        public ItemStack hovered() {
            final Minecraft minecraft = Minecraft.getInstance();
            final Window window = minecraft.getWindow();
            final int x = (int) (minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
            final int y = (int) (minecraft.mouseHandler.ypos() * window.getGuiScaledHeight()
                    / window.getScreenHeight());
            final EmiStackInteraction under = EmiApi.getHoveredStack(x, y, false);
            if (under.isEmpty()) {
                return ItemStack.EMPTY;
            }
            for (final EmiStack stack : under.getStack().getEmiStacks()) {
                final ItemStack item = stack.getItemStack();
                if (!item.isEmpty()) {
                    return item;
                }
            }
            return ItemStack.EMPTY;
        }
    }

    /* A kind's category, named as the kind is rather than by EMI's own key for it. */
    private static final class KindCategory extends EmiRecipeCategory {

        private final ProcessingKind kind;

        private KindCategory(final ProcessingKind kind, final EmiRenderable icon) {
            super(kind.id(), icon);
            this.kind = kind;
        }

        @Override
        public Component getName() {
            return GameText.component(kind.title());
        }
    }

    /* The multiblocks' category, named by the Core's own words rather than by EMI's key for it. */
    private static final class NamedCategory extends EmiRecipeCategory {

        private NamedCategory(final ResourceLocation id, final EmiRenderable icon) {
            super(id, icon);
        }

        @Override
        public Component getName() {
            return GameText.component(MultiblockViewTexts.CATEGORY);
        }
    }
}
