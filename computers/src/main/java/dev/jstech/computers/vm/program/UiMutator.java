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
 *
 * <p>What a widget holds is on the program's heap like everything else, so it counts against the program's memory and
 * comes back after a save. Whatever the runtime makes for a widget (a row of a list, a stroke, a place in a window) is
 * adopted as it is made, and a list that grows or shrinks is resized with it. The texts a widget holds are copies of
 * its own: a program's text is copied in and a read hands a copy out, so when a widget lets go of a text it replaced,
 * or a canvas of the strokes it cleared, nothing the program holds goes with it.
 */
final class UiMutator {

    private final Heap heap;

    UiMutator(final Heap heap) {
        this.heap = heap;
    }

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
        this.heap.adopt(where, line);
        placed.items().add(where);
        this.fit(placed, line);
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
        this.fit(children, line);
        this.fit(weights, line);
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
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, UiWidgets.LIST_BOX + " has no " + member);
        }
        this.fit(items, line);
        this.fit(rights, line);
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
            for (final Object one : drawing.items()) {
                if (one instanceof Values.Obj stroke) {
                    this.letGo(stroke.all().values());
                    this.heap.release(stroke);
                }
            }
            drawing.items().clear();
            final Values.Obj stroke = stroke("Clear");
            stroke.set("Colour", Numbers.toInt(arguments.isEmpty() ? 0 : arguments.getFirst()));
            this.draw(drawing, stroke, line);
            return null;
        }
        if (drawing.items().size() >= UiWidgets.MOST_STROKES) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                    "a canvas holds at most " + UiWidgets.MOST_STROKES + " strokes");
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
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, UiWidgets.CANVAS + " has no " + member);
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

    /** What a program writes on a widget, kept as the widget holds it: a text as a copy of the widget's own. */
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
            default -> {
                if (value instanceof String said) {
                    this.replace(self, name, said, line);
                } else {
                    self.set(name, value);
                }
            }
        }
    }

    /**
     * Takes what a player did to a widget: the widget is changed the way they changed it, and the name
     * of the handler to tell comes back, or null when that is not something the widget answers. What a player
     * types can run the program out of memory like anything the program holds, which halts it.
     */
    String accept(final Values.Obj widget, final String kind, final List<Object> values) {
        final Object first = values.isEmpty() ? null : values.getFirst();
        return switch (widget.type() + "/" + kind) {
            case UiWidgets.BUTTON + "/click" -> "OnClick";
            case UiWidgets.TEXT_BOX + "/text", UiWidgets.TEXT_BOX + "/submit" -> {
                this.replace(widget, UiWidgets.TEXT, first == null ? "" : String.valueOf(first), 0);
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

    /* A list that grew or shrank weighs what it now holds. */
    private void fit(final Values.ListValue list, final int line) {
        this.heap.resize(list, list.bytes(), line);
    }
}
