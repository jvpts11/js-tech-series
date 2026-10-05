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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The windows and widgets a Σ# program makes, as the runtime keeps them.
 *
 * <p>A widget is an ordinary object on the program's own heap, so it is counted like everything else the
 * program holds and is written down and brought back with it. What it holds is what it is: a label holds
 * its text, a row holds its widgets and their weights, a canvas holds the drawing asked for. Nothing here
 * knows how wide a letter is or what a button looks like: where things land is worked out where they are
 * drawn, by the machine's own system, and this is only what the program said. Changing one goes through
 * {@link UiMutator}.
 *
 * <p>A widget that holds others holds them in one of three ways, and everything that walks a window follows
 * all three: the widgets of a row, a column or a tab view are its children, a group box or a scroll view shows
 * one widget as its content, and any widget may have a context menu of its own. A window may also have a file
 * dialog up over it, which is how a dialog's answer finds its way back.
 */
@TextHolder
public final class UiWidgets {

    public static final String WIDGET = "Widget";
    public static final String WINDOW = "Window";
    public static final String ROW = "Row";
    public static final String COLUMN = "Column";
    public static final String LABEL = "Label";
    public static final String BUTTON = "Button";
    public static final String TEXT_BOX = "TextBox";
    public static final String CHECK_BOX = "CheckBox";
    public static final String PROGRESS_BAR = "ProgressBar";
    public static final String LIST_BOX = "ListBox";
    public static final String CANVAS = "Canvas";
    public static final String MESSAGE_BOX = "MessageBox";
    public static final String TEXT_AREA = "TextArea";
    public static final String NUMBER_BOX = "NumberBox";
    public static final String SLIDER = "Slider";
    public static final String RADIO_GROUP = "RadioGroup";
    public static final String COMBO_BOX = "ComboBox";
    public static final String TAB_VIEW = "TabView";
    public static final String GROUP_BOX = "GroupBox";
    public static final String SCROLL_VIEW = "ScrollView";
    public static final String TABLE = "Table";
    public static final String TREE_VIEW = "TreeView";
    public static final String MENU_BAR = "MenuBar";
    public static final String CONTEXT_MENU = "ContextMenu";
    public static final String STATUS_BAR = "StatusBar";
    public static final String IMAGE = "Image";
    public static final String CHART = "Chart";
    public static final String LOG_VIEW = "LogView";
    public static final String OPEN_FILE_DIALOG = "OpenFileDialog";
    public static final String SAVE_FILE_DIALOG = "SaveFileDialog";
    public static final String ITEM_SLOT = "ItemSlot";
    public static final String ITEM_PICKER = "ItemPicker";
    public static final String OPERATION_VIEW = "OperationView";
    public static final String GENERIC = "GenericComponent";
    /** What a generic component hands its handler when a player acts on it: a name and a value. */
    public static final String COMPONENT_ACTION = "ComponentAction";

