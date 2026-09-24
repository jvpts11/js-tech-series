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

    private final DesktopScreen desktop;

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

    FramesLaunchers(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /**
     * The launcher of a Legacy-era Unix desktop: a raised panel with a coloured side band carrying the
     * desktop's name and one vertical list of programs. Drawn from the skin's own primitives, so it carries
     * the same relief as that skin's windows and panel instead of the modern flat chrome.
     */
    void renderPeriod(final GuiGraphics g, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int h = desktop.startMenuTall();
        final int w = desktop.startMenuWide();
        final int y = tbY - h;
        final OsSkin skin = desktop.panelSkin();
        skin.panel(g, x, y, w, h);

        // Side band with the desktop's name, rotated, the way the launchers of that period carried it.
        g.fill(x + 2, y + 2, x + 2 + DesktopScreen.BAND_W, y + h - 2, skin.accent());
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + DesktopScreen.BAND_W - 3, y + h - 8, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        g.drawString(desktop.textFont(), desktop.deskName(), 0, 0, PERIOD.get().bandInk(), false);
        pose.popPose();

        final int itemX = x + DesktopScreen.BAND_W + 6;
        int my = y + 4;
        for (final DesktopScreen.Launcher l : desktop.launcherList()) {
            final boolean hov = desktop.hoverIn(itemX, my, x + w - itemX, DesktopScreen.MENU_ITEM_H);
            skin.listRow(g, itemX, my, x + w - 4 - itemX, DesktopScreen.MENU_ITEM_H, hov, false);
            ProgramIcons.draw(g, itemX + 2, my + 1, 14, 14, desktop.programIdFor(l.key()), desktop.icons());
            g.drawString(desktop.textFont(), l.label(), itemX + 20, my + 4,
                    hov ? skin.listRowText(true) : skin.text(), false);
            my += DesktopScreen.MENU_ITEM_H;
        }
    }

    boolean clickPeriod(final int mx, final int my, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = desktop.startMenuWide();
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        final int idx = (int) Math.floor((my - (y + 4)) / (double) DesktopScreen.MENU_ITEM_H);
        if (idx >= 0 && idx < desktop.launcherList().size()) {
            desktop.launchAt(idx);
        }
        desktop.closeLauncher();
        return true;
    }

    /** Frames 95: the classic Start menu with a rotated OS-name side band and a single vertical program list. */
    void render95(final GuiGraphics g, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        final int w = DesktopScreen.MENU_W;
        final DesktopTheme theme = desktop.themeColours();
        final Classic c = CLASSIC.get();
        // Raised panel.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, theme.menuBg());
        g.fill(x, y, x + w, y + 1, c.light());
        g.fill(x, y, x + 1, y + h, c.light());
        // Side band with the OS name, drawn rotated like the classic Start menu.
        g.fill(x + 1, y + 1, x + 1 + DesktopScreen.BAND_W, y + h - 1, theme.titleActive());
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + DesktopScreen.BAND_W - 5, y + h - 7, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        g.drawString(desktop.textFont(), desktop.osBand(), 0, 0, c.bandInk(), false);
        pose.popPose();
        // Program items with icons.
        final int itemX = x + DesktopScreen.BAND_W + 4;
        int my = y + 4;
        for (final DesktopScreen.Launcher l : desktop.launcherList()) {
            final boolean hov = desktop.hoverIn(itemX, my, x + w - itemX, DesktopScreen.MENU_ITEM_H);
            if (hov) {
                g.fill(itemX, my, x + w - 2, my + DesktopScreen.MENU_ITEM_H, theme.titleActive());
            }
            ProgramIcons.draw(g, itemX, my, 16, 14, l.programId(), desktop.icons());
            g.drawString(desktop.textFont(), l.label(), itemX + 20, my + 3,
                    hov ? c.hoverInk() : theme.menuText(), false);
            my += DesktopScreen.MENU_ITEM_H;
        }
        // Separator, then Shut Down.
        g.fill(itemX, my + 1, x + w - 4, my + 2, c.ruleDark());
        g.fill(itemX, my + 2, x + w - 4, my + 3, c.ruleLight());
        my += 6;
        g.fill(itemX + 3, my + 2, itemX + 13, my + 12, c.powerMark());
        g.fill(itemX + 7, my, itemX + 9, my + 6, c.powerStem());
        g.drawString(desktop.textFont(), GameText.resolve(DesktopTexts.START_SHUT_DOWN), itemX + 20, my + 3,
                theme.menuText(), false);
    }

    boolean click95(final int mx, final int my, final int tbY) {
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        final int x = desktop.startMenuLeft();
        if (mx < x || mx > x + DesktopScreen.MENU_W || my < y || my > y + h) {
            return false;
        }
        final int itemsTop = y + 4;
        final List<DesktopScreen.Launcher> all = desktop.launcherList();
        final int idx = (int) Math.floor((my - itemsTop) / (double) DesktopScreen.MENU_ITEM_H);
        if (idx >= 0 && idx < all.size()) {
            desktop.launchAt(idx);
        } else {
            final int shutY = itemsTop + all.size() * DesktopScreen.MENU_ITEM_H + 6;
            if (my >= shutY && my <= shutY + DesktopScreen.MENU_ITEM_H) {
                desktop.askToPowerOff();
            }
        }
        desktop.closeLauncher();
        return true;
    }

    /** Frames XP: a two-column panel (programs left, system places right) with header and footer bands. */
    void renderXp(final GuiGraphics g, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = DesktopScreen.XP_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        final DesktopTheme theme = desktop.themeColours();
        final Luna c = LUNA.get();
        // Panel with a thin border.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.border());
        g.fill(x, y, x + w, y + h, theme.menuBg());
        // Header band: the player's own face and name over the Luna blue, the way this menu always opened.
        g.fillGradient(x, y, x + w, y + DesktopScreen.XP_HEADER_H, c.bandFrom(), c.bandTo());
        drawPlayerFace(g, x + 4, y + 3, DesktopScreen.XP_HEADER_H - 6);
        g.drawString(desktop.textFont(), playerName(),
                x + 4 + DesktopScreen.XP_HEADER_H - 6 + 5, y + (DesktopScreen.XP_HEADER_H - 8) / 2,
                c.bandInk(), true);
        // The orange rule under the header, lit along its top edge.
        g.fill(x, y + DesktopScreen.XP_HEADER_H, x + w, y + DesktopScreen.XP_HEADER_H + 1, c.orangeTop());
        g.fill(x, y + DesktopScreen.XP_HEADER_H + 1,
                x + w, y + DesktopScreen.XP_HEADER_H + DesktopScreen.XP_ORANGE_H, c.orange());
        // Body: left programs column over white, right places column over a tinted panel.
        final int bodyTop = y + DesktopScreen.XP_HEADER_H + DesktopScreen.XP_ORANGE_H;
        final int bodyBot = y + h - DesktopScreen.XP_FOOTER_H;
        final int split = x + DesktopScreen.XP_LEFT_W;
        g.fill(x, bodyTop, split, bodyBot, c.leftBody());
        g.fill(split, bodyTop, x + w, bodyBot, c.rightBody());
        g.fill(split, bodyTop, split + 1, bodyBot, c.split());
        drawXpLeftColumn(g, x + 3, bodyTop + 3, DesktopScreen.XP_LEFT_W - 6);
        drawXpColumn(g, desktop.xpRight(), split + 3, bodyTop + 3,
                w - DesktopScreen.XP_LEFT_W - 6, c.rightInk(), 0, false);
        drawXpFooter(g, x, y, w, h, bodyBot);
    }

    boolean clickXp(final int mx, final int my, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = DesktopScreen.XP_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = tbY - h;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        final int bodyTop = y + DesktopScreen.XP_HEADER_H + DesktopScreen.XP_ORANGE_H;
        final int bodyBot = y + h - DesktopScreen.XP_FOOTER_H;
        final int split = x + DesktopScreen.XP_LEFT_W;
        if (my >= bodyTop && my < bodyBot) {
            if (clickXpBody(mx, my - (bodyTop + 3), split)) {
                return true;
            }
        } else if (my >= bodyBot) {
            // The footer: log off leaves the machine, turn off asks the power dialog.
            if (mx >= desktop.xpFooterOff(x, w)) {
                desktop.askToPowerOff();
            } else if (mx >= desktop.xpFooterLog(x, w)) {
                desktop.closeLauncher();
                desktop.leaveDesktop();
                return true;
            }
        }
        desktop.closeLauncher();
        return true;
    }

    /** Frames 11: a centered floating panel with a search box, a pinned-app grid, and a footer power button. */
    void render11(final GuiGraphics g, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = DesktopScreen.W11_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = desktop.startMenuTop(tbY);
        final OsSkin skin = desktop.panelSkin();
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
        final List<DesktopScreen.Launcher> filtered = desktop.searchedLaunchers();
        if (desktop.searchText().isEmpty()) {
            drawW11Pinned(g, x, w, contentTop, filtered, panelDim, panelText, panelHover, panelEdge);
        } else {
            drawW11Results(g, x, w, contentTop, filtered, panelDim, panelText, panelHover);
        }
        drawW11Footer(g, x, y, w, h, panelEdge, panelText, panelHover);
    }

    boolean click11(final int mx, final int my, final int tbY) {
        final int x = desktop.startMenuLeft();
        final int w = DesktopScreen.W11_MENU_W;
        final int h = desktop.startMenuTall();
        final int y = desktop.startMenuTop(tbY);
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        // Footer power button (right side): shut the computer down.
        final int footY = y + h - DesktopScreen.W11_FOOTER_H;
        if (my >= footY) {
            if (mx >= x + w - 24) {
                desktop.askToPowerOff();
                desktop.closeLauncher();
            }
            return true; // clicks elsewhere in the footer are absorbed, keeping the menu open
        }
        final int contentTop = y + 6 + DesktopScreen.W11_SEARCH_H + 5;
        final List<DesktopScreen.Launcher> filtered = desktop.searchedLaunchers();
        if (!desktop.searchText().isEmpty()) {
            int ry = contentTop + 11;
            for (final DesktopScreen.Launcher l : filtered) {
                if (my >= ry && my < ry + 15) {
                    desktop.launch(l);
                    desktop.closeLauncher();
                    return true;
                }
                ry += 15;
            }
            return true; // absorb clicks on the search box / empty space
        }
        // Pinned grid tiles.
        final int gridTop = contentTop + 9;
        final int gridX = x + (w - DesktopScreen.W11_COLS * DesktopScreen.W11_TILE_W) / 2;
        if (my >= gridTop && mx >= gridX) {
            final int col = (mx - gridX) / DesktopScreen.W11_TILE_W;
            final int row = (my - gridTop) / DesktopScreen.W11_TILE_H;
            if (col >= 0 && col < DesktopScreen.W11_COLS) {
                final int idx = row * DesktopScreen.W11_COLS + col;
                if (idx >= 0 && idx < filtered.size()) {
                    desktop.launch(filtered.get(idx));
                    desktop.closeLauncher();
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
        final List<DesktopScreen.Launcher> items = desktop.xpLeft();
        final int pinned = Math.min(DesktopScreen.XP_PINNED, items.size());
        drawXpColumn(g, items, colX, colY, colW, desktop.themeColours().menuText(), pinned, true);
        if (items.size() > pinned) {
            final int sepY = colY + pinned * DesktopScreen.XP_ROW_H + DesktopScreen.XP_SEP_H / 2;
            g.fill(colX + 3, sepY, colX + colW - 3, sepY + 1, c.rule());
        }
        final int afterRows = colY + desktop.xpLeftRow(items.size());
        g.fill(colX + 3, afterRows + DesktopScreen.XP_SEP_H / 2,
                colX + colW - 3, afterRows + DesktopScreen.XP_SEP_H / 2 + 1, c.rule());
        final int allY = colY + desktop.xpAllRow();
        if (desktop.hoverIn(colX, allY, colW, DesktopScreen.XP_ALL_ROW_H)) {
            g.fill(colX, allY, colX + colW, allY + DesktopScreen.XP_ALL_ROW_H, c.rowHover());
        }
        g.drawString(desktop.textFont(), GameText.component(DesktopTexts.ALL_PROGRAMS).withStyle(ChatFormatting.BOLD),
                colX + 4, allY + 4, desktop.themeColours().menuText(), false);
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
    private void drawXpColumn(final GuiGraphics g, final List<DesktopScreen.Launcher> items, final int colX,
                              final int colY, final int colW, final int textColor, final int boldCount,
                              final boolean leftColumn) {
        for (int i = 0; i < items.size(); i++) {
            final DesktopScreen.Launcher l = items.get(i);
            final int my = colY + (leftColumn ? desktop.xpLeftRow(i) : i * DesktopScreen.XP_ROW_H);
            if (desktop.hoverIn(colX, my, colW, DesktopScreen.XP_ROW_H)) {
                g.fill(colX, my, colX + colW, my + DesktopScreen.XP_ROW_H, LUNA.get().rowHover());
            }
            ProgramIcons.draw(g, colX + 1, my, 14, 12, l.programId(), desktop.icons());
            final String label = desktop.shorten(l.label(), (colW - 20) / 6);
            if (i < boldCount) {
                g.drawString(desktop.textFont(), Component.literal(label).withStyle(ChatFormatting.BOLD),
                        colX + 18, my + 4, textColor, false);
            } else {
                g.drawString(desktop.textFont(), label, colX + 18, my + 4, textColor, false);
            }
        }
    }

    /** The XP footer band: Log Off and Turn Off Computer, right-aligned, mirroring the header gradient. */
    private void drawXpFooter(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int footY) {
        final Luna c = LUNA.get();
        g.fillGradient(x, footY, x + w, y + h, c.bandFrom(), c.bandTo());
        final int offX = desktop.xpFooterOff(x, w);
        final int logX = desktop.xpFooterLog(x, w);
        final int textY = footY + (DesktopScreen.XP_FOOTER_H - 8) / 2;
        if (desktop.hoverBelowRight(footY, logX, offX - 4)) {
            g.fill(logX - 2, footY + 2, offX - 6, y + h - 2, c.footerHover());
        }
        g.fill(logX, footY + 5, logX + 8, footY + 13, c.logOffMark());
        g.fill(logX + 3, footY + 8, logX + 8, footY + 10, c.markStem());
        g.drawString(desktop.textFont(), GameText.resolve(DesktopTexts.XP_LOG_OFF), logX + 12, textY, c.bandInk(),
                true);
        if (desktop.hoverBelowRight(footY, offX, x + w - 2)) {
            g.fill(offX - 2, footY + 2, x + w - 3, y + h - 2, c.footerHover());
        }
        g.fill(offX, footY + 5, offX + 8, footY + 13, c.turnOffMark());
        g.fill(offX + 3, footY + 3, offX + 5, footY + 9, c.markStem());
        g.drawString(desktop.textFont(), GameText.resolve(DesktopTexts.TURN_OFF_COMPUTER), offX + 12, textY,
                c.bandInk(), true);
    }

    /** A click inside the XP menu's two columns; false when it landed on neither a row nor All Programs. */
    private boolean clickXpBody(final int mx, final int dy, final int split) {
        if (mx < split) {
            // The left column's rows are spaced around a separator, so they are walked, not divided.
            final List<DesktopScreen.Launcher> col = desktop.xpLeft();
            for (int i = 0; i < col.size(); i++) {
                final int ry = desktop.xpLeftRow(i);
                if (dy >= ry && dy < ry + DesktopScreen.XP_ROW_H) {
                    desktop.launch(col.get(i));
                    desktop.closeLauncher();
                    return true;
                }
            }
            final int allY = desktop.xpAllRow();
            if (dy >= allY && dy < allY + DesktopScreen.XP_ALL_ROW_H) {
                desktop.openEverythingInstalled();
            }
            return false;
        }
        final List<DesktopScreen.Launcher> col = desktop.xpRight();
        final int row = dy / DesktopScreen.XP_ROW_H;
        if (row >= 0 && row < col.size()) {
            desktop.launch(col.get(row));
        }
        return false;
    }

    /** The Frames 11 search field, with its magnifier; gives back where the content under it starts. */
    private int drawW11Search(final GuiGraphics g, final int x, final int y, final int w, final int panelEdge,
                              final int panelDim, final int panelText, final OsSkin skin) {
        final int fieldX = x + 6;
        final int fieldW = w - 12;
        final int fieldY = y + 6;
        g.fill(fieldX, fieldY, fieldX + fieldW, fieldY + DesktopScreen.W11_SEARCH_H, skin.fieldBg());
        desktop.drawOutline(g, fieldX, fieldY, fieldW, DesktopScreen.W11_SEARCH_H, panelEdge);
        // Magnifier glyph.
        desktop.drawOutline(g, fieldX + 4, fieldY + 3, 5, 5, panelDim);
        g.fill(fieldX + 8, fieldY + 7, fieldX + 10, fieldY + 9, panelDim);
        final String q = desktop.searchText();
        if (q.isEmpty()) {
            g.drawString(desktop.textFont(), GameText.resolve(DesktopTexts.SEARCH_HINT), fieldX + 13, fieldY + 3,
                    panelDim, false);
        } else {
            g.drawString(desktop.textFont(), desktop.shorten(q, (fieldW - 16) / 6),
                    fieldX + 13, fieldY + 3, panelText, false);
        }
        return fieldY + DesktopScreen.W11_SEARCH_H + 5;
    }

    private void drawW11Pinned(final GuiGraphics g, final int x, final int w, final int contentTop,
                               final List<DesktopScreen.Launcher> filtered, final int panelDim,
                               final int panelText, final int panelHover, final int panelEdge) {
        g.drawString(desktop.textFont(), GameText.resolve(DesktopTexts.PINNED), x + 8, contentTop, panelDim, false);
        final int gridTop = contentTop + 9;
        final int gridX = x + (w - DesktopScreen.W11_COLS * DesktopScreen.W11_TILE_W) / 2;
        for (int i = 0; i < filtered.size(); i++) {
            final int col = i % DesktopScreen.W11_COLS;
            final int row = i / DesktopScreen.W11_COLS;
            drawW11Tile(g, filtered.get(i), gridX + col * DesktopScreen.W11_TILE_W,
                    gridTop + row * DesktopScreen.W11_TILE_H, panelText, panelHover, panelEdge);
        }
    }

    private void drawW11Results(final GuiGraphics g, final int x, final int w, final int contentTop,
                                final List<DesktopScreen.Launcher> filtered, final int panelDim,
                                final int panelText, final int panelHover) {
        g.drawString(desktop.textFont(),
                GameText.resolve(filtered.isEmpty() ? DesktopTexts.NO_RESULTS : DesktopTexts.BEST_MATCH),
                x + 8, contentTop, panelDim, false);
        int my = contentTop + 11;
        final int rowW = w - 12;
        for (final DesktopScreen.Launcher l : filtered) {
            if (desktop.hoverIn(x + 6, my, rowW, 15)) {
                g.fill(x + 6, my, x + 6 + rowW, my + 15, panelHover);
            }
            ProgramIcons.draw(g, x + 8, my + 1, 13, 12, l.programId(), desktop.icons());
            g.drawString(desktop.textFont(), l.label(), x + 24, my + 4, panelText, false);
            my += 15;
        }
    }

    /** The Frames 11 footer: a separator, the account on the left, the power button on the right. */
    private void drawW11Footer(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int panelEdge, final int panelText, final int panelHover) {
        final int footY = y + h - DesktopScreen.W11_FOOTER_H;
        g.fill(x + 8, footY, x + w - 8, footY + 1, panelEdge);
        g.drawString(desktop.textFont(), desktop.accountLabel(),
                x + 12, footY + (DesktopScreen.W11_FOOTER_H - 8) / 2, panelText, false);
        final int pwX = x + w - 22;
        final int pwY = footY + (DesktopScreen.W11_FOOTER_H - 12) / 2;
        if (desktop.hoverBelowRight(footY, pwX - 2, pwX + 14)) {
            g.fill(pwX - 3, footY + 2, pwX + 15, footY + DesktopScreen.W11_FOOTER_H - 2, panelHover);
        }
        desktop.drawOutline(g, pwX, pwY, 12, 12, panelText);
        g.fill(pwX + 5, pwY - 1, pwX + 7, pwY + 6, panelText); // power stem
    }

    /** Draws one Frames 11 pinned tile: an icon over a centered label, with a hover background. */
    private void drawW11Tile(final GuiGraphics g, final DesktopScreen.Launcher l, final int tx, final int ty,
                             final int labelColor, final int hoverBg, final int hoverEdge) {
        if (desktop.hoverIn(tx, ty, DesktopScreen.W11_TILE_W, DesktopScreen.W11_TILE_H)) {
            g.fill(tx + 1, ty + 1, tx + DesktopScreen.W11_TILE_W - 1, ty + DesktopScreen.W11_TILE_H - 1, hoverBg);
            desktop.drawOutline(g, tx + 1, ty + 1,
                    DesktopScreen.W11_TILE_W - 2, DesktopScreen.W11_TILE_H - 2, hoverEdge);
        }
        ProgramIcons.draw(g, tx + (DesktopScreen.W11_TILE_W - 16) / 2, ty + 3, 16, 14,
                l.programId(), desktop.icons());
        // Truncate the label to the tile width by dropping characters (no ellipsis, which would be wider).
        String label = l.label();
        while (label.length() > 3 && desktop.textFont().width(label) > DesktopScreen.W11_TILE_W - 2) {
            label = label.substring(0, label.length() - 1);
        }
        g.drawString(desktop.textFont(), label,
                tx + (DesktopScreen.W11_TILE_W - desktop.textFont().width(label)) / 2, ty + 20, labelColor, false);
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
