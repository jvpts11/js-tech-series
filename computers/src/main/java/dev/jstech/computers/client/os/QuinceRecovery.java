/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.TextTable;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * quinceOS's recovery, its start-up options' own (computers/docs/design/quince.md): quinceOS Utilities on a dark
 * ground under its menu bar, a window of its three choices, Startup Disk, Reinstall quinceOS and Disk Utility, and
 * Continue. Laid out on the glass from its top left corner.
 */
@TextHolder
@PaletteHolder
public final class QuinceRecovery {

    /** What each of the three choices does: the disk the machine starts from, a fresh system, or First Aid. */
    public static final int STARTUP_DISK = 0;
    public static final int REINSTALL = 1;
    public static final int DISK_UTILITY = 2;

    private static final TextKey UTILITIES = TextKey.of("jsc.quince.recovery.utilities", "quinceOS Utilities");
    private static final TextKey STARTUP_DISK_NAME = TextKey.of("jsc.quince.recovery.startup_disk", "Startup Disk");
    private static final TextKey REINSTALL_NAME = TextKey.of("jsc.quince.recovery.reinstall", "Reinstall quinceOS");
    private static final TextKey DISK_UTILITY_NAME = TextKey.of("jsc.quince.recovery.disk_utility", "Disk Utility");
    private static final TextKey STARTUP_DISK_WHAT = TextKey.of("jsc.quince.recovery.startup_disk_what",
            "Choose the disk the computer starts from.");
    private static final TextKey REINSTALL_WHAT = TextKey.of("jsc.quince.recovery.reinstall_what",
            "Put a fresh copy of quinceOS on the disk.");
    private static final TextKey DISK_UTILITY_WHAT = TextKey.of("jsc.quince.recovery.disk_utility_what",
            "Repair or erase a disk.");
    private static final List<TextKey> NAMES = List.of(STARTUP_DISK_NAME, REINSTALL_NAME, DISK_UTILITY_NAME);
    private static final List<TextKey> WHATS = List.of(STARTUP_DISK_WHAT, REINSTALL_WHAT, DISK_UTILITY_WHAT);
    private static final TextKey CONTINUE = TextKey.of("jsc.quince.recovery.continue", "Continue");
    private static final TextKey CHECKING = TextKey.of("jsc.quince.recovery.checking",
            "First Aid is checking the startup disk.");
    /* The window, as the mock lays it on the glass. */
    private static final int X0 = 72;
    private static final int Y0 = 42;
    private static final int X1 = 312;
    private static final int Y1 = 210;
    private static final int ROW_Y = 72;
    private static final int ROW_H = 34;
    /* A choice's words from the window's left, past its tile, and a line of them. */
    private static final int TEXT_X = 40;
    private static final int LINE_H = 9;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "quince/recovery",
            new Colours(0xFF2A2A34, 0xFF3A8AE0, 0xFF6A4AC0, 0xFF7A7A86, 0xFFE2DCEE, 0xFFFFFFFF));

    private QuinceRecovery() {
    }

    /** Draws the page with the choice {@code picked} lit, and the line First Aid leaves when {@code checking}. */
    public static void draw(final GuiGraphics g, final Font font, final int w, final int h, final int picked,
                            final boolean checking) {
        final QuinceLook look = QuinceLook.QUINCE_OS;
        final QuinceLook.Colours c = look.colours();
        final Colours own = PALETTE.get();
        g.fill(0, 0, w, h, own.ground());
        QuinceBars.menuBar(g, font, look, 0, 0, w, GameText.resolve(UTILITIES), null, "quince_os_x",
                List.of("Edit", "Utilities", "Window"), QuinceBars.clock(), -1);
        QuinceArt.window(g, font, look, X0, Y0, X1, Y1, "", true);
        Draw.textCentered(g, font, Component.literal(GameText.resolve(UTILITIES)).withStyle(ChatFormatting.BOLD),
                (X0 + X1) / 2, Y0 + 18, c.ink());
        for (int i = 0; i < NAMES.size(); i++) {
            final int ry = ROW_Y + i * ROW_H;
            if (i == picked) {
                QuinceArt.roundRect(g, X0 + 8, ry - 3, X1 - 8, ry + ROW_H - 5, 4, own.picked());
            }
            final int tile = i == 0 ? own.disk() : i == 1 ? own.system() : own.utility();
            QuinceArt.roundRect(g, X0 + 14, ry, X0 + 32, ry + 18, 3, tile);
            if (i == REINSTALL) {
                QuinceArt.fruit(g, look, X0 + 18, ry + 3, false);
            } else {
                disk(g, X0 + 14, ry, tile);
            }
            Draw.text(g, font, Component.literal(GameText.resolve(NAMES.get(i))).withStyle(ChatFormatting.BOLD),
                    X0 + TEXT_X, ry, c.ink());
            // The line under the name breaks within the window, as many lines as its row holds.
            int ly = ry + 10;
            for (final String line : TextTable.wrap(GameText.resolve(WHATS.get(i)), X1 - 8 - (X0 + TEXT_X),
                    font::width)) {
                if (ly + LINE_H > ry + ROW_H - 4) {
                    break;
                }
                Draw.text(g, font, line, X0 + TEXT_X, ly, c.dim());
                ly += LINE_H;
            }
        }
        if (checking) {
            Draw.text(g, font, GameText.resolve(CHECKING), X0 + 14, Y1 - 30, c.ink());
        }
        final int[] go = continueButton(font);
        QuinceArt.button(g, look, go[0], go[1], go[2], go[3], true, false);
        Draw.textCentered(g, font, GameText.resolve(CONTINUE), go[0] + go[2] / 2, go[1] + 2,
                QuinceArt.buttonInk(look, true));
    }

    /* A disk on a choice's tile, its slot in the tile's own colour, as the disk tools draw theirs. */
    private static void disk(final GuiGraphics g, final int x, final int y, final int tile) {
        QuinceArt.roundRect(g, x + 5, y + 5, x + 13, y + 13, 2, PALETTE.get().glyph());
        g.fill(x + 7, y + 9, x + 11, y + 10, tile);
    }

    /** The choice under a point of the glass, or -1. */
    public static int choiceAt(final double gx, final double gy) {
        if (gx < X0 + 8 || gx >= X1 - 8 || gy < ROW_Y - 3) {
            return -1;
        }
        final int row = (int) ((gy - ROW_Y + 3) / ROW_H);
        return row >= 0 && row < NAMES.size() ? row : -1;
    }

    /** Where Continue is on the glass, {@code {x, y, w, h}}. */
    public static int[] continueButton(final Font font) {
        final int w = font.width(GameText.resolve(CONTINUE)) + 24;
        return new int[] {X1 - 10 - w, Y1 - 18, w, 12};
    }

    /**
     * The recovery's own colours.
     *
     * @param ground  the dark ground behind the window
     * @param disk    Startup Disk's tile
     * @param system  Reinstall's tile
     * @param utility Disk Utility's tile
     * @param picked  the row picked
     * @param glyph   the disk drawn on a tile
     */
    private record Colours(int ground, int disk, int system, int utility, int picked, int glyph) {
    }
}
