/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.DosShellLayout;
import dev.jstech.computers.program.cli.menushell.DirectoryTree;
import dev.jstech.computers.program.cli.menushell.MenuShellListing;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * Draws the MC-DOS Shell on a text screen in the sixteen colours, the way the shell of DOS 4 and 5 looked in text
 * mode: the blue title bar, the grey menu bar with each menu's letter lit, the path and the drives, the tree and the
 * file list over the program list and the Active Task List, each area's title bar lit while it has the focus, the
 * cyan key line with the clock, and over them the menu that is down, a dialog with its shadow, or the switcher's
 * banner naming the task Tab brings.
 */
final class DosShellPainter {

    private static final int INK = TextScreen.cga(TextScreen.BLACK);
    private static final int GROUND = TextScreen.cga(TextScreen.GREY);
    private static final int BRIGHT = TextScreen.cga(TextScreen.WHITE);
    private static final int BAR = TextScreen.cga(TextScreen.BLUE);
    private static final int TITLE_GROUND = TextScreen.cga(TextScreen.CYAN);
    private static final int GREYED = TextScreen.cga(TextScreen.DARK_GREY);
    private static final int SHADOW_GROUND = TextScreen.cga(TextScreen.BLACK);
    /** How wide the clock in the key line is, its letter for the half of the day included. */
    private static final int CLOCK_WIDTH = 6;
    /** How many characters of the file list the name and extension take, the way DOS wrote them, 8 and 3. */
    private static final int NAME_WIDTH = 8;
    private static final int EXT_WIDTH = 3;
    private static final int SIZE_WIDTH = 8;
    private static final int MENU_MIN_WIDTH = 20;
    /** The ticks of a day, and the hour tick zero falls on. */
    private static final long DAY = 24_000L;
    private static final long DAWN = 6L;

    private DosShellPainter() {
    }

    /** The shell's screen as it stands, at that many cells. */
    static TextScreen paint(final DosShellKeys s, final int columns, final int rows) {
        final TextScreen screen = new TextScreen(columns, rows, INK, GROUND);
        final TaskPrompt task = s.current();
        if (task != null) {
            task.drawInto(screen, 0);
            banner(s, screen);
            return screen;
        }
        if (s.viewer() != null) {
            viewer(s, screen);
            banner(s, screen);
            return screen;
        }
        final DosShellLayout layout = s.layout();
        screen.fill(0, DosShellLayout.TITLE_ROW, columns, 1, BRIGHT, BAR);
        final String title = GameText.resolve(DosShellTexts.TITLE);
        screen.put((columns - title.length()) / 2, DosShellLayout.TITLE_ROW, title, BRIGHT, BAR);
        menuBar(s, screen);
        screen.put(0, DosShellLayout.PATH_ROW, s.folder(), INK, GROUND);
        drives(s, screen);
        if (layout.showsFiles()) {
            tree(s, screen, layout);
            files(s, screen, layout);
        }
        if (layout.showsPrograms()) {
            programs(s, screen, layout);
            if (layout.tasks()) {
                tasks(s, screen, layout);
            }
        }
        keyLine(s, screen, layout);
        if (s.results() != null) {
            results(s, screen, layout);
        }
        if (s.menu() >= 0 && s.dropped()) {
            dropdown(s, screen);
        }
        if (s.dialog() != null) {
            s.dialog().draw(screen);
        }
        banner(s, screen);
        return screen;
    }

    /** The menu whose title is under {@code column} of the menu bar, or -1. */
    static int menuAt(final int column) {
        int x = DosShellLayout.MENU_LEFT;
        for (int i = 0; i < DosShellMenus.MENUS.size(); i++) {
            final int wide = GameText.resolve(DosShellMenus.MENUS.get(i).title()).length();
            if (column >= x && column < x + wide) {
                return i;
            }
            x += wide + DosShellLayout.MENU_GAP;
        }
        return -1;
    }

    /** The item of the menu that is down under the cell ({@code column}, {@code row}), or -1. */
    static int menuItemAt(final DosShellKeys s, final int row, final int column) {
        final int[] box = dropdownBox(s.menu());
        final int index = row - box[1] - 1;
        final List<DosShellMenus.Item> items = DosShellMenus.MENUS.get(s.menu()).items();
        if (column > box[0] && column < box[0] + box[2] - 1 && index >= 0 && index < items.size()) {
            return items.get(index).rule() ? -1 : index;
        }
        return -1;
    }

