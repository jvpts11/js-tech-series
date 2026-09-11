/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.ui;

import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.Heap;
import dev.jstech.computers.cannon.run.Numbers;
import dev.jstech.computers.cannon.run.Values;
import java.util.List;

/**
 * The windows and widgets a Cannon program makes, as the runtime keeps them.
 *
 * <p>A widget is an ordinary object on the program's own heap, so it is counted like everything else the
 * program holds and is written down and brought back with it. What it holds is what it is: a label holds
 * its text, a row holds its widgets and their weights, a canvas holds the drawing asked for. Nothing here
 * knows how wide a letter is or what a button looks like: where things land is worked out where they are
 * drawn, by the machine's own system, and this is only what the program said.
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

    /** How wide and tall a window may be asked to be, in the desktop's own pixels. */
    public static final int LEAST_SIDE = 60;
    public static final int MOST_WIDE = 640;
    public static final int MOST_TALL = 360;

    private static final List<String> KINDS = List.of(WINDOW, ROW, COLUMN, LABEL, BUTTON, TEXT_BOX, CHECK_BOX,
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

    /**
     * Answers a call on one of them, giving back what the call gives back or null.
     *
     * <p>Opening and closing a window are not here: those reach the machine, and the runtime makes them.
     */
    public static Object call(final Values.Obj self, final String member, final List<Object> arguments,
                              final int line) {
        return switch (self.type()) {
            case ROW, COLUMN -> box(self, member, arguments, line);
            case LIST_BOX -> list(self, member, arguments, line);
            case CANVAS -> canvas(self, member, arguments, line);
            case WINDOW -> window(self, member, arguments, line);
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, self.type() + " has no " + member);
        };
    }

    private static Object window(final Values.Obj self, final String member, final List<Object> arguments,
                                 final int line) {
        if (!"Add".equals(member) || arguments.size() < 5) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, WINDOW + " has no " + member);
        }
        final Values.ListValue placed = listOf(self, PLACED, line);
        if (placed.items().size() >= MOST_WIDGETS) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                    "a window holds at most " + MOST_WIDGETS + " widgets");
        }
        final Values.Obj where = new Values.Obj(PLACE);
        where.set("Widget", widget(arguments.getFirst(), line));
        where.set("X", Numbers.toInt(arguments.get(1)));
        where.set("Y", Numbers.toInt(arguments.get(2)));
        where.set(WIDTH, Numbers.toInt(arguments.get(3)));
        where.set(HEIGHT, Numbers.toInt(arguments.get(4)));
        placed.items().add(where);
        return null;
    }

    private static Object box(final Values.Obj self, final String member, final List<Object> arguments,
                              final int line) {
        final Values.ListValue children = listOf(self, CHILDREN, line);
        final Values.ListValue weights = listOf(self, WEIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (children.items().size() >= MOST_WIDGETS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                            "a row or a column holds at most " + MOST_WIDGETS + " widgets");
                }
                children.items().add(widget(arguments.isEmpty() ? null : arguments.getFirst(), line));
                weights.items().add(arguments.size() > 1 ? Math.max(0, Numbers.toInt(arguments.get(1))) : 0);
            }
            case "Clear" -> {
                children.items().clear();
                weights.items().clear();
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, self.type() + " has no " + member);
        }
        return null;
    }

    private static Object list(final Values.Obj self, final String member, final List<Object> arguments,
                               final int line) {
        final Values.ListValue items = listOf(self, ITEMS, line);
        final Values.ListValue rights = listOf(self, RIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (items.items().size() >= MOST_ROWS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "a list holds at most " + MOST_ROWS + " rows");
                }
                items.items().add(text(arguments, 0, ""));
                rights.items().add(text(arguments, 1, ""));
            }
            case "Clear" -> {
                items.items().clear();
                rights.items().clear();
                self.set(SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, LIST_BOX + " has no " + member);
        }
        self.set(COUNT, items.items().size());
        return null;
    }

    /*
     * What a canvas is asked to draw is kept as it was asked, a stroke at a time, and drawn again
     * whenever the window is: a picture is what the program said, not pixels the machine has to keep.
     */
    private static Object canvas(final Values.Obj self, final String member, final List<Object> arguments,
                                 final int line) {
        final Values.ListValue drawing = listOf(self, DRAWING, line);
        if ("Clear".equals(member)) {
            drawing.items().clear();
            final Values.Obj stroke = new Values.Obj("Stroke");
            stroke.set("Kind", "Clear");
            stroke.set("Colour", Numbers.toInt(arguments.isEmpty() ? 0 : arguments.getFirst()));
            drawing.items().add(stroke);
            return null;
        }
        if (drawing.items().size() >= MOST_STROKES) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "a canvas holds at most " + MOST_STROKES + " strokes");
        }
        final Values.Obj stroke = new Values.Obj("Stroke");
        stroke.set("Kind", member);
        switch (member) {
            case "FillRect", "DrawLine" -> {
                stroke.set("X", Numbers.toInt(arguments.get(0)));
                stroke.set("Y", Numbers.toInt(arguments.get(1)));
                stroke.set("X2", Numbers.toInt(arguments.get(2)));
                stroke.set("Y2", Numbers.toInt(arguments.get(3)));
                stroke.set("Colour", Numbers.toInt(arguments.get(4)));
            }
            case "DrawText" -> {
                stroke.set(TEXT, text(arguments, 0, ""));
                stroke.set("X", Numbers.toInt(arguments.get(1)));
                stroke.set("Y", Numbers.toInt(arguments.get(2)));
                stroke.set("Colour", Numbers.toInt(arguments.get(3)));
            }
            case "SetPixel" -> {
                stroke.set("X", Numbers.toInt(arguments.get(0)));
                stroke.set("Y", Numbers.toInt(arguments.get(1)));
                stroke.set("Colour", Numbers.toInt(arguments.get(2)));
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, CANVAS + " has no " + member);
        }
        drawing.items().add(stroke);
        return null;
    }

    /** What a program writes on a widget, kept as the widget holds it. */
    public static void write(final Values.Obj self, final String name, final Object value, final int line) {
        final int most = WIDTH.equals(name) ? MOST_WIDE : MOST_TALL;
        switch (name) {
            // A window has a size of its own; a widget asking for none takes whatever it needs.
            case WIDTH, HEIGHT -> self.set(name, WINDOW.equals(self.type())
                    ? Math.clamp(Numbers.toInt(value), LEAST_SIDE, most) : side(Numbers.toInt(value), most));
            case CONTENT -> self.set(CONTENT, value == null ? null : widget(value, line));
            case OPEN, ID, COUNT, PLACED, CHILDREN, WEIGHTS, ITEMS, RIGHTS, DRAWING ->
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, name + " is not a program's to write");
            default -> self.set(name, value);
        }
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

    /**
     * Takes what a player did to a widget: the widget is changed the way they changed it, and the name
     * of the handler to tell comes back, or null when that is not something the widget answers.
     */
    public static String accept(final Values.Obj widget, final String kind, final List<Object> values) {
        final Object first = values.isEmpty() ? null : values.getFirst();
        return switch (widget.type() + "/" + kind) {
            case BUTTON + "/click" -> "OnClick";
            case TEXT_BOX + "/text", TEXT_BOX + "/submit" -> {
                widget.set(TEXT, first == null ? "" : String.valueOf(first));
                yield "text".equals(kind) ? "OnChange" : "OnSubmit";
            }
            case CHECK_BOX + "/toggle" -> {
                widget.set(CHECKED, Boolean.TRUE.equals(first));
                yield "OnToggle";
            }
            case LIST_BOX + "/select" -> {
                widget.set(SELECTED, Math.max(0, Numbers.toInt(first)));
                yield "OnSelect";
            }
            case CANVAS + "/click" -> {
                widget.set(CLICK_X, Numbers.toInt(first));
                widget.set(CLICK_Y, values.size() > 1 ? Numbers.toInt(values.get(1)) : 0);
                yield "OnClick";
            }
            default -> null;
        };
    }

    /** The widget a value is, or a halt saying it is not one. */
    private static Values.Obj widget(final Object value, final int line) {
        if (value instanceof Values.Obj object && isWidget(object.type())) {
            return object;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "this is not a widget to put in a window");
    }

    private static Values.ListValue listOf(final Values.Obj self, final String name, final int line) {
        if (self.get(name) instanceof Values.ListValue list) {
            return list;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "this " + self.type() + " has no " + name);
    }

    private static String text(final List<Object> arguments, final int index, final String fallback) {
        return arguments.size() > index && arguments.get(index) != null
                ? String.valueOf(arguments.get(index)) : fallback;
    }

    private static int whole(final List<Object> arguments, final int index, final int fallback) {
        return arguments.size() > index && arguments.get(index) instanceof Number number
                ? number.intValue() : fallback;
    }

    /** A size the machine can draw: nothing means whatever it needs, and nothing is past what fits. */
    private static int side(final int asked, final int most) {
        if (asked <= 0) {
            return 0;
        }
        return Math.clamp(asked, LEAST_SIDE, most);
    }
}
