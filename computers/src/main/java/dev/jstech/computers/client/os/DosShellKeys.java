/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.gui.layout.DosShellLayout;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.menushell.DirectoryTree;
import dev.jstech.computers.program.cli.menushell.MenuShellListing;
import dev.jstech.computers.program.cli.menushell.Wildcards;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The MC-DOS Shell on a terminal it has taken whole: the keyboard, the mouse and what the shell holds. The screen is
 * drawn by {@link DosShellPainter} from what is kept here; the machine is asked for its folders by name, as a file is,
 * and every action of the menus is one of MC-DOS's commands, run on a session the shell keeps out of sight.
 *
 * <p>The shell keeps the tasks it started: a command prompt (Shift+F9, or Command Prompt in the Main group) or a
 * program, each with its own screen. Alt+Tab, held, names the task Tab brings; letting go of Alt brings it.
 */
public final class DosShellKeys implements TtyEditor.IKeys {

    private MenuShellListing listing = MenuShellListing.empty("C:\\");
    private DirectoryTree tree = new DirectoryTree("C:\\", List.of());
    /** The folder whose files are listed, written whole. */
    private String folder = "C:\\";
    private Area focus = Area.TREE;
    private int treeRow;
    private int treeTop;
    private int fileRow;
    private int fileTop;
    private int programRow;
    private int taskRow;
    /** The menu picked in the bar, or -1; whether it is dropped down, and its item picked. */
    private int menu = -1;
    private boolean dropped;
    private int menuItem;
    @Nullable
    private DosShellDialog dialog;
    /** What View File Contents shows over the whole screen, and how far down it is read; null when not shown. */
    @Nullable
    private List<String> viewer;
    private String viewerName = "";
    private int viewerTop;
    /** What a search found, shown over the file list, and the one picked; null when not shown. */
    @Nullable
    private List<String> results;
    private String searched = "";
    private int resultRow;
    private Group group = Group.MAIN;
    private DosShellLayout.View view = DosShellLayout.View.PROGRAMS_AND_FILES;
    private boolean swapper = true;
    private boolean confirmDelete = true;
    private String pattern = ALL;
    private int sort;
    private final List<TaskPrompt> tasks = new ArrayList<>();
    /** The task in front, or null for the shell itself. */
    @Nullable
    private TaskPrompt current;
    /** While Alt is held after Alt+Tab, which of the shell (0) and its tasks (1 on) Tab has named; -1 otherwise. */
    private int switching = -1;
    private String banner = "";
    @Nullable
    private ShellActions actions;
    @Nullable
    private TtyEditor editor;
    private int columns = DEFAULT_COLUMNS;
    private int rows = DEFAULT_ROWS;
    private long clickedAt;
    private int clickedRow = -1;
    private int clickedColumn = -1;

    private static final String ALL = "*.*";
    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 25;
    /** How close together two clicks on one cell are to count as a double click, in milliseconds. */
    private static final long DOUBLE_CLICK_MS = 400L;
    /* The letters that open each menu with Alt, in the bar's order, the first letter of each menu's name. */
    private static final int[] MENU_KEYS = {GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_T,
        GLFW.GLFW_KEY_H};
    /* The extensions of what MC-DOS runs. */
    private static final List<String> RUNNABLE = List.of("EXE", "COM", "BAT");

    /** The areas Tab moves between. */
    enum Area {
        DRIVES, TREE, FILES, PROGRAMS, TASKS
    }

    /** Which group the program list shows. */
    enum Group {
        MAIN, PROGRAMS, DISK_UTILITIES
    }

    /** One line of the program list: what it says and what Enter on it does. */
    record ProgramLine(Text label, Runnable open) {
    }

    @Override
    public String status(final TtyEditor editor) {
        return "";
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        this.editor = editor;
        final BlockPos host = editor.machine();
        if (host != null) {
            this.actions = new ShellActions(host, this::ask);
        }
        take(MenuShellListing.read(editor.document().text()));
    }

    @Override
    @Nullable
    public TtyEditor inner(final TtyEditor editor) {
        settleSwitch();
        dropEndedTasks();
        return this.current != null ? this.current.editor() : null;
    }

