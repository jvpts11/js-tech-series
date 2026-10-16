/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * How the effects editor and the card of a new effect are drawn, in the game's own look: its grey window with the
 * white and dark bevels, its words, the debug lines in the corner as the game writes its own, and the marks in pink
 * on the world where an effect starts.
 */
@PaletteHolder
public final class EffectCards {

    /** The cards' colours, the game's own. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "effects_editor", new Colours(
            0xFF << 24 | 0xC6C6C6, 0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0x555555, 0xFF << 24,
            0xFF << 24 | 0x404040, 0xFF << 24 | 0x707070, 0xFF << 24 | 0x8B8B8B, 0x90 << 24 | 0x505050,
            0xFF << 24 | 0xE0E0E0, 0xFF << 24 | 0xFF55FF, 0xFF << 24 | 0xFFFFFF));
    private static final int LINE = 9;
    private static final int DOT = 2;

    private EffectCards() {
    }

    /*
     * A window as the game draws its own: the black outline stepping in at each corner, a white bevel two pixels deep
     * on the top and left, a dark one on the bottom and right, and the grey ground.
     */
    static void window(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        final Colours c = PALETTE.get();
        g.fill(x0 + 2, y0 + 1, x1 - 2, y1 - 1, c.ground());
        g.fill(x0 + 1, y0 + 2, x1 - 1, y1 - 2, c.ground());
        g.fill(x0 + 2, y0, x1 - 2, y0 + 1, c.outline());
        g.fill(x0 + 2, y1 - 1, x1 - 2, y1, c.outline());
        g.fill(x0, y0 + 2, x0 + 1, y1 - 2, c.outline());
        g.fill(x1 - 1, y0 + 2, x1, y1 - 2, c.outline());
        final int[][] corners = {{x0 + 1, y0 + 1}, {x1 - 2, y0 + 1}, {x0 + 1, y1 - 2}, {x1 - 2, y1 - 2}};
        for (final int[] corner : corners) {
            g.fill(corner[0], corner[1], corner[0] + 1, corner[1] + 1, c.outline());
        }
        g.fill(x0 + 2, y0 + 1, x1 - 3, y0 + 3, c.light());
        g.fill(x0 + 1, y0 + 2, x0 + 3, y1 - 3, c.light());
        g.fill(x0 + 3, y1 - 3, x1 - 2, y1 - 1, c.dark());
        g.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 2, c.dark());
    }

    /** The debug lines in the screen's corner, each on its grey ground, as the game writes its own. */
    static void debugLines(final GuiGraphics g, final Font font, final List<Component> lines) {
        final Colours c = PALETTE.get();
        for (int i = 0; i < lines.size(); i++) {
            final int y = 2 + i * LINE;
            g.fill(1, y - 1, 3 + font.width(lines.get(i)), y + LINE - 1, c.debugGround());
            Draw.text(g, font, lines.get(i), 2, y, c.debugText(), c.debugGround());
        }
    }

    /** A dotted line in pink from {@code (x0, y0)} to {@code (x1, y1)}, on the screen. */
    static void dotted(final GuiGraphics g, final float x0, final float y0, final float x1, final float y1) {
        final int colour = PALETTE.get().marker();
        final double length = Math.hypot(x1 - x0, y1 - y0);
        for (double at = 0; at <= length; at += DOT * 2) {
            final int x = (int) Math.round(x0 + (x1 - x0) * at / Math.max(1, length));
            final int y = (int) Math.round(y0 + (y1 - y0) * at / Math.max(1, length));
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    /** Where {@code at} is on a screen {@code width} by {@code height}, in its units; none behind the eye. */
    static float[] onScreen(final Vec3 at, final int width, final int height) {
        final float[] share = ScreenEffects.toScreen(at);
        return share == null ? null : new float[] {share[0] * width, share[1] * height};
    }

    /**
     * The cards' colours.
     *
     * @param ground      a window's grey
     * @param light       its white bevel
     * @param dark        its dark bevel
     * @param outline     its black outline
     * @param ink         the words, as the game writes a window's title
     * @param soft        the softer words under a title
     * @param chosen      the ground of the chosen row
     * @param debugGround the ground of a debug line
     * @param debugText   a debug line's words
     * @param marker      the marks on the world
     * @param white       the words of the chosen row
     */
    public record Colours(int ground, int light, int dark, int outline, int ink, int soft, int chosen,
                          int debugGround, int debugText, int marker, int white) {
    }
}
