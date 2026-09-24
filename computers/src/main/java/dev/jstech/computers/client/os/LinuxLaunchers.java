/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How the Linux desktops open their programs: KDE's Kickoff, Cinnamon's Mint menu, GNOME's overview.
 *
 * <p>Three desktops, three answers to the same question, and none of them looks like the others. KDE puts a
 * places column beside an app list with a session footer under it. Cinnamon rails its favourites down the
 * left, then categories, then the list. GNOME does not have a menu at all: it covers the whole desktop with
 * an overview, with a search field, the workspaces and a grid of everything. Getting each of them right is
 * the point of shipping several desktops rather than one with three colour schemes.
 *
 * <p>Each is drawn and clicked in the same place, so the rectangle a player sees is the rectangle they hit.
 * That pairing is the reason these live together rather than with the panels they hang off: a menu that is
 * drawn in one file and hit-tested in another drifts, and what drifts is where the click lands.
 *
 * <p>Their colours are the palettes {@code jsc:launcher/kde}, {@code jsc:launcher/gnome} and
 * {@code jsc:launcher/cinnamon}.
 */
@PaletteHolder
final class LinuxLaunchers {

    private final DesktopScreen desktop;

    /** KDE Plasma: a Kickoff-style launcher (places column left, app list right, header, session footer). */
    static final int KDE_MENU_W = 214;
    static final int KDE_SIDE_W = 74;
    static final int KDE_HEADER_H = 26;
    static final int KDE_ROW_H = 16;
    static final int KDE_FOOTER_H = 16;

    /** Cinnamon: the Mint menu (favourites rail, categories, app list with a search box). */
    static final int CIN_MENU_W = 236;
    static final int CIN_RAIL_W = 30;
    static final int CIN_CATS_W = 84;
    static final int CIN_HEADER_H = 22;
    static final int CIN_ROW_H = 16;

    /** GNOME: the Activities overview (search, workspace strip, app grid). */
    static final int GN_COLS = 6;
    static final int GN_TILE_W = 40;
    static final int GN_TILE_H = 34;

    /** Kickoff's places column and Cinnamon's categories, top to bottom; the first of each is the one shown. */
    private static final List<TextKey> KDE_PLACES =
            List.of(DesktopTexts.FAVORITES, DesktopTexts.ALL_APPS, DesktopTexts.SYSTEM, DesktopTexts.UTILITIES);
    private static final List<TextKey> CIN_CATEGORIES = List.of(DesktopTexts.ALL, DesktopTexts.ACCESSORIES,
            DesktopTexts.OFFICE, DesktopTexts.SYSTEM, DesktopTexts.PREFERENCES);

    private static final Palette<Kickoff> KDE = Palettes.declare(JsComputers.MODID, "launcher/kde",
            new Kickoff(0x40000000, 0xFF1B1E24, 0xFF31363B, 0xFFEFF0F1, 0xFF8A9199, 0xFF232629,
                    0xFFFFFFFF, 0xFFBDC3C7, 0x443DAEE9));
    private static final Palette<Overview> GNOME = Palettes.declare(JsComputers.MODID, "launcher/gnome",
            new Overview(0xD00F0F14, 0x33FFFFFF, 0xFFB8BBC8, 0xFFFFFFFF, 0x33FFFFFF, 0xFFFFFFFF, 0xFFF6F5F4,
                    0x22FFFFFF, 0x33FFFFFF, 0x66000000));
    private static final Palette<MintMenu> CINNAMON = Palettes.declare(JsComputers.MODID, "launcher/cinnamon",
            new MintMenu(0x40000000, 0xFF1F1F1F, 0xFF2F2F2F, 0xFF262626, 0x3369B03B, 0xFF3A3A3A,
                    0xFFFFFFFF, 0xFFBDBDBD, 0xFF222222, 0xFF444444, 0xFF9A9A9A, 0xFFE8E8E8));

