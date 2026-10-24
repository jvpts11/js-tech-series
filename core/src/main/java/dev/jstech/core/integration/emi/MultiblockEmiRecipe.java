/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.jstech.core.client.recipeview.MultiblockCard;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * One multiblock in EMI, laid out as JEI lays it out: its materials and its choice as what it takes, its terminal as
 * what it makes. EMI passes no drags to a recipe, so a click turns its preview.
 */
final class MultiblockEmiRecipe extends BasicEmiRecipe {

    private final MultiblockEntry entry;
    private final List<EmiIngredient> materials = new ArrayList<>();
    private final List<EmiIngredient> choices = new ArrayList<>();

    MultiblockEmiRecipe(final EmiRecipeCategory category, final MultiblockEntry entry) {
        super(category, entry.id(), MultiblockViewLayout.WIDTH, MultiblockViewLayout.HEIGHT);
        this.entry = entry;
        for (final MultiblockEntry.Material material : entry.materials()) {
            this.materials.add(EmiIngredient.of(material.stacks().stream().map(EmiStack::of).toList(),
                    material.count()));
        }
        entry.choice().ifPresent(choice -> {
            final List<ItemStack> stacks = choice.stacks();
            for (int index = 0; index < Math.min(stacks.size(), MultiblockViewLayout.MOST_CHOICES); index++) {
                // The last slot takes the rest of the choice in turn, so no block that will do goes unshown.
                final List<ItemStack> held = index == MultiblockViewLayout.MOST_CHOICES - 1
                        ? stacks.subList(index, stacks.size()) : List.of(stacks.get(index));
                this.choices.add(EmiIngredient.of(held.stream().map(EmiStack::of).toList()));
            }
        });
        inputs.addAll(this.materials);
        inputs.addAll(this.choices);
        if (!entry.terminal().isEmpty()) {
            outputs.add(EmiStack.of(entry.terminal()));
        }
    }

    @Override
    public void addWidgets(final WidgetHolder widgets) {
        widgets.add(new CardWidget(new MultiblockCard(this.entry, false)));
        for (int index = 0; index < this.materials.size(); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.material(index);
            final SlotWidget slot = widgets.addSlot(this.materials.get(index), at.x(), at.y());
            if (this.entry.materials().get(index).terminal()) {
                slot.appendTooltip(GameText.component(MultiblockViewTexts.TERMINAL));
            }
        }
        for (int index = 0; index < this.choices.size(); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.choice(index);
            widgets.addSlot(this.choices.get(index), at.x(), at.y());
        }
    }

    /* The card drawn over the whole recipe, under its slots, which takes its clicks. */
    private static final class CardWidget extends Widget {

        private final MultiblockCard card;

        private static final Bounds BOUNDS = new Bounds(0, 0, MultiblockViewLayout.WIDTH,
                MultiblockViewLayout.HEIGHT);

        private CardWidget(final MultiblockCard card) {
            this.card = card;
        }

        @Override
        public Bounds getBounds() {
            return BOUNDS;
        }

        @Override
        public void render(final GuiGraphics draw, final int mouseX, final int mouseY, final float delta) {
            this.card.draw(draw, mouseX, mouseY);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(final int mouseX, final int mouseY) {
            return this.card.tooltip(mouseX, mouseY).stream().map(EmiTooltipComponents::of).toList();
        }

        @Override
        public boolean mouseClicked(final int mouseX, final int mouseY, final int button) {
            return this.card.mouseClicked(mouseX, mouseY, button);
        }
    }
}
