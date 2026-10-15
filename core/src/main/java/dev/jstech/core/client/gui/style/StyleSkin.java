/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A style worn as a skin: every component draws through it as it draws through a computer's system, so the same button
 * is the game's own in the Game style, the series' dark one in J's Tech, and a phosphor green one in a Vintage era's.
 * Its pieces, colours, font, shadow and finish are the style's.
 */
public final class StyleSkin implements ISkin {

    private final Style style;

    private static final ResourceLocation DEFAULT_FONT = ResourceLocation.withDefaultNamespace("default");

    public StyleSkin(final Style style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    /** The style worn. */
    public Style style() {
        return style;
    }

    /** The colour of {@code role} in the style. */
    public int colour(final StyleRole role) {
        return style.colour(role);
    }

    /** Draws {@code piece} of the style over the rectangle. */
    public void piece(final GuiGraphics g, final StylePiece piece, final int x, final int y, final int w,
                      final int h) {
        StylePainter.draw(g, style, style.piece(piece), x, y, w, h);
    }

    @Override
    public int text() {
        return style.colour(StyleRole.TEXT);
    }

    @Override
    public int dim() {
        return style.colour(StyleRole.DIM);
    }

    @Override
    public int accent() {
        return style.colour(StyleRole.ACCENT);
    }

    @Override
    public int edge() {
        return style.colour(StyleRole.LINE);
    }

    @Override
    public int windowBg() {
        return style.colour(StyleRole.SCREEN);
    }

    @Override
    public int panelBg() {
        return style.colour(StyleRole.PANEL);
    }

    @Override
    public int fieldBg() {
        return style.colour(StyleRole.SLOT_BG);
    }

    @Override
    public int listHover() {
        return style.colour(StyleRole.HOVER);
    }

    @Override
    public int listRowText(final boolean selected) {
        return style.colour(selected ? StyleRole.TAB_LABEL_ON : StyleRole.TEXT);
    }

    @Override
    public void panel(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.PANEL, x, y, w, h);
    }

    @Override
    public void button(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final String label, final boolean hovered, final boolean pressed, final boolean primary) {
        piece(g, primary ? StylePiece.BUTTON_PRIMARY : hovered ? StylePiece.BUTTON_HOVER : StylePiece.BUTTON, x, y, w,
                h);
        sheen(g, x, y, w, h);
        label(g, font, label, x, y, w, h, pressed, StyleRole.BUTTON_TEXT);
    }

    @Override
    public void button(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final String label, final boolean hovered, final boolean pressed, final boolean primary,
                       final boolean enabled) {
        if (enabled) {
            button(g, font, x, y, w, h, label, hovered, pressed, primary);
            return;
        }
        piece(g, StylePiece.BUTTON_DISABLED, x, y, w, h);
        label(g, font, label, x, y, w, h, false, StyleRole.BUTTON_TEXT_DISABLED);
    }

    @Override
    public void field(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean focused) {
        piece(g, focused ? StylePiece.FIELD_FOCUSED : StylePiece.FIELD, x, y, w, h);
    }

    @Override
    public void tab(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                    final String label, final boolean active) {
        piece(g, active ? StylePiece.TAB_ACTIVE : StylePiece.TAB, x, y, w, h);
        if (active) {
            sheen(g, x, y, w, h);
        }
        final int tw = textWidth(font, label);
        drawText(g, font, label, x + (w - tw) / 2, y + (h - 8) / 2,
                style.colour(active ? StyleRole.TAB_LABEL_ON : StyleRole.DIM));
    }