    LinuxLaunchers(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** KDE Plasma's Kickoff: a dark two-pane launcher with a places column, an app list and a session footer. */
    void renderKde(final GuiGraphics g, final int tbY) {
        final Kickoff c = KDE.get();
        final int x = desktop.startMenuLeft();
        final int w = KDE_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, c.shadow());
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, c.fill());
        // Header: the user and the search hint.
        g.fill(x + 6, y + 6, x + 20, y + 20, desktop.themeColours().startButton());
        g.drawString(desktop.textFont(), desktop.accountLabel(), x + 26, y + 6, c.ink(), false);
        g.drawString(desktop.textFont(), words(DesktopTexts.TYPE_TO_SEARCH_MORE), x + 26, y + 15, c.hint(), false);
        // Side column: places.
        final int bodyTop = y + KDE_HEADER_H;
        final int bodyBot = y + h - KDE_FOOTER_H;
        g.fill(x, bodyTop, x + KDE_SIDE_W, bodyBot, c.side());
        for (int i = 0; i < KDE_PLACES.size(); i++) {
            final int py = bodyTop + 4 + i * 14;
            if (i == 0) {
                g.fill(x, py - 2, x + KDE_SIDE_W, py + 10, desktop.themeColours().startButton());
            }
            g.drawString(desktop.textFont(), words(KDE_PLACES.get(i)), x + 8, py, i == 0 ? c.chosen() : c.muted(),
                    false);
        }
        // App list.
        int my = bodyTop + 4;
        final int listX = x + KDE_SIDE_W + 4;
        final int listW = w - KDE_SIDE_W - 8;
        for (final DesktopScreen.Launcher l : desktop.launcherList()) {
            final boolean hov = desktop.hoverIn(listX, my, listW, KDE_ROW_H);
            if (hov) {
                g.fill(listX, my, listX + listW, my + KDE_ROW_H, c.rowHover());
            }
            ProgramIcons.draw(g, listX + 2, my, 16, KDE_ROW_H, l.programId(), desktop.icons());
            g.drawString(desktop.textFont(), desktop.shorten(l.label(), 18), listX + 22, my + 4, c.ink(), false);
            my += KDE_ROW_H;
        }
        // Footer: session actions.
        g.fill(x, bodyBot, x + w, y + h, c.side());
        g.drawString(desktop.textFont(), words(DesktopTexts.SLEEP), x + 8, bodyBot + 4, c.hint(), false);
        final String off = words(DesktopTexts.START_SHUT_DOWN);
        final int offX = x + w - desktop.textFont().width(off) - 8;
        final boolean offHov = desktop.hoverBelowRight(bodyBot, offX - 4, x + w);
        g.drawString(desktop.textFont(), off, offX, bodyBot + 4, offHov ? c.chosen() : c.muted(), false);
    }

    boolean clickKde(final int mx, final int my, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = KDE_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        final int bodyTop = y + KDE_HEADER_H;
        final int bodyBot = y + h - KDE_FOOTER_H;
        if (my >= bodyBot) {
            if (mx >= x + w - desktop.textFont().width(words(DesktopTexts.START_SHUT_DOWN)) - 12) {
                desktop.askToPowerOff();
                desktop.closeLauncher();
            }
            return true;
        }
        if (my >= bodyTop && mx >= x + KDE_SIDE_W + 4) {
            final int row = (my - (bodyTop + 4)) / KDE_ROW_H;
            if (row >= 0 && row < desktop.launcherList().size()) {
                desktop.launchAt(row);
                desktop.closeLauncher();
            }
        }
        return true;
    }

    /**
     * GNOME's Activities overview: a translucent layer over the desktop with a search box, a workspace strip,
     * the app grid (or the search results) and a dash of the first few apps along the bottom.
     */
    void renderGnomeOverview(final GuiGraphics g) {
        final Overview c = GNOME.get();
        final int sw = desktop.screenW();
        final int sh = desktop.screenH();
        final int top = DesktopScreen.TASKBAR_H;
        g.fill(0, top, sw, sh, c.shade());
        // Search box.
        final int fieldW = Math.min(180, sw - 40);
        final int fieldX = (sw - fieldW) / 2;
        final int fieldY = top + 8;
        g.fill(fieldX, fieldY, fieldX + fieldW, fieldY + 14, c.field());
        final String q = desktop.searchText();
        if (q.isEmpty()) {
            final String hint = words(DesktopTexts.TYPE_TO_SEARCH);
            g.drawString(desktop.textFont(), hint,
                    fieldX + (fieldW - desktop.textFont().width(hint)) / 2, fieldY + 3, c.hint(), false);
        } else {
            g.drawString(desktop.textFont(), desktop.shorten(q, (fieldW - 8) / 6),
                    fieldX + 6, fieldY + 3, c.ink(), false);
        }
        final List<DesktopScreen.Launcher> filtered = desktop.searchedLaunchers();
        int contentTop = fieldY + 22;
        if (q.isEmpty()) {
            contentTop = drawWorkspaceStrip(g, sw, contentTop);
            drawAppGrid(g, sw, sh, contentTop, filtered);
            drawDash(g, sw, sh);
        } else {
            drawSearchResults(g, fieldX, fieldW, contentTop, filtered);
        }
    }

    boolean clickGnomeOverview(final int mx, final int my) {
        final int sw = desktop.screenW();
        final int sh = desktop.screenH();
        final int top = DesktopScreen.TASKBAR_H;
        if (my < top) {
            return false; // the top bar handles its own clicks
        }
        final int fieldW = Math.min(180, sw - 40);
        final int fieldX = (sw - fieldW) / 2;
        final int fieldY = top + 8;
        final List<DesktopScreen.Launcher> filtered = desktop.searchedLaunchers();
        if (!desktop.searchText().isEmpty()) {
            return clickSearchResults(mx, my, fieldX, fieldW, fieldY, filtered);
        }
        final int gridTop = fieldY + 22 + 42;
        final int gridX = (sw - GN_COLS * GN_TILE_W) / 2;
        if (my >= gridTop && mx >= gridX && mx < gridX + GN_COLS * GN_TILE_W) {
            final int col = (mx - gridX) / GN_TILE_W;
            final int row = (my - gridTop) / GN_TILE_H;
            final int idx = row * GN_COLS + col;
            if (idx >= 0 && idx < filtered.size() && gridTop + (row + 1) * GN_TILE_H <= sh - 26) {
                desktop.launch(filtered.get(idx));
                desktop.closeLauncher();
                return true;
            }
        }
        final int dashN = Math.min(5, desktop.launcherList().size());
        final int dashW = dashN * 22 + 8;
        final int dashX = (sw - dashW) / 2;
        final int dashY = sh - 24;
        if (my >= dashY && my < dashY + 20 && mx >= dashX && mx < dashX + dashW) {
            final int idx = (mx - dashX - 4) / 22;
            if (idx >= 0 && idx < dashN) {
                desktop.launchAt(idx);
                desktop.closeLauncher();
            }
            return true;
        }
        if (my >= fieldY && my < fieldY + 14) {
            return true; // the search box keeps the overview open
        }
        desktop.closeLauncher(); // clicking the overview backdrop leaves it, as GNOME does
        return true;
    }

    /** Cinnamon's Mint menu: a favourites rail, a categories column and the app list with a search hint. */
    void renderCinnamon(final GuiGraphics g, final int tbY) {
        final MintMenu c = CINNAMON.get();
        final int x = desktop.startMenuLeft();
        final int w = CIN_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, c.shadow());
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, c.fill());
        // Favourites rail: the first apps as icons.
        g.fill(x, y, x + CIN_RAIL_W, y + h, c.rail());
        final List<DesktopScreen.Launcher> all = desktop.launcherList();
        final int favN = Math.min(4, all.size());
        for (int i = 0; i < favN; i++) {
            final int fy = y + 8 + i * 22;
            if (desktop.hoverIn(x, fy - 3, CIN_RAIL_W, 22)) {
                g.fill(x + 2, fy - 3, x + CIN_RAIL_W - 2, fy + 19, c.hover());
            }
            ProgramIcons.draw(g, x + (CIN_RAIL_W - 16) / 2, fy, 16, 16, all.get(i).programId(), desktop.icons());
        }
        // Categories.
        final int catsX = x + CIN_RAIL_W;
        g.fill(catsX + CIN_CATS_W - 1, y, catsX + CIN_CATS_W, y + h, c.categoriesRule());
        for (int i = 0; i < CIN_CATEGORIES.size(); i++) {
            final int cy = y + CIN_HEADER_H + i * 14;
            if (i == 0) {
                g.fill(catsX, cy - 2, catsX + CIN_CATS_W - 1, cy + 10, desktop.themeColours().startButton());
            }
            g.drawString(desktop.textFont(), words(CIN_CATEGORIES.get(i)), catsX + 8, cy,
                    i == 0 ? c.chosen() : c.category(), false);
        }
        // Search hint + app list.
        final int listX = catsX + CIN_CATS_W + 4;
        final int listW = x + w - listX - 4;
        g.fill(listX, y + 5, listX + listW, y + 17, c.search());
        desktop.drawOutline(g, listX, y + 5, listW, 12, c.searchEdge());
        g.drawString(desktop.textFont(), words(DesktopTexts.SEARCH), listX + 4, y + 7, c.searchHint(), false);
        int my = y + CIN_HEADER_H;
        for (final DesktopScreen.Launcher l : all) {
            final boolean hov = desktop.hoverIn(listX, my, listW, CIN_ROW_H);
            if (hov) {
                g.fill(listX, my, listX + listW, my + CIN_ROW_H, c.hover());
            }
            ProgramIcons.draw(g, listX + 2, my, 16, CIN_ROW_H, l.programId(), desktop.icons());
            g.drawString(desktop.textFont(), desktop.shorten(l.label(), 16), listX + 22, my + 4, c.ink(), false);
            my += CIN_ROW_H;
        }
    }

    boolean clickCinnamon(final int mx, final int my, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = CIN_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        if (mx < x + CIN_RAIL_W) {
            final int favN = Math.min(4, desktop.launcherList().size());
            for (int i = 0; i < favN; i++) {
                final int fy = y + 8 + i * 22;
                if (my >= fy - 3 && my < fy + 19) {
                    desktop.launchAt(i);
                    desktop.closeLauncher();
                    return true;
                }
            }
            return true;
        }
        final int listX = x + CIN_RAIL_W + CIN_CATS_W + 4;
        if (mx >= listX && my >= y + CIN_HEADER_H) {
            final int row = (my - (y + CIN_HEADER_H)) / CIN_ROW_H;
            if (row >= 0 && row < desktop.launcherList().size()) {
                desktop.launchAt(row);
                desktop.closeLauncher();
            }
        }
        return true;
    }

    /** The workspace strip: the one in use, with a hint of its open windows, and an empty one beside it. */
    private int drawWorkspaceStrip(final GuiGraphics g, final int sw, final int contentTop) {
        final Overview c = GNOME.get();
        final int wsW = Math.min(90, (sw - 40) / 2);
        final int wsX = (sw - (wsW * 2 + 10)) / 2;
        g.fill(wsX, contentTop, wsX + wsW, contentTop + 34, c.workspace());
        desktop.drawOutline(g, wsX, contentTop, wsW, 34, c.workspaceEdge());
        if (desktop.anyWindowOpen()) {
            g.fill(wsX + 8, contentTop + 8, wsX + wsW - 8, contentTop + 26, c.windowHint());
        }
        g.fill(wsX + wsW + 10, contentTop, wsX + wsW * 2 + 10, contentTop + 34, c.emptyWorkspace());
        return contentTop + 42;
    }

    private void drawAppGrid(final GuiGraphics g, final int sw, final int sh, final int contentTop,
                             final List<DesktopScreen.Launcher> filtered) {
        final Overview c = GNOME.get();
        final int gridX = (sw - GN_COLS * GN_TILE_W) / 2;
        for (int i = 0; i < filtered.size(); i++) {
            final int col = i % GN_COLS;
            final int row = i / GN_COLS;
            final int tx = gridX + col * GN_TILE_W;
            final int ty = contentTop + row * GN_TILE_H;
            if (ty + GN_TILE_H > sh - 26) {
                break;
            }
            final DesktopScreen.Launcher l = filtered.get(i);
            if (desktop.hoverIn(tx, ty, GN_TILE_W, GN_TILE_H)) {
                g.fill(tx + 2, ty, tx + GN_TILE_W - 2, ty + GN_TILE_H - 2, c.hover());
            }
            ProgramIcons.draw(g, tx + (GN_TILE_W - 16) / 2, ty + 3, 16, 16, l.programId(), desktop.icons());
            String label = l.label();
            while (label.length() > 3 && desktop.textFont().width(label) > GN_TILE_W - 2) {
                label = label.substring(0, label.length() - 1);
            }
            g.drawString(desktop.textFont(), label,
                    tx + (GN_TILE_W - desktop.textFont().width(label)) / 2, ty + 22, c.ink(), false);
        }
    }

    /** The dash: the first few apps as a pill along the bottom, which GNOME keeps there whatever is open. */
    private void drawDash(final GuiGraphics g, final int sw, final int sh) {
        final List<DesktopScreen.Launcher> all = desktop.launcherList();
        final int dashN = Math.min(5, all.size());
        final int dashW = dashN * 22 + 8;
        final int dashX = (sw - dashW) / 2;
        final int dashY = sh - 24;
        g.fill(dashX, dashY, dashX + dashW, dashY + 20, GNOME.get().dash());
        for (int i = 0; i < dashN; i++) {
            ProgramIcons.draw(g, dashX + 4 + i * 22 + 3, dashY + 2, 16, 16,
                    all.get(i).programId(), desktop.icons());
        }
    }

    private void drawSearchResults(final GuiGraphics g, final int fieldX, final int fieldW, final int contentTop,
                                   final List<DesktopScreen.Launcher> filtered) {
        final Overview c = GNOME.get();
        g.drawString(desktop.textFont(),
                words(filtered.isEmpty() ? DesktopTexts.NO_RESULTS : DesktopTexts.APPLICATIONS),
                fieldX, contentTop, c.hint(), false);
        int my = contentTop + 12;
        for (final DesktopScreen.Launcher l : filtered) {
            if (desktop.hoverIn(fieldX, my, fieldW, 16)) {
                g.fill(fieldX, my, fieldX + fieldW, my + 16, c.hover());
            }
            ProgramIcons.draw(g, fieldX + 2, my, 16, 16, l.programId(), desktop.icons());
            g.drawString(desktop.textFont(), l.label(), fieldX + 22, my + 4, c.ink(), false);
            my += 16;
        }
    }

    private boolean clickSearchResults(final int mx, final int my, final int fieldX, final int fieldW,
                                       final int fieldY, final List<DesktopScreen.Launcher> filtered) {
        int ry = fieldY + 22 + 12;
        for (final DesktopScreen.Launcher l : filtered) {
            if (my >= ry && my < ry + 16 && mx >= fieldX && mx < fieldX + fieldW) {
                desktop.launch(l);
                desktop.closeLauncher();
                return true;
            }
            ry += 16;
        }
        if (my >= fieldY && my < fieldY + 14) {
            return true;
        }
        desktop.closeLauncher();
        return true;
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }

    /**
     * Kickoff: the shadow under it, its border and body, its ink and the hint's, the column beside the list and the
     * footer, a chosen place and the rest, and a hovered row.
     */
    private record Kickoff(int shadow, int border, int fill, int ink, int hint, int side, int chosen, int muted,
                           int rowHover) {
    }

    /**
     * The overview: the shade over the desktop, the search field with its hint and what is typed in it, the
     * workspace in use with its edge and its windows, the empty workspace beside it, a hovered tile or row, and
     * the dash.
     */
    private record Overview(int shade, int field, int hint, int ink, int workspace, int workspaceEdge,
                            int windowHint, int emptyWorkspace, int hover, int dash) {
    }

    /**
     * The Mint menu: the shadow under it, its border and body, the favourites rail, a hovered row, the rule after
     * the categories, the chosen category and the rest, the search box with its edge and hint, and a row's ink.
     */
    private record MintMenu(int shadow, int border, int fill, int rail, int hover, int categoriesRule, int chosen,
                            int category, int search, int searchEdge, int searchHint, int ink) {
    }
}
