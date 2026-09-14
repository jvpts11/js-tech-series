/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.List;

/**
 * Every change a window or a widget goes through, whether the program makes it or a player does: the one door, so what
 * a change costs and what it leaves on the program's heap is worked out in a single place.
 */
final class UiMutator {

    /**
     * Answers a call on one of them, giving back what the call gives back or null.
     *
     * <p>Opening and closing a window are not here: those reach the machine, and the runtime makes them.
     */
    Object call(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        return switch (self.type()) {
            case UiWidgets.ROW, UiWidgets.COLUMN -> this.box(self, member, arguments, line);
            case UiWidgets.LIST_BOX -> this.list(self, member, arguments, line);
            case UiWidgets.CANVAS -> this.canvas(self, member, arguments, line);
            case UiWidgets.WINDOW -> this.window(self, member, arguments, line);
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, self.type() + " has no " + member);
        };
    }

    private Object window(final Values.Obj self, final String member, final List<Object> arguments,
                          final int line) {
        if (!"Add".equals(member) || arguments.size() < 5) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, UiWidgets.WINDOW + " has no " + member);
        }
        final Values.ListValue placed = UiWidgets.listOf(self, UiWidgets.PLACED, line);
        if (placed.items().size() >= UiWidgets.MOST_WIDGETS) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                    "a window holds at most " + UiWidgets.MOST_WIDGETS + " widgets");
        }
        final Values.Obj where = new Values.Obj(UiWidgets.PLACE);
        where.set("Widget", UiWidgets.widget(arguments.getFirst(), line));
        where.set("X", Numbers.toInt(arguments.get(1)));
        where.set("Y", Numbers.toInt(arguments.get(2)));
        where.set(UiWidgets.WIDTH, Numbers.toInt(arguments.get(3)));
        where.set(UiWidgets.HEIGHT, Numbers.toInt(arguments.get(4)));
        placed.items().add(where);
        return null;
    }

    private Object box(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue children = UiWidgets.listOf(self, UiWidgets.CHILDREN, line);
        final Values.ListValue weights = UiWidgets.listOf(self, UiWidgets.WEIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (children.items().size() >= UiWidgets.MOST_WIDGETS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                            "a row or a column holds at most " + UiWidgets.MOST_WIDGETS + " widgets");
                }
                children.items().add(UiWidgets.widget(arguments.isEmpty() ? null : arguments.getFirst(), line));
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

    private Object list(final Values.Obj self, final String member, final List<Object> arguments, final int line) {
        final Values.ListValue items = UiWidgets.listOf(self, UiWidgets.ITEMS, line);
        final Values.ListValue rights = UiWidgets.listOf(self, UiWidgets.RIGHTS, line);
        switch (member) {
            case "Add" -> {
                if (items.items().size() >= UiWidgets.MOST_ROWS) {
                    throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                            "a list holds at most " + UiWidgets.MOST_ROWS + " rows");
                }
                items.items().add(UiWidgets.text(arguments, 0, ""));
                rights.items().add(UiWidgets.text(arguments, 1, ""));
            }
            case "Clear" -> {
                items.items().clear();
                rights.items().clear();
                self.set(UiWidgets.SELECTED, 0);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, UiWidgets.LIST_BOX + " has no " + member);
        }
        self.set(UiWidgets.COUNT, items.items().size());
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
            drawing.items().clear();
            final Values.Obj stroke = new Values.Obj("Stroke");
            stroke.set("Kind", "Clear");
            stroke.set("Colour", Numbers.toInt(arguments.isEmpty() ? 0 : arguments.getFirst()));
            drawing.items().add(stroke);
            return null;
        }
        if (drawing.items().size() >= UiWidgets.MOST_STROKES) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                    "a canvas holds at most " + UiWidgets.MOST_STROKES + " strokes");
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
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, UiWidgets.CANVAS + " has no " + member);
        }
        drawing.items().add(stroke);
        return null;
    }

    /** What a program writes on a widget, kept as the widget holds it. */
    void write(final Values.Obj self, final String name, final Object value, final int line) {
        final int most = UiWidgets.WIDTH.equals(name) ? UiWidgets.MOST_WIDE : UiWidgets.MOST_TALL;
        switch (name) {
            // A window has a size of its own; a widget asking for none takes whatever it needs.
            case UiWidgets.WIDTH, UiWidgets.HEIGHT -> self.set(name, UiWidgets.WINDOW.equals(self.type())
                    ? Math.clamp(Numbers.toInt(value), UiWidgets.LEAST_SIDE, most)
                    : UiWidgets.side(Numbers.toInt(value), most));
            case UiWidgets.CONTENT -> self.set(UiWidgets.CONTENT, value == null ? null : UiWidgets.widget(value, line));
            case UiWidgets.OPEN, UiWidgets.ID, UiWidgets.COUNT, UiWidgets.PLACED, UiWidgets.CHILDREN,
                 UiWidgets.WEIGHTS, UiWidgets.ITEMS, UiWidgets.RIGHTS, UiWidgets.DRAWING ->
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, name + " is not a program's to write");
            default -> self.set(name, value);
        }
    }

    /**
     * Takes what a player did to a widget: the widget is changed the way they changed it, and the name
     * of the handler to tell comes back, or null when that is not something the widget answers.
     */
    String accept(final Values.Obj widget, final String kind, final List<Object> values) {
        final Object first = values.isEmpty() ? null : values.getFirst();
        return switch (widget.type() + "/" + kind) {
            case UiWidgets.BUTTON + "/click" -> "OnClick";
            case UiWidgets.TEXT_BOX + "/text", UiWidgets.TEXT_BOX + "/submit" -> {
                widget.set(UiWidgets.TEXT, first == null ? "" : String.valueOf(first));
                yield "text".equals(kind) ? "OnChange" : "OnSubmit";
            }
            case UiWidgets.CHECK_BOX + "/toggle" -> {
                widget.set(UiWidgets.CHECKED, Boolean.TRUE.equals(first));
                yield "OnToggle";
            }
            case UiWidgets.LIST_BOX + "/select" -> {
                widget.set(UiWidgets.SELECTED, Math.max(0, Numbers.toInt(first)));
                yield "OnSelect";
            }
            case UiWidgets.CANVAS + "/click" -> {
                widget.set(UiWidgets.CLICK_X, Numbers.toInt(first));
                widget.set(UiWidgets.CLICK_Y, values.size() > 1 ? Numbers.toInt(values.get(1)) : 0);
                yield "OnClick";
            }
            default -> null;
        };
    }
}