    private static void menuBar(final DosShellKeys s, final TextScreen screen) {
        int x = DosShellLayout.MENU_LEFT;
        for (int i = 0; i < DosShellMenus.MENUS.size(); i++) {
            final String label = GameText.resolve(DosShellMenus.MENUS.get(i).title());
            if (s.menu() == i) {
                screen.put(x, DosShellLayout.MENU_ROW, label, BRIGHT, INK);
            } else {
                screen.put(x, DosShellLayout.MENU_ROW, label, INK, GROUND);
                screen.put(x, DosShellLayout.MENU_ROW, label.substring(0, 1), BRIGHT, GROUND);
            }
            x += label.length() + DosShellLayout.MENU_GAP;
        }
    }

    private static void drives(final DosShellKeys s, final TextScreen screen) {
        final List<MenuShellListing.Drive> drives = s.listing().drives();
        final char current = s.folder().isEmpty() ? 'C' : Character.toUpperCase(s.folder().charAt(0));
        for (int i = 0; i < drives.size(); i++) {
            final MenuShellListing.Drive drive = drives.get(i);
            final boolean on = drive.letter() == current;
            final boolean focused = on && s.focus() == DosShellKeys.Area.DRIVES;
            screen.put(1 + i * DosShellLayout.DRIVE_STEP, DosShellLayout.DRIVES_ROW, "▄" + drive.letter() + ":",
                    on ? BRIGHT : INK, on ? (focused ? INK : BAR) : GROUND);
        }
    }

    private static void tree(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        areaTitle(screen, 0, layout.topTitle(), layout.leftWidth(), GameText.resolve(DosShellTexts.DIRECTORY_TREE),
                s.focus() == DosShellKeys.Area.TREE);
        final List<DirectoryTree.Row> rows = s.treeRows();
        for (int i = 0; i < layout.topRows(); i++) {
            final int index = s.treeTop() + i;
            final int row = layout.topFirst() + i;
            screen.put(layout.split() - 1, row, "│", INK, GROUND);
            if (index >= rows.size()) {
                continue;
            }
            final DirectoryTree.Row line = rows.get(index);
            final String mark = line.hasChildren() && !line.open() ? "+" : "▬";
            final int x = 1 + line.lead().length();
            screen.put(1, row, line.lead(), INK, GROUND);
            final String name = mark + line.name();
            if (index == s.treeRow()) {
                final boolean focused = s.focus() == DosShellKeys.Area.TREE;
                screen.put(x, row, name, focused ? BRIGHT : INK, focused ? BAR : TITLE_GROUND);
            } else {
                screen.put(x, row, name, INK, GROUND);
            }
        }
        scrollBar(screen, layout.leftWidth() - 1, layout.topFirst(), layout.topRows(), s.treeTop(), rows.size());
    }

    private static void files(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        final String folder = s.folder();
        areaTitle(screen, layout.split(), layout.topTitle(), layout.rightWidth(),
                (folder.endsWith("\\") ? folder : folder + "\\") + s.pattern(), s.focus() == DosShellKeys.Area.FILES);
        final List<MenuShellListing.Entry> files = s.files();
        final int x = layout.split() + 1;
        if (files.isEmpty()) {
            screen.put(x + 1, layout.topFirst(), GameText.resolve(s.listing().found() ? DosShellTexts.NO_FILES
                    : DosShellTexts.NOT_READY), INK, GROUND);
        }
        for (int i = 0; i < layout.topRows(); i++) {
            final int index = s.fileTop() + i;
            if (index >= files.size()) {
                break;
            }
            final MenuShellListing.Entry file = files.get(index);
            final String line = "▫ " + nameColumns(file) + " " + pad(group(file.weight()) + " mB", SIZE_WIDTH, true)
                    + "  " + file.stamp().trim();
            final int row = layout.topFirst() + i;
            final boolean picked = index == s.fileRow() && s.focus() == DosShellKeys.Area.FILES;
            if (picked) {
                screen.fill(layout.split(), row, layout.rightWidth() - 1, 1, BRIGHT, BAR);
            }
            screen.put(x, row, clip(line, layout.rightWidth() - 2), picked ? BRIGHT : INK, picked ? BAR : GROUND);
        }
        scrollBar(screen, screen.columns() - 1, layout.topFirst(), layout.topRows(), s.fileTop(), files.size());
    }

    private static void programs(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        final TextKey heading = switch (s.group()) {
            case MAIN -> DosShellTexts.MAIN;
            case PROGRAMS -> DosShellTexts.PROGRAMS;
            case DISK_UTILITIES -> DosShellTexts.DISK_UTILITIES;
        };
        final boolean focused = s.focus() == DosShellKeys.Area.PROGRAMS;
        areaTitle(screen, 0, layout.bottomTitle(), layout.programsWidth(), GameText.resolve(heading), focused);
        final List<DosShellKeys.ProgramLine> lines = s.programLines();
        for (int i = 0; i < layout.bottomRows() && i < lines.size(); i++) {
            final String label = GameText.resolve(lines.get(i).label());
            final boolean picked = i == s.programRow() && focused;
            screen.put(2, layout.bottomFirst() + i, clip(label, layout.programsWidth() - 3), picked ? BRIGHT : INK,
                    picked ? BAR : GROUND);
        }
    }

