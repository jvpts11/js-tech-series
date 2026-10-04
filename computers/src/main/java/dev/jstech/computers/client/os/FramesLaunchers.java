/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * How the Frames systems open a program, from the classic Start menu to the one on Frames 11.
 *
 * <p>Three editions, three menus, and each one is the menu of its own age. Frames 95 is the raised panel with
 * the edition's name running up its side and one plain list of programs. XP is two columns over white and
 * blue, with the player's own face in the header band, a rule of orange under it, and Log Off and Turn Off in
 * the footer. Frames 11 is a floating panel with a search field, a grid of pinned programs, and the power
 * button in the corner. The period launcher is the odd one out: the plain vertical list a Legacy-era desktop
 * had, drawn out of that skin's own relief rather than from flat colours.
 *
 * <p>Each is drawn and hit-tested here, together, for the same reason the Linux ones are: a menu drawn in one
 * file and clicked in another drifts, and what drifts is where the click lands.
 *
 * <p>The colours each menu adds to its theme's are the palettes {@code jsc:launcher/period},
 * {@code jsc:launcher/frames_95}, {@code jsc:launcher/frames_xp} and {@code jsc:launcher/frames_11}.
 */
@PaletteHolder
final class FramesLaunchers {

    private final DesktopState desktop;

    private static final Palette<Period> PERIOD = Palettes.declare(JsComputers.MODID, "launcher/period",
            new Period(0xFFFFFFFF));
    private static final Palette<Classic> CLASSIC = Palettes.declare(JsComputers.MODID, "launcher/frames_95",
            new Classic(0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF808080, 0xFFFFFFFF, 0xFFC03030,
                    0xFFFFFFFF));
    private static final Palette<Luna> LUNA = Palettes.declare(JsComputers.MODID, "launcher/frames_xp",
            new Luna(0xFF13315F, 0xFF3B7BD4, 0xFF1E4E9E, 0xFFFFFFFF, 0xFFFFD268, 0xFFF4A11E,
                    0xFFFFFFFF, 0xFFDCE7F6, 0xFFB6C6E0, 0xFF1A3A70, 0xFF9FBBE6, 0x333B7BD4, 0xFF2F9A33,
                    0x33FFFFFF, 0xFFE0A020, 0xFFE24C4C, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF2F6FD6));
    private static final Palette<Modern> MODERN = Palettes.declare(JsComputers.MODID, "launcher/frames_11",
            new Modern(0x40000000));

    // The classic Start menu, and the period launcher that shares its list: a side band and one row per program.
    static final int MENU_W = 130;
    static final int BAND_W = 22;
    static final int MENU_ITEM_H = 18;
    // Frames XP Start: a two-column panel (programs left, system "places" right) with a header and a footer band.
    static final int XP_MENU_W = 202;
    static final int XP_HEADER_H = 26;
    /** The orange band the Luna Start menu ran under its user header. */
    static final int XP_ORANGE_H = 2;
    static final int XP_FOOTER_H = 18;
    static final int XP_ROW_H = 16;
    static final int XP_LEFT_W = 120;
    /** The gap a separator sits in, between the pinned block and the rest of the left column. */
    static final int XP_SEP_H = 5;
    /** How many of the left column's entries are drawn as pinned (bold) at its top. */
    static final int XP_PINNED = 2;
    static final int XP_ALL_ROW_H = 15;
    /*
     * Frames 11 Start: a compact floating panel with a search box, a pinned-app grid, and a footer power button.
     * Kept small (5 columns, tight tiles) so even a Mainframe's full app set fits above the taskbar.
     */
    static final int W11_MENU_W = 172;
    static final int W11_COLS = 5;
    static final int W11_TILE_W = 32;
    static final int W11_TILE_H = 30;
    static final int W11_SEARCH_H = 14;
    static final int W11_FOOTER_H = 18;

