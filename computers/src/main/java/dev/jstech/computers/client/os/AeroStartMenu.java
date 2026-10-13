/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Frames 7's Start menu: two columns in a frame of glass. The left one is white, the programs listed down it with the
 * first two in bold, a rule, All Programs, and the search box at its foot, which filters the list as it is typed into.
 * The right one is the glass itself, with the picture of the machine's account standing at its head, the system's
 * places under it in white, and Shut down at its foot.
 *
 * <p>It opens over the corner, flush with the top of the superbar, and the orb stays in the bar beneath it. Its colours
 * are {@code jsc:launcher/frames_7}.
 */
@PaletteHolder
final class AeroStartMenu {

    private final DesktopState desktop;

    static final int MENU_W = 226;
    static final int LEFT_W = 124;
    static final int ROW_H = 14;
    static final int PLACE_H = 13;
    static final int SEARCH_H = 13;
    private static final int PAD = 4;
    private static final int PINNED = 2;
    private static final int SEP_H = 5;
    private static final int ALL_ROW_H = 13;
    private static final int PICTURE = 22;
    private static final int SHUT_DOWN_H = 14;
    /** The system's places, which the right column lists in this order rather than among the programs. */
    private static final List<ResourceLocation> PLACES = List.of(Programs.FILES,
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "this_pc"),
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "network"), Programs.SETTINGS,
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "device_manager"));
    private static final Set<ResourceLocation> PLACE_SET = Set.copyOf(PLACES);

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "launcher/frames_7",
            new Colours(0xCC3C5A82, 0xE612203A, 0xFF4B6B94, 0xFFFFFFFF, 0xFF6D8BB0, 0xFF000000, 0xFFDBEAFB,
                    0xFF7DA2CE, 0xFFD6DFEA, 0xFF7F9DB9, 0xFF8A8A8A, 0xFFFFFFFF, 0x33FFFFFF, 0xFFE8F0FA,
                    0xFF7FC26B, 0xFF6B4A2C, 0xFF8A6A45, 0x4DFFFFFF, 0x0DFFFFFF, 0xFF4A9A3E));

    AeroStartMenu(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The programs of the left column, filtered by what is typed in the search box. */
    List<Launcher> programs() {
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : desktop.start().filtered()) {
            if (!PLACE_SET.contains(l.programId())) {
                out.add(l);
            }
        }
        return out;
    }

    /** The places of the right column, in their order, of those this machine has. */
    List<Launcher> places() {
        final List<Launcher> out = new ArrayList<>();
        for (final ResourceLocation id : PLACES) {
            for (final Launcher l : desktop.launcherList()) {
                if (id.equals(l.programId())) {
                    out.add(l);
                    break;
                }
            }
        }
        return out;
    }

    /** The menu's height: the taller of its two columns, both sized to every program, so it never resizes typing. */
    int height() {
        int all = 0;
        for (final Launcher l : desktop.launcherList()) {
            if (!PLACE_SET.contains(l.programId())) {
                all++;
            }
        }
        final int left = PAD + all * ROW_H + SEP_H + ALL_ROW_H + 4 + SEARCH_H + PAD;
        final int right = PAD + PICTURE + 6 + PLACE_H + places().size() * PLACE_H + 8 + SHUT_DOWN_H + PAD;
        return Math.max(left, right) + 2 * PAD;
    }

    /** Where a left-column row starts below the top of the column, leaving the gap under the pinned two. */
    int rowY(final int i) {
        final int n = programs().size();
        return i * ROW_H + (i >= PINNED && n > PINNED ? SEP_H : 0);
    }

    void render(final GuiGraphics g, final int tbY) {
        final Colours c = PALETTE.get();
        final int x = desktop.start().left();
        final int h = height();
        final int y = tbY - h;
        final boolean glass = !desktop.prefs().effects().isOff(OsMotions.GLASS);
        // The frame of glass, its top corners rounded, and its rim.
        for (int i = 0; i < 3; i++) {
            g.fill(x + 3 - i, y + i, x + MENU_W - 3 + i, y + i + 1, glass ? c.glassTop() : c.basic());
        }
        g.fillGradient(x, y + 3, x + MENU_W, tbY, glass ? c.glassTop() : c.basic(), glass ? c.glassBottom()
                : c.basic());
        g.fill(x + 3, y, x + MENU_W - 3, y + 1, c.rim());
        g.fill(x, y + 3, x + 1, tbY, c.rim());
        g.fill(x + MENU_W - 1, y + 3, x + MENU_W, tbY, c.rim());

        // The white column.
        final int lx = x + PAD;
        final int ly = y + PAD;
        final int lh = h - 2 * PAD;
        g.fill(lx, ly, lx + LEFT_W, ly + lh, c.paper());
        Draw.outline(g, lx, ly, LEFT_W, lh, c.paperRim());
        final List<Launcher> list = programs();
        final int pinned = desktop.start().searchText().isEmpty() ? Math.min(PINNED, list.size()) : 0;
        for (int i = 0; i < list.size(); i++) {
            final Launcher l = list.get(i);
            final int ry = ly + 2 + rowY(i);
            if (ry + ROW_H > ly + lh - SEARCH_H - ALL_ROW_H - 8) {
                break;
            }
            if (desktop.hoverIn(lx + 2, ry, LEFT_W - 4, ROW_H)) {
                g.fill(lx + 2, ry, lx + LEFT_W - 2, ry + ROW_H, c.rowHot());
                Draw.outline(g, lx + 2, ry, LEFT_W - 4, ROW_H, c.rowRim());
            }
            ProgramIcons.draw(g, lx + 4, ry + 1, 12, 12, l.programId(), desktop.icons());
            final String label = desktop.textFont().plainSubstrByWidth(l.label(), LEFT_W - 24);
            if (i < pinned) {
                Draw.text(g, desktop.textFont(), Component.literal(label).withStyle(ChatFormatting.BOLD), lx + 20,
                        ry + 3, c.ink());
            } else {
                Draw.text(g, desktop.textFont(), label, lx + 20, ry + 3, c.ink());
            }
        }
        if (pinned > 0 && list.size() > pinned) {
            final int sep = ly + 2 + pinned * ROW_H + SEP_H / 2;
            g.fill(lx + 6, sep, lx + LEFT_W - 6, sep + 1, c.rule());
        }
        // All Programs over the search box, at the column's foot.
        final int searchY = ly + lh - SEARCH_H - 3;
        final int allY = searchY - ALL_ROW_H - 3;
        g.fill(lx + 6, allY - 2, lx + LEFT_W - 6, allY - 1, c.rule());
        if (desktop.hoverIn(lx + 2, allY, LEFT_W - 4, ALL_ROW_H)) {
            g.fill(lx + 2, allY, lx + LEFT_W - 2, allY + ALL_ROW_H, c.rowHot());
        }
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.ALL_PROGRAMS), lx + 20, allY + 3, c.ink());
        for (int i = 0; i < 4; i++) {
            g.fill(lx + 8 + i, allY + 3 + i, lx + 9 + i, allY + 10 - i, c.chevron());
        }
        g.fill(lx + 3, searchY, lx + LEFT_W - 3, searchY + SEARCH_H, c.paper());
        Draw.outline(g, lx + 3, searchY, LEFT_W - 6, SEARCH_H, c.fieldRim());
        final String typed = desktop.start().searchText();
        Draw.text(g, desktop.textFont(), typed.isEmpty()
                ? desktop.textFont().plainSubstrByWidth(GameText.resolve(DesktopTexts.SEARCH_PROGRAMS_AND_FILES),
                LEFT_W - 12) : desktop.textFont().plainSubstrByWidth(typed, LEFT_W - 12),
                lx + 6, searchY + 3, typed.isEmpty() ? c.hint() : c.ink());

        // The glass column: the account's picture, its name, the places, Shut down.
        final int rx = lx + LEFT_W + 4;
        final int rw = x + MENU_W - PAD - rx;
        final int px = rx + (rw - PICTURE) / 2;
        final int py = ly + 2;
        g.fill(px - 2, py - 2, px + PICTURE + 2, py + PICTURE + 2, c.pictureFrame());
        g.fill(px, py, px + PICTURE, py + PICTURE * 4 / 10, c.grass());
        g.fill(px, py + PICTURE * 4 / 10, px + PICTURE, py + PICTURE * 6 / 10, c.dirtDark());
        g.fill(px, py + PICTURE * 6 / 10, px + PICTURE, py + PICTURE, c.dirt());
        int ry = py + PICTURE + 6;
        Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(desktop.accountLabel(), rw - 6),
                rx + 3, ry + 2, c.placeInk());
        ry += PLACE_H;
        for (final Launcher l : places()) {
            if (desktop.hoverIn(rx, ry, rw, PLACE_H)) {
                g.fill(rx, ry, rx + rw, ry + PLACE_H, c.placeHot());
            }
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(l.label(), rw - 6), rx + 3, ry + 3,
                    c.placeInk());
            ry += PLACE_H;
        }
        final int sy = ly + lh - SHUT_DOWN_H - 2;
        final String shut = GameText.resolve(DesktopTexts.SHUT_DOWN);
        final int bw = Math.min(rw, desktop.textFont().width(shut) + 18);
        if (desktop.hoverIn(rx, sy, bw, SHUT_DOWN_H)) {
            g.fill(rx, sy, rx + bw, sy + SHUT_DOWN_H, c.placeHot());
        }
        g.fillGradient(rx, sy, rx + bw, sy + SHUT_DOWN_H, c.buttonTop(), c.buttonBottom());
        Draw.outline(g, rx, sy, bw, SHUT_DOWN_H, c.buttonRim());
        Draw.text(g, desktop.textFont(), shut, rx + 5, sy + 3, c.placeInk());
        g.fill(rx + bw - 9, sy + 5, rx + bw - 4, sy + 6, c.placeInk());
        g.fill(rx + bw - 8, sy + 6, rx + bw - 5, sy + 7, c.placeInk());
        g.fill(rx + bw - 7, sy + 7, rx + bw - 6, sy + 8, c.placeInk());
    }

    boolean click(final int mx, final int my, final int tbY) {
        final int x = desktop.start().left();
        final int h = height();
        final int y = tbY - h;
        if (mx < x || mx > x + MENU_W || my < y || my > tbY) {
            return false;
        }
        final int lx = x + PAD;
        final int ly = y + PAD;
        final int lh = h - 2 * PAD;
        final int searchY = ly + lh - SEARCH_H - 3;
        final int allY = searchY - ALL_ROW_H - 3;
        if (mx < lx + LEFT_W) {
            if (my >= allY && my < allY + ALL_ROW_H) {
                desktop.start().openAllPrograms();
                desktop.start().close();
                return true;
            }
            if (my >= searchY) {
                return true;
            }
            final List<Launcher> list = programs();
            for (int i = 0; i < list.size(); i++) {
                final int ry = ly + 2 + rowY(i);
                if (my >= ry && my < ry + ROW_H) {
                    desktop.start().choose(list.get(i));
                    desktop.start().close();
                    return true;
                }
            }
            return true;
        }
        final int rx = lx + LEFT_W + 4;
        final int sy = ly + lh - SHUT_DOWN_H - 2;
        if (my >= sy) {
            desktop.power().open();
            desktop.start().close();
            return true;
        }
        int ry = ly + 2 + PICTURE + 6 + PLACE_H;
        for (final Launcher l : places()) {
            if (my >= ry && my < ry + PLACE_H && mx >= rx) {
                desktop.start().choose(l);
                desktop.start().close();
                return true;
            }
            ry += PLACE_H;
        }
        return true;
    }

    /** The desktop-local centre of the entry that starts {@code target}, where a test clicks it. */
    int[] pointOf(final Launcher target, final int tbY) {
        final int x = desktop.start().left();
        final int h = height();
        final int ly = tbY - h + PAD;
        final int place = places().indexOf(target);
        if (place >= 0) {
            final int rx = x + PAD + LEFT_W + 4;
            return new int[] {rx + 20, ly + 2 + PICTURE + 6 + PLACE_H + place * PLACE_H + PLACE_H / 2};
        }
        final int row = Math.max(0, programs().indexOf(target));
        return new int[] {x + PAD + LEFT_W / 2, ly + 2 + rowY(row) + ROW_H / 2};
    }

    /**
     * The menu's colours: the glass and the same opaque for Frames 7 Basic, the rim, the white column and its rim,
     * the ink, a row under the cursor and its rim, the rules, the search box's rim and its hint, the places' ink and a
     * place under the cursor, the frame of the account's picture and the grass block in it, Shut down's button, and
     * the green chevron of All Programs.
     */
    private record Colours(int glassTop, int glassBottom, int basic, int paper, int paperRim, int ink, int rowHot,
                           int rowRim, int rule, int fieldRim, int hint, int placeInk, int placeHot,
                           int pictureFrame, int grass, int dirtDark, int dirt, int buttonTop, int buttonBottom,
                           int chevron) {

        /** The rim of Shut down's button, the same light as the glass's own edge. */
        int buttonRim() {
            return this.paperRim;
        }

        /** The rim round the whole frame. */
        int rim() {
            return this.paperRim;
        }
    }
}
