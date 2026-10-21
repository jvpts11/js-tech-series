/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipe;

import dev.jstech.core.client.gui.component.Draw;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * One recipe in the chooser's tooltip: what it makes, its icon and its name, the chosen one marked and in white, the
 * others in grey.
 *
 * @param result what the recipe makes
 * @param name   its name
 * @param chosen whether it is the one used
 */
public record RecipeChoiceRow(ItemStack result, Component name, boolean chosen) implements TooltipComponent,
        ClientTooltipComponent {

    private static final int ROW = 18;
    private static final int ICON = 20;
    private static final int TEXT_DOWN = 4;
    private static final int MARK_DOWN = 1;
    private static final int MARK_HEIGHT = 14;

    public RecipeChoiceRow {
        result = result.copy();
        Objects.requireNonNull(name, "name");
    }

    @Override
    public int getHeight() {
        return ROW;
    }

    @Override
    public int getWidth(final Font font) {
        return ICON + font.width(this.name);
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics graphics) {
        graphics.renderItem(this.result, x, y);
        if (this.chosen) {
            graphics.fill(x - 1, y + MARK_DOWN, x, y + MARK_DOWN + MARK_HEIGHT, RecipeChooserPalette.get().chosen());
        }
        Draw.text(graphics, font, this.name, x + ICON, y + TEXT_DOWN, RecipeChooserClient.rowColour(this.chosen));
    }
}
