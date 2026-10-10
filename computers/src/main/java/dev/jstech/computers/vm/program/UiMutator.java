/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every change a window or a widget goes through, whether the program makes it or a player does: the one door, so what
 * a change costs and what it leaves on the program's heap is worked out in a single place.
 *
 * <p>What a widget holds is on the program's heap like everything else, so it counts against the program's memory and
 * comes back after a save. Whatever the runtime makes for a widget (a row of a list, a stroke, a place in a window) is
 * adopted as it is made, and a list that grows or shrinks is resized with it. The texts a widget holds are copies of
 * its own: a program's text is copied in and a read hands a copy out, so when a widget lets go of a text it replaced,
 * or a canvas of the strokes it cleared, nothing the program holds goes with it.
 */
@TextHolder
final class UiMutator {

    /**
     * What a player did, as the program is to hear it.
     *
     * @param handler the name of the handler to call
     * @param action  for a generic component, the name of what happened and what came with it, as plain values; null
     *                for everything else, whose handlers take nothing
     */
    record Heard(String handler, List<Object> action) {
    }

    private final Heap heap;
    /** The windows the program has open, as they are now, which a row or a column growing inside one must still fit. */
    private final List<Values.Obj> open;

    /** What a window may not hold more of, counting every widget inside its rows and columns. */
    private static final TextKey TOO_MANY = TextKey.of("jsc.vm.ui_mutator.too_many",
            "a window holds at most %s widgets, counting the ones inside rows and columns");
    private static final TextKey WINDOW_FULL = TextKey.of("jsc.vm.ui_mutator.window_full",
            "a window holds at most %s widgets");
    private static final TextKey BOX_FULL = TextKey.of("jsc.vm.ui_mutator.box_full",
            "a row or a column holds at most %s widgets");
    private static final TextKey BOX_INSIDE_ITSELF = TextKey.of("jsc.vm.ui_mutator.box_inside_itself",
            "a row or a column cannot hold itself or a row or a column it is inside");
    private static final TextKey HOLDS_ITSELF = TextKey.of("jsc.vm.ui_mutator.holds_itself",
            "a widget cannot hold itself or a widget it is inside");
    private static final TextKey LIST_FULL = TextKey.of("jsc.vm.ui_mutator.list_full",
            "a list holds at most %s rows");
    private static final TextKey CANVAS_FULL = TextKey.of("jsc.vm.ui_mutator.canvas_full",
            "a canvas holds at most %s strokes");
    private static final TextKey MENU_FULL = TextKey.of("jsc.vm.ui_mutator.menu_full",
            "a menu holds at most %s entries");
    private static final TextKey TAB_FULL = TextKey.of("jsc.vm.ui_mutator.tab_full",
            "a tab view holds at most %s pages");
    private static final TextKey COLUMNS_FULL = TextKey.of("jsc.vm.ui_mutator.columns_full",
            "a table has at most %s columns");
    private static final TextKey SECTIONS_FULL = TextKey.of("jsc.vm.ui_mutator.sections_full",
            "a status bar shows at most %s sections");
    private static final TextKey NO_SUCH_NODE = TextKey.of("jsc.vm.ui_mutator.no_such_node",
            "there is no node %s in this tree");
    private static final TextKey NO_SUCH_CELL = TextKey.of("jsc.vm.ui_mutator.no_such_cell",
            "there is no row %s, column %s in this table");
    private static final TextKey WINDOW_NOT_OPEN = TextKey.of("jsc.vm.ui_mutator.window_not_open",
            "a dialog opens over a window that is open");
    private static final TextKey DIALOG_UP = TextKey.of("jsc.vm.ui_mutator.dialog_up",
            "this window already has a dialog open over it");
    private static final TextKey KIND_REFUSES = TextKey.of("jsc.vm.ui_mutator.kind_refuses",
            "a %s component does not take that value");
    private static final TextKey NOT_WRITABLE = TextKey.of("jsc.vm.ui_mutator.not_writable",
            "%s is not a program's to write");

    /** The fields the runtime keeps for itself, which a program reads and never writes. */
    private static final Set<String> KEPT = Set.of(UiWidgets.OPEN, UiWidgets.ID, UiWidgets.COUNT, UiWidgets.PLACED,
            UiWidgets.CHILDREN, UiWidgets.WEIGHTS, UiWidgets.ITEMS, UiWidgets.RIGHTS, UiWidgets.DRAWING,
            UiWidgets.REVISION, UiWidgets.EPOCH, UiWidgets.COLUMNS, UiWidgets.PARENTS, UiWidgets.POINTS,
            UiWidgets.MENU, UiWidgets.DIALOG, UiWidgets.PICKED, UiWidgets.PICKED_MENU, UiWidgets.KIND,
            UiWidgets.ON_ACTION);
    /** The calls that only read what a widget holds, which change nothing anybody sees. */
    private static final Set<String> READS = Set.of("Cell", "NodeText");
    /** The longest name a component's renderer may give what happened. */
    private static final int MOST_ACTION_NAME = 64;