    private static void tasks(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        final boolean focused = s.focus() == DosShellKeys.Area.TASKS;
        areaTitle(screen, layout.split(), layout.bottomTitle(), layout.rightWidth(),
                GameText.resolve(DosShellTexts.TASKS), focused);
        final List<TaskPrompt> tasks = s.tasks();
        for (int i = 0; i < layout.bottomRows(); i++) {
            final int row = layout.bottomFirst() + i;
            screen.put(layout.split() - 1, row, "│", INK, GROUND);
            if (i < tasks.size()) {
                final boolean picked = i == s.taskRow() && focused;
                screen.put(layout.split() + 2, row, clip(tasks.get(i).name(), layout.rightWidth() - 3),
                        picked ? BRIGHT : INK, picked ? BAR : GROUND);
            }
        }
    }

    private static void keyLine(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        final int row = layout.keyRow();
        screen.fill(0, row, screen.columns(), 1, INK, TITLE_GROUND);
        String says = GameText.resolve(s.swapper() ? DosShellTexts.KEYS : DosShellTexts.KEYS_NO_TASKS);
        if (s.menu() >= 0 && s.dropped()) {
            final DosShellMenus.Item item = DosShellMenus.MENUS.get(s.menu()).items().get(s.menuItem());
            says = item.help() == null ? "" : GameText.resolve(item.help());
        }
        screen.put(1, row, clip(says, screen.columns() - CLOCK_WIDTH - 3), INK, TITLE_GROUND);
        final String clock = clock();
        screen.put(screen.columns() - clock.length() - 1, row, clock, INK, TITLE_GROUND);
    }

    private static void dropdown(final DosShellKeys s, final TextScreen screen) {
        final int[] box = dropdownBox(s.menu());
        final List<DosShellMenus.Item> items = DosShellMenus.MENUS.get(s.menu()).items();
        screen.box(box[0], box[1], box[2], box[3], INK, GROUND, false);
        for (int i = 0; i < items.size(); i++) {
            final DosShellMenus.Item item = items.get(i);
            final int row = box[1] + 1 + i;
            if (item.rule()) {
                screen.put(box[0], row, "├" + "─".repeat(box[2] - 2) + "┤", INK, GROUND);
                continue;
            }
            final boolean enabled = s.enabled(item);
            final boolean picked = i == s.menuItem();
            final int ink = picked ? BRIGHT : enabled ? INK : GREYED;
            final int ground = picked ? INK : GROUND;
            screen.fill(box[0] + 1, row, box[2] - 2, 1, ink, ground);
            final String label = GameText.resolve(item.label());
            final String mark = s.ticked(item) ? "•" : " ";
            screen.put(box[0] + 1, row, mark + label, ink, ground);
            if (enabled && !picked) {
                screen.put(box[0] + 2, row, label.substring(0, 1), BRIGHT, ground);
            }
            screen.put(box[0] + box[2] - 2 - item.key().length(), row, item.key(), ink, ground);
        }
        screen.shadow(box[0], box[1], box[2], box[3], GREYED, SHADOW_GROUND);
    }

    /* Where menu {@code index} drops down: its left, top, width and height. */
    private static int[] dropdownBox(final int index) {
        int x = DosShellLayout.MENU_LEFT;
        for (int i = 0; i < index; i++) {
            x += GameText.resolve(DosShellMenus.MENUS.get(i).title()).length() + DosShellLayout.MENU_GAP;
        }
        final List<DosShellMenus.Item> items = DosShellMenus.MENUS.get(index).items();
        int wide = MENU_MIN_WIDTH;
        for (final DosShellMenus.Item item : items) {
            if (!item.rule()) {
                wide = Math.max(wide, GameText.resolve(item.label()).length() + item.key().length() + 6);
            }
        }
        return new int[] {Math.max(0, x - 1), DosShellLayout.MENU_ROW + 1, wide, items.size() + 2};
    }