    FramesLaunchers(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /**
     * The launcher of a Legacy-era Unix desktop: a raised panel with a coloured side band carrying the
     * desktop's name and one vertical list of programs. Drawn from the skin's own primitives, so it carries
     * the same relief as that skin's windows and panel instead of the modern flat chrome.
     */
    void renderPeriod(final GuiGraphics g, final int tbY) {
        final int x = desktop.start().left();
        final int h = desktop.start().height();
        final int w = desktop.start().width();
        final int y = tbY - h;
        final OsSkin skin = desktop.prefs().skin();
        skin.panel(g, x, y, w, h);

        // Side band with the desktop's name, rotated, the way the launchers of that period carried it.
        g.fill(x + 2, y + 2, x + 2 + BAND_W, y + h - 2, skin.accent());
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + BAND_W - 3, y + h - 8, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        Draw.text(g, desktop.textFont(), desktop.deskName(), 0, 0, PERIOD.get().bandInk());
        pose.popPose();

        final int itemX = x + BAND_W + 6;
        int my = y + 4;
        for (final Launcher l : desktop.launcherList()) {
            final boolean hov = desktop.hoverIn(itemX, my, x + w - itemX, MENU_ITEM_H);
            skin.listRow(g, itemX, my, x + w - 4 - itemX, MENU_ITEM_H, hov, false);
            ProgramIcons.draw(g, itemX + 2, my + 1, 14, 14, desktop.programIdFor(l.key()), desktop.icons());
            Draw.text(g, desktop.textFont(), l.label(), itemX + 20, my + 4,
                    hov ? skin.listRowText(true) : skin.text());
            my += MENU_ITEM_H;
        }
    }

    boolean clickPeriod(final int mx, final int my, final int tbY) {
        final int x = desktop.start().left();
        final int w = desktop.start().width();
        final int h = desktop.start().height();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        final int idx = (int) Math.floor((my - (y + 4)) / (double) MENU_ITEM_H);
        if (idx >= 0 && idx < desktop.launcherList().size()) {
            desktop.start().choose(idx);
        }
        desktop.start().close();
        return true;
    }

    /** Frames 95: the classic Start menu with a rotated OS-name side band and a single vertical program list. */
    void render95(final GuiGraphics g, final int tbY) {
        final int x = desktop.start().left();
        final int h = desktop.start().height();
        final int y = tbY - h;
        final int w = MENU_W;
        final DesktopTheme theme = desktop.themeColours();
        final Classic c = CLASSIC.get();
        // Raised panel.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, theme.menuBg());
        g.fill(x, y, x + w, y + 1, c.light());
        g.fill(x, y, x + 1, y + h, c.light());
        // Side band with the OS name, drawn rotated like the classic Start menu.
        g.fill(x + 1, y + 1, x + 1 + BAND_W, y + h - 1, theme.titleActive());
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + BAND_W - 5, y + h - 7, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        Draw.text(g, desktop.textFont(), desktop.deskName(), 0, 0, c.bandInk());
        pose.popPose();
        // Program items with icons.
        final int itemX = x + BAND_W + 4;
        int my = y + 4;
        for (final Launcher l : desktop.launcherList()) {
            final boolean hov = desktop.hoverIn(itemX, my, x + w - itemX, MENU_ITEM_H);
            if (hov) {
                g.fill(itemX, my, x + w - 2, my + MENU_ITEM_H, theme.titleActive());
            }
            ProgramIcons.draw(g, itemX, my, 16, 14, l.programId(), desktop.icons());
            Draw.text(g, desktop.textFont(), l.label(), itemX + 20, my + 3,
                    hov ? c.hoverInk() : theme.menuText());
            my += MENU_ITEM_H;
        }
        // Separator, then Shut Down.
        g.fill(itemX, my + 1, x + w - 4, my + 2, c.ruleDark());
        g.fill(itemX, my + 2, x + w - 4, my + 3, c.ruleLight());
        my += 6;
        g.fill(itemX + 3, my + 2, itemX + 13, my + 12, c.powerMark());
        g.fill(itemX + 7, my, itemX + 9, my + 6, c.powerStem());
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.START_SHUT_DOWN), itemX + 20, my + 3,
                theme.menuText());
    }

    boolean click95(final int mx, final int my, final int tbY) {
        final int h = desktop.start().height();
        final int y = tbY - h;
        final int x = desktop.start().left();
        if (mx < x || mx > x + MENU_W || my < y || my > y + h) {
            return false;
        }
        final int itemsTop = y + 4;
        final List<Launcher> all = desktop.launcherList();
        final int idx = (int) Math.floor((my - itemsTop) / (double) MENU_ITEM_H);
        if (idx >= 0 && idx < all.size()) {
            desktop.start().choose(idx);
        } else {
            final int shutY = itemsTop + all.size() * MENU_ITEM_H + 6;
            if (my >= shutY && my <= shutY + MENU_ITEM_H) {
                desktop.power().open();
            }
        }
        desktop.start().close();
        return true;
    }

    /** Frames XP: a two-column panel (programs left, system places right) with header and footer bands. */
    void renderXp(final GuiGraphics g, final int tbY) {
        final int x = desktop.start().left();
        final int w = XP_MENU_W;
        final int h = desktop.start().height();
        final int y = tbY - h;
        final DesktopTheme theme = desktop.themeColours();
        final Luna c = LUNA.get();
        // Panel with a thin border.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, theme.menuBg());
        // Header band: the player's own face and name over the Luna blue, the way this menu always opened.
        g.fillGradient(x, y, x + w, y + XP_HEADER_H, c.bandFrom(), c.bandTo());
        drawPlayerFace(g, x + 4, y + 3, XP_HEADER_H - 6);
        Draw.text(g, desktop.textFont(), playerName(), x + 4 + XP_HEADER_H - 6 + 5, y + (XP_HEADER_H - 8) / 2,
                c.bandInk());
        // The orange rule under the header, lit along its top edge.
        g.fill(x, y + XP_HEADER_H, x + w, y + XP_HEADER_H + 1, c.orangeTop());
        g.fill(x, y + XP_HEADER_H + 1,
                x + w, y + XP_HEADER_H + XP_ORANGE_H, c.orange());
        // Body: left programs column over white, right places column over a tinted panel.
        final int bodyTop = y + XP_HEADER_H + XP_ORANGE_H;
        final int bodyBot = y + h - XP_FOOTER_H;
        final int split = x + XP_LEFT_W;
        g.fill(x, bodyTop, split, bodyBot, c.leftBody());
        g.fill(split, bodyTop, x + w, bodyBot, c.rightBody());
        g.fill(split, bodyTop, split + 1, bodyBot, c.split());
        drawXpLeftColumn(g, x + 3, bodyTop + 3, XP_LEFT_W - 6);
        drawXpColumn(g, desktop.start().xpRight(), split + 3, bodyTop + 3,
                w - XP_LEFT_W - 6, c.rightInk(), 0, false);
        drawXpFooter(g, x, y, w, h, bodyBot);
    }

    boolean clickXp(final int mx, final int my, final int tbY) {
        final int x = desktop.start().left();
        final int w = XP_MENU_W;
        final int h = desktop.start().height();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        final int bodyTop = y + XP_HEADER_H + XP_ORANGE_H;
        final int bodyBot = y + h - XP_FOOTER_H;
        final int split = x + XP_LEFT_W;
        if (my >= bodyTop && my < bodyBot) {
            if (clickXpBody(mx, my - (bodyTop + 3), split)) {
                return true;
            }
        } else if (my >= bodyBot) {
            // The footer: log off leaves the machine, turn off asks the power dialog.
            if (mx >= desktop.start().xpFooterOff(x, w)) {
                desktop.power().open();
            } else if (mx >= desktop.start().xpFooterLog(x, w)) {
                desktop.start().close();
                desktop.leaveDesktop();
                return true;
            }
        }
        desktop.start().close();
        return true;
    }

    /** Frames 11: a centered floating panel with a search box, a pinned-app grid, and a footer power button. */
    void render11(final GuiGraphics g, final int tbY) {
        final int x = desktop.start().left();
        final int w = W11_MENU_W;
        final int h = desktop.start().height();
        final int y = desktop.start().top(tbY);
        final OsSkin skin = desktop.prefs().skin();
        // The Start panel follows the window skin, so dark mode darkens it along with every program.
        final int panelBg = skin.windowBg();
        final int panelText = skin.text();
        final int panelDim = skin.dim();
        final int panelEdge = skin.edge();
        final int panelHover = skin.listHover();
        // Soft drop shadow, then the panel with a hairline border.
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, MODERN.get().shadow());
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, skin.windowBorder());
        g.fill(x, y, x + w, y + h, panelBg);
        final int contentTop = drawW11Search(g, x, y, w, panelEdge, panelDim, panelText, skin);
        final List<Launcher> filtered = desktop.start().filtered();
        if (desktop.start().searchText().isEmpty()) {
            drawW11Pinned(g, x, w, contentTop, filtered, panelDim, panelText, panelHover, panelEdge);
        } else {
            drawW11Results(g, x, w, contentTop, filtered, panelDim, panelText, panelHover);
        }
        drawW11Footer(g, x, y, w, h, panelEdge, panelText, panelHover);
    }

    boolean click11(final int mx, final int my, final int tbY) {
        final int x = desktop.start().left();
        final int w = W11_MENU_W;
        final int h = desktop.start().height();
        final int y = desktop.start().top(tbY);
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        // Footer power button (right side): shut the computer down.
        final int footY = y + h - W11_FOOTER_H;
        if (my >= footY) {
            if (mx >= x + w - 24) {
                desktop.power().open();
                desktop.start().close();
            }
            return true; // clicks elsewhere in the footer are absorbed, keeping the menu open
        }
        final int contentTop = y + 6 + W11_SEARCH_H + 5;
        final List<Launcher> filtered = desktop.start().filtered();
        if (!desktop.start().searchText().isEmpty()) {
            int ry = contentTop + 11;
            for (final Launcher l : filtered) {
                if (my >= ry && my < ry + 15) {
                    desktop.start().choose(l);
                    desktop.start().close();
                    return true;
                }
                ry += 15;
            }
            return true; // absorb clicks on the search box / empty space
        }
        // Pinned grid tiles.
        final int gridTop = contentTop + 9;
        final int gridX = x + (w - W11_COLS * W11_TILE_W) / 2;
        if (my >= gridTop && mx >= gridX) {
            final int col = (mx - gridX) / W11_TILE_W;
            final int row = (my - gridTop) / W11_TILE_H;
            if (col >= 0 && col < W11_COLS) {
                final int idx = row * W11_COLS + col;
                if (idx >= 0 && idx < filtered.size()) {
                    desktop.start().choose(filtered.get(idx));
                    desktop.start().close();
                    return true;
                }
            }
        }
        return true; // clicks on the search box or padding keep the menu open
    }

    /**
     * The XP menu's left column: the pinned entries in bold, a separator, the rest, and the All Programs row
     * that opens the page listing everything installed, services included.
     */
    private void drawXpLeftColumn(final GuiGraphics g, final int colX, final int colY, final int colW) {
        final Luna c = LUNA.get();
        final List<Launcher> items = desktop.start().xpLeft();
        final int pinned = Math.min(XP_PINNED, items.size());
        drawXpColumn(g, items, colX, colY, colW, desktop.themeColours().menuText(), pinned, true);
        if (items.size() > pinned) {
            final int sepY = colY + pinned * XP_ROW_H + XP_SEP_H / 2;
            g.fill(colX + 3, sepY, colX + colW - 3, sepY + 1, c.rule());
        }
        final int afterRows = colY + desktop.start().xpLeftRow(items.size());
        g.fill(colX + 3, afterRows + XP_SEP_H / 2,
                colX + colW - 3, afterRows + XP_SEP_H / 2 + 1, c.rule());
        final int allY = colY + desktop.start().xpAllRow();
        if (desktop.hoverIn(colX, allY, colW, XP_ALL_ROW_H)) {
            g.fill(colX, allY, colX + colW, allY + XP_ALL_ROW_H, c.rowHover());
        }
        Draw.text(g, desktop.textFont(), GameText.component(DesktopTexts.ALL_PROGRAMS).withStyle(ChatFormatting.BOLD),
                colX + 4, allY + 4, desktop.themeColours().menuText());
        // The green chevron that always sat at the end of this row.
        final int ax = colX + colW - 10;
        for (int i = 0; i < 5; i++) {
            g.fill(ax + i, allY + 3 + i, ax + i + 1, allY + 12 - i, c.chevron());
        }
    }

    /**
     * Draws one XP column as an icon and label list, with a highlight on the row under the cursor. The first
     * {@code boldCount} entries are the pinned ones and are drawn in bold. The left column's rows are spaced
     * to leave the gap its separator sits in.
     */
    private void drawXpColumn(final GuiGraphics g, final List<Launcher> items, final int colX,
                              final int colY, final int colW, final int textColor, final int boldCount,
                              final boolean leftColumn) {
        for (int i = 0; i < items.size(); i++) {
            final Launcher l = items.get(i);
            final int my = colY + (leftColumn ? desktop.start().xpLeftRow(i) : i * XP_ROW_H);
            if (desktop.hoverIn(colX, my, colW, XP_ROW_H)) {
                g.fill(colX, my, colX + colW, my + XP_ROW_H, LUNA.get().rowHover());
            }
            ProgramIcons.draw(g, colX + 1, my, 14, 12, l.programId(), desktop.icons());
            final String label = desktop.shorten(l.label(), (colW - 20) / 6);
            if (i < boldCount) {
                Draw.text(g, desktop.textFont(), Component.literal(label).withStyle(ChatFormatting.BOLD),
                        colX + 18, my + 4, textColor);
            } else {
                Draw.text(g, desktop.textFont(), label, colX + 18, my + 4, textColor);
            }
        }
    }

    /** The XP footer band: Log Off and Turn Off Computer, right-aligned, mirroring the header gradient. */
    private void drawXpFooter(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int footY) {
        final Luna c = LUNA.get();
        g.fillGradient(x, footY, x + w, y + h, c.bandFrom(), c.bandTo());
        final int offX = desktop.start().xpFooterOff(x, w);
        final int logX = desktop.start().xpFooterLog(x, w);
        final int textY = footY + (XP_FOOTER_H - 8) / 2;
        if (desktop.hoverBelowRight(footY, logX, offX - 4)) {
            g.fill(logX - 2, footY + 2, offX - 6, y + h - 2, c.footerHover());
        }
        g.fill(logX, footY + 5, logX + 8, footY + 13, c.logOffMark());
        g.fill(logX + 3, footY + 8, logX + 8, footY + 10, c.markStem());
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.XP_LOG_OFF), logX + 12, textY, c.bandInk());
        if (desktop.hoverBelowRight(footY, offX, x + w - 2)) {
            g.fill(offX - 2, footY + 2, x + w - 3, y + h - 2, c.footerHover());
        }
        g.fill(offX, footY + 5, offX + 8, footY + 13, c.turnOffMark());
        g.fill(offX + 3, footY + 3, offX + 5, footY + 9, c.markStem());
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.TURN_OFF_COMPUTER), offX + 12, textY,
                c.bandInk());
    }

    /** A click inside the XP menu's two columns; false when it landed on neither a row nor All Programs. */
    private boolean clickXpBody(final int mx, final int dy, final int split) {
        if (mx < split) {
            // The left column's rows are spaced around a separator, so they are walked, not divided.
            final List<Launcher> col = desktop.start().xpLeft();
            for (int i = 0; i < col.size(); i++) {
                final int ry = desktop.start().xpLeftRow(i);
                if (dy >= ry && dy < ry + XP_ROW_H) {
                    desktop.start().choose(col.get(i));
                    desktop.start().close();
                    return true;
                }
            }
            final int allY = desktop.start().xpAllRow();
            if (dy >= allY && dy < allY + XP_ALL_ROW_H) {
                desktop.start().openAllPrograms();
            }
            return false;
        }
        final List<Launcher> col = desktop.start().xpRight();
        final int row = dy / XP_ROW_H;
        if (row >= 0 && row < col.size()) {
            desktop.start().choose(col.get(row));
        }
        return false;
    }

    /** The Frames 11 search field, with its magnifier; gives back where the content under it starts. */
    private int drawW11Search(final GuiGraphics g, final int x, final int y, final int w, final int panelEdge,
                              final int panelDim, final int panelText, final OsSkin skin) {
        final int fieldX = x + 6;
        final int fieldW = w - 12;
        final int fieldY = y + 6;
        g.fill(fieldX, fieldY, fieldX + fieldW, fieldY + W11_SEARCH_H, skin.fieldBg());
        desktop.drawOutline(g, fieldX, fieldY, fieldW, W11_SEARCH_H, panelEdge);
        // Magnifier glyph.
        desktop.drawOutline(g, fieldX + 4, fieldY + 3, 5, 5, panelDim);
        g.fill(fieldX + 8, fieldY + 7, fieldX + 10, fieldY + 9, panelDim);
        final String q = desktop.start().searchText();
        if (q.isEmpty()) {
            Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.SEARCH_HINT), fieldX + 13, fieldY + 3,
                    panelDim);
        } else {
            Draw.text(g, desktop.textFont(), desktop.shorten(q, (fieldW - 16) / 6),
                    fieldX + 13, fieldY + 3, panelText);
        }
        return fieldY + W11_SEARCH_H + 5;
    }

    private void drawW11Pinned(final GuiGraphics g, final int x, final int w, final int contentTop,
                               final List<Launcher> filtered, final int panelDim,
                               final int panelText, final int panelHover, final int panelEdge) {
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.PINNED), x + 8, contentTop, panelDim);
        final int gridTop = contentTop + 9;
        final int gridX = x + (w - W11_COLS * W11_TILE_W) / 2;
        for (int i = 0; i < filtered.size(); i++) {
            final int col = i % W11_COLS;
            final int row = i / W11_COLS;
            drawW11Tile(g, filtered.get(i), gridX + col * W11_TILE_W,
                    gridTop + row * W11_TILE_H, panelText, panelHover, panelEdge);
        }
    }

    private void drawW11Results(final GuiGraphics g, final int x, final int w, final int contentTop,
                                final List<Launcher> filtered, final int panelDim,
                                final int panelText, final int panelHover) {
        Draw.text(g, desktop.textFont(),
                GameText.resolve(filtered.isEmpty() ? DesktopTexts.NO_RESULTS : DesktopTexts.BEST_MATCH),
                x + 8, contentTop, panelDim);
        int my = contentTop + 11;
        final int rowW = w - 12;
        for (final Launcher l : filtered) {
            if (desktop.hoverIn(x + 6, my, rowW, 15)) {
                g.fill(x + 6, my, x + 6 + rowW, my + 15, panelHover);
            }
            ProgramIcons.draw(g, x + 8, my + 1, 13, 12, l.programId(), desktop.icons());
            Draw.text(g, desktop.textFont(), l.label(), x + 24, my + 4, panelText);
            my += 15;
        }
    }

    /** The Frames 11 footer: a separator, the account on the left, the power button on the right. */
    private void drawW11Footer(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int panelEdge, final int panelText, final int panelHover) {
        final int footY = y + h - W11_FOOTER_H;
        g.fill(x + 8, footY, x + w - 8, footY + 1, panelEdge);
        Draw.text(g, desktop.textFont(), desktop.accountLabel(),
                x + 12, footY + (W11_FOOTER_H - 8) / 2, panelText);
        final int pwX = x + w - 22;
        final int pwY = footY + (W11_FOOTER_H - 12) / 2;
        if (desktop.hoverBelowRight(footY, pwX - 2, pwX + 14)) {
            g.fill(pwX - 3, footY + 2, pwX + 15, footY + W11_FOOTER_H - 2, panelHover);
        }
        desktop.drawOutline(g, pwX, pwY, 12, 12, panelText);
        g.fill(pwX + 5, pwY - 1, pwX + 7, pwY + 6, panelText); // power stem
    }

    /** Draws one Frames 11 pinned tile: an icon over a centered label, with a hover background. */
    private void drawW11Tile(final GuiGraphics g, final Launcher l, final int tx, final int ty,
                             final int labelColor, final int hoverBg, final int hoverEdge) {
        if (desktop.hoverIn(tx, ty, W11_TILE_W, W11_TILE_H)) {
            g.fill(tx + 1, ty + 1, tx + W11_TILE_W - 1, ty + W11_TILE_H - 1, hoverBg);
            desktop.drawOutline(g, tx + 1, ty + 1,
                    W11_TILE_W - 2, W11_TILE_H - 2, hoverEdge);
        }
        ProgramIcons.draw(g, tx + (W11_TILE_W - 16) / 2, ty + 3, 16, 14,
                l.programId(), desktop.icons());
        // Truncate the label to the tile width by dropping characters (no ellipsis, which would be wider).
        String label = l.label();
        while (label.length() > 3 && desktop.textFont().width(label) > W11_TILE_W - 2) {
            label = label.substring(0, label.length() - 1);
        }
        Draw.text(g, desktop.textFont(), label,
                tx + (W11_TILE_W - desktop.textFont().width(label)) / 2, ty + 20, labelColor);
    }

    /** The name shown on the XP menu's header: the player's own. */
    private static String playerName() {
        return Minecraft.getInstance().getUser().getName();
    }

    /**
     * The player's face from their own skin, hat layer included, at {@code size} pixels square. A client
     * without a player yet falls back to a plain plate, so the header never renders as a hole.
     */
    private static void drawPlayerFace(final GuiGraphics g, final int x, final int y, final int size) {
        final Luna c = LUNA.get();
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, c.faceFrame()); // the little white frame XP drew
        final AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            g.fill(x, y, x + size, y + size, c.faceFallback());
            return;
        }
        final ResourceLocation skin = player.getSkin().texture();
        g.blit(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        g.blit(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    /** The period launcher's one colour of its own: the desktop's name on its side band. */
    private record Period(int bandInk) {
    }

    /**
     * The grey menu: its border and light edges, the edition's name on its band, a hovered row's words, the rule
     * over Shut Down, and the power mark beside it.
     */
    private record Classic(int border, int light, int bandInk, int hoverInk, int ruleDark, int ruleLight,
                           int powerMark, int powerStem) {
    }

    /**
     * The two-column menu: its border, the blue bands at its head and foot with their ink, the orange rule, the
     * two columns and the line between them, the right column's ink, the separators, a hovered row, the All
     * Programs chevron, the footer's hover and marks, and the frame and stand-in of the player's face.
     */
    private record Luna(int border, int bandFrom, int bandTo, int bandInk, int orangeTop, int orange,
                        int leftBody, int rightBody, int split, int rightInk, int rule, int rowHover, int chevron,
                        int footerHover, int logOffMark, int turnOffMark, int markStem, int faceFrame,
                        int faceFallback) {
    }

    /** The floating menu's one colour of its own: the soft shadow under it. */
    private record Modern(int shadow) {
    }
}
