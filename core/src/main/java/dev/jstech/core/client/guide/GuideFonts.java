/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.TextSize;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * How each size of a manual's text is drawn in a style: the font, bold or not, and the scale. Running text is the
 * style's body font at full size; headings are bold; big titles and the index's letters twice the size; heads, feet
 * and captions three quarters; tables in the style's table font, which lines figures up.
 */
public final class GuideFonts {

    /** The scale of small text: three quarters, the smallest the game's font stays readable at. */
    public static final float SMALL = 0.75F;
    /** The scale of big titles. */
    public static final float BIG = 2.0F;

    private GuideFonts() {
    }

    /** The text as it is drawn at that size in that style, before any scale. */
    public static MutableComponent styled(final String text, final TextSize size, final GuideStyle style) {
        final String fontId = size == TextSize.TABLE ? style.tableFont() : style.bodyFont();
        Style look = Style.EMPTY;
        if (!fontId.isEmpty()) {
            look = look.withFont(ResourceLocation.parse(fontId));
        }
        if (bold(size)) {
            look = look.withBold(true);
        }
        return Component.literal(text).withStyle(look);
    }

    /** How much a size is scaled. */
    public static float scale(final TextSize size) {
        return switch (size) {
            case TITLE, LETTER -> BIG;
            case SMALL, SMALL_BOLD -> SMALL;
            default -> 1.0F;
        };
    }

    /** How many pixels wide the text is drawn at that size, rounded up. */
    public static int width(final Font font, final String text, final TextSize size, final GuideStyle style) {
        return (int) Math.ceil(font.width(styled(text, size, style)) * scale(size));
    }

    /** Draws the text at that size, its top left corner where given. */
    public static void draw(final GuiGraphics g, final Font font, final String text, final TextSize size,
                            final GuideStyle style, final int x, final int y, final int colour) {
        final float scale = scale(size);
        final MutableComponent styled = styled(text, size, style);
        if (scale == 1.0F) {
            Draw.text(g, font, styled, x, y, colour);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        Draw.text(g, font, styled, 0, 0, colour);
        g.pose().popPose();
    }

    private static boolean bold(final TextSize size) {
        return size == TextSize.BOLD || size == TextSize.HEADING || size == TextSize.TITLE
                || size == TextSize.SMALL_BOLD || size == TextSize.LETTER;
    }
}
