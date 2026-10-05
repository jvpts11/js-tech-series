/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.motion.FadeLayer;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionStyles;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The launcher a desktop opens from its panel: the Start menu of each Frames edition, the Linux desktops' Kickoff,
 * Mint menu and Activities overview, and the plain list a period desktop had. Each is drawn and hit-tested by the
 * family it belongs to; this holds what they share: whether it is open, the search typed into it, where it stands
 * and how big it is, and what choosing an entry does. The left button starts the program; the right one opens that
 * program's own menu, the one its icon on the wallpaper has, so a program listed anywhere answers it the same way.
 */
final class StartMenus {

    private final DesktopState desktop;
    /** The Frames systems' own: the classic Start menu, XP's two columns, Frames 11's floating panel. */
    private final FramesLaunchers frames;
    /** The Linux desktops' own ways of opening a program: Kickoff, the Mint menu, the Activities overview. */
    private final LinuxLaunchers linux;
    /** Frames 7's two columns in glass, and Frames 10's list and tiles. */
    private final AeroStartMenu aero;
    private final MetroStartMenu metro;
    private boolean open;
    /** What has been typed into the launcher's search box; while it is not empty it filters what is listed. */
    private final StringBuilder search = new StringBuilder();
    /*
     * Which button chose an entry, and where the cursor was. Every launcher lays its entries out differently and each
     * already does that arithmetic, so rather than a second hit test that would have to match all of them, they say
     * which entry was hit and this decides what to do with it.
     */
    private boolean rightButton;
    private int clickX;
    private int clickY;
    /** How the launcher is coming up: sliding out of the panel or growing, as its system shows one. */
    private Motion motion = Motion.FINISHED;

