/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.client.recipeview.MultiblockCard;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The multiblocks in REI, as in JEI and EMI: each one's preview, which a drag turns, its materials and its choice
 * beside it, and its terminal.
 */
final class MultiblockReiCategory implements DisplayCategory<MultiblockReiDisplay> {

    /* REI's frame round a recipe, and a slot's item a pixel inside its frame. */
    private static final int PADDING = 4;
    private static final int SLOT_INSET = 1;

    @Override
    public CategoryIdentifier<? extends MultiblockReiDisplay> getCategoryIdentifier() {
        return MultiblockReiDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return GameText.component(MultiblockViewTexts.CATEGORY);
    }

    @Override
    public Renderer getIcon() {
        final List<ItemStack> tools = MultiblockShows.tools();
        return EntryStacks.of(tools.isEmpty() ? new ItemStack(Items.BRICKS) : tools.getFirst());
    }

    @Override
    public int getDisplayWidth(final MultiblockReiDisplay display) {
        return MultiblockViewLayout.WIDTH + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return MultiblockViewLayout.HEIGHT + 2 * PADDING;
    }

    @Override
    public List<Widget> setupDisplay(final MultiblockReiDisplay display, final Rectangle bounds) {
        final int x0 = bounds.x + PADDING;
        final int y0 = bounds.y + PADDING;
        final List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(new CardWidget(new MultiblockCard(display.entry(), true), new Rectangle(x0, y0,
                MultiblockViewLayout.WIDTH, MultiblockViewLayout.HEIGHT)));
        for (int index = 0; index < display.materials().size(); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.material(index);
            final Slot slot = Widgets.createSlot(new Point(x0 + at.x() + SLOT_INSET, y0 + at.y() + SLOT_INSET))
                    .entries(display.materials().get(index)).markInput();
            widgets.add(display.entry().materials().get(index).terminal() ? Widgets.withTooltip(slot,
                    GameText.component(MultiblockViewTexts.TERMINAL)) : slot);
        }
        for (int index = 0; index < display.choices().size(); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.choice(index);
            widgets.add(Widgets.createSlot(new Point(x0 + at.x() + SLOT_INSET, y0 + at.y() + SLOT_INSET))
                    .entries(display.choices().get(index)).markInput());
        }
        return widgets;
    }

    /* The card drawn at its place in the page, under the slots, which takes the mouse in its own places. */
    private static final class CardWidget extends WidgetWithBounds {

        private final MultiblockCard card;
        private final Rectangle bounds;

        private CardWidget(final MultiblockCard card, final Rectangle bounds) {
            this.card = card;
            this.bounds = bounds;
        }

        @Override
        public Rectangle getBounds() {
            return this.bounds;
        }

        @Override
        public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float delta) {
            graphics.pose().pushPose();
            graphics.pose().translate(this.bounds.x, this.bounds.y, 0);
            this.card.draw(graphics, mouseX - this.bounds.x, mouseY - this.bounds.y);
            graphics.pose().popPose();
            if (containsMouse(mouseX, mouseY)) {
                final List<Component> lines = this.card.tooltip(mouseX - this.bounds.x, mouseY - this.bounds.y);
                if (!lines.isEmpty()) {
                    Tooltip.create(new Point(mouseX, mouseY), lines).queue();
                }
            }
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            return containsMouse(mouseX, mouseY)
                    && this.card.mouseClicked(mouseX - this.bounds.x, mouseY - this.bounds.y, button);
        }

        @Override
        public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                    final double dragY) {
            return containsMouse(mouseX, mouseY)
                    && this.card.mouseDragged(mouseX - this.bounds.x, mouseY - this.bounds.y, dragX, dragY);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }
    }
}
