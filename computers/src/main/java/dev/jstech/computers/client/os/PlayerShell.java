/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.PlayerPanelLayout;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.RunProgramPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The panel and the menu of a desktop a player wrote (computers/docs/design/player-systems.md): one bar along the top
 * or the foot with the picture and the name of the desktop at its left and the clock at its right, and, opened from
 * the name, the menu of its programs, the mod's terminal and settings after them, and Shut down under a rule. The
 * programs are the desktop's own, built in Σ#, and each runs from its package's folder.
 */
@TextHolder
final class PlayerShell {

    private final DesktopState desktop;
    private boolean open;
    /* What the menu lists this frame, which a click acts on: the desktop's programs, then the mod's two. */
    private List<Row> rows = List.of();

    private static final TextKey SHUT_DOWN = TextKey.of("jsc.desktop.player.shut_down", "Shut down");

    PlayerShell(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Whether the menu is down. */
    boolean menuOpen() {
        return this.open;
    }

    /** Puts the menu away. */
    void close() {
        this.open = false;
    }

    /** The panel, along the edge the desktop puts it on. */
    void render(final GuiGraphics g, final int sw, final int sh, final int lmx, final int lmy) {
        final PlayerDesktop player = this.desktop.player();
        if (player == null) {
            return;
        }
        final PlayerLooks.Colours c = PlayerLooks.colours(player.facts());
        final Font font = this.desktop.textFont();
        final int barY = barY(player, sh);
        final int barH = DesktopScreen.TASKBAR_H;
        g.fill(0, barY, sw, barY + barH, c.panel());
        // The rule that parts the panel from the desktop, on whichever side the desktop is.
        final int ruleY = top(player) ? barY + barH - 1 : barY;
        g.fill(0, ruleY, sw, ruleY + 1, c.accent());
        final int textY = barY + (barH - font.lineHeight) / 2 + 1;
        final int iconY = barY + (barH - PlayerPanelLayout.ICON) / 2;
        final Optional<PixImage> menuIcon = SigmaImages.get(this.desktop.hostPos(), player.menuIconPath());
        menuIcon.ifPresent(image -> picture(g, image, PlayerPanelLayout.PAD_X, iconY, PlayerPanelLayout.ICON));
        final int nameX = PlayerPanelLayout.PAD_X + (menuIcon.isPresent()
                ? PlayerPanelLayout.ICON + PlayerPanelLayout.ICON_GAP : 0);
        final boolean lit = this.open || onName(player, font, lmx, lmy, sh);
        Draw.text(g, font, player.facts().menu(), nameX, textY, lit ? c.title() : c.accent());
        final String clock = this.desktop.prefs().clockText();
        Draw.text(g, font, clock, sw - PlayerPanelLayout.PAD_X - font.width(clock), textY, c.text());
    }

    /** The menu, when it is down, over everything on the desktop. */
    void renderMenu(final GuiGraphics g, final int sh, final int lmx, final int lmy) {
        final PlayerDesktop player = this.desktop.player();
        if (!this.open || player == null) {
            return;
        }
        final PlayerLooks.Colours c = PlayerLooks.colours(player.facts());
        final Font font = this.desktop.textFont();
        this.rows = rowsOf(player);
        final int menuW = menuWidth(font);
        final int menuH = PlayerPanelLayout.menuHeight(this.rows.size());
        final int menuX = PlayerPanelLayout.MENU_X;
        final int menuY = PlayerPanelLayout.menuY(top(player), DesktopScreen.TASKBAR_H, sh, menuH);
        g.fill(menuX, menuY, menuX + menuW, menuY + menuH, c.accent());
        g.fill(menuX + 1, menuY + 1, menuX + menuW - 1, menuY + menuH - 1, c.menu());
        final int hover = lmx >= menuX && lmx < menuX + menuW
                ? PlayerPanelLayout.rowAt(menuY, this.rows.size(), lmy) : -1;
        for (int i = 0; i <= this.rows.size(); i++) {
            final boolean shutDown = i == this.rows.size();
            final int rowY = shutDown ? PlayerPanelLayout.shutDownY(menuY, this.rows.size())
                    : PlayerPanelLayout.rowY(menuY, i);
            if (i == hover) {
                g.fill(menuX + 2, rowY, menuX + menuW - 2, rowY + PlayerPanelLayout.ROW_H, c.lit());
            }
            final Row row = shutDown ? null : this.rows.get(i);
            final Optional<PixImage> icon = row == null || row.icon().isEmpty() ? Optional.empty()
                    : SigmaImages.get(this.desktop.hostPos(), row.icon());
            final int markX = menuX + PlayerPanelLayout.MARK_X;
            if (icon.isPresent()) {
                picture(g, icon.get(), markX, rowY + (PlayerPanelLayout.ROW_H - PlayerPanelLayout.PICTURE) / 2,
                        PlayerPanelLayout.PICTURE);
            } else {
                final int markY = rowY + (PlayerPanelLayout.ROW_H - PlayerPanelLayout.MARK) / 2;
                final int mark = shutDown ? c.close() : row.window() != null ? c.quiet() : c.accent();
                g.fill(markX, markY, markX + PlayerPanelLayout.MARK, markY + PlayerPanelLayout.MARK, mark);
            }
            final String words = shutDown ? GameText.resolve(SHUT_DOWN) : row.label();
            Draw.text(g, font, words, menuX + PlayerPanelLayout.TEXT_X,
                    rowY + (PlayerPanelLayout.ROW_H - font.lineHeight) / 2 + 1, c.text());
        }
        final int ruleY = PlayerPanelLayout.rowY(menuY, this.rows.size()) + PlayerPanelLayout.RULE_H / 2;
        g.fill(menuX + PlayerPanelLayout.MARK_X, ruleY, menuX + menuW - PlayerPanelLayout.MARK_X, ruleY + 1,
                c.quiet());
    }

    /**
     * A click at a desktop-local point. While the menu is down it takes the click wherever it lands, running the row
     * it is on and putting the menu away; with the menu up, the name opens it and the rest of the panel swallows the
     * click. Answers whether the click was taken.
     */
    boolean click(final double x, final double y, final int sh) {
        final PlayerDesktop player = this.desktop.player();
        if (player == null) {
            return false;
        }
        if (this.open) {
            this.open = false;
            final Font font = this.desktop.textFont();
            final int menuH = PlayerPanelLayout.menuHeight(this.rows.size());
            final int menuY = PlayerPanelLayout.menuY(top(player), DesktopScreen.TASKBAR_H, sh, menuH);
            if (x >= PlayerPanelLayout.MENU_X && x < PlayerPanelLayout.MENU_X + menuWidth(font)) {
                this.act(player, PlayerPanelLayout.rowAt(menuY, this.rows.size(), (int) y));
            }
            return true;
        }
        if (onName(player, this.desktop.textFont(), x, y, sh)) {
            this.rows = rowsOf(player);
            this.open = true;
            return true;
        }
        final int barY = barY(player, sh);
        return y >= barY && y < barY + DesktopScreen.TASKBAR_H;
    }

    /* Runs the row picked: a program of the desktop's, one of the mod's two, or Shut down. */
    private void act(final PlayerDesktop player, final int index) {
        if (index < 0) {
            return;
        }
        if (index == this.rows.size()) {
            this.desktop.cyclePower(MachinePowerPayload.ACTION_SHUTDOWN);
            return;
        }
        final Row row = this.rows.get(index);
        if (row.window() != null) {
            this.desktop.wm().restoreGroup(row.window().groupKey());
        } else if (row.launcher() != null) {
            this.desktop.opener().run(row.launcher());
        } else {
            PacketDistributor.sendToServer(new RunProgramPayload(this.desktop.hostPos(), row.listing()));
        }
    }

    /* The desktop's programs by the names it gave them, then whichever of the mod's two this machine has. */
    private List<Row> rowsOf(final PlayerDesktop player) {
        final List<Row> out = new ArrayList<>();
        for (final String program : player.facts().programs()) {
            out.add(new Row(named(program), player.programPath(program), player.iconPath(program), null, null));
        }
        final DesktopEnvironmentDef def = player.def();
        for (final ResourceLocation id : def.bundledPrograms()) {
            final Launcher launcher = this.desktop.catalogue().byProgram(id);
            if (launcher != null) {
                out.add(new Row(GameText.resolve(def.nativeNames().get(id)), "", "", launcher, null));
            }
        }
        // The panel lists no windows, so one put away is listed here, and picking it brings it back.
        for (final DesktopWindow window : this.desktop.wm().all()) {
            if (window.minimized() && !window.dialog()) {
                out.add(new Row(window.titleOn(def), "", "", null, window));
            }
        }
        return out;
    }

    /* How wide the menu is for the words it holds now. */
    private int menuWidth(final Font font) {
        int widest = font.width(GameText.resolve(SHUT_DOWN));
        for (final Row row : this.rows) {
            widest = Math.max(widest, font.width(row.label()));
        }
        return PlayerPanelLayout.menuWidth(widest);
    }

    /* Whether a point is on the panel's picture and name, which open the menu. */
    private boolean onName(final PlayerDesktop player, final Font font, final double x, final double y,
                           final int sh) {
        final int barY = barY(player, sh);
        final int right = PlayerPanelLayout.PAD_X + PlayerPanelLayout.ICON + PlayerPanelLayout.ICON_GAP
                + font.width(player.facts().menu());
        return y >= barY && y < barY + DesktopScreen.TASKBAR_H && x >= 0 && x < right;
    }

    private static boolean top(final PlayerDesktop player) {
        return "top".equals(player.facts().panelAt());
    }

    private static int barY(final PlayerDesktop player, final int sh) {
        return top(player) ? 0 : sh - DesktopScreen.TASKBAR_H;
    }

    /* A program's name as its menu shows it: "notes" is Notes. */
    private static String named(final String program) {
        return program.isEmpty() ? program
                : program.substring(0, 1).toUpperCase(Locale.ROOT) + program.substring(1);
    }

    /*
     * A picture drawn into a square {@code size} wide, one sample of it for each pixel of the square, so a larger
     * picture than the square is shown whole and a smaller one grows by whole pixels.
     */
    private static void picture(final GuiGraphics g, final PixImage image, final int x, final int y, final int size) {
        if (image.width() <= size && image.height() <= size) {
            SigmaImages.paint(g, image, x, y, size, size);
            return;
        }
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                final int index = image.get(column * image.width() / size, row * image.height() / size);
                if (!PixImage.isTransparent(index)) {
                    g.fill(x + column, y + row, x + column + 1, y + row + 1, PixImage.colourOf(index));
                }
            }
        }
    }

    /**
     * One row of the menu.
     *
     * @param label    what it is called
     * @param listing  where the desktop's program it runs stands, or empty for one of the mod's
     * @param icon     where that program's picture stands, or empty
     * @param launcher the mod's program it opens, or null for one of the desktop's
     * @param window   the window put away that it brings back, or null for a program
     */
    private record Row(String label, String listing, String icon, @Nullable Launcher launcher,
                       @Nullable DesktopWindow window) {
    }
}