    UiMutator(final Heap heap, final List<Values.Obj> open) {
        this.heap = heap;
        this.open = open;
    }

    /**
     * Answers a call on one of them, giving back what the call gives back or null.
     *
     * <p>Opening and closing a window are not here: those reach the machine, and the runtime makes them.
     */
    Object call(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Object answer = switch (self.type()) {
            case UiWidgets.ROW, UiWidgets.COLUMN -> this.box(self, member, arguments, line);
            case UiWidgets.LIST_BOX -> this.list(self, member, arguments, line);
            case UiWidgets.RADIO_GROUP, UiWidgets.COMBO_BOX, UiWidgets.ITEM_PICKER, UiWidgets.CONTEXT_MENU,
                 UiWidgets.STATUS_BAR, UiWidgets.LOG_VIEW -> this.entries(self, member, arguments, line);
            case UiWidgets.TAB_VIEW -> this.tabs(self, member, arguments, line);
            case UiWidgets.TABLE -> this.table(self, member, arguments, line);
            case UiWidgets.TREE_VIEW -> this.tree(self, member, arguments, line);
            case UiWidgets.MENU_BAR -> this.menuBar(self, member, arguments, line);
            case UiWidgets.CHART -> this.chart(self, member, arguments, line);
            case UiWidgets.OPEN_FILE_DIALOG, UiWidgets.SAVE_FILE_DIALOG -> this.dialog(self, member, arguments, line);
            case UiWidgets.OPERATION_VIEW -> this.operation(self, member, arguments, line);
            case UiWidgets.GENERIC -> this.generic(self, member, arguments, line);
            case UiWidgets.CANVAS -> this.canvas(self, member, arguments, line);
            case UiWidgets.WINDOW -> this.window(self, member, arguments, line);
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        };
        if (!READS.contains(member)) {
            this.changed(self);
        }
        return answer;
    }

    /**
     * Gives a widget the context menu just made for it, as a menu made for a widget is that widget's; one it had
     * before is let go of the widget and shows nowhere any more.
     */
    void attachMenu(final Values.Obj owner, final Values.Obj menu, final int line) {
        final Values.Obj widget = UiWidgets.placeableWidget(owner, line);
        widget.set(UiWidgets.MENU, menu);
        this.changed(widget);
    }

    /* Something shown has changed: the window's own revision moves, or that of every open window showing the widget. */
    private void changed(final Values.Obj self) {
        if (UiWidgets.WINDOW.equals(self.type())) {
            UiWidgets.touch(self);
            return;
        }
        for (final Values.Obj window : this.open) {
            if (UiWidgets.shows(window, self)) {
                UiWidgets.touch(window);
            }
        }
    }

