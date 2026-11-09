/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Quince's crash pages, drawn over the desktop the system went down on (computers/docs/design/quince.md): the bomb of
 * the classic systems, whose Restart loses whatever was open, and the grey page of Quince OS X and quinceOS, which says
 * it in four languages at once.
 *
 * <p>Everything here is laid out on the monitor's glass from its top left corner.
 */
@TextHolder
@PaletteHolder
public final class QuincePages {

    private static final TextKey SORRY = TextKey.of("jsc.quince.crash.sorry", "Sorry, a system error occurred.");
    private static final TextKey RESTART = TextKey.of("jsc.quince.crash.restart", "Restart");
    private static final TextKey RESUME = TextKey.of("jsc.quince.crash.resume", "Resume");

    /*
     * The grey page's words, a paragraph a line. The real one printed them in English, French, German and Spanish
     * whatever the machine was set to, so they are the page's own picture and not the player's words: they are kept
     * as a file beside the textures and read as they are in every language.
     */
    private static final ResourceLocation RESTART_WORDS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "quince/restart_page.txt");

    /* The bomb's dialog on the Quince System and on Quince OS 9, as the mocks lay them on the glass. */
    private static final int BOMB_X0 = 82;
    private static final int BOMB_Y0 = 90;
    private static final int BOMB_X1 = 302;
    private static final int BOMB_Y1 = 158;
    private static final int BOMB9_X0 = 76;
    private static final int BOMB9_Y0 = 84;
    private static final int BOMB9_X1 = 306;
    private static final int BOMB9_Y1 = 156;
    private static final int SIGN_INSET = 14;
    private static final int CLASSIC_BUTTON_H = 13;
    private static final int PLATINUM_BUTTON_H = 14;
    /* The grey page's sheet, the power mark's place on it, and its rows of words. */
    private static final int SHEET_X0 = 52;
    private static final int SHEET_Y0 = 34;
    private static final int SHEET_X1 = 332;
    private static final int SHEET_Y1 = 222;
    private static final int SHEET_ROUND = 8;
    private static final int POWER_Y = 22;
    private static final int POWER_RADIUS = 10;
    private static final int WORDS_Y = 40;
    private static final int WORDS_MARGIN = 14;
    private static final int ROW = 10;
    private static final int PARAGRAPH_GAP = 5;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "quince/crash",
            new Colours(0x8C000000, 0xEB3C3C40, 0xFFF2F2F2));

    /** The grey page's paragraphs once read, or null before the first page that needed them. */
    private static List<String> restartWords;

    private QuincePages() {
    }

    /**
     * Draws a Quince page over whatever the glass already shows, the glass's top left at the pose's origin; returns
     * where the bomb's Restart is, as {@code {x, y, w, h}} on the glass, or null for the grey page, which has none.
     */
    public static int[] draw(final GuiGraphics g, final Font font, final CrashPage page, final int glassW,
                             final int glassH) {
        return switch (page.style()) {
            case QUINCE_BOMB -> bomb(g, font, QuinceLook.ONE_BIT, page, BOMB_X0, BOMB_Y0, BOMB_X1, BOMB_Y1);
            case QUINCE_OS_9_BOMB -> bomb(g, font, QuinceLook.PLATINUM, page, BOMB9_X0, BOMB9_Y0, BOMB9_X1,
                    BOMB9_Y1);
            default -> {
                restart(g, font, glassW, glassH);
                yield null;
            }
        };
    }

    /** The desktop's colour, for a page whose desktop could not be drawn under it. */
    public static int ground(final CrashStyle style) {
        return switch (style) {
            case QUINCE_BOMB -> QuinceLook.ONE_BIT.colours().desktop();
            case QUINCE_OS_9_BOMB -> QuinceLook.PLATINUM.colours().desktop();
            default -> QuinceLook.PULP.colours().desktop();
        };
    }

    private static int[] bomb(final GuiGraphics g, final Font font, final QuinceLook look, final CrashPage page,
                              final int x0, final int y0, final int x1, final int y1) {
        final QuinceLook.Colours c = look.colours();
        QuinceArt.dialog(g, look, x0, y0, x1, y1);
        final boolean classic = look == QuinceLook.ONE_BIT;
        QuinceArt.sprite(g, QuinceArt.BOMB, x0 + SIGN_INSET, y0 + SIGN_INSET,
                classic ? c.ink() : QuinceArt.colours().grey());
        final int tx = x0 + (classic ? 42 : 40);
        int ty = y0 + (classic ? 16 : 14);
        Draw.text(g, font, GameText.resolve(SORRY), tx, ty, c.ink());
        ty += 13;
        if (!page.stopLine().isEmpty()) {
            Draw.text(g, font, page.stopLine(), tx, ty, c.ink());
            ty += 11;
        }
        Draw.text(g, font, page.stopCode(), tx, ty, c.ink());
        final String restart = GameText.resolve(RESTART);
        final int bw = classic ? 50 : 54;
        final int bh = classic ? CLASSIC_BUTTON_H : PLATINUM_BUTTON_H;
        final int bx = x1 - (classic ? 66 : 68);
        final int by = y1 - 24;
        QuinceArt.button(g, look, bx, by, bw, bh, true, false);
        Draw.textCentered(g, font, restart, bx + bw / 2, by + 3, QuinceArt.buttonInk(look, true));
        if (classic) {
            // The Resume that never worked: there, and dimmed.
            final String resume = GameText.resolve(RESUME);
            final int rx = x1 - 128;
            QuinceArt.button(g, look, rx, by, bw, bh, false, false);
            QuinceArt.dimmed(g, font, resume, rx + (bw - font.width(resume)) / 2, by + 3, c.ink(), c.button());
        }
        return new int[] {bx, by, bw, bh};
    }

    /** The grey page: the desktop dimmed, and over it the sheet with the power mark and the four languages. */
    private static void restart(final GuiGraphics g, final Font font, final int glassW, final int glassH) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, glassW, glassH, c.dim());
        QuinceArt.roundRect(g, SHEET_X0, SHEET_Y0, SHEET_X1, SHEET_Y1, SHEET_ROUND, c.sheet());
        final int cx = (SHEET_X0 + SHEET_X1) / 2;
        final int cy = SHEET_Y0 + POWER_Y;
        // The power mark: a ring open at the top, and the bar through the opening.
        for (int degrees = 0; degrees < 360; degrees += 3) {
            if (Math.abs(degrees - 90) < 40) {
                continue;
            }
            final double a = Math.toRadians(degrees);
            for (int r = POWER_RADIUS - 1; r <= POWER_RADIUS; r++) {
                final int px = (int) (cx + r * Math.cos(a));
                final int py = (int) (cy - r * Math.sin(a));
                g.fill(px, py, px + 1, py + 1, c.ink());
            }
        }
        g.fill(cx - 1, cy - 13, cx + 1, cy - 1, c.ink());
        int y = SHEET_Y0 + WORDS_Y;
        for (final String words : restartWords()) {
            for (final String line : wrap(font, words, SHEET_X1 - SHEET_X0 - 2 * WORDS_MARGIN)) {
                Draw.textCentered(g, font, line, cx, y, c.ink());
                y += ROW;
            }
            y += PARAGRAPH_GAP;
        }
    }

    /* The grey page's paragraphs, read from their file once; a pack that took the file away leaves the sheet bare. */
    private static List<String> restartWords() {
        if (restartWords == null) {
            final List<String> read = new ArrayList<>();
            Minecraft.getInstance().getResourceManager().getResource(RESTART_WORDS).ifPresent(resource -> {
                try (BufferedReader reader = resource.openAsReader()) {
                    reader.lines().filter(line -> !line.isBlank()).forEach(read::add);
                } catch (final IOException ignored) {
                    // A file that cannot be read is a sheet with no words, not a page that cannot be drawn.
                }
            });
            restartWords = List.copyOf(read);
        }
        return restartWords;
    }

    /* The words broken into lines no wider than {@code width}, at the spaces. */
    private static List<String> wrap(final Font font, final String words, final int width) {
        final List<String> lines = new ArrayList<>();
        String line = "";
        for (final String word : words.split(" ")) {
            final String probe = line.isEmpty() ? word : line + " " + word;
            if (font.width(probe) <= width || line.isEmpty()) {
                line = probe;
            } else {
                lines.add(line);
                line = word;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    /**
     * The grey page's colours.
     *
     * @param dim   the shade laid over the desktop
     * @param sheet the sheet the words are on
     * @param ink   the words and the power mark
     */
    private record Colours(int dim, int sheet, int ink) {
    }
}