    @Override
    public void listRow(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean hovered,
                        final boolean selected) {
        if (selected) {
            piece(g, StylePiece.LIST_SELECTED, x, y, w, h);
        } else if (hovered) {
            piece(g, StylePiece.LIST_HOVER, x, y, w, h);
        }
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.SCROLL_THUMB, x, y, w, h);
    }

    @Override
    public void statusBar(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.STATUS_BAR, x, y, w, h);
    }

    @Override
    public void windowFrame(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.POPUP, x, y, w, h);
    }

    @Override
    public void window(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.WINDOW, x, y, w, h);
        final int scanline = style.colour(StyleRole.SCANLINE);
        if (style.finish().scanlines() && (scanline >>> 24) != 0) {
            for (int ly = y + 1; ly < y + h; ly += 2) {
                g.fill(x, ly, x + w, ly + 1, scanline);
            }
        }
    }

    @Override
    public void slot(final GuiGraphics g, final int x, final int y) {
        piece(g, StylePiece.SLOT, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
    }

    @Override
    public void checkbox(final GuiGraphics g, final Font font, final int x, final int y, final int size,
                         final boolean checked, final boolean hovered) {
        final StylePiece piece = checked ? hovered ? StylePiece.CHECKBOX_ON_HOVER : StylePiece.CHECKBOX_ON
                : hovered ? StylePiece.CHECKBOX_HOVER : StylePiece.CHECKBOX;
        piece(g, piece, x, y, size, size);
    }

    @Override
    public void progress(final GuiGraphics g, final int x, final int y, final int w, final int h,
                         final double fraction) {
        piece(g, StylePiece.PROGRESS_TRACK, x, y, w, h);
        final int filled = (int) Math.round((w - 2) * Math.max(0.0, Math.min(1.0, fraction)));
        if (filled > 0) {
            piece(g, StylePiece.PROGRESS_FILL, x + 1, y + 1, filled, h - 2);
            final int glow = style.colour(StyleRole.GLOW);
            if (style.finish().glowAccent() && (glow >>> 24) != 0) {
                g.fill(x + 1, y, x + 1 + filled, y + 1, glow);
                g.fill(x + 1, y + h - 1, x + 1 + filled, y + h, glow);
            }
        }
    }

    @Override
    public void separator(final GuiGraphics g, final int x, final int y, final int w) {
        final StyleSprite sprite = style.piece(StylePiece.SEPARATOR);
        StylePainter.draw(g, style, sprite, x, y, w, StylePainter.naturalHeight(sprite));
    }

    @Override
    public void tooltip(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.TOOLTIP, x, y, w, h);
    }

    /** A header strip, with the line under it in the accent where the style's finish says so. */
    public void header(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        piece(g, StylePiece.HEADER, x, y, w, h);
        sheen(g, x, y, w, h);
        if (style.finish().accentRule()) {
            g.fill(x, y + h - 1, x + w, y + h, style.colour(StyleRole.ACCENT));
        }
    }

    @Override
    public void drawText(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                         final int colour) {
        if (plain()) {
            Draw.text(g, font, text, x, y, colour);
        } else {
            Draw.text(g, font, styled(text), x, y, colour, style.textShadow());
        }
    }

    @Override
    public void drawText(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                         final int colour, final float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        drawText(g, font, text, 0, 0, colour);
        g.pose().popPose();
    }

    @Override
    public int textWidth(final Font font, final String text) {
        return plain() ? font.width(text) : font.width(styled(text));
    }

    /* Whether the style's text is the game's font with no shadow, which is drawn and measured as plain text. */
    private boolean plain() {
        return DEFAULT_FONT.equals(style.font()) && !style.textShadow();
    }

    private Component styled(final String text) {
        return Component.literal(text).withStyle(s -> s.withFont(style.font()));
    }

    /* The label centred on its button, a pixel lower while it is pressed. */
    private void label(final GuiGraphics g, final Font font, final String label, final int x, final int y,
                       final int w, final int h, final boolean pressed, final StyleRole colour) {
        if (label.isEmpty()) {
            return;
        }
        final int tw = textWidth(font, label);
        drawText(g, font, label, x + (w - tw) / 2, y + (h - 8) / 2 + (pressed ? 1 : 0), style.colour(colour));
    }

    /* The lighter band over the upper half of a glass surface, where the style's finish has it. */
    private void sheen(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final int sheen = style.colour(StyleRole.SHEEN);
        if (style.finish().glassBands() && (sheen >>> 24) != 0) {
            g.fill(x, y, x + w, y + h / 2, sheen);
        }
    }
}
