/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One recipe of a kind from the Core's kit, in EMI: laid out as the kind lays it ({@link RecipeView}), each slot one of
 * EMI's, a chance shown as EMI shows chances, a catalyst marked as EMI marks them.
 *
 * @param <T> what one recipe is
 */
final class DeclaredEmiRecipe<T> extends BasicEmiRecipe {

    private final RecipeViewKind<T> kind;
    private final T recipe;
    private final List<EmiIngredient> laid = new ArrayList<>();

    /* A recipe's arrow fills in its time: a tick is fifty milliseconds. */
    private static final int MILLIS_PER_TICK = 50;

    DeclaredEmiRecipe(final EmiRecipeCategory category, final RecipeViewKind<T> kind, final T recipe,
                      final RecipeViewKind.Size size) {
        super(category, kind.idOf(recipe), size.width(), size.height());
        this.kind = kind;
        this.recipe = recipe;
        // What goes in and comes out is the recipe's own, the same however much the player knows.
        for (final RecipeView.Slot slot : kind.view(recipe).slots()) {
            final EmiIngredient held = ingredientOf(slot);
            this.laid.add(held);
            switch (slot.role()) {
                case INPUT -> inputs.add(held);
                case CATALYST -> catalysts.add(held);
                case OUTPUT -> {
                    for (final EmiStack made : held.getEmiStacks()) {
                        outputs.add(slot.chance() < 1.0f ? made.copy().setChance(slot.chance()) : made);
                    }
                }
            }
        }
    }

    @Override
    public void addWidgets(final WidgetHolder widgets) {
        // Laid out as it is now, each time it is shown: a padlock goes as soon as its knowledge is learnt.
        final RecipeView page = this.kind.view(this.recipe);
        for (int i = 0; i < page.slots().size(); i++) {
            final RecipeView.Slot slot = page.slots().get(i);
            EmiIngredient held = this.laid.get(i);
            // Every stack an output slot may show carries its chance, one alone or any of several.
            if (slot.role() == RecipeView.Role.OUTPUT && slot.chance() < 1.0f) {
                final List<EmiStack> chanced = new ArrayList<>();
                for (final EmiStack made : held.getEmiStacks()) {
                    chanced.add(made.copy().setChance(slot.chance()));
                }
                held = EmiIngredient.of(chanced);
            }
            final SlotWidget widget = widgets.addSlot(held, slot.x(), slot.y());
            if (slot.role() == RecipeView.Role.OUTPUT) {
                widget.recipeContext(this);
            } else if (slot.role() == RecipeView.Role.CATALYST) {
                widget.catalyst(true);
            }
            for (final Component line : slot.tooltip()) {
                widget.appendTooltip(line);
            }
        }
        page.arrow().ifPresent(arrow -> {
            if (arrow.ticks() > 0) {
                // Widened first: a long recipe time in ticks times 50 would wrap an int.
                widgets.addFillingArrow(arrow.x(), arrow.y(),
                        (int) Math.min((long) arrow.ticks() * MILLIS_PER_TICK, Integer.MAX_VALUE));
            } else {
                widgets.addTexture(EmiTexture.EMPTY_ARROW, arrow.x(), arrow.y());
            }
        });
        for (final RecipeView.Line line : page.lines()) {
            widgets.addText(line.text(), line.x(), line.y(), line.colour(), false);
        }
        for (final RecipeView.Sprite sprite : page.sprites()) {
            widgets.addDrawable(sprite.x(), sprite.y(), sprite.width(), sprite.height(),
                    (graphics, mouseX, mouseY, delta) -> graphics.blitSprite(sprite.sprite(), 0, 0, sprite.width(),
                            sprite.height()));
        }
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return this.recipe instanceof RecipeHolder<?> holder ? holder : null;
    }

    /* What a slot holds as EMI holds it: any of its items or fluids, with their amounts. */
    private static EmiIngredient ingredientOf(final RecipeView.Slot slot) {
        final List<EmiStack> stacks = new ArrayList<>();
        for (final ItemStack item : slot.items()) {
            stacks.add(EmiStack.of(item));
        }
        for (final FluidStack fluid : slot.fluids()) {
            stacks.add(NeoForgeEmiStack.of(fluid));
        }
        return EmiIngredient.of(stacks);
    }
}
