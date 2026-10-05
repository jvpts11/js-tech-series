/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * KDE 4's Kickoff: a pale menu with the account and the search along its top, a list in the middle, and five tabs along
 * its foot. Favorites holds the everyday programs, Applications every program in the order of its name, Computer the
 * settings and the places, Recently Used what was opened this session, and Leave the ways out. Each row is a program's
 * name over what it does. Typing searches every program.
 *
 * <p>Drawn and clicked against the same rows, so the row a player sees is the row they hit. Its colours are
 * {@code jsc:launcher/kickoff4}.
 */
@PaletteHolder
final class Kickoff4Menu {

    private final DesktopState desktop;
    /** The tab that is up. */
    private Tab tab = Tab.FAVORITES;

    static final int W = 262;
    static final int H = 236;
    private static final int HEADER_H = 32;
    private static final int TABS_H = 28;
    private static final int ROW_H = 21;
    private static final int ICON = 16;
    private static final float TAB_SCALE = 0.65F;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "launcher/kickoff4",
            new Colours(0xF7FAFCFF, 0xF7E9EFF6, 0xFFFFFFFF, 0xFFDEE9F5, 0xFF34495E, 0xFF9FB0C4, 0xFFFFFFFF,
                    0xFF8A9AAC, 0x808CBEF0, 0x4D5A96DC, 0xFF1D2B3A, 0xFF6B7A8A, 0xFFC5D3E2, 0xFFB8D6F5,
                    0xFFE3A21A, 0xFFC0392B, 0x40001E46));

    /** The everyday programs Favorites starts with on a machine that has them, by id path. */
    private static final List<String> EVERYDAY = List.of("files", "command_prompt", "editor", "system_monitor",
            "soundfoundry", "network_manager", "settings");

    /** Kickoff's five tabs along its foot, each in its column from the left. */
    enum Tab {
        FAVORITES(0, TransitionTexts.FAVORITES),
        APPLICATIONS(1, TransitionTexts.APPLICATIONS),
        COMPUTER(2, TransitionTexts.COMPUTER),
        RECENTLY_USED(3, TransitionTexts.RECENTLY_USED),
        LEAVE(4, TransitionTexts.LEAVE);

        private final int column;
        private final TextKey label;

        Tab(final int column, final TextKey label) {
            this.column = column;
            this.label = label;
        }

        /* The tab in that column, or the nearest one at either end. */
        static Tab inColumn(final int column) {
            Tab found = FAVORITES;
            for (final Tab each : values()) {
                if (each.column <= column) {
                    found = each;
                }
            }
            return found;
        }
    }

    Kickoff4Menu(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Opens on Favorites, as Kickoff did every time. */
    void reset() {
        tab = Tab.FAVORITES;
    }

    Tab tab() {
        return tab;
    }

    void render(final GuiGraphics g, final int tbY) {
        final Colours c = PALETTE.get();
        final int x = desktop.start().left();
        final int y = tbY - H;
        g.fill(x + 3, y + 4, x + W + 3, y + H + 2, c.shadow());
        ChromeShapes.roundedRect(g, x - 1, y - 1, W + 2, H + 1, c.border(), 4, 0);
        g.fillGradient(x, y, x + W, y + H, c.top(), c.bottom());
        // The header: the account in bold weight, the desktop's name beside it, the search box under them.
        g.fillGradient(x, y, x + W, y + HEADER_H, c.headerTop(), c.headerBottom());
        Draw.text(g, desktop.textFont(), desktop.shorten(desktop.accountLabel(), 20), x + 6, y + 4, c.headerInk());
        final String name = desktop.deskName();
        Texts.small(g, desktop.textFont(), name, x + W - 6 - Texts.smallWidth(desktop.textFont(), name), y + 5,
                c.headerInk());
        g.fill(x + 6, y + 15, x + W - 6, y + 27, c.search());
        ChromeShapes.roundedOutline(g, x + 6, y + 15, W - 12, 12, c.searchRim(), 2);
        final String typed = desktop.start().searchText();
        Draw.text(g, desktop.textFont(), typed.isEmpty() ? GameText.resolve(TransitionTexts.SEARCH) : typed, x + 10,
                y + 17, typed.isEmpty() ? c.hint() : c.ink());
        // The list of the tab that is up, or of what the search found.
        final List<Row> rows = rows();
        final int listTop = y + HEADER_H + 2;
        final int listBottom = y + H - TABS_H;
        if (rows.isEmpty()) {
            final TextKey empty = typed.isEmpty() ? TransitionTexts.NOTHING_RECENT : DesktopTexts.NO_RESULTS;
            Draw.text(g, desktop.textFont(), GameText.resolve(empty), x + 10, listTop + 6, c.line());
        }
        for (int i = 0; i < rows.size(); i++) {
            final int ry = listTop + i * ROW_H;
            if (ry + ROW_H > listBottom) {
                break;
            }
            final Row row = rows.get(i);
            if (desktop.hoverIn(x + 3, ry, W - 6, ROW_H)) {
                g.fillGradient(x + 3, ry, x + W - 3, ry + ROW_H, c.rowTop(), c.rowBottom());
            }
            ProgramIcons.draw(g, x + 6, ry + 2, ICON, ICON, row.icon(), desktop.icons());
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(row.name(), W - 36), x + 28,
                    ry + 2, c.ink());
            Texts.small(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(row.line(),
                    Texts.smallFits(W - 36)), x + 28, ry + 12, c.line());
        }
        drawTabs(g, x, y + H - TABS_H, c);
    }

    /** A click at a desktop-local point while Kickoff is up: true when it landed on it. */
    boolean click(final int mx, final int my, final int tbY) {
        final int x = desktop.start().left();
        final int y = tbY - H;
        if (mx < x || mx >= x + W || my < y || my >= tbY) {
            return false;
        }
        final int tabsTop = y + H - TABS_H;
        if (my >= tabsTop) {
            tab = Tab.inColumn((mx - x) * Tab.values().length / W);
            return true;
        }
        final int listTop = y + HEADER_H + 2;
        if (my >= listTop) {
            final int index = (my - listTop) / ROW_H;
            final List<Row> rows = rows();
            if (index >= 0 && index < rows.size() && listTop + (index + 1) * ROW_H <= tabsTop) {
                desktop.start().close();
                rows.get(index).action().run();
            }
        }
        return true;
    }

    /** The desktop-local middle of tab {@code tab}, for a test. */
    int[] tabPoint(final Tab which, final int tbY) {
        final int x = desktop.start().left();
        final int width = W / Tab.values().length;
        return new int[] {x + which.column * width + width / 2, tbY - TABS_H / 2};
    }

    /** The desktop-local middle of the row that says {@code name}, or null when the list has none. */
    @Nullable
    int[] rowPoint(final String name, final int tbY) {
        final List<Row> rows = rows();
        final int listTop = tbY - H + HEADER_H + 2;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).name().equals(name)) {
                return new int[] {desktop.start().left() + W / 2, listTop + i * ROW_H + ROW_H / 2};
            }
        }
        return null;
    }

    /** What the list holds: the search's matches while something is typed, the tab's rows otherwise. */
    List<Row> rows() {
        final String typed = desktop.start().searchText().toLowerCase(Locale.ROOT).trim();
        if (!typed.isEmpty()) {
            final List<Row> out = new ArrayList<>();
            for (final Launcher l : desktop.start().filtered()) {
                out.add(program(l));
            }
            return out;
        }
        return switch (tab) {
            case FAVORITES -> favorites();
            case APPLICATIONS -> applications();
            case COMPUTER -> computer();
            case RECENTLY_USED -> recentlyUsed();
            case LEAVE -> leave();
        };
    }

    private void drawTabs(final GuiGraphics g, final int x, final int y, final Colours c) {
        g.fill(x, y, x + W, y + 1, c.tabRule());
        final int width = W / Tab.values().length;
        for (final Tab each : Tab.values()) {
            final int tx = x + each.column * width;
            if (each == tab) {
                g.fillGradient(tx, y + 1, tx + width, y + TABS_H, c.rowTop(), c.rowBottom());
            }
            final int ix = tx + (width - ICON) / 2;
            switch (each) {
                case FAVORITES -> star(g, ix + 3, y + 3, c.star());
                case LEAVE -> leaveMark(g, ix + 3, y + 3, c.leave());
                default -> ProgramIcons.draw(g, ix, y + 2, ICON, ICON, tabIcon(each), desktop.icons());
            }
            // The tabs' words in the smallest letters the menu uses, so the longest of them still fits its tab.
            final String label = GameText.resolve(each.label);
            final int fits = (int) (width / TAB_SCALE);
            final String shown = desktop.textFont().plainSubstrByWidth(label, fits);
            final int lw = (int) (desktop.textFont().width(shown) * TAB_SCALE);
            Texts.scaled(g, desktop.textFont(), shown, tx + Math.max(0, (width - lw) / 2), y + 20, TAB_SCALE,
                    c.headerInk());
        }
    }

    private List<Row> favorites() {
        final List<Row> out = new ArrayList<>();
        for (final String id : EVERYDAY) {
            final Launcher l = launcher(id);
            if (l != null) {
                out.add(program(l));
            }
        }
        return out;
    }

    private List<Row> applications() {
        final List<Launcher> all = new ArrayList<>(desktop.launcherList());
        all.sort(Comparator.comparing(l -> l.label().toLowerCase(Locale.ROOT)));
        final List<Row> out = new ArrayList<>();
        for (final Launcher l : all) {
            out.add(program(l));
        }
        return out;
    }

    /** Computer: the settings, the home folder, the network and the trash, those this machine has. */
    private List<Row> computer() {
        final List<Row> out = new ArrayList<>();
        final Launcher settings = launcher("settings");
        if (settings != null) {
            out.add(new Row(settings.programId(), settings.label(),
                    GameText.resolve(TransitionTexts.SYSTEM_SETTINGS_LINE), () -> desktop.opener().run(settings)));
        }
        final Launcher files = launcher("files");
        if (files != null) {
            out.add(new Row(files.programId(), GameText.resolve(TransitionTexts.HOME),
                    GameText.resolve(TransitionTexts.HOME_LINE), () -> desktop.opener().run(files)));
        }
        final Launcher network = launcher("network");
        if (network != null) {
            out.add(new Row(network.programId(), network.label(), GameText.resolve(TransitionTexts.NETWORK_LINE),
                    () -> desktop.opener().run(network)));
        }
        final Launcher trash = desktop.trash().launcher();
        out.add(new Row(trash.programId(), trash.label(), GameText.resolve(TransitionTexts.TRASH_LINE),
                desktop::openTrash));
        return out;
    }

    /** What was opened this session, the most opened first. */
    private List<Row> recentlyUsed() {
        final List<Launcher> used = new ArrayList<>();
        for (final Launcher l : desktop.launcherList()) {
            if (desktop.wm().openedCount(l.key()) > 0) {
                used.add(l);
            }
        }
        used.sort(Comparator.comparingInt((Launcher l) -> -desktop.wm().openedCount(l.key())));
        final List<Row> out = new ArrayList<>();
        for (final Launcher l : used) {
            out.add(program(l));
        }
        return out;
    }

    private List<Row> leave() {
        final ResourceLocation power = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "setup");
        return List.of(
                new Row(power, GameText.resolve(TransitionTexts.SHUT_DOWN),
                        GameText.resolve(TransitionTexts.SHUT_DOWN_LINE),
                        () -> desktop.cyclePower(MachinePowerPayload.ACTION_SHUTDOWN)),
                new Row(power, GameText.resolve(TransitionTexts.RESTART),
                        GameText.resolve(TransitionTexts.RESTART_LINE),
                        () -> desktop.cyclePower(MachinePowerPayload.ACTION_RESTART)));
    }

    /** A program's row: its name over what it does, in the player's language. */
    private Row program(final Launcher l) {
        final ProgramSpec spec = OsRegistry.getProgram(l.programId());
        final String line = spec == null ? "" : Component.translatable(spec.descriptionKey()).getString();
        return new Row(l.programId(), l.label(), line, () -> desktop.opener().run(l));
    }

    @Nullable
    private Launcher launcher(final String programPath) {
        for (final Launcher l : desktop.launcherList()) {
            if (l.programId().getPath().equals(programPath)) {
                return l;
            }
        }
        return null;
    }

    private static ResourceLocation tabIcon(final Tab tab) {
        final String path = switch (tab) {
            case APPLICATIONS -> "application_manager";
            case COMPUTER -> "this_pc";
            default -> "editor";
        };
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    /** Favorites' star, drawn out of rows. */
    private static void star(final GuiGraphics g, final int x, final int y, final int colour) {
        final int[][] rows = {{4, 6}, {4, 6}, {0, 10}, {1, 9}, {2, 8}, {2, 8}, {1, 9}, {1, 3}, {7, 9}};
        for (int i = 0; i < rows.length; i++) {
            g.fill(x + rows[i][0], y + i, x + rows[i][1], y + i + 1, colour);
        }
    }

    /** Leave's mark: the power ring with its bar. */
    private static void leaveMark(final GuiGraphics g, final int x, final int y, final int colour) {
        g.fill(x, y + 3, x + 1, y + 9, colour);
        g.fill(x + 9, y + 3, x + 10, y + 9, colour);
        g.fill(x + 1, y + 9, x + 9, y + 10, colour);
        g.fill(x + 1, y + 2, x + 3, y + 3, colour);
        g.fill(x + 7, y + 2, x + 9, y + 3, colour);
        g.fill(x + 4, y, x + 6, y + 6, colour);
    }

    /** One row of the list: its picture, its name, its line, and what a click does. */
    record Row(ResourceLocation icon, String name, String line, Runnable action) {
    }

    /**
     * Kickoff's colours: the menu's crown and foot, the header's crown and foot and its ink, the search box's rim and
     * fill, the hint, a lit row's crown and foot, the ink and a row's line, the rule over the tabs, the frame, the
     * star and the leave mark, and the shadow.
     */
    private record Colours(int top, int bottom, int headerTop, int headerBottom, int headerInk, int searchRim,
                           int search, int hint, int rowTop, int rowBottom, int ink, int line, int tabRule,
                           int border, int star, int leave, int shadow) {
    }
}
