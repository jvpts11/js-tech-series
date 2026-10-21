/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Arrow;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A kind of recipe from the Core's kit, in REI: under the kind's title, each page laid out by the kind
 * ({@link RecipeView}) and drawn with REI's own slots, arrow and labels, within REI's frame of a recipe.
 *
 * @param <T> what one recipe is
 */
final class DeclaredReiCategory<T> implements DisplayCategory<DeclaredReiDisplay<T>> {

    private final RecipeViewKind<T> kind;
    private final RecipeViewKind.Size size;

    /* REI's frame round a recipe, and a slot's item a pixel inside its frame. */
    private static final int PADDING = 4;
    private static final int SLOT_INSET = 1;

    DeclaredReiCategory(final RecipeViewKind<T> kind, final RecipeViewKind.Size size) {
        this.kind = kind;
        this.size = size;
    }

    @Override
    public CategoryIdentifier<? extends DeclaredReiDisplay<T>> getCategoryIdentifier() {
        return CategoryIdentifier.of(this.kind.id());
    }

    @Override
    public Component getTitle() {
        return GameText.component(this.kind.title());
    }

    @Override
    public Renderer getIcon() {
        final ItemStack shown = this.kind.icon();
        return shown.isEmpty() ? EntryStacks.of(ItemStack.EMPTY) : EntryStacks.of(shown);
    }

    @Override
    public int getDisplayWidth(final DeclaredReiDisplay<T> display) {
        return this.size.width() + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return this.size.height() + 2 * PADDING;
    }

    @Override
    public List<Widget> setupDisplay(final DeclaredReiDisplay<T> display, final Rectangle bounds) {
        final RecipeView page = display.page();
        final int x0 = bounds.x + PADDING;
        final int y0 = bounds.y + PADDING;
        final List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        for (final RecipeView.Sprite sprite : page.sprites()) {
            widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> graphics.blitSprite(
                    sprite.sprite(), x0 + sprite.x(), y0 + sprite.y(), sprite.width(), sprite.height())));
        }
        page.arrow().ifPresent(arrow -> {
            final Arrow made = Widgets.createArrow(new Point(x0 + arrow.x(), y0 + arrow.y()));
            widgets.add(arrow.ticks() > 0 ? made.animationDurationTicks(arrow.ticks()) : made);
        });
        for (final RecipeView.Slot slot : page.slots()) {
            final Slot made = Widgets.createSlot(new Point(x0 + slot.x() + SLOT_INSET, y0 + slot.y() + SLOT_INSET))
                    .entries(DeclaredReiDisplay.ingredientOf(slot));
            widgets.add(slot.role() == RecipeView.Role.OUTPUT ? made.markOutput() : made.markInput());
        }
        for (final RecipeView.Line line : page.lines()) {
            widgets.add(Widgets.createLabel(new Point(x0 + line.x(), y0 + line.y()), line.text()).leftAligned()
                    .noShadow().color(line.colour(), line.colour()));
        }
        return widgets;
    }
}
