/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The windows and widgets a Cannon program makes, as the runtime keeps them.
 *
 * <p>A widget is an ordinary object on the program's own heap, so it is counted like everything else the
 * program holds and is written down and brought back with it. What it holds is what it is: a label holds
 * its text, a row holds its widgets and their weights, a canvas holds the drawing asked for. Nothing here
 * knows how wide a letter is or what a button looks like: where things land is worked out where they are
 * drawn, by the machine's own system, and this is only what the program said. Changing one goes through
 * {@link UiMutator}.
 */
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
    public static final String DRAWING = "Drawing";
    public static final String CLICK_X = "ClickX";
    public static final String CLICK_Y = "ClickY";
    public static final String PLACE = "Place";
    public static final String ID = "Id";
    /** Set on a window that is only there to say something, which is what a message box is. */
    public static final String ASK = "Ask";

    /** The most a program may keep in one list or draw on one canvas. */
    public static final int MOST_ROWS = 1024;
    public static final int MOST_STROKES = 4096;
    /** The most widgets one window may hold, counting its rows and columns. */
    public static final int MOST_WIDGETS = 256;
    /** The longest line a player can put in a program's text box. */
    public static final int MOST_TEXT = 256;

    /** How wide and tall a window may be asked to be, in the desktop's own pixels. */
    public static final int LEAST_SIDE = 60;
    public static final int MOST_WIDE = 640;
    public static final int MOST_TALL = 360;

    private static final Set<String> KINDS = Set.of(WINDOW, ROW, COLUMN, LABEL, BUTTON, TEXT_BOX, CHECK_BOX,
            PROGRESS_BAR, LIST_BOX, CANVAS, MESSAGE_BOX);

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
            }
            case ROW, COLUMN -> {
                made.set(CHILDREN, new Values.ListValue());
                made.set(WEIGHTS, new Values.ListValue());
                made.set(SPACING, 4);
            }
            case LABEL, BUTTON, TEXT_BOX -> made.set(TEXT, text(arguments, 0, ""));
            case CHECK_BOX -> {
                made.set(TEXT, text(arguments, 0, ""));
                made.set(CHECKED, arguments.size() > 1 && Boolean.TRUE.equals(arguments.get(1)));
            }
            case PROGRESS_BAR -> {
                made.set(VALUE, 0);
                made.set(LEAST, whole(arguments, 0, 0));
                made.set(MOST, whole(arguments, 1, 100));
            }
            case LIST_BOX -> {
                made.set(ITEMS, new Values.ListValue());
                made.set(RIGHTS, new Values.ListValue());
                made.set(SELECTED, 0);
                made.set(COUNT, 0);
            }
            case CANVAS -> {
                made.set(DRAWING, new Values.ListValue());
                made.set(CLICK_X, 0);
                made.set(CLICK_Y, 0);
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

    /** Every widget a window holds, the ones inside rows and columns among them, from the top down. */
    public static List<Values.Obj> inside(final Values.Obj window) {
        final List<Values.Obj> all = new java.util.ArrayList<>();
        if (window.get(CONTENT) instanceof Values.Obj content) {
            gather(content, all);
        }
        if (window.get(PLACED) instanceof Values.ListValue placed) {
            for (final Object one : placed.items()) {
                if (one instanceof Values.Obj where && where.get("Widget") instanceof Values.Obj widget) {
                    gather(widget, all);
                }
            }
        }
        return all;
    }

    private static void gather(final Values.Obj widget, final List<Values.Obj> into) {
        if (into.size() >= MOST_WIDGETS || into.contains(widget)) {
            return;
        }
        into.add(widget);
        if (widget.get(CHILDREN) instanceof Values.ListValue children) {
            for (final Object child : children.items()) {
                if (child instanceof Values.Obj one) {
                    gather(one, into);
                }
            }
        }
    }

    /** Whether {@code widget} is {@code other} or holds it anywhere inside its rows and columns. */
    static boolean holds(final Values.Obj widget, final Values.Obj other) {
        final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Values.Obj> left = new ArrayDeque<>();
        left.push(widget);
        while (!left.isEmpty()) {
            final Values.Obj at = left.pop();
            if (at == other) {
                return true;
            }
            if (seen.add(at) && at.get(CHILDREN) instanceof Values.ListValue children) {
                for (final Object child : children.items()) {
                    if (child instanceof Values.Obj one) {
                        left.push(one);
                    }
                }
            }
        }
        return false;
    }

    /** How many widgets a window holds, rows and columns counted, stopping once it is past the most it may hold. */
    static int count(final Values.Obj window) {
        final Set<Values.Obj> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (window.get(CONTENT) instanceof Values.Obj content) {
            tally(content, seen);
        }
        if (window.get(PLACED) instanceof Values.ListValue placed) {
            for (final Object one : placed.items()) {
                if (one instanceof Values.Obj where && where.get("Widget") instanceof Values.Obj widget) {
                    tally(widget, seen);
                }
            }
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
            if (seen.add(at) && at.get(CHILDREN) instanceof Values.ListValue children) {
                for (final Object child : children.items()) {
                    if (child instanceof Values.Obj one) {
                        left.push(one);
                    }
                }
            }
        }
    }

    /** The widget a value is, or a halt saying it is not one. */
    static Values.Obj widget(final Object value, final int line) {
        if (value instanceof Values.Obj object && isWidget(object.type())) {
            return object;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "this is not a widget to put in a window");
    }

    static Values.ListValue listOf(final Values.Obj self, final String name, final int line) {
        if (self.get(name) instanceof Values.ListValue list) {
            return list;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "this " + self.type() + " has no " + name);
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
}
