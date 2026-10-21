/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.gui.layout.ToolHudLayout;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * The bars on the icon of an item that holds something, over the game's own durability bar when it has one: a bar for
 * each thing it holds, the energy, the fluid (a fuel when it burns it), the gas and the heat, in their colours; and in
 * the icon's top left corner, the first letter of the mode it is in.
 */
final class ToolIconBars implements IItemDecorator {

    /* The letter of the mode is written this small, to fit its square. */
    private static final float LETTER = 0.625F;
    /* Above the item, as the game draws its durability bar. */
    private static final float ABOVE_ITEM = 200.0F;
    private static final ToolIconBars INSTANCE = new ToolIconBars();

    private ToolIconBars() {
    }

    /** Gives every item of every mod's content that holds something or has modes its bars. */
    static void onRegister(final RegisterItemDecorationsEvent event) {
        for (final ModContent content : ModContent.all()) {
            for (final ItemEntry<?> entry : content.declaredItems()) {
                if (!entry.state().isNothing() || entry.tool().flatMap(spec -> spec.heat()).isPresent()) {
                    event.register(entry.get(), INSTANCE);
                }
            }
        }
    }

    @Override
    public boolean render(final GuiGraphics g, final Font font, final ItemStack stack, final int x, final int y) {
        final List<ToolReadings.Reading> readings = ToolReadings.held(stack);
        final Optional<ItemMode> mode = ItemStates.mode(stack);
        if (readings.isEmpty() && mode.isEmpty()) {
            return false;
        }
        final ToolHudPalette.Colours colours = ToolHudPalette.get();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, ABOVE_ITEM);
        int index = stack.isBarVisible() ? 1 : 0;
        for (final ToolReadings.Reading reading : readings) {
            if (index >= ToolHudLayout.MOST_ICON_BARS) {
                break;
            }
            final int row = y + ToolHudLayout.iconBarRow(index++);
            final int left = x + ToolHudLayout.ICON_BAR_LEFT;
            g.fill(left, row, left + ToolHudLayout.ICON_BAR_WIDTH, row + 2, colours.barGround());
            final int fill = ToolHudLayout.iconFill(reading.fraction());
            if (fill > 0) {
                g.fill(left, row, left + fill, row + 1, reading.colour());
            }
        }
        mode.ifPresent(current -> {
            final String name = GameText.component(current.name().text()).getString();
            if (!name.isEmpty()) {
                g.fill(x, y, x + ToolHudLayout.LETTER_BOX, y + ToolHudLayout.LETTER_BOX, colours.letterBox());
                Draw.textScaled(g, Minecraft.getInstance().font, name.substring(0, 1).toUpperCase(Locale.ROOT), x + 1,
                        y, colours.pointedText(), LETTER);
            }
        });
        g.pose().popPose();
        return false;
    }
}