    /** The fields every widget has. */
    public static final String VISIBLE = "Visible";
    public static final String ENABLED = "Enabled";
    public static final String WIDTH = "Width";
    public static final String HEIGHT = "Height";
    public static final String TEXT = "Text";
    public static final String TITLE = "Title";
    public static final String CONTENT = "Content";
    public static final String CHILDREN = "Children";
    public static final String WEIGHTS = "Weights";
    public static final String SPACING = "Spacing";
    public static final String OPEN = "Open";
    public static final String PLACED = "Placed";
    public static final String ITEMS = "Items";
    public static final String RIGHTS = "Rights";
    public static final String SELECTED = "Selected";
    public static final String COUNT = "Count";
    public static final String CHECKED = "Checked";
    public static final String VALUE = "Value";
    public static final String LEAST = "Least";
    public static final String MOST = "Most";
    public static final String STEP = "Step";
    public static final String DRAWING = "Drawing";
    public static final String CLICK_X = "ClickX";
    public static final String CLICK_Y = "ClickY";
    public static final String PLACE = "Place";
    public static final String ID = "Id";
    /** The headers of a table's columns. */
    public static final String COLUMNS = "Columns";
    /** Under each node of a tree, the number of the node it hangs from, zero for a node at the top. */
    public static final String PARENTS = "Parents";
    /** The numbers a chart draws, oldest first. */
    public static final String POINTS = "Points";
    /** The context menu a widget opens when it is clicked with the other button. */
    public static final String MENU = "Menu";
    /** Whether a group of choices lies across rather than down. */
    public static final String ACROSS = "Across";
    /** What a player last picked from a menu, and from which menu of a menu bar. */
    public static final String PICKED = "Picked";
    public static final String PICKED_MENU = "PickedMenu";
    /** Where a file dialog starts and what it answered, where an image is read from. */
    public static final String PATH = "Path";
    /** The kinds of file a dialog offers, as a pattern such as {@code *.txt}. */
    public static final String FILTER = "Filter";
    /** The name a save dialog offers to begin with. */
    public static final String NAME = "Name";
    /** The file dialog a window has up over it, if any. */
    public static final String DIALOG = "Dialog";
    /** The item an item slot shows, and how many. */
    public static final String ITEM = "Item";
    public static final String AMOUNT = "Amount";
    /** What an operation view shows besides its statement. */
    public static final String STATE = "State";
    public static final String DONE = "Done";
    public static final String TOTAL = "Total";
    public static final String COMPUTER = "Computer";
    /** The kind a generic component is, which says what draws it, and what it holds. */
    public static final String KIND = "Kind";
    public static final String DATA = "Data";
    /** The handler a generic component calls with what a player did. */
    public static final String ON_ACTION = "OnAction";
    /** Set on a window that is only there to say something, which is what a message box is. */
    public static final String ASK = "Ask";
    /** How many times what a window shows has changed, so whatever draws it can tell one it already has. */
    public static final String REVISION = "Revision";
    /** How many times a canvas was cleared, or a dialog shown; between two clears its strokes only grow. */
    public static final String EPOCH = "Epoch";

    /** The most a program may keep in one list or draw on one canvas. */
    public static final int MOST_ROWS = 1024;
    public static final int MOST_STROKES = 4096;
    /** The most widgets one window may hold, counting its rows and columns. */
    public static final int MOST_WIDGETS = 256;
    /** The longest line a player can put in a program's text box. */
    public static final int MOST_TEXT = 256;
    /** The most a text area holds, every line of it. */
    public static final int MOST_AREA = 4096;
    /** The most columns a table has, menu entries a menu holds, sections a status bar shows. */
    public static final int MOST_COLUMNS = 8;
    public static final int MOST_MENU = 64;
    public static final int MOST_SECTIONS = 4;
    /** The most numbers a chart keeps, and lines a log view; past these the oldest go. */
    public static final int MOST_POINTS = 120;
    public static final int MOST_LOG = 256;

    /** How wide and tall a window may be asked to be, in the desktop's own pixels. */
    public static final int LEAST_SIDE = 60;
    /** The least a widget may be asked to be across or down; a widget is often far smaller than a window. */
    public static final int LEAST_WIDGET = 8;
    public static final int MOST_WIDE = 640;
    public static final int MOST_TALL = 360;

    private static final Set<String> KINDS = Set.of(WINDOW, ROW, COLUMN, LABEL, BUTTON, TEXT_BOX, CHECK_BOX,
            PROGRESS_BAR, LIST_BOX, CANVAS, MESSAGE_BOX, TEXT_AREA, NUMBER_BOX, SLIDER, RADIO_GROUP, COMBO_BOX,
            TAB_VIEW, GROUP_BOX, SCROLL_VIEW, TABLE, TREE_VIEW, MENU_BAR, CONTEXT_MENU, STATUS_BAR, IMAGE, CHART,
            LOG_VIEW, OPEN_FILE_DIALOG, SAVE_FILE_DIALOG, ITEM_SLOT, ITEM_PICKER, OPERATION_VIEW, GENERIC);
    /** What a window does not lay out with the rest: a menu waits for its click, a dialog is a window of its own. */
    private static final Set<String> FLOATING = Set.of(CONTEXT_MENU, OPEN_FILE_DIALOG, SAVE_FILE_DIALOG);
    /**
     * What a screen of letters cannot show: a picture, a chart, an item and what draws itself. A program on a machine
     * that only has its terminal is told so when it makes one, rather than given something that will never appear.
     */
    private static final Set<String> NOT_IN_TEXT = Set.of(IMAGE, CHART, ITEM_SLOT, ITEM_PICKER, OPERATION_VIEW,
            CANVAS, GENERIC);
    private static final TextKey NOT_A_WIDGET = TextKey.of("jsc.vm.ui_widgets.not_a_widget",
            "this is not a widget to put in a window");
    private static final TextKey NOT_PLACEABLE = TextKey.of("jsc.vm.ui_widgets.not_placeable",
            "a %s is not put in a window: it opens over one");
    private static final TextKey THIS_HAS_NO = TextKey.of("jsc.vm.ui_widgets.this_has_no", "this %s has no %s");

