/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.text.GameText;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of recipe from the Core's kit, in JEI: under the kind's title, with its workstations beside it, each page
 * laid out by the kind ({@link RecipeView}) and drawn with JEI's own slots, arrow and text.
 *
 * @param <T> what one recipe is
 */
final class DeclaredCategory<T> implements IRecipeCategory<T> {

    private final RecipeViewKind<T> kind;
    private final RecipeType<T> type;
    private final IDrawable icon;
    private final RecipeViewKind.Size size;
    /* Each recipe's page as JEI last laid it, for its sprites to be drawn without laying it every frame. */
    private final Map<T, RecipeView> pages = Collections.synchronizedMap(new WeakHashMap<>());

    /* A slot's picture is drawn a pixel outside the 16 pixels of what it holds. */
    private static final int SLOT_INSET = 1;
    private static final int ITEM_SIZE = 16;
    private static final int LINE_HEIGHT = 10;

    DeclaredCategory(final RecipeViewKind<T> kind, final IGuiHelper gui, final RecipeViewKind.Size size) {
        this.kind = kind;
        this.type = typeOf(kind);
        final ItemStack shown = kind.icon();
        this.icon = shown.isEmpty() ? gui.createBlankDrawable(ITEM_SIZE, ITEM_SIZE)
                : gui.createDrawableItemStack(shown);
        this.size = size;
    }

    /** The JEI recipe type of {@code kind}: its own id, so a bookmark names the kind it came from. */
    @SuppressWarnings("unchecked")
    static <T> RecipeType<T> typeOf(final RecipeViewKind<T> kind) {
        if (kind.holders()) {
            return (RecipeType<T>) RecipeType.createRecipeHolderType(kind.id());
        }
        return new RecipeType<>(kind.id(), (Class<? extends T>) kind.type());
    }

    /** The JEI recipe type of a kind of the game's recipes whose id is {@code id}. */
    static <R extends Recipe<?>> RecipeType<RecipeHolder<R>> holderType(final ResourceLocation id) {
        return RecipeType.createRecipeHolderType(id);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return GameText.component(this.kind.title());
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return this.size.width();
    }

    @Override
    public int getHeight() {
        return this.size.height();
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final T recipe, final IFocusGroup focuses) {
        final RecipeView page = this.kind.view(recipe);
        this.pages.put(recipe, page);
        for (final RecipeView.Slot slot : page.slots()) {
            final int x = slot.x() + SLOT_INSET;
            final int y = slot.y() + SLOT_INSET;
            final IRecipeSlotBuilder made = (slot.role() == RecipeView.Role.OUTPUT ? builder.addOutputSlot(x, y)
                    : builder.addInputSlot(x, y)).setStandardSlotBackground();
            if (slot.holdsFluid()) {
                made.setFluidRenderer(slot.capacity(), false, ITEM_SIZE, ITEM_SIZE)
                        .addIngredients(NeoForgeTypes.FLUID_STACK, slot.fluids());
            } else {
                made.addItemStacks(slot.items());
            }
            final List<Component> lines = ViewKinds.tooltipOf(slot);
            if (!lines.isEmpty()) {
                made.addRichTooltipCallback((view, tooltip) -> lines.forEach(tooltip::add));
            }
        }
    }

    @Override
    public void createRecipeExtras(final IRecipeExtrasBuilder builder, final T recipe, final IFocusGroup focuses) {
        final RecipeView page = pageOf(recipe);
        page.arrow().ifPresent(arrow -> {
            if (arrow.ticks() > 0) {
                builder.addAnimatedRecipeArrow(arrow.ticks()).setPosition(arrow.x(), arrow.y());
            } else {
                builder.addRecipeArrow().setPosition(arrow.x(), arrow.y());
            }
        });
        for (final RecipeView.Line line : page.lines()) {
            builder.addText(line.text(), Math.max(1, line.width()), LINE_HEIGHT).setPosition(line.x(), line.y())
                    .setColor(line.colour());
        }
    }

    @Override
    public void draw(final T recipe, final IRecipeSlotsView slots, final GuiGraphics graphics, final double mouseX,
                     final double mouseY) {
        for (final RecipeView.Sprite sprite : pageOf(recipe).sprites()) {
            graphics.blitSprite(sprite.sprite(), sprite.x(), sprite.y(), sprite.width(), sprite.height());
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(final T recipe) {
        return this.kind.idOf(recipe);
    }

    private RecipeView pageOf(final T recipe) {
        return this.pages.computeIfAbsent(recipe, this.kind::view);
    }
}
