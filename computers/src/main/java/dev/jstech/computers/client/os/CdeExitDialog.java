/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdeExitMessage;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What EXIT on CDE's Front Panel asks: shut the workstation down, restart it, or think again. There is no
 * logging out because nobody logged in, and it says how many programs are still open before anything is lost.
 *
 * <p>It only draws and says which button a point is on. What each button does to the machine is the desktop's to
 * carry out, the same way for every desktop.
 */
@PaletteHolder
final class CdeExitDialog {

    private static final List<TextKey> BUTTONS =
            List.of(CdeExitMessage.SHUT_DOWN, CdeExitMessage.RESTART, CdeExitMessage.CANCEL);
    /** What the exit dialog dims the desktop behind it with, {@code jsc:desktop/exit_dialog}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/exit_dialog",
            new Colours(0x66000000));
    private static final int MENU_BUTTON = 12;

    private CdeExitDialog() {
    }

    /** Draws the dialog over a desktop that size, for a machine with that many programs open. */
    static void render(final GuiGraphics g, final Font font, final int sw, final int sh, final int open,
                       final CdePalette p) {
        g.fill(0, 0, sw, sh, PALETTE.get().dim());
        final Rect d = CdeExitLayout.dialog(sw, sh);
        MotifChrome.windowFrame(g, d.x(), d.y(), d.w(), d.h(), p);
        // A dialog's bar keeps the menu button and nothing else, since a question is neither put away nor grown.
        MotifChrome.titleBar(g, d.x(), d.y(), d.w(), CdeExitLayout.TITLE_H, true, MENU_BUTTON + 2, 0, p);
        MotifChrome.control(g, d.x() + 1, d.y() + 1, MENU_BUTTON, MENU_BUTTON, OsSkin.Control.CLOSE, false, p);
        final String title = GameText.resolve(CdeExitMessage.TITLE);
        g.drawString(font, title, d.x() + (d.w() - font.width(title)) / 2, d.y() + 3, p.activeInk(), false);
        final List<Text> lines = CdeExitMessage.lines(open);
        for (int i = 0; i < lines.size() && i < CdeExitLayout.LINES; i++) {
            final String line = GameText.resolve(lines.get(i));
            g.drawString(font, line, d.x() + (d.w() - font.width(line)) / 2, CdeExitLayout.lineY(i, sw, sh),
                    p.ink(), false);
        }
        for (int i = 0; i < CdeExitLayout.BUTTONS; i++) {
            final Rect r = CdeExitLayout.button(i, sw, sh);
            MotifChrome.button(g, r.x(), r.y(), r.w(), r.h(), false, i == CdeExitLayout.SHUT_DOWN, p);
            final String label = GameText.resolve(BUTTONS.get(i));
            g.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + 3, p.ink(), false);
        }
    }

    /** How far the exit dialog dims the desktop behind it. */
    private record Colours(int dim) {
    }
}