    private UiWidgets() {
    }

    /** Whether the runtime makes and answers for objects of that type. */
    public static boolean handles(final String type) {
        return KINDS.contains(type);
    }

    /** Whether a widget of that type is drawn inside a window rather than being the window. */
    public static boolean isWidget(final String type) {
        return handles(type) && !WINDOW.equals(type) && !MESSAGE_BOX.equals(type);
    }

    /** Whether a widget of that type takes a place in a window's layout, which a menu or a dialog does not. */
    public static boolean placeable(final String type) {
        return isWidget(type) && !FLOATING.contains(type);
    }

    /** Whether a machine whose screen is only letters can show a widget of that type. */
    public static boolean drawnInText(final String type) {
        return !NOT_IN_TEXT.contains(type);
    }

    /** Whether a call of that type is made on one of these objects rather than on the type itself. */
    public static boolean takesTarget(final String type) {
        return handles(type) && !MESSAGE_BOX.equals(type);
    }

    /** One of them, as the program asked for it; what it was made with fills what it holds. */
    public static Values.Obj create(final String type, final List<Object> arguments, final int line) {
        final Values.Obj made = new Values.Obj(type);
        if (isWidget(type)) {
            made.set(ID, 0L);
            made.set(VISIBLE, Boolean.TRUE);
            made.set(ENABLED, Boolean.TRUE);
            made.set(WIDTH, 0);
            made.set(HEIGHT, 0);
        }
        switch (type) {
            case WINDOW -> {
                made.set(TITLE, text(arguments, 0, ""));
                made.set(WIDTH, Math.clamp(whole(arguments, 1, 240), LEAST_SIDE, MOST_WIDE));
                made.set(HEIGHT, Math.clamp(whole(arguments, 2, 160), LEAST_SIDE, MOST_TALL));
                made.set(CONTENT, null);
                made.set(PLACED, new Values.ListValue());
                made.set(OPEN, Boolean.FALSE);
                made.set(ID, 0L);
                made.set(REVISION, 0L);
                made.set(DIALOG, null);
            }
            case ROW, COLUMN -> {
                made.set(CHILDREN, new Values.ListValue());
                made.set(WEIGHTS, new Values.ListValue());
                made.set(SPACING, 4);
            }
            case LABEL, BUTTON, TEXT_BOX, TEXT_AREA -> made.set(TEXT, text(arguments, 0, ""));
            case CHECK_BOX -> {
                made.set(TEXT, text(arguments, 0, ""));
                made.set(CHECKED, arguments.size() > 1 && Boolean.TRUE.equals(arguments.get(1)));
            }
            case PROGRESS_BAR -> {
                made.set(VALUE, 0);
                made.set(LEAST, whole(arguments, 0, 0));
                made.set(MOST, whole(arguments, 1, 100));
            }
            case NUMBER_BOX, SLIDER -> {
                final int least = whole(arguments, 0, 0);
                made.set(LEAST, least);
                made.set(MOST, Math.max(least, whole(arguments, 1, 100)));
                made.set(VALUE, least);
                made.set(STEP, 1);
            }
            case LIST_BOX -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(RIGHTS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
            }
            case RADIO_GROUP, COMBO_BOX, ITEM_PICKER -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
                if (RADIO_GROUP.equals(type)) {
                    made.set(ACROSS, Boolean.TRUE);
                } else if (ITEM_PICKER.equals(type)) {
                    made.set(PICKED, "");
                }
            }
            case LOG_VIEW -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(COUNT, 0);
            }
            case TAB_VIEW -> {
                made.set(CHILDREN, new Values.ListValue());
                made.set(ITEMS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
            }
            case GROUP_BOX -> {
                made.set(TEXT, text(arguments, 0, ""));
                made.set(CONTENT, null);
            }
            case SCROLL_VIEW -> made.set(CONTENT, null);
            case TABLE -> {
                made.set(COLUMNS, new Values.ListValue());
                made.set(ITEMS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
            }
            case TREE_VIEW -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(PARENTS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
            }
            case MENU_BAR -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(RIGHTS, new Values.ListValue());
                made.set(PICKED, nothing());
                made.set(PICKED_MENU, nothing());
            }
            case CONTEXT_MENU -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(PICKED, nothing());
            }
            case STATUS_BAR -> {
                made.set(TEXT, text(arguments, 0, ""));
                made.set(ITEMS, new Values.ListValue());
            }
            case IMAGE -> made.set(PATH, text(arguments, 0, ""));
            case CHART -> {
                made.set(TEXT, nothing());
                made.set(POINTS, new Values.ListValue());
                made.set(LEAST, whole(arguments, 0, 0));
                made.set(MOST, whole(arguments, 1, 100));
                made.set(COUNT, 0);
            }
            case OPEN_FILE_DIALOG, SAVE_FILE_DIALOG -> {
                made.set(TITLE, text(arguments, 0, ""));
                made.set(PATH, nothing());
                made.set(FILTER, nothing());
                made.set(NAME, nothing());
                made.set(EPOCH, 0);
            }
            case ITEM_SLOT -> {
                made.set(ITEM, text(arguments, 0, ""));
                made.set(AMOUNT, arguments.size() > 1 ? Math.max(0, whole(arguments, 1, 1)) : 1);
            }
            case OPERATION_VIEW -> {
                made.set(TEXT, nothing());
                made.set(STATE, nothing());
                made.set(DONE, 0L);
                made.set(TOTAL, 0L);
                made.set(COMPUTER, nothing());
            }
            case GENERIC -> {
                made.set(KIND, text(arguments, 0, ""));
                made.set(DATA, null);
            }
            case CANVAS -> {
                made.set(DRAWING, new Values.ListValue());
                made.set(CLICK_X, 0);
                made.set(CLICK_Y, 0);
                made.set(EPOCH, 0);
                if (!arguments.isEmpty()) {
                    made.set(WIDTH, side(whole(arguments, 0, 0), MOST_WIDE));
                    made.set(HEIGHT, side(whole(arguments, 1, 0), MOST_TALL));
                }
            }
            default -> {
                // A MessageBox is only ever asked to show something; it holds nothing of its own.
            }
        }
        return made;
    }

    /** The small window a message box is: a title, what it has to say, and nothing to do but close it. */
    public static Values.Obj message(final String title, final String said, final int line) {
        final Values.Obj window = create(WINDOW, List.of(title, 220, 90), line);
        window.set(CONTENT, create(LABEL, List.of(said), line));
        window.set(ASK, Boolean.TRUE);
        return window;
    }

    /** What one of them weighs on the heap: its own header and a place for everything it holds. */
    public static long bytesOf(final Values.Obj widget) {
        return Heap.HEADER + (long) Heap.REFERENCE * widget.all().size();
    }

    /** The widget of that number inside a window, or null when the window holds no such widget. */
    public static Values.Obj widgetOf(final Values.Obj window, final long id) {
        for (final Values.Obj widget : inside(window)) {
            if (Numbers.toLong(widget.get(ID)) == id) {
                return widget;
            }
        }
        return null;
    }

    /** Every widget a window holds, the ones inside rows, columns and the rest among them, from the top down. */
    public static List<Values.Obj> inside(final Values.Obj window) {
        final List<Values.Obj> all = new ArrayList<>();
        for (final Values.Obj top : tops(window)) {
            gather(top, all);
        }
        return all;
    }

    /**
     * The widgets one widget holds directly: its children, the one it shows as its content and its context menu,
     * in that order.
     */
    public static List<Values.Obj> held(final Values.Obj widget) {
        final List<Values.Obj> held = new ArrayList<>();
        if (widget.get(CHILDREN) instanceof Values.ListValue children) {
            for (final Object child : children.items()) {
                if (child instanceof Values.Obj one) {
                    held.add(one);
                }
            }
        }
        if (!WINDOW.equals(widget.type()) && widget.get(CONTENT) instanceof Values.Obj content) {
            held.add(content);
        }
        if (widget.get(MENU) instanceof Values.Obj menu) {
            held.add(menu);
        }
        return held;
    }

    /* What a window holds at its top: what it shows, what was placed in it, and the dialog it has up. */
    private static List<Values.Obj> tops(final Values.Obj window) {
        final List<Values.Obj> tops = new ArrayList<>();
        if (window.get(CONTENT) instanceof Values.Obj content) {
            tops.add(content);
        }
        if (window.get(PLACED) instanceof Values.ListValue placed) {
            for (final Object one : placed.items()) {
                if (one instanceof Values.Obj where && where.get("Widget") instanceof Values.Obj widget) {
                    tops.add(widget);
                }
            }
        }
        if (window.get(DIALOG) instanceof Values.Obj dialog) {
            tops.add(dialog);
        }
        return tops;
    }

    private static void gather(final Values.Obj widget, final List<Values.Obj> into) {
        if (into.size() >= MOST_WIDGETS || into.contains(widget)) {
            return;
        }
        into.add(widget);
        for (final Values.Obj one : held(widget)) {
            gather(one, into);
        }
    }

    /** Moves a window's revision on, because something it shows has changed. */
    static void touch(final Values.Obj window) {
        window.set(REVISION, Numbers.toLong(window.get(REVISION)) + 1);
    }

    /** Whether a window shows that widget, anywhere inside what it holds. */
    static boolean shows(final Values.Obj window, final Values.Obj widget) {
        for (final Values.Obj top : tops(window)) {
            if (holds(top, widget)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code widget} is {@code other} or holds it anywhere inside what it holds. */
    static boolean holds(final Values.Obj widget, final Values.Obj other) {
        final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Values.Obj> left = new ArrayDeque<>();
        left.push(widget);
        while (!left.isEmpty()) {
            final Values.Obj at = left.pop();
            if (at == other) {
                return true;
            }
            if (seen.add(at)) {
                for (final Values.Obj one : held(at)) {
                    left.push(one);
                }
            }
        }
        return false;
    }

    /** How many widgets a window holds, everything inside counted, stopping once it is past the most it may hold. */
    static int count(final Values.Obj window) {
        final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Values.Obj top : tops(window)) {
            tally(top, seen);
        }
        return seen.size();
    }

    /** How many widgets that one is with everything inside it, stopping past the most a window may hold. */
    static int size(final Values.Obj widget) {
        final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        tally(widget, seen);
        return seen.size();
    }

    private static void tally(final Values.Obj widget, final Set<Values.Obj> seen) {
        final Deque<Values.Obj> left = new ArrayDeque<>();
        left.push(widget);
        while (!left.isEmpty() && seen.size() <= MOST_WIDGETS) {
            final Values.Obj at = left.pop();
            if (seen.add(at)) {
                for (final Values.Obj one : held(at)) {
                    left.push(one);
                }
            }
        }
    }

    /** The widget a value is, or a halt saying it is not one. */
    static Values.Obj widget(final Object value, final int line) {
        if (value instanceof Values.Obj object && isWidget(object.type())) {
            return object;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, NOT_A_WIDGET.text());
    }

    /** The widget a value is when it can take a place in a window, or a halt saying why it cannot. */
    static Values.Obj placeableWidget(final Object value, final int line) {
        final Values.Obj widget = widget(value, line);
        if (!placeable(widget.type())) {
            throw new Halt(Halt.Reason.REFUSED, line, NOT_PLACEABLE.with(widget.type()));
        }
        return widget;
    }

    static Values.ListValue listOf(final Values.Obj self, final String name, final int line) {
        if (self.get(name) instanceof Values.ListValue list) {
            return list;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, THIS_HAS_NO.with(self.type(), name));
    }

    /*
     * A copy of its own of the text at that place, never the object the program passed: a widget lets go of its texts
     * when they change, and must never take one the program still holds.
     */
    static String text(final List<Object> arguments, final int index, final String fallback) {
        final String said = arguments.size() > index && arguments.get(index) != null
                ? String.valueOf(arguments.get(index)) : fallback;
        return new String(said.toCharArray());
    }

    /*
     * An empty text of its own. Never the literal: one instance of "" would be shared by every widget holding it, and
     * the first of them to let it go would take it off the heap under all the others.
     */
    static String nothing() {
        return new String(new char[0]);
    }

    static int whole(final List<Object> arguments, final int index, final int fallback) {
        return arguments.size() > index && arguments.get(index) instanceof Number number
                ? number.intValue() : fallback;
    }

    /** A size the machine can draw: nothing means whatever it needs, and nothing is past what fits. */
    static int side(final int asked, final int most) {
        if (asked <= 0) {
            return 0;
        }
        return Math.clamp(asked, LEAST_SIDE, most);
    }

    /** A size a widget asked for: nothing means whatever it needs, and a widget may be as small as a line. */
    static int widgetSide(final int asked, final int most) {
        if (asked <= 0) {
            return 0;
        }
        return Math.clamp(asked, LEAST_WIDGET, most);
    }
}