    private static void results(final DosShellKeys s, final TextScreen screen, final DosShellLayout layout) {
        final List<String> found = s.results();
        final int left = layout.showsFiles() ? layout.split() : 0;
        final int top = DosShellLayout.AREAS_TOP;
        final int width = screen.columns() - left;
        final int height = layout.keyRow() - top;
        screen.fill(left, top, width, height, INK, GROUND);
        areaTitle(screen, left, top, width, GameText.resolve(DosShellTexts.SEARCH_RESULTS.with(s.searched())), true);
        if (found == null || found.isEmpty()) {
            screen.put(left + 2, top + 1, GameText.resolve(DosShellTexts.NO_FILES), INK, GROUND);
            return;
        }
        final int shown = height - 1;
        final int first = Math.max(0, s.resultRow() - shown + 1);
        for (int i = 0; i < shown && first + i < found.size(); i++) {
            final boolean picked = first + i == s.resultRow();
            if (picked) {
                screen.fill(left, top + 1 + i, width, 1, BRIGHT, BAR);
            }
            screen.put(left + 1, top + 1 + i, clip(found.get(first + i), width - 2), picked ? BRIGHT : INK,
                    picked ? BAR : GROUND);
        }
    }

    private static void viewer(final DosShellKeys s, final TextScreen screen) {
        final int columns = screen.columns();
        screen.fill(0, 0, columns, 1, BRIGHT, BAR);
        final String title = GameText.resolve(DosShellTexts.VIEWER_TITLE) + "  " + s.viewerName();
        screen.put((columns - title.length()) / 2, 0, title, BRIGHT, BAR);
        screen.put(1, 1, clip(GameText.resolve(DosShellTexts.VIEWER_KEYS), columns - 2), INK, GROUND);
        final List<String> lines = s.viewer();
        final int top = 3;
        screen.box(0, top - 1, columns, screen.rows() - top + 1, INK, GROUND, false);
        for (int i = 0; lines != null && i < screen.rows() - top - 1 && s.viewerTop() + i < lines.size(); i++) {
            screen.put(1, top + i, clip(lines.get(s.viewerTop() + i), columns - 2), INK, GROUND);
        }
    }

    private static void banner(final DosShellKeys s, final TextScreen screen) {
        if (s.switching() < 0) {
            return;
        }
        screen.fill(0, 0, screen.columns(), 1, BRIGHT, BAR);
        final String says = GameText.resolve(DosShellTexts.SWITCHING.with(s.banner()));
        screen.put(Math.max(0, (screen.columns() - says.length()) / 2), 0, says, BRIGHT, BAR);
    }

    private static void areaTitle(final TextScreen screen, final int left, final int row, final int width,
                                  final String title, final boolean focused) {
        final int ink = focused ? BRIGHT : INK;
        final int ground = focused ? BAR : TITLE_GROUND;
        screen.fill(left, row, width, 1, ink, ground);
        final String shown = clip(title, width);
        screen.put(left + (width - shown.length()) / 2, row, shown, ink, ground);
    }

    /* A scroll bar down column {@code column}: the arrows at its ends, the track, and the box where the view is. */
    private static void scrollBar(final TextScreen screen, final int column, final int top, final int height,
                                  final int first, final int count) {
        if (height < 3) {
            return;
        }
        screen.put(column, top, "↑", INK, GROUND);
        for (int r = top + 1; r < top + height - 1; r++) {
            screen.put(column, r, "░", INK, GROUND);
        }
        final int track = height - 2;
        final int thumb = count <= height ? 0 : Math.min(track - 1, first * track / Math.max(1, count - height + 1));
        screen.put(column, top + 1 + thumb, "█", INK, GROUND);
        screen.put(column, top + height - 1, "↓", INK, GROUND);
    }

    /* The name and extension the way DOS listed them, or the whole name in their room when it is longer. */
    private static String nameColumns(final MenuShellListing.Entry file) {
        if (file.name().length() <= NAME_WIDTH && file.ext().length() <= EXT_WIDTH) {
            return pad(file.name(), NAME_WIDTH, false) + " " + pad(file.ext(), EXT_WIDTH, false);
        }
        return pad(clip(file.fullName(), NAME_WIDTH + 1 + EXT_WIDTH), NAME_WIDTH + 1 + EXT_WIDTH, false);
    }

    /* The time of the world's day the way the shell's clock wrote it: 10:42a. */
    private static String clock() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        final long time = mc.level.getDayTime() % DAY;
        final long hour = (time / 1_000L + DAWN) % 24L;
        final long minute = (time % 1_000L) * 60L / 1_000L;
        final long twelve = hour % 12L == 0L ? 12L : hour % 12L;
        return String.format(Locale.ROOT, "%d:%02d%s", twelve, minute, hour < 12L ? "a" : "p");
    }

    private static String group(final long number) {
        return String.format(Locale.ROOT, "%,d", number);
    }

    private static String pad(final String text, final int width, final boolean right) {
        if (text.length() >= width) {
            return text;
        }
        final String gap = " ".repeat(width - text.length());
        return right ? gap + text : text + gap;
    }

    private static String clip(final String text, final int width) {
        return width <= 0 ? "" : text.length() <= width ? text : text.substring(0, width);
    }
}
