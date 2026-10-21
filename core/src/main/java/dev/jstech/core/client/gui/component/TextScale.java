/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.gui.AccessibilitySettings;
import dev.jstech.core.gui.TextSizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The scale the text of the series' screens is drawn at on this game, as large as the player wants it apart from the
 * GUI scale: the GUI scale's whole screen pixels to a font pixel and one more each step the player chose
 * ({@link TextSizes}). A screen whose boxes are measured from its text grows with it; the icons, frames and the game's
 * own widgets keep the GUI scale.
 */
public final class TextScale {

    private TextScale() {
    }

    /** The scale to draw a text at that the screen draws at {@code scale} at the normal size. */
    public static float of(final float scale) {
        return TextSizes.scale(guiScale(), AccessibilitySettings.textSteps(), scale);
    }

    /** The scale to draw a text of the normal size at: 1 at the normal size, more at a larger one. */
    public static float factor() {
        return of(1f);
    }

    /** Whether the player wants the text larger than the normal. */
    public static boolean larger() {
        return AccessibilitySettings.textSteps() > 0;
    }

    /**
     * Draws {@code text} through {@code skin} at {@code scale}, which {@link #of} gave: at 1, just as the skin draws a
     * text of the normal size.
     */
    public static void draw(final ISkin skin, final GuiGraphics g, final Font font, final String text, final int x,
                            final int y, final int colour, final float scale) {
        if (scale == 1f) {
            skin.drawText(g, font, text, x, y, colour);
        } else {
            skin.drawText(g, font, text, x, y, colour, scale);
        }
    }

    /** How wide {@code text} is through {@code skin} at {@code scale}, rounded up to a whole pixel. */
    public static int width(final ISkin skin, final Font font, final String text, final float scale) {
        return (int) Math.ceil(skin.textWidth(font, text) * scale);
    }

    private static int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }
}