    /** The longest search the box takes. */
    private static final int SEARCH_MAX = 24;
    /** The programs the Frames XP Start menu lists in its right column, the system's own places. */
    private static final Set<ResourceLocation> XP_PLACES = Set.of(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "this_pc"), Programs.FILES, Programs.SETTINGS,
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "network"));

    StartMenus(final DesktopState desktop) {
        this.desktop = desktop;
        this.frames = new FramesLaunchers(desktop);
        this.linux = new LinuxLaunchers(desktop);
        this.aero = new AeroStartMenu(desktop);
        this.metro = new MetroStartMenu(desktop);
    }

    /** Frames 10's Start, whose tiles the machine keeps. */
    MetroStartMenu metro() {
        return metro;
    }

    /** Frames 7's Start. */
    AeroStartMenu aero() {
        return aero;
    }

    /** Takes the tiles of Frames 10's Start, as the machine keeps them. */
    void takeTiles(final List<String> encoded) {
        metro.takeTiles(encoded);
    }

    /** The cursor moved with the button down over an open launcher: Frames 10's tiles are dragged so. */
    boolean dragged(final double mx, final double my) {
        return open && desktop.panelStyle() == PanelStyle.FRAMES_10 && metro.dragged(mx, my);
    }

    /** The button came up over an open launcher: a tile pressed starts its program, a dragged one is placed. */
    boolean released(final double mx, final double my) {
        return open && desktop.panelStyle() == PanelStyle.FRAMES_10 && metro.released(mx, my);
    }

    /** The wheel over an open launcher's list. */
    boolean scrolled(final double mx, final double my, final double dy) {
        return open && desktop.panelStyle() == PanelStyle.FRAMES_10 && metro.scrolled(mx, my, dy);
    }

    boolean isOpen() {
        return open;
    }

    /** Whether the launcher is open and still on its way in. */
    boolean moving() {
        return open && !motion.done(DesktopMotion.now());
    }

    /** Opens the launcher, or closes it when it is open; it always opens with an empty search box. */
    void toggle() {
        // GNOME 2 has no launcher of this kind: what opens its programs is its Applications menu along the top.
        if (desktop.gnome2()) {
            desktop.transitionPanels().openApplications();
            return;
        }
        if (open) {
            close();
        } else {
            open = true;
            search.setLength(0);
            metro.reset();
            desktop.kickoff4().reset();
            motion = desktop.motion().start(MotionKinds.MENU_SHOW);
        }
    }

    /** Closes the launcher and clears its search, so it opens fresh next time. */
    void close() {
        open = false;
        search.setLength(0);
    }

    /** What has been typed into the search box, empty when nothing has. */
    String searchText() {
        return search.toString();
    }

    /** The programs whose names hold what was typed, or all of them when nothing was. */
    List<Launcher> filtered() {
        final List<Launcher> all = desktop.launcherList();
        final String q = search.toString().toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return all;
        }
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : all) {
            if (l.label().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(l);
            }
        }
        return out;
    }

    /** The launcher's width, which each edition decides. */
    int width() {
        if (desktop.periodPanel()) {
            return FramesLaunchers.MENU_W; // the period launcher is a narrow list, whatever its desktop does today
        }
        if (desktop.kde4()) {
            return Kickoff4Menu.W;
        }
        return switch (desktop.panelStyle()) {
            case FRAMES_XP -> FramesLaunchers.XP_MENU_W;
            case FRAMES_7 -> AeroStartMenu.MENU_W;
            case FRAMES_10 -> MetroStartMenu.MENU_W;
            case FRAMES_11 -> FramesLaunchers.W11_MENU_W;
            case KDE -> LinuxLaunchers.KDE_MENU_W;
            case GNOME -> desktop.view().width();
            case CINNAMON -> LinuxLaunchers.CIN_MENU_W;
            default -> FramesLaunchers.MENU_W;
        };
    }

    /** The launcher's height, which what it lists decides. */
    int height() {
        final int count = desktop.launcherList().size();
        if (desktop.periodPanel()) {
            /*
             * A period launcher is a small program list, not a Plasma menu and not a full-screen overview: it is
             * sized by its own contents, like the classic launcher it is.
             */
            return Math.max(4, count) * FramesLaunchers.MENU_ITEM_H + 8;
        }
        if (desktop.kde4()) {
            return Kickoff4Menu.H;
        }
        return switch (desktop.panelStyle()) {
            case KDE -> LinuxLaunchers.KDE_HEADER_H + Math.max(6, count) * LinuxLaunchers.KDE_ROW_H
                    + LinuxLaunchers.KDE_FOOTER_H + 8;
            // The overview covers the whole desktop below the top bar.
            case GNOME -> desktop.view().height() - DesktopScreen.TASKBAR_H;
            case CINNAMON -> LinuxLaunchers.CIN_HEADER_H + Math.max(5, count) * LinuxLaunchers.CIN_ROW_H + 10;
            case FRAMES_7 -> aero.height();
            case FRAMES_10 -> metro.height();
            // Two columns: the taller of the programs (left) and the places (right) sets the body's height.
            case FRAMES_XP -> FramesLaunchers.XP_HEADER_H + FramesLaunchers.XP_ORANGE_H
                    + Math.max(xpLeftColumnHeight(), xpRight().size() * FramesLaunchers.XP_ROW_H)
                    + FramesLaunchers.XP_FOOTER_H + 6;
            /*
             * A pinned grid sized to every program, so the panel does not resize as the search filters it: a pad,
             * the search box, the "Pinned" label, the rows, the footer and a pad under it.
             */
            case FRAMES_11 -> {
                final int rows = Math.max(1, (count + FramesLaunchers.W11_COLS - 1) / FramesLaunchers.W11_COLS);
                yield 6 + FramesLaunchers.W11_SEARCH_H + 5 + 9 + rows * FramesLaunchers.W11_TILE_H + 5
                        + FramesLaunchers.W11_FOOTER_H + 5;
            }
            default -> count * FramesLaunchers.MENU_ITEM_H + 6 + FramesLaunchers.MENU_ITEM_H + 8;
        };
    }

    /** The launcher's left edge: at the corner, except Frames 11's, which opens over its Start button, clamped. */
    int left() {
        final DesktopViewport view = desktop.view();
        if (desktop.panelStyle() == PanelStyle.FRAMES_11) {
            final int w = width();
            final int startCenter = desktop.taskbar().modernStartLeft(view.width()) + DesktopScreen.WIN11_SLOT / 2;
            return Math.max(4, Math.min(view.width() - w - 4, startCenter - w / 2));
        }
        if (view.panelOnTop()) {
            return 0; // the Activities overview spans the desktop
        }
        // Frames 7's and 10's menus stand flush in the corner, over their Start buttons.
        if (desktop.panelStyle() == PanelStyle.FRAMES_7 || desktop.panelStyle() == PanelStyle.FRAMES_10) {
            return 0;
        }
        return 4;
    }

    /** The launcher's top edge over a panel whose top is {@code tbY}: flush with it, but floating on Frames 11. */
    int top(final int tbY) {
        if (desktop.panelStyle() == PanelStyle.FRAMES_11) {
            return Math.max(2, tbY - height() - 6); // it floats above the taskbar, but never off the top
        }
        if (desktop.view().panelOnTop()) {
            return DesktopScreen.TASKBAR_H; // the overview hangs below the top bar
        }
        return tbY - height();
    }

    /** The programs in the Frames XP menu's left column: everything that is not one of the system's places. */
    List<Launcher> xpLeft() {
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : desktop.launcherList()) {
            if (!XP_PLACES.contains(l.programId())) {
                out.add(l);
            }
        }
        return out;
    }

    /** The system's places in the Frames XP menu's right column, in launcher order. */
    List<Launcher> xpRight() {
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : desktop.launcherList()) {
            if (XP_PLACES.contains(l.programId())) {
                out.add(l);
            }
        }
        return out;
    }

    /** Where the left column's row {@code i} sits below the top of the menu's body, leaving its separator's gap. */
    int xpLeftRow(final int i) {
        final int n = xpLeft().size();
        final int pinned = Math.min(FramesLaunchers.XP_PINNED, n);
        return i * FramesLaunchers.XP_ROW_H + (i >= pinned && n > pinned ? FramesLaunchers.XP_SEP_H : 0);
    }

    /** Where the "All Programs" row sits below the top of the menu's body. */
    int xpAllRow() {
        return xpLeftRow(xpLeft().size()) + FramesLaunchers.XP_SEP_H;
    }

    /** The left edge of the footer's "Turn Off Computer" entry, shared by the drawing and the hit test. */
    int xpFooterOff(final int x, final int w) {
        return x + w - 6 - (desktop.textFont().width(GameText.resolve(DesktopTexts.TURN_OFF_COMPUTER)) + 14);
    }

    /** The left edge of the footer's "Log Off" entry, just before the Turn Off one. */
    int xpFooterLog(final int x, final int w) {
        return xpFooterOff(x, w) - 8 - (desktop.textFont().width(GameText.resolve(DesktopTexts.XP_LOG_OFF)) + 14);
    }

    /** Chooses the program at {@code index} in the launcher list. */
    void choose(final int index) {
        final List<Launcher> all = desktop.launcherList();
        if (index >= 0 && index < all.size()) {
            choose(all.get(index));
        }
    }

    /** Chooses a program: the left button starts it, the right one opens its own menu where the click was. */
    void choose(final Launcher launcher) {
        if (rightButton) {
            close();
            // The menu is the icon's, so it is found among the icons; the same launcher stands in both lists.
            desktop.deskMenu().openFor(desktop.deskIcons().indexOf(launcher), clickX, clickY);
            return;
        }
        desktop.opener().run(launcher);
    }

    /** "All Programs": the page that lists everything installed on this machine, services included. */
    void openAllPrograms() {
        for (final Launcher l : desktop.launcherList()) {
            if (Programs.SETTINGS.equals(l.programId())) {
                desktop.opener().run(l);
                return;
            }
        }
    }

    /**
     * Draws the open launcher over a panel whose top is {@code tbY}. Each edition has its own layout, not just its
     * own palette, and a period desktop had a plain vertical list, not a modern Plasma menu and certainly not the
     * GNOME overview, which belongs to a shell released a decade later.
     */
    void render(final GuiGraphics g, final int tbY) {
        if (!open) {
            return;
        }
        final double now = DesktopMotion.now();
        if (motion.done(now)) {
            renderLauncher(g, tbY);
            return;
        }
        /*
         * Coming up, it slides out from behind the panel, so it is cut off at the panel's edge and seems to rise out
         * of it; or it grows where it stands. Either way it is the whole launcher, drawn moved.
         */
        final DesktopViewport view = desktop.view();
        final boolean onTop = view.panelOnTop();
        if (onTop) {
            Draw.pushScissor(g, 0, DesktopScreen.TASKBAR_H, view.width(), view.height());
        } else {
            Draw.pushScissor(g, 0, 0, view.width(), tbY);
        }
        g.pose().pushPose();
        if (motion.is(MotionStyles.SLIDE)) {
            final float away = (float) (motion.offset(now) * height());
            g.pose().translate(0, onTop ? -away : away, 0);
        } else {
            DesktopMotion.pose(g, motion, now, left(), top(tbY), width(), height(), 0, 0, 0, 0);
        }
        // A launcher that fades in (Frames XP's, or one that slides and fades) is laid down as one picture.
        if (motion.fades()) {
            FadeLayer.draw(g, (float) motion.opacity(now), () -> renderLauncher(g, tbY));
        } else {
            renderLauncher(g, tbY);
        }
        g.pose().popPose();
        Draw.popScissor(g);
    }

    /** Draws the launcher of this desktop's family where it rests. */
    private void renderLauncher(final GuiGraphics g, final int tbY) {
        if (desktop.periodPanel()) {
            frames.renderPeriod(g, tbY);
            return;
        }
        if (desktop.kde4()) {
            desktop.kickoff4().render(g, tbY);
            return;
        }
        switch (desktop.panelStyle()) {
            case FRAMES_XP -> frames.renderXp(g, tbY);
            case FRAMES_7 -> aero.render(g, tbY);
            case FRAMES_10 -> metro.render(g, tbY);
            case FRAMES_11 -> frames.render11(g, tbY);
            case KDE -> linux.renderKde(g, tbY);
            case GNOME -> linux.renderGnomeOverview(g);
            case CINNAMON -> linux.renderCinnamon(g, tbY);
            default -> frames.render95(g, tbY);
        }
    }

    /**
     * A click at a desktop-local point while the launcher is open. Returns whether it landed on the launcher, which
     * acted on it or absorbed it; a click anywhere else closes the launcher and goes on to whatever is under it.
     */
    boolean click(final double mouseX, final double mouseY, final int button, final int tbY) {
        if (!open) {
            return false;
        }
        rightButton = button == 1;
        clickX = (int) mouseX;
        clickY = (int) mouseY;
        final boolean handled = hit(clickX, clickY, tbY);
        rightButton = false;
        if (!handled) {
            close();
        }
        return handled;
    }

    /** A character typed while the launcher is open: its search box takes it, when it has one and there is room. */
    boolean type(final char c) {
        if (open && searchable() && c >= 32 && c != 127 && search.length() < SEARCH_MAX) {
            search.append(c);
            return true;
        }
        return false;
    }

    /**
     * A key while the launcher is open. Escape closes it; a launcher with a search box takes Backspace to edit it and
     * Enter to start the first result, and swallows every other key, so a window behind it cannot eat the letters
     * and the inventory key cannot close the desktop. The characters themselves arrive separately, as typed.
     */
    boolean keyPressed(final int key) {
        if (!open) {
            return false;
        }
        if (key == 256) {
            close();
            return true;
        }
        if (!searchable()) {
            return false;
        }
        if (key == 259) {
            if (!search.isEmpty()) {
                search.deleteCharAt(search.length() - 1);
            }
        } else if ((key == 257 || key == 335) && !search.isEmpty()) {
            final List<Launcher> hits = filtered();
            if (!hits.isEmpty()) {
                desktop.opener().run(hits.get(0));
                close();
            }
        }
        return true;
    }

    /**
     * The desktop-local x of the middle of the launcher's entry {@code index}, where a test clicks it. Frames XP lays
     * its entries in two columns, so there the column depends on the entry.
     */
    int itemX(final int index) {
        final List<Launcher> all = desktop.launcherList();
        final int[] later = laterPoint(index);
        if (later != null) {
            return later[0];
        }
        if (desktop.panelStyle() == PanelStyle.FRAMES_XP && index >= 0 && index < all.size()) {
            final boolean place = XP_PLACES.contains(all.get(index).programId());
            final int colX = left() + (place ? FramesLaunchers.XP_LEFT_W + 3 : 3);
            final int colW = place ? FramesLaunchers.XP_MENU_W - FramesLaunchers.XP_LEFT_W - 6
                    : FramesLaunchers.XP_LEFT_W - 6;
            return colX + colW / 2;
        }
        return left() + FramesLaunchers.BAND_W + 30;
    }

    /** The desktop-local y of the middle of the launcher's entry {@code index}, where a test clicks it. */
    int itemY(final int index) {
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        final List<Launcher> all = desktop.launcherList();
        final int[] later = laterPoint(index);
        if (later != null) {
            return later[1];
        }
        if (desktop.panelStyle() == PanelStyle.FRAMES_XP && index >= 0 && index < all.size()) {
            final Launcher target = all.get(index);
            final boolean place = XP_PLACES.contains(target.programId());
            final List<Launcher> column = place ? xpRight() : xpLeft();
            final int row = Math.max(0, column.indexOf(target));
            // The left column has a separator under its pinned block, so its rows are not a plain multiple.
            final int rowY = place ? row * FramesLaunchers.XP_ROW_H : xpLeftRow(row);
            return tbY - height() + FramesLaunchers.XP_HEADER_H + FramesLaunchers.XP_ORANGE_H + 3 + rowY
                    + FramesLaunchers.XP_ROW_H / 2;
        }
        return tbY - height() + 4 + index * FramesLaunchers.MENU_ITEM_H + FramesLaunchers.MENU_ITEM_H / 2;
    }

    /** Whether the launcher has a live search box: Frames 11's Start and GNOME's Activities overview do. */
    private boolean searchable() {
        // A period launcher is a plain list with no search box, even on the GNOME whose modern shell has one.
        final PanelStyle style = desktop.panelStyle();
        return style == PanelStyle.FRAMES_11 || style == PanelStyle.FRAMES_7 || style == PanelStyle.FRAMES_10
                || desktop.kde4() || (style == PanelStyle.GNOME && !desktop.periodPanel() && !desktop.gnome2());
    }

    /**
     * Where Frames 7's and Frames 10's menus list entry {@code index}: the row of its column on 7; on 10 its row in the
     * list, or its tile when the list has scrolled it away. Null on the other menus, or for no such entry.
     */
    private int[] laterPoint(final int index) {
        final List<Launcher> all = desktop.launcherList();
        if (index < 0 || index >= all.size()) {
            return null;
        }
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        if (desktop.kde4()) {
            return desktop.kickoff4().rowPoint(all.get(index).label(), tbY);
        }
        return switch (desktop.panelStyle()) {
            case FRAMES_7 -> aero.pointOf(all.get(index), tbY);
            case FRAMES_10 -> {
                final int[] row = metro.listPoint(all.get(index));
                yield row != null ? row : metro.tilePoint(all.get(index).programId().getPath());
            }
            default -> null;
        };
    }

    /** How tall the XP left column runs: its rows, its separator, and the "All Programs" row under them. */
    private int xpLeftColumnHeight() {
        return xpAllRow() + FramesLaunchers.XP_ALL_ROW_H;
    }

    /** Hands a click to the launcher of this desktop's family, which knows where its entries are. */
    private boolean hit(final int mx, final int my, final int tbY) {
        if (desktop.periodPanel()) {
            return frames.clickPeriod(mx, my, tbY);
        }
        if (desktop.kde4()) {
            return desktop.kickoff4().click(mx, my, tbY);
        }
        return switch (desktop.panelStyle()) {
            case FRAMES_XP -> frames.clickXp(mx, my, tbY);
            case FRAMES_7 -> aero.click(mx, my, tbY);
            case FRAMES_10 -> metro.click(mx, my, rightButton ? 1 : 0, tbY);
            case FRAMES_11 -> frames.click11(mx, my, tbY);
            case KDE -> linux.clickKde(mx, my, tbY);
            case GNOME -> linux.clickGnomeOverview(mx, my);
            case CINNAMON -> linux.clickCinnamon(mx, my, tbY);
            default -> frames.click95(mx, my, tbY);
        };
    }
}