    @Override
    public TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
        this.editor = editor;
        this.columns = columns;
        this.rows = rows;
        settleSwitch();
        dropEndedTasks();
        return DosShellPainter.paint(this, columns, rows);
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        this.editor = editor;
        final boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (alt && key == GLFW.GLFW_KEY_TAB) {
            nextTask(shift);
            return true;
        }
        if (this.current != null) {
            this.current.key(key, modifiers);
            dropEndedTasks();
            return true;
        }
        if (this.dialog != null) {
            this.dialog.key(key, modifiers);
            return true;
        }
        if (this.viewer != null) {
            viewerKey(key);
            return true;
        }
        if (this.results != null) {
            resultsKey(key);
            return true;
        }
        if (this.menu >= 0) {
            menuKey(key, alt);
            return true;
        }
        if (alt) {
            for (int i = 0; i < MENU_KEYS.length; i++) {
                if (key == MENU_KEYS[i]) {
                    openMenu(i, true);
                    return true;
                }
            }
            if (key == GLFW.GLFW_KEY_F4) {
                act(DosShellMenus.Action.EXIT);
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_F10 -> openMenu(0, false);
            case GLFW.GLFW_KEY_F3 -> act(DosShellMenus.Action.EXIT);
            case GLFW.GLFW_KEY_F5 -> act(shift ? DosShellMenus.Action.REPAINT : DosShellMenus.Action.REFRESH);
            case GLFW.GLFW_KEY_F7 -> act(DosShellMenus.Action.MOVE);
            case GLFW.GLFW_KEY_F8 -> act(DosShellMenus.Action.COPY);
            case GLFW.GLFW_KEY_F9 -> {
                if (shift) {
                    startPrompt();
                } else {
                    act(DosShellMenus.Action.VIEW_CONTENTS);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> act(DosShellMenus.Action.DELETE);
            case GLFW.GLFW_KEY_TAB -> moveFocus(shift ? -1 : 1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> enter();
            case GLFW.GLFW_KEY_UP -> move(-1);
            case GLFW.GLFW_KEY_DOWN -> move(1);
            case GLFW.GLFW_KEY_PAGE_UP -> move(-pageRows());
            case GLFW.GLFW_KEY_PAGE_DOWN -> move(pageRows());
            case GLFW.GLFW_KEY_HOME -> move(-Integer.MAX_VALUE / 2);
            case GLFW.GLFW_KEY_END -> move(Integer.MAX_VALUE / 2);
            case GLFW.GLFW_KEY_LEFT -> sideways(-1);
            case GLFW.GLFW_KEY_RIGHT -> sideways(1);
            case GLFW.GLFW_KEY_ESCAPE -> {
                // Leaving a group goes back to Main, as Escape did in the shell; otherwise it does nothing.
                if (this.group != Group.MAIN && this.focus == Area.PROGRAMS) {
                    this.group = Group.MAIN;
                    this.programRow = 0;
                }
            }
            default -> {
                // A key the shell has no use for.
            }
        }
        return true;
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (this.current != null) {
            this.current.typed(c);
            return true;
        }
        if (this.dialog != null) {
            this.dialog.typed(c);
            return true;
        }
        if (this.menu >= 0 || this.viewer != null || this.results != null) {
            return true;
        }
        if (this.focus == Area.TREE) {
            switch (c) {
                case '+' -> act(DosShellMenus.Action.EXPAND_ONE);
                case '*' -> act(DosShellMenus.Action.EXPAND_BRANCH);
                case '-' -> act(DosShellMenus.Action.COLLAPSE);
                default -> jumpTo(c);
            }
            return true;
        }
        jumpTo(c);
        return true;
    }

    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        if (this.current != null) {
            return true;
        }
        final long now = System.currentTimeMillis();
        final boolean twice = row == this.clickedRow && column == this.clickedColumn
                && now - this.clickedAt < DOUBLE_CLICK_MS;
        this.clickedAt = now;
        this.clickedRow = row;
        this.clickedColumn = column;
        if (this.dialog != null) {
            return this.dialog.clicked(DosShellPainter.paint(this, this.columns, this.rows), column, row);
        }
        if (this.viewer != null || this.results != null) {
            return true;
        }
        if (row == DosShellLayout.MENU_ROW) {
            final int at = DosShellPainter.menuAt(column);
            if (at >= 0) {
                openMenu(at, true);
            }
            return true;
        }
        if (this.menu >= 0 && this.dropped) {
            final int item = DosShellPainter.menuItemAt(this, row, column);
            if (item >= 0) {
                this.menuItem = item;
                pick();
            } else {
                closeMenu();
            }
            return true;
        }
        clickedArea(row, column, twice);
        return true;
    }

    /* Readings for the painter and for tests */

    MenuShellListing listing() {
        return listing;
    }

    List<DirectoryTree.Row> treeRows() {
        return tree.rows();
    }

    String folder() {
        return folder;
    }

    Area focus() {
        return focus;
    }

    int treeRow() {
        return treeRow;
    }

    int treeTop() {
        return treeTop;
    }

    int fileRow() {
        return fileRow;
    }

    int fileTop() {
        return fileTop;
    }

    int programRow() {
        return programRow;
    }

    int taskRow() {
        return taskRow;
    }

    int menu() {
        return menu;
    }

    boolean dropped() {
        return dropped;
    }

    int menuItem() {
        return menuItem;
    }

    @Nullable
    DosShellDialog dialog() {
        return dialog;
    }

    @Nullable
    List<String> viewer() {
        return viewer;
    }

    String viewerName() {
        return viewerName;
    }

    int viewerTop() {
        return viewerTop;
    }

    @Nullable
    List<String> results() {
        return results;
    }

    String searched() {
        return searched;
    }

    int resultRow() {
        return resultRow;
    }

    Group group() {
        return group;
    }

    DosShellLayout layout() {
        return DosShellLayout.of(columns, rows, view, swapper);
    }

    String pattern() {
        return pattern;
    }

    List<TaskPrompt> tasks() {
        return tasks;
    }

    @Nullable
    TaskPrompt current() {
        return current;
    }

    int switching() {
        return switching;
    }

    /** What the switcher's banner said last, for a test: the task Tab named. */
    public String banner() {
        return banner;
    }

    /** How many tasks the shell is keeping, for a test. */
    public int taskCount() {
        return tasks.size();
    }

    /** Whether a task, rather than the shell, is in front, for a test. */
    public boolean taskInFront() {
        return current != null;
    }

    boolean swapper() {
        return swapper;
    }

    /** The files of the folder, as the File Display Options say: those like the pattern, sorted. */
    List<MenuShellListing.Entry> files() {
        final List<MenuShellListing.Entry> out = new ArrayList<>();
        for (final MenuShellListing.Entry entry : listing.entries()) {
            if (!entry.folder() && likePattern(entry.fullName())) {
                out.add(entry);
            }
        }
        final Comparator<MenuShellListing.Entry> byName = Comparator.comparing(
                (MenuShellListing.Entry e) -> e.name().toUpperCase(Locale.ROOT));
        out.sort(switch (sort) {
            case 1 -> Comparator.comparing((MenuShellListing.Entry e) -> e.ext().toUpperCase(Locale.ROOT))
                    .thenComparing(byName);
            case 2 -> Comparator.comparing(MenuShellListing.Entry::stamp).thenComparing(byName);
            case 3 -> Comparator.comparingLong(MenuShellListing.Entry::weight).thenComparing(byName);
            default -> byName;
        });
        return out;
    }

    /** The lines of the program list for the group shown. */
    List<ProgramLine> programLines() {
        final List<ProgramLine> out = new ArrayList<>();
        switch (group) {
            case MAIN -> {
                out.add(new ProgramLine(DosShellTexts.COMMAND_PROMPT.text(), this::startPrompt));
                for (final MenuShellListing.Program program : listing.programs()) {
                    if ("sigma".equalsIgnoreCase(program.command())) {
                        out.add(new ProgramLine(DosShellTexts.SIGMA.text(),
                                () -> startProgram(DosShellTexts.SIGMA.text(), program.command())));
                    }
                }
                out.add(new ProgramLine(DosShellTexts.GROUP.with(DosShellTexts.PROGRAMS),
                        () -> enterGroup(Group.PROGRAMS)));
                out.add(new ProgramLine(DosShellTexts.GROUP.with(DosShellTexts.DISK_UTILITIES),
                        () -> enterGroup(Group.DISK_UTILITIES)));
            }
            case PROGRAMS -> {
                for (final MenuShellListing.Program program : listing.programs()) {
                    if (!"cmd".equalsIgnoreCase(program.command())) {
                        out.add(new ProgramLine(Text.literal(program.label()),
                                () -> startProgram(Text.literal(program.label()), program.command())));
                    }
                }
            }
            case DISK_UTILITIES -> out.add(new ProgramLine(DosShellTexts.FORMAT.text(), this::askFormat));
        }
        return out;
    }

    /** Whether a menu item can be picked on this machine now. */
    boolean enabled(final DosShellMenus.Item item) {
        return switch (item.action()) {
            case NONE -> false;
            case PRINT -> listing.printer() && selectedFile() != null;
            case VIEW_CONTENTS, MOVE, COPY, RENAME -> selectedFile() != null;
            case DELETE -> selectedFile() != null || (focus == Area.TREE && !atRoot());
            default -> true;
        };
    }

    /** Whether a menu item is ticked: the task swapper while it is on, the view shown. */
    boolean ticked(final DosShellMenus.Item item) {
        return switch (item.action()) {
            case SWAPPER -> swapper;
            case VIEW_FILES -> view == DosShellLayout.View.FILES;
            case VIEW_BOTH -> view == DosShellLayout.View.PROGRAMS_AND_FILES;
            case VIEW_PROGRAMS -> view == DosShellLayout.View.PROGRAMS;
            default -> false;
        };
    }

    /** The file picked in the file list, or null. */
    @Nullable
    MenuShellListing.Entry selectedFile() {
        final List<MenuShellListing.Entry> files = files();
        return fileRow >= 0 && fileRow < files.size() ? files.get(fileRow) : null;
    }

    /* The input */

    private void move(final int by) {
        switch (focus) {
            case TREE -> {
                final int count = tree.rows().size();
                final int to = clamp(treeRow + by, count);
                if (to != treeRow) {
                    treeRow = to;
                    showFolder(tree.rows().get(treeRow).folder());
                }
            }
            case FILES -> fileRow = clamp(fileRow + by, files().size());
            case PROGRAMS -> programRow = clamp(programRow + by, programLines().size());
            case TASKS -> taskRow = clamp(taskRow + by, tasks.size());
            case DRIVES -> sideways(by > 0 ? 1 : -1);
        }
        scrollIntoView();
    }

    private void sideways(final int by) {
        if (focus != Area.DRIVES || listing.drives().isEmpty()) {
            return;
        }
        final int at = driveIndex();
        final int to = Math.floorMod(at + by, listing.drives().size());
        switchDrive(listing.drives().get(to).letter());
    }

    private void enter() {
        switch (focus) {
            case TREE -> act(DosShellMenus.Action.EXPAND_ONE);
            case FILES -> act(DosShellMenus.Action.OPEN);
            case PROGRAMS -> {
                final List<ProgramLine> lines = programLines();
                if (programRow >= 0 && programRow < lines.size()) {
                    lines.get(programRow).open().run();
                }
            }
            case TASKS -> {
                if (taskRow >= 0 && taskRow < tasks.size()) {
                    current = tasks.get(taskRow);
                }
            }
            case DRIVES -> {
                // Enter on a drive reads it again.
                fetch(folder);
            }
        }
    }

    private void moveFocus(final int by) {
        final List<Area> shown = shownAreas();
        final int at = shown.indexOf(focus);
        focus = shown.get(Math.floorMod((at < 0 ? 0 : at) + by, shown.size()));
    }

    private List<Area> shownAreas() {
        final DosShellLayout layout = layout();
        final List<Area> out = new ArrayList<>();
        if (layout.showsFiles()) {
            if (!listing.drives().isEmpty()) {
                out.add(Area.DRIVES);
            }
            out.add(Area.TREE);
            out.add(Area.FILES);
        }
        if (layout.showsPrograms()) {
            out.add(Area.PROGRAMS);
            if (swapper) {
                out.add(Area.TASKS);
            }
        }
        return out;
    }

    private void jumpTo(final char c) {
        final String letter = String.valueOf(Character.toUpperCase(c));
        switch (focus) {
            case FILES -> {
                final List<MenuShellListing.Entry> files = files();
                for (int i = 0; i < files.size(); i++) {
                    final int at = (fileRow + 1 + i) % files.size();
                    if (files.get(at).name().toUpperCase(Locale.ROOT).startsWith(letter)) {
                        fileRow = at;
                        break;
                    }
                }
            }
            case TREE -> {
                final List<DirectoryTree.Row> rowsShown = tree.rows();
                for (int i = 0; i < rowsShown.size(); i++) {
                    final int at = (treeRow + 1 + i) % rowsShown.size();
                    if (rowsShown.get(at).name().toUpperCase(Locale.ROOT).startsWith(letter)) {
                        treeRow = at;
                        showFolder(rowsShown.get(at).folder());
                        break;
                    }
                }
            }
            default -> {
                // Letters jump only in the lists of names.
            }
        }
        scrollIntoView();
    }

    private void clickedArea(final int row, final int column, final boolean twice) {
        final DosShellLayout layout = layout();
        if (row == DosShellLayout.DRIVES_ROW && !listing.drives().isEmpty()) {
            final int index = (column - 1) / DosShellLayout.DRIVE_STEP;
            if (index >= 0 && index < listing.drives().size()) {
                focus = Area.DRIVES;
                switchDrive(listing.drives().get(index).letter());
            }
        } else if (layout.inTree(column, row)) {
            focus = Area.TREE;
            final int at = treeTop + row - layout.topFirst();
            if (at < tree.rows().size()) {
                treeRow = at;
                showFolder(tree.rows().get(at).folder());
                if (twice) {
                    act(DosShellMenus.Action.EXPAND_ONE);
                }
            }
        } else if (layout.inFiles(column, row)) {
            focus = Area.FILES;
            final int at = fileTop + row - layout.topFirst();
            if (at < files().size()) {
                fileRow = at;
                if (twice) {
                    act(DosShellMenus.Action.OPEN);
                }
            }
        } else if (layout.inPrograms(column, row)) {
            focus = Area.PROGRAMS;
            final int at = row - layout.bottomFirst();
            if (at < programLines().size()) {
                programRow = at;
                if (twice) {
                    enter();
                }
            }
        } else if (layout.inTasks(column, row)) {
            focus = Area.TASKS;
            final int at = row - layout.bottomFirst();
            if (at < tasks.size()) {
                taskRow = at;
                if (twice) {
                    enter();
                }
            }
        }
    }

    private void menuKey(final int key, final boolean alt) {
        final List<DosShellMenus.Item> items = DosShellMenus.MENUS.get(menu).items();
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F10 -> closeMenu();
            case GLFW.GLFW_KEY_LEFT -> openMenu(Math.floorMod(menu - 1, DosShellMenus.MENUS.size()), dropped);
            case GLFW.GLFW_KEY_RIGHT -> openMenu(Math.floorMod(menu + 1, DosShellMenus.MENUS.size()), dropped);
            case GLFW.GLFW_KEY_UP -> {
                if (dropped) {
                    menuItem = nextItem(items, menuItem, -1);
                }
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (dropped) {
                    menuItem = nextItem(items, menuItem, 1);
                } else {
                    openMenu(menu, true);
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (dropped) {
                    pick();
                } else {
                    openMenu(menu, true);
                }
            }
            default -> {
                if (alt) {
                    for (int i = 0; i < MENU_KEYS.length; i++) {
                        if (key == MENU_KEYS[i]) {
                            openMenu(i, true);
                        }
                    }
                }
            }
        }
    }

    private void openMenu(final int index, final boolean drop) {
        menu = index;
        dropped = drop;
        menuItem = nextItem(DosShellMenus.MENUS.get(index).items(), -1, 1);
    }

    private void closeMenu() {
        menu = -1;
        dropped = false;
    }

    /* The next item from {@code from} that is not a rule, going {@code by}; greyed items are passed over too. */
    private int nextItem(final List<DosShellMenus.Item> items, final int from, final int by) {
        int at = from;
        for (int i = 0; i < items.size(); i++) {
            at = Math.floorMod(at + by, items.size());
            if (!items.get(at).rule() && enabled(items.get(at))) {
                return at;
            }
        }
        return Math.max(0, from);
    }

    private void pick() {
        final DosShellMenus.Item item = DosShellMenus.MENUS.get(menu).items().get(menuItem);
        if (item.rule() || !enabled(item)) {
            return;
        }
        closeMenu();
        act(item.action());
    }

    private void viewerKey(final int key) {
        final List<String> lines = viewer == null ? List.of() : viewer;
        final int page = Math.max(1, rows - 4);
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F9 -> viewer = null;
            case GLFW.GLFW_KEY_UP -> viewerTop = Math.max(0, viewerTop - 1);
            case GLFW.GLFW_KEY_DOWN -> viewerTop = Math.max(0, Math.min(lines.size() - 1, viewerTop + 1));
            case GLFW.GLFW_KEY_PAGE_UP -> viewerTop = Math.max(0, viewerTop - page);
            case GLFW.GLFW_KEY_PAGE_DOWN -> viewerTop = Math.max(0, Math.min(lines.size() - 1, viewerTop + page));
            default -> {
                // The viewer reads a file and nothing else.
            }
        }
    }

    private void resultsKey(final int key) {
        final List<String> found = results == null ? List.of() : results;
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> results = null;
            case GLFW.GLFW_KEY_UP -> resultRow = Math.max(0, resultRow - 1);
            case GLFW.GLFW_KEY_DOWN -> resultRow = Math.max(0, Math.min(found.size() - 1, resultRow + 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (resultRow < found.size()) {
                    final String path = found.get(resultRow);
                    final int slash = path.lastIndexOf('\\');
                    results = null;
                    final String in = slash <= 2 ? path.substring(0, 3) : path.substring(0, slash);
                    tree.reveal(in);
                    showFolder(in);
                    focus = Area.FILES;
                }
            }
            default -> {
                // Only the arrows, Enter and Escape mean anything in the list of what was found.
            }
        }
    }

    /* The actions */

    private void act(final DosShellMenus.Action action) {
        switch (action) {
            case OPEN -> openSelected();
            case RUN -> dialog = DosShellDialog.form(DosShellTexts.RUN_TITLE.text(), List.of(),
                    List.of(DosShellDialog.field(DosShellTexts.COMMAND_LINE, "")),
                    d -> {
                        dialog = null;
                        final String line = d.typed(0).trim();
                        if (!line.isEmpty()) {
                            startProgram(Text.literal(firstWord(line)), line);
                        }
                    }, this::closeDialog);
            case PRINT -> command("PRINT " + selectedPath());
            case SEARCH -> dialog = DosShellDialog.form(DosShellTexts.SEARCH_TITLE.text(), List.of(),
                    List.of(DosShellDialog.field(DosShellTexts.SEARCH_FOR, ALL)),
                    d -> {
                        dialog = null;
                        search(d.typed(0).trim());
                    }, this::closeDialog);
            case VIEW_CONTENTS -> viewContents();
            case MOVE -> fromTo(DosShellTexts.MOVE_TITLE.text(), "MOVE");
            case COPY -> fromTo(DosShellTexts.COPY_TITLE.text(), "COPY");
            case DELETE -> delete();
            case RENAME -> rename();
            case MAKE_FOLDER -> makeFolder();
            case EXIT -> exit();
            case CONFIRMATION -> dialog = DosShellDialog.form(DosShellTexts.CONFIRM_TITLE.text(), List.of(),
                    List.of(DosShellDialog.check(DosShellTexts.CONFIRM_DELETE, confirmDelete)),
                    d -> {
                        dialog = null;
                        confirmDelete = d.ticked(0);
                    }, this::closeDialog);
            case DISPLAY_OPTIONS -> dialog = DosShellDialog.form(DosShellTexts.DISPLAY_TITLE.text(), List.of(),
                    List.of(DosShellDialog.field(DosShellTexts.NAME, pattern),
                            DosShellDialog.choice(DosShellTexts.SORT_BY, List.of(DosShellTexts.BY_NAME,
                                    DosShellTexts.BY_EXTENSION, DosShellTexts.BY_DATE, DosShellTexts.BY_SIZE), sort)),
                    d -> {
                        dialog = null;
                        pattern = d.typed(0).isBlank() ? ALL : d.typed(0).trim();
                        sort = d.chosen(1);
                        fileRow = 0;
                        fileTop = 0;
                    }, this::closeDialog);
            case INFORMATION -> information();
            case SWAPPER -> {
                if (swapper && !tasks.isEmpty()) {
                    say(DosShellTexts.STILL_RUNNING.text());
                } else {
                    swapper = !swapper;
                    keepFocusShown();
                }
            }
            case VIEW_FILES -> setView(DosShellLayout.View.FILES);
            case VIEW_BOTH -> setView(DosShellLayout.View.PROGRAMS_AND_FILES);
            case VIEW_PROGRAMS -> setView(DosShellLayout.View.PROGRAMS);
            case REPAINT -> {
                // Every frame is drawn afresh; there is nothing left over to repaint.
            }
            case REFRESH -> fetch(folder);
            case EXPAND_ONE -> tree.expand(selectedFolder());
            case EXPAND_BRANCH -> tree.expandBranch(selectedFolder());
            case EXPAND_ALL -> tree.expandAll();
            case COLLAPSE -> tree.collapse(selectedFolder());
            case HELP_KEYS -> say(DosShellTexts.KEYS_PAGE.text());
            case HELP_COMMANDS -> say(DosShellTexts.COMMANDS_PAGE.text());
            case ABOUT -> say(DosShellTexts.ABOUT_PAGE.text());
            case NONE -> {
                // A greyed item does nothing.
            }
        }
    }

    private void openSelected() {
        final MenuShellListing.Entry file = selectedFile();
        if (focus == Area.TREE) {
            tree.expand(selectedFolder());
            return;
        }
        if (focus == Area.PROGRAMS || focus == Area.TASKS) {
            enter();
            return;
        }
        if (file != null && RUNNABLE.contains(file.ext().toUpperCase(Locale.ROOT))) {
            startProgram(Text.literal(file.fullName()), selectedPath());
        }
    }

    private void viewContents() {
        final MenuShellListing.Entry file = selectedFile();
        if (file == null || actions == null) {
            return;
        }
        final String name = file.fullName();
        actions.run("TYPE " + selectedPath(), lines -> {
            final List<String> out = new ArrayList<>();
            for (final CliLine line : lines) {
                out.add(line.text(GameText.LOADED));
            }
            viewer = out;
            viewerName = name;
            viewerTop = 0;
        });
    }

    private void fromTo(final Text title, final String verb) {
        final String from = selectedPath();
        dialog = DosShellDialog.form(title, List.of(),
                List.of(DosShellDialog.field(DosShellTexts.FROM, from), DosShellDialog.field(DosShellTexts.TO,
                        folder)),
                d -> {
                    dialog = null;
                    command(verb + " " + d.typed(0).trim() + " " + d.typed(1).trim());
                }, this::closeDialog);
    }

    private void delete() {
        final boolean wholeFolder = focus == Area.TREE;
        final String path = wholeFolder ? selectedFolder() : selectedPath();
        if (path.isEmpty() || (wholeFolder && atRoot())) {
            return;
        }
        final String verb = (wholeFolder ? "RMDIR " : "DEL ") + path;
        if (!confirmDelete) {
            command(verb);
            return;
        }
        dialog = DosShellDialog.ask(DosShellTexts.DELETE_TITLE.text(), List.of(DosShellTexts.DELETE_ASK.with(path)),
                () -> {
                    dialog = null;
                    command(verb);
                }, this::closeDialog);
    }

    private void rename() {
        final MenuShellListing.Entry file = selectedFile();
        if (file == null) {
            return;
        }
        final String path = selectedPath();
        dialog = DosShellDialog.form(DosShellTexts.RENAME_TITLE.text(), List.of(Text.literal(path)),
                List.of(DosShellDialog.field(DosShellTexts.NEW_NAME, file.fullName())),
                d -> {
                    dialog = null;
                    if (!d.typed(0).isBlank()) {
                        command("REN " + path + " " + d.typed(0).trim());
                    }
                }, this::closeDialog);
    }

    private void makeFolder() {
        final String parent = selectedFolder();
        dialog = DosShellDialog.form(DosShellTexts.MAKE_FOLDER_TITLE.text(),
                List.of(Text.literal(GameText.resolve(DosShellTexts.PARENT) + " " + parent)),
                List.of(DosShellDialog.field(DosShellTexts.FOLDER_NAME, "")),
                d -> {
                    dialog = null;
                    if (!d.typed(0).isBlank()) {
                        command("MKDIR " + join(parent, d.typed(0).trim()));
                    }
                }, this::closeDialog);
    }

    private void askFormat() {
        dialog = DosShellDialog.form(DosShellTexts.FORMAT_TITLE.text(), List.of(),
                List.of(DosShellDialog.field(DosShellTexts.PARAMETERS, "a:")),
                d -> {
                    dialog = null;
                    startProgram(DosShellTexts.FORMAT.text(), "FORMAT " + d.typed(0).trim());
                }, this::closeDialog);
    }

    private void information() {
        final MenuShellListing.Entry file = selectedFile();
        long size = 0L;
        int count = 0;
        for (final MenuShellListing.Entry entry : listing.entries()) {
            if (!entry.folder()) {
                size += entry.weight();
                count++;
            }
        }
        final List<Text> lines = new ArrayList<>();
        lines.add(DosShellTexts.INFO_FILE.text());
        lines.add(DosShellTexts.INFO_NAME.with(file == null ? "" : file.fullName()));
        lines.add(DosShellTexts.INFO_ATTR.with(file == null ? "" : file.readOnly() ? "r" : "."));
        lines.add(DosShellTexts.INFO_FOLDER.text());
        lines.add(DosShellTexts.INFO_NAME.with(folder));
        lines.add(DosShellTexts.INFO_SIZE.with(size));
        lines.add(DosShellTexts.INFO_FILES.with(count));
        for (final MenuShellListing.Drive drive : listing.drives()) {
            if (drive.letter() == driveLetter()) {
                lines.add(DosShellTexts.INFO_DISK.text());
                lines.add(DosShellTexts.INFO_NAME.with(drive.letter() + ":"));
                lines.add(DosShellTexts.INFO_SIZE.with(drive.capacity()));
                lines.add(DosShellTexts.INFO_AVAILABLE.with(drive.free()));
            }
        }
        dialog = DosShellDialog.message(DosShellTexts.INFO_TITLE.text(), lines, this::closeDialog);
    }

    private void exit() {
        if (!tasks.isEmpty()) {
            say(DosShellTexts.STILL_RUNNING.text());
            return;
        }
        if (actions != null) {
            actions.release();
        }
        if (editor != null) {
            editor.quit();
        }
    }

    private void setView(final DosShellLayout.View to) {
        view = to;
        keepFocusShown();
    }

    private void keepFocusShown() {
        final List<Area> shown = shownAreas();
        if (!shown.contains(focus)) {
            focus = shown.get(0);
        }
    }

    private void enterGroup(final Group to) {
        group = to;
        programRow = 0;
    }

    /* Runs an MC-DOS command for an action, says what went wrong if anything did, and reads the folder again. */
    private void command(final String line) {
        if (actions == null) {
            return;
        }
        actions.run(line, lines -> {
            final List<Text> wrong = new ArrayList<>();
            for (final CliLine said : lines) {
                if (said.style() == CliStyle.ERROR) {
                    wrong.add(Text.literal(said.text(GameText.LOADED)));
                }
            }
            if (!wrong.isEmpty()) {
                dialog = DosShellDialog.message(DosShellTexts.MESSAGE_TITLE.text(), wrong, this::closeDialog);
            }
            fetch(folder);
        });
    }

    /* A command that stopped to ask: the question in a dialog, the answer back to it. */
    private void ask(final ShellActions.Question question) {
        dialog = DosShellDialog.form(DosShellTexts.MESSAGE_TITLE.text(), List.of(Text.literal(question.asked())),
                List.of(DosShellDialog.field(DosShellTexts.COMMAND_LINE, "")),
                d -> {
                    dialog = null;
                    question.answer().accept(d.typed(0));
                }, () -> {
                    dialog = null;
                    question.answer().accept("");
                });
    }

    private void say(final Text words) {
        dialog = DosShellDialog.message(DosShellTexts.MESSAGE_TITLE.text(), List.of(words), this::closeDialog);
    }

    private void closeDialog() {
        dialog = null;
    }

    private void search(final String like) {
        if (editor == null) {
            return;
        }
        final String asked = like.isEmpty() ? ALL : like;
        editor.read(MenuShellListing.search(asked).substring(MenuShellListing.SCHEME.length()),
                (content, existed) -> {
                    results = MenuShellListing.read(content).results();
                    searched = asked;
                    resultRow = 0;
                });
    }

    /* Tasks */

    private void startPrompt() {
        startTask(DosShellTexts.COMMAND_PROMPT.text(), null);
    }

    private void startProgram(final Text name, final String line) {
        startTask(name, line);
    }

    private void startTask(final Text name, @Nullable final String line) {
        final BlockPos host = editor == null ? null : editor.machine();
        if (host == null) {
            return;
        }
        // A second task of the same name is told apart by a number, the way the task list numbered them.
        final String base = GameText.resolve(name);
        int same = 0;
        for (final TaskPrompt task : tasks) {
            if (task.name().equals(base) || task.name().startsWith(base + " (")) {
                same++;
            }
        }
        final String named = same == 0 ? base : GameText.resolve(DosShellTexts.NUMBERED.with(base, same + 1));
        final TaskPrompt task = new TaskPrompt(host, named, Platform.MC_DOS, line,
                line == null ? null : DosShellTexts.RETURN.text());
        if (swapper) {
            tasks.add(task);
        }
        current = task;
    }

    private void nextTask(final boolean back) {
        if (!swapper || tasks.isEmpty()) {
            return;
        }
        final int count = tasks.size() + 1;
        final int at = switching >= 0 ? switching : current == null ? 0 : tasks.indexOf(current) + 1;
        switching = Math.floorMod(at + (back ? -1 : 1), count);
        banner = switching == 0 ? GameText.resolve(DosShellTexts.TITLE) : tasks.get(switching - 1).name();
    }

    /* Brings the task Tab named once Alt is let go, the way the switcher of that age did. */
    private void settleSwitch() {
        if (switching < 0 || altHeld()) {
            return;
        }
        current = switching == 0 ? null : tasks.get(Math.min(tasks.size(), switching) - 1);
        switching = -1;
    }

    private void dropEndedTasks() {
        tasks.removeIf(TaskPrompt::ended);
        if (current != null && current.ended()) {
            current = null;
            // What a program run as a task did to the disk shows when the shell comes back.
            fetch(folder);
        }
        taskRow = clamp(taskRow, tasks.size());
        if (switching > tasks.size()) {
            switching = -1;
        }
    }

    private static boolean altHeld() {
        final long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    /* The machine */

    private void fetch(final String dir) {
        if (editor == null) {
            return;
        }
        editor.read(MenuShellListing.view(dir).substring(MenuShellListing.SCHEME.length()),
                (content, existed) -> take(MenuShellListing.read(content)));
    }

    private void take(final MenuShellListing taken) {
        final String root = taken.dir().length() >= 3 ? taken.dir().substring(0, 3) : "C:\\";
        final boolean sameDrive = tree.root().equalsIgnoreCase(root);
        listing = taken;
        tree = sameDrive ? tree.rebuilt(taken.tree()) : new DirectoryTree(root, taken.tree());
        if (taken.found()) {
            folder = taken.dir();
        }
        tree.reveal(folder);
        final List<DirectoryTree.Row> shown = tree.rows();
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).folder().equalsIgnoreCase(folder)) {
                treeRow = i;
            }
        }
        fileRow = clamp(fileRow, files().size());
        scrollIntoView();
    }

    private void showFolder(final String to) {
        folder = to;
        fileRow = 0;
        fileTop = 0;
        fetch(to);
    }

    private void switchDrive(final char letter) {
        for (final MenuShellListing.Drive drive : listing.drives()) {
            if (drive.letter() == letter && !drive.ready()) {
                say(DosShellTexts.NOT_READY.text());
                return;
            }
        }
        treeRow = 0;
        treeTop = 0;
        showFolder(letter + ":\\");
    }

    private void scrollIntoView() {
        final DosShellLayout layout = layout();
        final int shown = Math.max(1, layout.topRows());
        if (treeRow < treeTop) {
            treeTop = treeRow;
        } else if (treeRow >= treeTop + shown) {
            treeTop = treeRow - shown + 1;
        }
        if (fileRow < fileTop) {
            fileTop = fileRow;
        } else if (fileRow >= fileTop + shown) {
            fileTop = fileRow - shown + 1;
        }
    }

    private int pageRows() {
        return Math.max(1, layout().topRows() - 1);
    }

    private String selectedFolder() {
        final List<DirectoryTree.Row> shown = tree.rows();
        return treeRow >= 0 && treeRow < shown.size() ? shown.get(treeRow).folder() : folder;
    }

    private String selectedPath() {
        final MenuShellListing.Entry file = selectedFile();
        return file == null ? "" : join(folder, file.fullName());
    }

    private boolean atRoot() {
        return selectedFolder().length() <= 3;
    }

    private char driveLetter() {
        return folder.isEmpty() ? 'C' : Character.toUpperCase(folder.charAt(0));
    }

    private int driveIndex() {
        for (int i = 0; i < listing.drives().size(); i++) {
            if (listing.drives().get(i).letter() == driveLetter()) {
                return i;
            }
        }
        return 0;
    }

    private boolean likePattern(final String name) {
        return Wildcards.matches(pattern, name);
    }

    private static String join(final String in, final String name) {
        return in.endsWith("\\") ? in + name : in + "\\" + name;
    }

    private static String firstWord(final String line) {
        final int space = line.indexOf(' ');
        return (space < 0 ? line : line.substring(0, space)).toUpperCase(Locale.ROOT);
    }

    private static int clamp(final int value, final int count) {
        return count <= 0 ? 0 : Math.max(0, Math.min(count - 1, value));
    }
}