    private Object window(final Values.Obj self, final String member, final List<Object> arguments,
                          final int line) {
        if (!"Add".equals(member) || arguments.size() < 5) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(UiWidgets.WINDOW, member));
        }
        final Values.ListValue placed = UiWidgets.listOf(self, UiWidgets.PLACED, line);
        if (placed.items().size() >= UiWidgets.MOST_WIDGETS) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, WINDOW_FULL.with(UiWidgets.MOST_WIDGETS));
        }
        final Values.Obj where = new Values.Obj(UiWidgets.PLACE);
        where.set("Widget", UiWidgets.placeableWidget(arguments.getFirst(), line));
        where.set("X", Numbers.toInt(arguments.get(1)));
        where.set("Y", Numbers.toInt(arguments.get(2)));
        where.set(UiWidgets.WIDTH, Numbers.toInt(arguments.get(3)));
        where.set(UiWidgets.HEIGHT, Numbers.toInt(arguments.get(4)));
        this.heap.adopt(where, line);
        placed.items().add(where);
        if (UiWidgets.count(self) > UiWidgets.MOST_WIDGETS) {
            placed.items().removeLast();
            this.heap.release(where);
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_MANY.with(UiWidgets.MOST_WIDGETS));
        }
        this.fit(placed, line);
        return null;
    }

    private Object box(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue children = UiWidgets.listOf(self, UiWidgets.CHILDREN, line);
        final Values.ListValue weights = UiWidgets.listOf(self, UiWidgets.WEIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (children.items().size() >= UiWidgets.MOST_WIDGETS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, BOX_FULL.with(UiWidgets.MOST_WIDGETS));
                }
                final Values.Obj child =
                        UiWidgets.placeableWidget(arguments.isEmpty() ? null : arguments.getFirst(), line);
                if (UiWidgets.holds(child, self)) {
                    throw new Halt(Halt.Reason.REFUSED, line, BOX_INSIDE_ITSELF.text());
                }
                children.items().add(child);
                weights.items().add(arguments.size() > 1 ? Math.max(0, Numbers.toInt(arguments.get(1))) : 0);
                this.stillFits(self, children, weights, line);
            }
            case "Clear" -> {
                children.items().clear();
                weights.items().clear();
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(children, line);
        this.fit(weights, line);
        return null;
    }

    /*
     * A widget that grew must still fit a window, whether it is already inside an open one or on its way into one:
     * past the most, the widget just added comes back out and the program halts.
     */
    private void stillFits(final Values.Obj self, final Values.ListValue children, final Values.ListValue alongside,
                           final int line) {
        if (this.overfull(self)) {
            children.items().removeLast();
            alongside.items().removeLast();
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_MANY.with(UiWidgets.MOST_WIDGETS));
        }
    }

    private boolean overfull(final Values.Obj self) {
        boolean over = UiWidgets.size(self) > UiWidgets.MOST_WIDGETS;
        for (final Values.Obj window : this.open) {
            over = over || UiWidgets.count(window) > UiWidgets.MOST_WIDGETS;
        }
        return over;
    }

    private Object list(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue items = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        final Values.ListValue rights = UiWidgets.listOf(self, UiWidgets.RIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (items.items().size() >= UiWidgets.MOST_ROWS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, LIST_FULL.with(UiWidgets.MOST_ROWS));
                }
                items.items().add(this.heap.adopt(UiWidgets.text(arguments, 0, ""), line));
                rights.items().add(this.heap.adopt(UiWidgets.text(arguments, 1, ""), line));
            }
            case "Clear" -> {
                this.letGo(items.items());
                this.letGo(rights.items());
                items.items().clear();
                rights.items().clear();
                self.set(UiWidgets.SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                    FieldAccess.HAS_NO.with(UiWidgets.LIST_BOX, member));
        }
        this.fit(items, line);
        this.fit(rights, line);
        self.set(UiWidgets.COUNT, items.items().size());
        return null;
    }

    /*
     * A widget that is a run of texts: the choices of a radio group, a combo box or an item picker, the entries of a
     * context menu, the sections of a status bar and the lines of a log. A log keeps its newest lines and lets the
     * oldest go, as a log on any screen does; the rest refuse to grow past what they hold.
     */
    private Object entries(final Values.Obj self, final String member, final List<Object> arguments,
                           final int line) {
        final Values.ListValue items = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        switch (member) {
            case "Add" -> {
                final boolean log = UiWidgets.LOG_VIEW.equals(self.type());
                if (log && items.items().size() >= UiWidgets.MOST_LOG) {
                    this.heap.release(items.items().removeFirst());
                }
                final int most = mostEntries(self.type());
                if (items.items().size() >= most) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, fullOf(self.type()).with(most));
                }
                items.items().add(this.heap.adopt(UiWidgets.text(arguments, 0, ""), line));
            }
            case "Clear" -> {
                this.letGo(items.items());
                items.items().clear();
                if (self.get(UiWidgets.SELECTED) != null) {
                    self.set(UiWidgets.SELECTED, 0);
                }
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(items, line);
        if (self.get(UiWidgets.COUNT) != null) {
            self.set(UiWidgets.COUNT, items.items().size());
        }
        return null;
    }

    private static int mostEntries(final String type) {
        return switch (type) {
            case UiWidgets.CONTEXT_MENU -> UiWidgets.MOST_MENU;
            case UiWidgets.STATUS_BAR -> UiWidgets.MOST_SECTIONS;
            case UiWidgets.LOG_VIEW -> UiWidgets.MOST_LOG;
            default -> UiWidgets.MOST_ROWS;
        };
    }

    private static TextKey fullOf(final String type) {
        return switch (type) {
            case UiWidgets.CONTEXT_MENU -> MENU_FULL;
            case UiWidgets.STATUS_BAR -> SECTIONS_FULL;
            default -> LIST_FULL;
        };
    }

    /* A tab view: each page a widget with the title of its tab, the first one added the one shown. */
    private Object tabs(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue pages = UiWidgets.listOf(self, UiWidgets.CHILDREN, line);
        final Values.ListValue titles = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        switch (member) {
            case "Add" -> {
                if (pages.items().size() >= UiWidgets.MOST_TABS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TAB_FULL.with(UiWidgets.MOST_TABS));
                }
                final Values.Obj page = UiWidgets.placeableWidget(arguments.size() > 1 ? arguments.get(1) : null,
                        line);
                if (UiWidgets.holds(page, self)) {
                    throw new Halt(Halt.Reason.REFUSED, line, HOLDS_ITSELF.text());
                }
                pages.items().add(page);
                titles.items().add(this.heap.adopt(UiWidgets.text(arguments, 0, ""), line));
                if (this.overfull(self)) {
                    pages.items().removeLast();
                    this.heap.release(titles.items().removeLast());
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_MANY.with(UiWidgets.MOST_WIDGETS));
                }
                if (Numbers.toInt(self.get(UiWidgets.SELECTED)) == 0) {
                    self.set(UiWidgets.SELECTED, 1);
                }
            }
            case "Clear" -> {
                pages.items().clear();
                this.letGo(titles.items());
                titles.items().clear();
                self.set(UiWidgets.SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(pages, line);
        this.fit(titles, line);
        self.set(UiWidgets.COUNT, pages.items().size());
        return null;
    }

    /*
     * A table: its headers, and its rows each kept as one text with the cells parted by a tab, which no cell can hold
     * since a tab in a cell is written as a space. One text a row keeps a big table as light as a list.
     */
    private Object table(final Values.Obj self, final String member, final List<Object> arguments,
                         final int line) {
        final Values.ListValue columns = UiWidgets.listOf(self, UiWidgets.COLUMNS, line);
        final Values.ListValue rows = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        switch (member) {
            case "AddColumn" -> {
                if (columns.items().size() >= UiWidgets.MOST_COLUMNS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, COLUMNS_FULL.with(UiWidgets.MOST_COLUMNS));
                }
                columns.items().add(this.heap.adopt(cell(UiWidgets.text(arguments, 0, "")), line));
            }
            case "AddRow" -> {
                if (rows.items().size() >= UiWidgets.MOST_ROWS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, LIST_FULL.with(UiWidgets.MOST_ROWS));
                }
                rows.items().add(this.heap.adopt(joined(arguments), line));
            }
            case "Cell" -> {
                return this.cellAt(rows, Numbers.toInt(arguments.get(0)), Numbers.toInt(arguments.get(1)), line);
            }
            case "Clear" -> {
                this.letGo(rows.items());
                rows.items().clear();
                self.set(UiWidgets.SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(columns, line);
        this.fit(rows, line);
        self.set(UiWidgets.COUNT, rows.items().size());
        return null;
    }

    /* The cells a row was given, one text each or a list of them, joined the way a table keeps a row. */
    private static String joined(final List<Object> arguments) {
        final StringBuilder row = new StringBuilder();
        final List<?> cells = arguments.size() == 1 && arguments.getFirst() instanceof Values.ListValue list
                ? list.items() : arguments;
        for (int i = 0; i < Math.min(cells.size(), UiWidgets.MOST_COLUMNS); i++) {
            if (i > 0) {
                row.append('\t');
            }
            row.append(cell(cells.get(i) == null ? "" : String.valueOf(cells.get(i))));
        }
        return row.toString();
    }

    private static String cell(final String said) {
        return said.replace('\t', ' ');
    }

    /* A cell, counted from one as the table draws it; asking past the cells a row has is asking for nothing. */
    private String cellAt(final Values.ListValue rows, final int row, final int column, final int line) {
        if (row < 1 || row > rows.items().size() || column < 1 || column > UiWidgets.MOST_COLUMNS) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NO_SUCH_CELL.with(row, column));
        }
        final String[] cells = String.valueOf(rows.items().get(row - 1)).split("\t", -1);
        return this.heap.text(column <= cells.length ? cells[column - 1] : "", line);
    }

    /* A tree: each node a text and the node it hangs from, counted from one in the order they were added. */
    private Object tree(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue nodes = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        final Values.ListValue parents = UiWidgets.listOf(self, UiWidgets.PARENTS, line);
        switch (member) {
            case "Add" -> {
                if (nodes.items().size() >= UiWidgets.MOST_ROWS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, LIST_FULL.with(UiWidgets.MOST_ROWS));
                }
                final int parent = arguments.size() > 1 ? Numbers.toInt(arguments.get(1)) : 0;
                if (parent < 0 || parent > nodes.items().size()) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NO_SUCH_NODE.with(parent));
                }
                nodes.items().add(this.heap.adopt(UiWidgets.text(arguments, 0, ""), line));
                parents.items().add(parent);
            }
            case "NodeText" -> {
                final int node = Numbers.toInt(arguments.getFirst());
                if (node < 1 || node > nodes.items().size()) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NO_SUCH_NODE.with(node));
                }
                return this.heap.text(String.valueOf(nodes.items().get(node - 1)), line);
            }
            case "Clear" -> {
                this.letGo(nodes.items());
                nodes.items().clear();
                parents.items().clear();
                self.set(UiWidgets.SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(nodes, line);
        this.fit(parents, line);
        self.set(UiWidgets.COUNT, nodes.items().size());
        return "Add".equals(member) ? nodes.items().size() : null;
    }

    /* A menu bar: each entry under the menu it was added to; a menu is there once it has an entry. */
    private Object menuBar(final Values.Obj self, final String member, final List<Object> arguments,
                           final int line) {
        final Values.ListValue entries = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        final Values.ListValue menus = UiWidgets.listOf(self, UiWidgets.RIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (entries.items().size() >= UiWidgets.MOST_MENU) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, MENU_FULL.with(UiWidgets.MOST_MENU));
                }
                menus.items().add(this.heap.adopt(UiWidgets.text(arguments, 0, ""), line));
                entries.items().add(this.heap.adopt(UiWidgets.text(arguments, 1, ""), line));
            }
            case "Clear" -> {
                this.letGo(entries.items());
                this.letGo(menus.items());
                entries.items().clear();
                menus.items().clear();
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(entries, line);
        this.fit(menus, line);
        return null;
    }

    /* A chart keeps its newest numbers and lets the oldest go, so a program can add one a tick for ever. */
    private Object chart(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue points = UiWidgets.listOf(self, UiWidgets.POINTS, line);
        switch (member) {
            case "Add" -> {
                if (points.items().size() >= UiWidgets.MOST_POINTS) {
                    points.items().removeFirst();
                }
                points.items().add(Numbers.toDouble(arguments.getFirst()));
            }
            case "Clear" -> points.items().clear();
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        this.fit(points, line);
        self.set(UiWidgets.COUNT, points.items().size());
        return null;
    }

    /*
     * A file dialog opens over one of the program's open windows, which keeps it until the player answers; the epoch
     * moves on every showing, so a screen tells a dialog shown again from the one it already put up.
     */
    private Object dialog(final Values.Obj self, final String member, final List<Object> arguments,
                          final int line) {
        if (!"Show".equals(member)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        if (!((arguments.isEmpty() ? null : arguments.getFirst()) instanceof Values.Obj window)
                || !UiWidgets.WINDOW.equals(window.type()) || !Boolean.TRUE.equals(window.get(UiWidgets.OPEN))) {
            throw new Halt(Halt.Reason.REFUSED, line, WINDOW_NOT_OPEN.text());
        }
        final Object up = window.get(UiWidgets.DIALOG);
        if (up != null && up != self) {
            throw new Halt(Halt.Reason.REFUSED, line, DIALOG_UP.text());
        }
        // A dialog is up over one window at a time, so showing it again over another takes it off the first.
        this.lowerDialog(self);
        window.set(UiWidgets.DIALOG, self);
        self.set(UiWidgets.EPOCH, Numbers.toInt(self.get(UiWidgets.EPOCH)) + 1);
        UiWidgets.touch(window);
        return null;
    }

    /* An operation view shows what the network says of an operation: the statement, how it stands and how far. */
    private Object operation(final Values.Obj self, final String member, final List<Object> arguments,
                             final int line) {
        if (!"Show".equals(member) || !(arguments.getFirst() instanceof Values.Obj info)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        final String statement = String.join(" ", String.valueOf(info.get("Type")),
                String.valueOf(info.get("Requested")), String.valueOf(info.get("Item")));
        this.replace(self, UiWidgets.TEXT, statement, line);
        this.replace(self, UiWidgets.STATE, String.valueOf(info.get("Status")), line);
        self.set(UiWidgets.DONE, Math.max(0L, Numbers.toLong(info.get("Moved"))));
        self.set(UiWidgets.TOTAL, Math.max(0L, Numbers.toLong(info.get("Requested"))));
        return null;
    }

    /* A generic component only hears who is to be told what a player did; what it holds is written, not called. */
    private Object generic(final Values.Obj self, final String member, final List<Object> arguments,
                           final int line) {
        if (!UiWidgets.ON_ACTION.equals(member)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, FieldAccess.HAS_NO.with(self.type(), member));
        }
        self.set(UiWidgets.ON_ACTION, arguments.isEmpty() ? null : arguments.getFirst());
        return null;
    }

    /*
     * What a canvas is asked to draw is kept as it was asked, a stroke at a time, and drawn again
     * whenever the window is: a picture is what the program said, not pixels the machine has to keep.
     */
    private Object canvas(final Values.Obj self, final String member, final List<Object> arguments,
                          final int line) {
        final Values.ListValue drawing = UiWidgets.listOf(self, UiWidgets.DRAWING, line);
        if ("Clear".equals(member)) {
            for (final Object one : drawing.items()) {
                if (one instanceof Values.Obj stroke) {
                    this.letGo(stroke.all().values());
                    this.heap.release(stroke);
                }
            }
            drawing.items().clear();
            self.set(UiWidgets.EPOCH, Numbers.toInt(self.get(UiWidgets.EPOCH)) + 1);
            final Values.Obj stroke = stroke("Clear");
            stroke.set("Colour", Numbers.toInt(arguments.isEmpty() ? 0 : arguments.getFirst()));
            this.draw(drawing, stroke, line);
            return null;
        }
        if (drawing.items().size() >= UiWidgets.MOST_STROKES) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, CANVAS_FULL.with(UiWidgets.MOST_STROKES));
        }
        final Values.Obj stroke = stroke(member);
        switch (member) {
            case "FillRect", "DrawLine" -> {
                stroke.set("X", Numbers.toInt(arguments.get(0)));
                stroke.set("Y", Numbers.toInt(arguments.get(1)));
                stroke.set("X2", Numbers.toInt(arguments.get(2)));
                stroke.set("Y2", Numbers.toInt(arguments.get(3)));
                stroke.set("Colour", Numbers.toInt(arguments.get(4)));
            }
            case "DrawText" -> {
                stroke.set(UiWidgets.TEXT, UiWidgets.text(arguments, 0, ""));
                stroke.set("X", Numbers.toInt(arguments.get(1)));
                stroke.set("Y", Numbers.toInt(arguments.get(2)));
                stroke.set("Colour", Numbers.toInt(arguments.get(3)));
            }
            case "SetPixel" -> {
                stroke.set("X", Numbers.toInt(arguments.get(0)));
                stroke.set("Y", Numbers.toInt(arguments.get(1)));
                stroke.set("Colour", Numbers.toInt(arguments.get(2)));
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                    FieldAccess.HAS_NO.with(UiWidgets.CANVAS, member));
        }
        this.draw(drawing, stroke, line);
        return null;
    }

    /* A stroke of that kind, its name a copy of its own so a cleared canvas can let the whole stroke go. */
    private static Values.Obj stroke(final String kind) {
        final Values.Obj stroke = new Values.Obj("Stroke");
        stroke.set("Kind", new String(kind.toCharArray()));
        return stroke;
    }

    /* Puts a finished stroke on the heap and on the canvas; its fields are its size, so they are set first. */
    private void draw(final Values.ListValue drawing, final Values.Obj stroke, final int line) {
        this.heap.adopt(stroke, line);
        drawing.items().add(stroke);
        this.fit(drawing, line);
    }

    /**
     * What a program writes on a widget, kept as the widget holds it: a text as a copy of the widget's own, a number
     * within what the widget can show, and a generic component's value as a copy it alone holds.
     *
     * @return what the write weighs in kilobytes beyond the draw every write costs: a generic component's value is
     *         charged by its size, and anything else weighs nothing more
     */
    long write(final Values.Obj self, final String name, final Object value, final int line) {
        final int most = UiWidgets.WIDTH.equals(name) ? UiWidgets.MOST_WIDE : UiWidgets.MOST_TALL;
        long kilobytes = 0;
        if (KEPT.contains(name)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, NOT_WRITABLE.with(name));
        }
        switch (name) {
            // A window has a size of its own; a widget asking for none takes whatever it needs.
            case UiWidgets.WIDTH, UiWidgets.HEIGHT -> self.set(name, UiWidgets.WINDOW.equals(self.type())
                    ? Math.clamp(Numbers.toInt(value), UiWidgets.LEAST_SIDE, most)
                    : UiWidgets.widgetSide(Numbers.toInt(value), most));
            case UiWidgets.CONTENT -> this.content(self, value, line);
            case UiWidgets.VALUE, UiWidgets.LEAST, UiWidgets.MOST, UiWidgets.STEP -> this.number(self, name, value);
            case UiWidgets.SELECTED -> self.set(name, Math.clamp(Numbers.toInt(value),
                    UiWidgets.TAB_VIEW.equals(self.type()) ? Math.min(1, count(self)) : 0, count(self)));
            case UiWidgets.AMOUNT -> self.set(name, Math.max(0, Numbers.toInt(value)));
            case UiWidgets.DONE, UiWidgets.TOTAL -> self.set(name, Math.max(0L, Numbers.toLong(value)));
            case UiWidgets.DATA -> kilobytes = this.data(self, value, line);
            default -> {
                if (value instanceof String said) {
                    this.replace(self, name, said, line);
                } else {
                    self.set(name, value);
                }
            }
        }
        this.changed(self);
        return kilobytes;
    }

    private static int count(final Values.Obj self) {
        return Math.max(0, Numbers.toInt(self.get(UiWidgets.COUNT)));
    }

    /*
     * A number box or a slider keeps its value between its least and its most, moved in from wherever the program put
     * it; a bar shows whatever it is given, as it always has.
     */
    private void number(final Values.Obj self, final String name, final Object value) {
        final boolean ranged = UiWidgets.NUMBER_BOX.equals(self.type()) || UiWidgets.SLIDER.equals(self.type());
        if (!ranged) {
            self.set(name, Numbers.toInt(value));
            return;
        }
        final int asked = Numbers.toInt(value);
        switch (name) {
            case UiWidgets.STEP -> self.set(name, Math.max(1, asked));
            case UiWidgets.LEAST -> {
                self.set(name, asked);
                self.set(UiWidgets.MOST, Math.max(asked, Numbers.toInt(self.get(UiWidgets.MOST))));
            }
            case UiWidgets.MOST -> {
                self.set(name, asked);
                self.set(UiWidgets.LEAST, Math.min(asked, Numbers.toInt(self.get(UiWidgets.LEAST))));
            }
            default -> self.set(name, asked);
        }
        final int least = Numbers.toInt(self.get(UiWidgets.LEAST));
        final int top = Numbers.toInt(self.get(UiWidgets.MOST));
        self.set(UiWidgets.VALUE, Math.clamp(Numbers.toInt(self.get(UiWidgets.VALUE)), least, top));
    }

    /*
     * A generic component takes a copy of what it is handed, checked against the bounds every component shares and
     * then against its kind; the copy it held before is let go whole, since nothing else ever held any of it.
     */
    private long data(final Values.Obj self, final Object value, final int line) {
        final ComponentValues.Copied copied = ComponentValues.copy(value, this.heap, line);
        final IComponentRule kind = ComponentRules.find(String.valueOf(self.get(UiWidgets.KIND)));
        if (kind != null && !kind.takesData(ComponentValues.decode(ComponentValues.encode(copied.value())))) {
            this.letGoAll(copied.value());
            throw new Halt(Halt.Reason.REFUSED, line, KIND_REFUSES.with(kind.id()));
        }
        this.letGoAll(self.get(UiWidgets.DATA));
        self.set(UiWidgets.DATA, copied.value());
        return (copied.bytes() + 1023) / 1024;
    }

    /* What a window, a group box or a scroll view shows must fit with what is already there; past it, the old stays. */
    private void content(final Values.Obj self, final Object value, final int line) {
        final Object before = self.get(UiWidgets.CONTENT);
        final Values.Obj shown = value == null ? null : UiWidgets.placeableWidget(value, line);
        if (shown != null && !UiWidgets.WINDOW.equals(self.type()) && UiWidgets.holds(shown, self)) {
            throw new Halt(Halt.Reason.REFUSED, line, HOLDS_ITSELF.text());
        }
        self.set(UiWidgets.CONTENT, shown);
        final boolean over = UiWidgets.WINDOW.equals(self.type())
                ? UiWidgets.count(self) > UiWidgets.MOST_WIDGETS : this.overfull(self);
        if (over) {
            self.set(UiWidgets.CONTENT, before);
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_MANY.with(UiWidgets.MOST_WIDGETS));
        }
    }

    /**
     * Takes what a player did to a widget: the widget is changed the way they changed it, and what the program is to
     * hear comes back, or null when that is not something the widget answers. What a player types can run the program
     * out of memory like anything the program holds, which halts it. Whatever the widget could not really have
     * received (a row the list lacks, a point off the canvas, a line longer than a box takes, an action its kind
     * refuses) is refused the same way, since the server takes nothing on the client's word.
     */
    Heard accept(final Values.Obj widget, final String kind, final List<Object> values) {
        final Object first = values.isEmpty() ? null : values.getFirst();
        if (UiWidgets.GENERIC.equals(widget.type())) {
            final List<Object> action = "action".equals(kind) ? action(widget, first) : null;
            return action == null ? null : new Heard(UiWidgets.ON_ACTION, action);
        }
        final int picked = first instanceof Number number ? number.intValue() : -1;
        final String handler = switch (widget.type() + "/" + kind) {
            case UiWidgets.BUTTON + "/click", UiWidgets.ITEM_SLOT + "/click" -> "OnClick";
            case UiWidgets.TEXT_BOX + "/text", UiWidgets.TEXT_BOX + "/submit" ->
                    this.typed(widget, first, UiWidgets.MOST_TEXT) ? ("text".equals(kind) ? "OnChange" : "OnSubmit")
                            : null;
            case UiWidgets.TEXT_AREA + "/text" -> this.typed(widget, first, UiWidgets.MOST_AREA) ? "OnChange" : null;
            case UiWidgets.CHECK_BOX + "/toggle" -> {
                widget.set(UiWidgets.CHECKED, Boolean.TRUE.equals(first));
                yield "OnToggle";
            }
            case UiWidgets.NUMBER_BOX + "/number", UiWidgets.SLIDER + "/number" -> {
                if (picked < Numbers.toInt(widget.get(UiWidgets.LEAST))
                        || picked > Numbers.toInt(widget.get(UiWidgets.MOST))) {
                    yield null;
                }
                widget.set(UiWidgets.VALUE, picked);
                yield "OnChange";
            }
            // Counted from one as the list draws it, with none as zero: past the last row there is no row.
            case UiWidgets.LIST_BOX + "/select", UiWidgets.RADIO_GROUP + "/select", UiWidgets.COMBO_BOX + "/select",
                 UiWidgets.TABLE + "/select", UiWidgets.TREE_VIEW + "/select" -> this.select(widget, picked, 0);
            case UiWidgets.TAB_VIEW + "/select" -> this.select(widget, picked, 1);
            case UiWidgets.ITEM_PICKER + "/select" -> {
                final String said = this.select(widget, picked, 1);
                if (said != null) {
                    this.replace(widget, UiWidgets.PICKED, entry(widget, UiWidgets.ITEMS, picked), 0);
                }
                yield said;
            }
            case UiWidgets.MENU_BAR + "/pick", UiWidgets.CONTEXT_MENU + "/pick" -> this.pick(widget, picked);
            case UiWidgets.OPEN_FILE_DIALOG + "/file", UiWidgets.SAVE_FILE_DIALOG + "/file" -> {
                final String path = first == null ? "" : String.valueOf(first);
                if (path.isEmpty() || path.length() > UiWidgets.MOST_TEXT || !this.lowerDialog(widget)) {
                    yield null;
                }
                this.replace(widget, UiWidgets.PATH, path, 0);
                yield "OnChoose";
            }
            case UiWidgets.OPEN_FILE_DIALOG + "/cancel", UiWidgets.SAVE_FILE_DIALOG + "/cancel" ->
                    this.lowerDialog(widget) ? "OnCancel" : null;
            case UiWidgets.CANVAS + "/click" -> {
                final int x = Numbers.toInt(first);
                final int y = values.size() > 1 ? Numbers.toInt(values.get(1)) : 0;
                if (!onCanvas(x, Numbers.toInt(widget.get(UiWidgets.WIDTH)), UiWidgets.MOST_WIDE)
                        || !onCanvas(y, Numbers.toInt(widget.get(UiWidgets.HEIGHT)), UiWidgets.MOST_TALL)) {
                    yield null;
                }
                widget.set(UiWidgets.CLICK_X, x);
                widget.set(UiWidgets.CLICK_Y, y);
                yield "OnClick";
            }
            default -> null;
        };
        if (handler == null) {
            return null;
        }
        // A button pressed changes nothing it shows; anything else the player took has.
        if (!UiWidgets.BUTTON.equals(widget.type()) && !UiWidgets.ITEM_SLOT.equals(widget.type())) {
            this.changed(widget);
        }
        return new Heard(handler, null);
    }

    private boolean typed(final Values.Obj widget, final Object first, final int most) {
        final String said = first == null ? "" : String.valueOf(first);
        if (said.length() > most) {
            return false;
        }
        this.replace(widget, UiWidgets.TEXT, said, 0);
        return true;
    }

    private String select(final Values.Obj widget, final int picked, final int least) {
        if (picked < least || picked > count(widget)) {
            return null;
        }
        widget.set(UiWidgets.SELECTED, picked);
        return "OnSelect";
    }

    /* A menu entry, counted from one; a menu bar also says which of its menus the entry was under. */
    private String pick(final Values.Obj widget, final int picked) {
        final int entries = widget.get(UiWidgets.ITEMS) instanceof Values.ListValue list ? list.size() : 0;
        if (picked < 1 || picked > entries) {
            return null;
        }
        this.replace(widget, UiWidgets.PICKED, entry(widget, UiWidgets.ITEMS, picked), 0);
        if (UiWidgets.MENU_BAR.equals(widget.type())) {
            this.replace(widget, UiWidgets.PICKED_MENU, entry(widget, UiWidgets.RIGHTS, picked), 0);
        }
        return "OnPick";
    }

    private static String entry(final Values.Obj widget, final String field, final int picked) {
        return widget.get(field) instanceof Values.ListValue list && picked >= 1 && picked <= list.size()
                ? String.valueOf(list.items().get(picked - 1)) : "";
    }

    /* Takes a dialog off the window it was up over; false when it was up over none, so it could not be answered. */
    private boolean lowerDialog(final Values.Obj dialog) {
        boolean lowered = false;
        for (final Values.Obj window : this.open) {
            if (window.get(UiWidgets.DIALOG) == dialog) {
                window.set(UiWidgets.DIALOG, null);
                UiWidgets.touch(window);
                lowered = true;
            }
        }
        return lowered;
    }

    /*
     * What a player did to a generic component: a name and a value, sent together as the name, a nul, and the value's
     * letters. It is read under the bounds every component's value is held to and handed to the kind to judge; a kind
     * no mod in this game added hears nothing, since nobody can say what it would take.
     */
    private static List<Object> action(final Values.Obj widget, final Object first) {
        final String said = first == null ? "" : String.valueOf(first);
        final int cut = said.indexOf('\0');
        final IComponentRule kind = ComponentRules.find(String.valueOf(widget.get(UiWidgets.KIND)));
        if (cut < 1 || cut > MOST_ACTION_NAME || kind == null) {
            return null;
        }
        final String name = said.substring(0, cut);
        final Object value;
        try {
            value = ComponentValues.decode(said.substring(cut + 1));
        } catch (final IllegalArgumentException | IndexOutOfBoundsException unreadable) {
            return null;
        }
        if (!kind.takesAction(name, value)) {
            return null;
        }
        return Arrays.asList(name, value);
    }

    /*
     * Gives the widget a copy of its own of that text and lets its old copy go. The new copy is made first, so a
     * program out of memory halts with the widget as it was.
     */
    private void replace(final Values.Obj widget, final String field, final String said, final int line) {
        final Object own = this.heap.adopt(new String(said.toCharArray()), line);
        final Object old = widget.get(field);
        widget.set(field, own);
        if (old instanceof String) {
            this.heap.release(old);
        }
    }

    /* Lets go of the texts only a widget held; anything else among them weighs nothing of its own. */
    private void letGo(final Iterable<Object> held) {
        for (final Object one : held) {
            if (one instanceof String) {
                this.heap.release(one);
            }
        }
    }

    /* Lets go of a value a component alone held, every part of it, since the component made each part itself. */
    private void letGoAll(final Object held) {
        switch (held) {
            case null -> {
                return;
            }
            case Values.ListValue list -> list.items().forEach(this::letGoAll);
            case Values.Arr array -> array.all().forEach(this::letGoAll);
            case Values.MapValue map -> {
                for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                    this.letGoAll(entry.getKey());
                    this.letGoAll(entry.getValue());
                }
            }
            case Values.Obj object -> object.all().values().forEach(this::letGoAll);
            default -> {
                // A number, a bool or a character weighs nothing of its own; a text is let go below.
            }
        }
        this.heap.release(held);
    }

    /* Whether a point along one side falls on the canvas: its own length, or a window's most when it asked for none. */
    private static boolean onCanvas(final int at, final int asked, final int most) {
        return at >= 0 && at < (asked > 0 ? asked : most);
    }

    /* A list that grew or shrank weighs what it now holds. */
    private void fit(final Values.ListValue list, final int line) {
        this.heap.resize(list, list.bytes(), line);
    }
}
