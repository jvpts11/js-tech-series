/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.gui.tooltip.TooltipContents;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What an item holds, drawn in the slots of the game's bundle tooltip and in the middle of the tooltip: each kind with
 * its whole count, a count of a thousand or more in thousands, and the last slot saying how many kinds more.
 */
final class ClientTooltipContents implements ClientTooltipComponent {

    private final TooltipContents contents;
    private final List<ItemStack> shown;

    /** The game's own bundle tooltip's pictures and sizes. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace(
            "container/bundle/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/bundle/slot");
    private static final int SLOT_WIDE = 18;
    private static final int SLOT_TALL = 20;
    private static final int BORDER = 1;
    private static final int UNDER = 4;
    /** From a thousand on, a count is written in thousands, to fit its slot. */
    private static final int THOUSAND = 1_000;
    /** Where an item's count's text sits in its slot, as the game puts it; the "+n" lower in the slot. */
    private static final int TEXT_DOWN = 6;

    ClientTooltipContents(final TooltipContents contents) {
        this.contents = contents;
        this.shown = contents.shown();
    }

    @Override
    public int getHeight() {
        return rows() * SLOT_TALL + 2 * BORDER + UNDER;
    }

    @Override
    public int getWidth(final Font font) {
        return contents.columns() * SLOT_WIDE + 2 * BORDER;
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        final int wide = getWidth(font);
        final int left = TooltipPieces.centred(x, wide);
        g.blitSprite(BACKGROUND, left, y, wide, rows() * SLOT_TALL + 2 * BORDER);
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        // Every slot of the grid, the ones after what it holds empty, as the game's bundle draws them.
        for (int i = 0; i < rows() * contents.columns(); i++) {
            final int sx = left + BORDER + i % contents.columns() * SLOT_WIDE;
            final int sy = y + BORDER + i / contents.columns() * SLOT_TALL;
            g.blitSprite(SLOT, sx, sy, 0, SLOT_WIDE, SLOT_TALL);
            if (i < shown.size()) {
                final ItemStack stack = shown.get(i);
                g.renderItem(stack, sx + 1, sy + 1, i);
                g.renderItemDecorations(font, stack, sx + 1, sy + 1, count(format, stack.getCount()));
            } else if (i == shown.size() && contents.more() > 0) {
                final Component more = GameText.component(TooltipTexts.MORE_KINDS.with(contents.more()));
                Draw.text(g, font, more, sx + (SLOT_WIDE - font.width(more)) / 2, sy + TEXT_DOWN,
                        TooltipPalette.get().more(), true);
            }
        }
    }

    private int rows() {
        final int slots = shown.size() + (contents.more() > 0 ? 1 : 0);
        return Math.max(1, (slots + contents.columns() - 1) / contents.columns());
    }

    /* A count as its slot shows it: none for one, in thousands from a thousand on. */
    private static String count(final UnitFormatter format, final int count) {
        if (count == 1) {
            return "";
        }
        if (count < THOUSAND) {
            return Integer.toString(count);
        }
        final UnitFormatter.Scaled scaled = format.si(count);
        return scaled.number() + scaled.prefix();
    }
}
