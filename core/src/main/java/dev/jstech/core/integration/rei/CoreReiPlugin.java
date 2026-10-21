/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.client.recipeview.IKeepsViewersClear;
import dev.jstech.core.client.recipeview.IRecipeViewer;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.client.recipeview.MultiblockRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.multipart.MicroblockShowcase;
import dev.jstech.core.recipeview.RecipeViewKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.TextField;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.overlay.ScreenOverlay;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's bridge to REI, which finds it by its annotation and only when REI is installed: the same as the bridges
 * to JEI and EMI, a category for every kind of recipe from the Core's kit ({@link ViewKinds}), every mod's processing
 * machines among them, with the blocks that work them as its workstations and the "+" that moves a recipe into the
 * open machine; the multiblocks with their previews; the microblocks' rules as crafting recipes, each going through
 * every block; and the areas screens name kept clear of REI's lists.
 */
@REIPluginClient
public final class CoreReiPlugin implements REIClientPlugin {

    /* REI as the Core's screens ask it, added to the viewers running once REI reads its plugins. */
    private static final IRecipeViewer VIEWER = new ReiViewer();
    /* What the ids of the microblocks' rules start with. */
    private static final String MICROBLOCK = "microblock/";
    /* The kinds as the last reading of the plugins made them, the categories and the displays from the same. */
    private static volatile List<RecipeViewKind<?>> kinds = List.of();

    @Override
    public void registerCategories(final CategoryRegistry registry) {
        RecipeViewers.add(VIEWER);
        kinds = ViewKinds.all();
        final ClientLevel level = Minecraft.getInstance().level;
        for (final RecipeViewKind<?> kind : kinds) {
            addCategory(registry, kind, level);
        }
        registry.add(new MultiblockReiCategory());
        for (final ItemStack tool : MultiblockShows.tools()) {
            registry.addWorkstations(MultiblockReiDisplay.CATEGORY, EntryStacks.of(tool));
        }
    }

    @Override
    public void registerDisplays(final DisplayRegistry registry) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (final RecipeViewKind<?> kind : kinds) {
                addDisplays(registry, kind, level);
            }
        }
        for (final MultiblockEntry entry : MultiblockRecipeViews.entries()) {
            registry.add(new MultiblockReiDisplay(entry));
        }
        // The microblocks' rules, each one crafting recipe whose slots go through every block's frame together.
        MicroblockShowcase.forget();
        for (final MicroblockShowcase.Rule rule : MicroblockShowcase.Rule.values()) {
            final List<MicroblockShowcase.Frame> frames = MicroblockShowcase.frames(rule);
            if (!frames.isEmpty()) {
                registry.add(microblocks(rule, frames));
            }
        }
    }

    @Override
    public void registerTransferHandlers(final TransferHandlerRegistry registry) {
        registry.register(new MachineReiTransfer());
    }

    @Override
    public void registerExclusionZones(final ExclusionZones zones) {
        zones.register(AbstractContainerScreen.class, screen -> {
            if (!(screen instanceof IKeepsViewersClear keeps)) {
                return List.of();
            }
            final List<Rectangle> out = new ArrayList<>();
            for (final Rect2i area : keeps.areasKeptClear()) {
                out.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
            return out;
        });
    }

    /** How many recipes REI shows under the kind of recipe whose id is {@code id}. */
    public static int recipesShown(final ResourceLocation id) {
        return DisplayRegistry.getInstance().get(CategoryIdentifier.of(id)).size();
    }

    /** How many microblock rules REI shows among its crafting recipes. */
    public static int microblockRulesShown() {
        return (int) DisplayRegistry.getInstance().get(BuiltinPlugin.CRAFTING).stream()
                .filter(display -> display.getDisplayLocation().map(id -> id.getNamespace().equals(JsCore.MODID)
                        && id.getPath().startsWith(MICROBLOCK)).orElse(false))
                .count();
    }

    /** The id of the multiblocks' category in REI, the same as JEI's and EMI's. */
    public static ResourceLocation multiblocks() {
        return MultiblockReiDisplay.CATEGORY.getIdentifier();
    }

    /** Narrows REI's list to what the words find, as typing them in its search does (client tests do). */
    public static boolean search(final String words) {
        final TextField field = REIRuntime.getInstance().getSearchTextField();
        if (field == null) {
            return false;
        }
        field.setText(words);
        return true;
    }

    /** Opens REI's recipe screen on the kind of recipe whose id is {@code id}; whether it opened. */
    public static boolean showRecipes(final ResourceLocation id) {
        return ViewSearchBuilder.builder().addCategory(CategoryIdentifier.of(id)).open();
    }

    private static <T> void addCategory(final CategoryRegistry registry, final RecipeViewKind<T> kind,
                                        @Nullable final ClientLevel level) {
        final List<T> recipes = level == null ? List.of() : kind.recipes(level);
        registry.add(new DeclaredReiCategory<>(kind, kind.size(recipes)));
        for (final ItemStack station : kind.workstations()) {
            registry.addWorkstations(CategoryIdentifier.of(kind.id()), EntryStacks.of(station));
        }
    }

    private static <T> void addDisplays(final DisplayRegistry registry, final RecipeViewKind<T> kind,
                                        final ClientLevel level) {
        for (final T recipe : kind.recipes(level)) {
            registry.add(new DeclaredReiDisplay<>(kind, recipe));
        }
    }

    /* A rule as one crafting recipe: each place holds that place of every frame, in the frames' order. */
    private static MicroblockReiDisplay microblocks(final MicroblockShowcase.Rule rule,
                                                    final List<MicroblockShowcase.Frame> frames) {
        final List<EntryIngredient> grid = new ArrayList<>();
        for (int place = 0; place < frames.getFirst().grid().size(); place++) {
            final List<EntryStack<?>> held = new ArrayList<>();
            for (final MicroblockShowcase.Frame frame : frames) {
                final ItemStack stack = frame.grid().get(place);
                held.add(stack.isEmpty() ? EntryStack.empty() : EntryStacks.of(stack));
            }
            grid.add(EntryIngredient.of(held));
        }
        final List<EntryStack<?>> made = new ArrayList<>();
        frames.forEach(frame -> made.add(EntryStacks.of(frame.output())));
        return new MicroblockReiDisplay(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, MICROBLOCK + rule.id()),
                grid, List.of(EntryIngredient.of(made)));
    }

    /* REI as the Core's screens ask it: what the pointer is over in its lists, never a game slot. */
    private static final class ReiViewer implements IRecipeViewer {

        @Override
        public boolean showRecipesOf(final ItemStack stack) {
            return ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(stack)).open();
        }

        @Override
        public boolean showUsesOf(final ItemStack stack) {
            return ViewSearchBuilder.builder().addUsagesFor(EntryStacks.of(stack)).open();
        }

        @Override
        public ItemStack hovered() {
            final Optional<ScreenOverlay> overlay = REIRuntime.getInstance().getOverlay();
            if (overlay.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack listed = itemOf(overlay.get().getEntryList().getFocusedStack());
            if (!listed.isEmpty()) {
                return listed;
            }
            return overlay.get().getFavoritesList().map(OverlayListWidget::getFocusedStack)
                    .map(ReiViewer::itemOf).orElse(ItemStack.EMPTY);
        }

        private static ItemStack itemOf(final EntryStack<?> stack) {
            return stack != null && stack.getValue() instanceof ItemStack item ? item : ItemStack.EMPTY;
        }
    }
}
