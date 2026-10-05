/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a generic component may hold, and how it travels.
 *
 * <p>A generic component holds any value of the language a program hands it, so long as it is a value and not a
 * thing that acts: a handler, a file, a thread, a window or a widget each stands for something the program owns and
 * the screen could do nothing with, so each is refused. So is a value that holds itself, since it could never be
 * written out, and one past the bounds below, since every viewer of the window is sent it. What it holds is a copy
 * made when it is handed over, so the program changing its own list later changes nothing on the screen until it
 * hands the list over again.
 *
 * <p>On the wire a value is a run of letters, each part tagged by what it is. The same text comes back from a
 * player's screen with what they did, read under the same bounds, so a client sends nothing the server would not
 * itself have sent.
 */
@TextHolder
public final class ComponentValues {

    /** The most a component's value may weigh, how deep it may nest and how many parts it may have. */
    public static final int MOST_BYTES = 32 * 1024;
    public static final int MOST_DEPTH = 16;
    public static final int MOST_NODES = 4096;

    private static final TextKey NO_HANDLER = TextKey.of("jsc.vm.component_values.no_handler",
            "a component holds values, not handlers");
    private static final TextKey NO_HANDLE = TextKey.of("jsc.vm.component_values.no_handle",
            "a component holds values, and a %s is not one");
    private static final TextKey HOLDS_ITSELF = TextKey.of("jsc.vm.component_values.holds_itself",
            "a value that holds itself cannot be handed to a component");
    private static final TextKey TOO_DEEP = TextKey.of("jsc.vm.component_values.too_deep",
            "a component's value nests at most %s deep");
    private static final TextKey TOO_MANY = TextKey.of("jsc.vm.component_values.too_many",
            "a component's value has at most %s parts");
    private static final TextKey TOO_BIG = TextKey.of("jsc.vm.component_values.too_big",
            "a component's value weighs at most %s bytes");

    private ComponentValues() {
    }

    /**
     * A copy of that value for a component to hold, made on the program's heap, or a halt saying why it cannot be
     * held. The copy is checked whole before any of it is made, so a refused value leaves nothing behind.
     *
     * @return the copy, and what it weighs as it was counted against {@link #MOST_BYTES}
     */
    static Copied copy(final Object value, final Heap heap, final int line) {
        final Walk walk = new Walk(line);
        walk.check(value, 0);
        final Object made = clone(value);
        heap.adopt(made, line);
        return new Copied(made, walk.bytes);
    }

    /**
     * A value copied for a component, and what it weighs.
     *
     * @param value the copy
     * @param bytes what it weighs, the way the bound counts it
     */
    record Copied(Object value, long bytes) {
    }

    /** The value a component holds as the run of letters it travels as. */
    public static String encode(final Object value) {
        final StringBuilder out = new StringBuilder();
        write(value, out, Collections.newSetFromMap(new IdentityHashMap<>()));
        return out.toString();
    }

    /**
     * The run of letters a value travels as, read back as plain values: null, a bool, an int, a long, a double, a
     * character, a text, a list or a map, each map keeping its order.
     *
     * @throws IllegalArgumentException when the text is not a value, or one past the bounds
     */
    public static Object decode(final String text) {
        if (text.length() > MOST_BYTES) {
            throw new IllegalArgumentException("too long");
        }
        final Reader reader = new Reader(text);
        final Object value = reader.value(0);
        if (reader.at != text.length()) {
            throw new IllegalArgumentException("trailing text at " + reader.at);
        }
        return value;
    }

    /** A plain value, as {@link #decode} gives them, written as the run of letters it travels as. */
    public static String encodePlain(final Object value) {
        final StringBuilder out = new StringBuilder();
        writePlain(value, out, 0);
        return out.toString();
    }

    /** A plain value turned into the language's own, for handing to a program; not yet on any heap. */
    static Object toSigma(final Object plain) {
        return switch (plain) {
            case null -> null;
            case List<?> list -> {
                final Values.ListValue made = new Values.ListValue();
                for (final Object one : list) {
                    made.items().add(toSigma(one));
                }
                yield made;
            }
            case Map<?, ?> map -> {
                final Values.MapValue made = new Values.MapValue();
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    made.entries().put(toSigma(entry.getKey()), toSigma(entry.getValue()));
                }
                yield made;
            }
            case String text -> new String(text.toCharArray());
            default -> plain;
        };
    }

    /* Everything the bounds count, walked once before anything is copied. */
    private static final class Walk {

        private final int line;
        private final Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());
        private int nodes;
        private long bytes;

        Walk(final int line) {
            this.line = line;
        }

        void check(final Object value, final int depth) {
            if (depth > MOST_DEPTH) {
                throw new Halt(Halt.Reason.REFUSED, this.line, TOO_DEEP.with(MOST_DEPTH));
            }
            if (++this.nodes > MOST_NODES) {
                throw new Halt(Halt.Reason.REFUSED, this.line, TOO_MANY.with(MOST_NODES));
            }
            this.bytes += weight(value);
            if (this.bytes > MOST_BYTES) {
                throw new Halt(Halt.Reason.REFUSED, this.line, TOO_BIG.with(MOST_BYTES));
            }
            switch (value) {
                case Values.DelegateValue ignored -> throw new Halt(Halt.Reason.REFUSED, this.line, NO_HANDLER.text());
                case Values.ListValue list -> this.inside(list, list.items(), depth);
                case Values.Arr array -> this.inside(array, array.all(), depth);
                case Values.MapValue map -> {
                    this.enter(map);
                    for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                        this.check(entry.getKey(), depth + 1);
                        this.check(entry.getValue(), depth + 1);
                    }
                    this.path.remove(map);
                }
                case Values.Obj object -> {
                    if (isHandle(object.type())) {
                        throw new Halt(Halt.Reason.REFUSED, this.line, NO_HANDLE.with(object.type()));
                    }
                    this.inside(object, object.all().values(), depth);
                }
                default -> {
                    // A text, a number, a bool or a character is a value through and through.
                }
            }
        }

        private void inside(final Object holder, final Iterable<Object> parts, final int depth) {
            this.enter(holder);
            for (final Object part : parts) {
                this.check(part, depth + 1);
            }
            this.path.remove(holder);
        }

        private void enter(final Object holder) {
            if (!this.path.add(holder)) {
                throw new Halt(Halt.Reason.REFUSED, this.line, HOLDS_ITSELF.text());
            }
        }
    }

    /* What one part weighs, the way the heap would count it, without what it holds. */
    private static long weight(final Object value) {
        return switch (value) {
            case null -> Heap.REFERENCE;
            case String text -> Heap.sizeOfText(text);
            case Values.ListValue list -> list.bytes();
            case Values.MapValue map -> map.bytes();
            case Values.Arr array -> Heap.HEADER + (long) Heap.REFERENCE * array.length();
            case Values.Obj object -> UiWidgets.bytesOf(object);
            default -> Heap.REFERENCE;
        };
    }

    /*
     * Whether objects of that type stand for something rather than being a value: a window or a widget, or one of the
     * system's types that does anything, as a file or a thread does. A type of the program's own is a value, and so
     * is one of the system's records, which only says how things stood.
     */
    private static boolean isHandle(final String type) {
        if (UiWidgets.handles(type)) {
            return true;
        }
        final TypeSpec spec = SystemApi.type(type);
        if (spec == null) {
            return false;
        }
        for (final IMemberSpec member : spec.members()) {
            if (!(member instanceof PropertySpec property) || property.isStatic() || property.writable()) {
                return true;
            }
        }
        return false;
    }

    /* A copy made of fresh parts, so nothing of it is anything the program holds. */
    private static Object clone(final Object value) {
        return switch (value) {
            case null -> null;
            case String text -> new String(text.toCharArray());
            case Values.ListValue list -> {
                final Values.ListValue made = new Values.ListValue();
                for (final Object one : list.items()) {
                    made.items().add(clone(one));
                }
                yield made;
            }
            case Values.Arr array -> {
                final Values.Arr made = new Values.Arr(array.element(), array.length());
                final List<Object> all = array.all();
                for (int i = 0; i < all.size(); i++) {
                    made.set(i, clone(all.get(i)), 0);
                }
                yield made;
            }
            case Values.MapValue map -> {
                final Values.MapValue made = new Values.MapValue();
                for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                    made.entries().put(clone(entry.getKey()), clone(entry.getValue()));
                }
                yield made;
            }
            case Values.Obj object -> {
                final Values.Obj made = new Values.Obj(object.type());
                for (final Map.Entry<String, Object> field : object.all().entrySet()) {
                    made.set(field.getKey(), clone(field.getValue()));
                }
                yield made;
            }
            default -> value;
        };
    }

    private static void write(final Object value, final StringBuilder out, final Set<Object> path) {
        switch (value) {
            case null -> out.append('n');
            case Boolean bool -> out.append(bool ? 't' : 'f');
            case Integer number -> out.append('i').append(number).append(';');
            case Long number -> out.append('l').append(number).append(';');
            case Float number -> out.append('d').append(number.doubleValue()).append(';');
            case Double number -> out.append('d').append(number).append(';');
            case Character letter -> out.append('c').append(letter);
            case String text -> out.append('s').append(text.length()).append(':').append(text);
            case Values.ListValue list -> writeAll(list, list.items(), out, path);
            case Values.Arr array -> writeAll(array, array.all(), out, path);
            case Values.MapValue map -> {
                if (!path.add(map)) {
                    out.append('n');
                    return;
                }
                out.append('{');
                for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                    write(entry.getKey(), out, path);
                    write(entry.getValue(), out, path);
                }
                out.append('}');
                path.remove(map);
            }
            case Values.Obj object -> {
                if (!path.add(object)) {
                    out.append('n');
                    return;
                }
                // An object of the program's own goes as a map of its fields, which is all a screen can use of it.
                out.append('{');
                for (final Map.Entry<String, Object> field : object.all().entrySet()) {
                    write(field.getKey(), out, path);
                    write(field.getValue(), out, path);
                }
                out.append('}');
                path.remove(object);
            }
            default -> out.append('n');
        }
    }

    private static void writeAll(final Object holder, final List<Object> parts, final StringBuilder out,
                                 final Set<Object> path) {
        if (!path.add(holder)) {
            out.append('n');
            return;
        }
        out.append('[');
        for (final Object part : parts) {
            write(part, out, path);
        }
        out.append(']');
        path.remove(holder);
    }

    private static void writePlain(final Object value, final StringBuilder out, final int depth) {
        if (depth > MOST_DEPTH) {
            throw new IllegalArgumentException("too deep");
        }
        switch (value) {
            case null -> out.append('n');
            case Boolean bool -> out.append(bool ? 't' : 'f');
            case Integer number -> out.append('i').append(number).append(';');
            case Long number -> out.append('l').append(number).append(';');
            case Number number -> out.append('d').append(number.doubleValue()).append(';');
            case Character letter -> out.append('c').append(letter);
            case String text -> out.append('s').append(text.length()).append(':').append(text);
            case List<?> list -> {
                out.append('[');
                for (final Object part : list) {
                    writePlain(part, out, depth + 1);
                }
                out.append(']');
            }
            case Map<?, ?> map -> {
                out.append('{');
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    writePlain(entry.getKey(), out, depth + 1);
                    writePlain(entry.getValue(), out, depth + 1);
                }
                out.append('}');
            }
            default -> throw new IllegalArgumentException("not a value: " + value.getClass().getSimpleName());
        }
    }

    /* Reads a run of letters back, counting its parts and depth as it goes. */
    private static final class Reader {

        private final String text;
        private int at;
        private int nodes;

        Reader(final String text) {
            this.text = text;
        }

        Object value(final int depth) {
            if (depth > MOST_DEPTH || ++this.nodes > MOST_NODES) {
                throw new IllegalArgumentException("past the bounds");
            }
            final char tag = this.next();
            return switch (tag) {
                case 'n' -> null;
                case 't' -> Boolean.TRUE;
                case 'f' -> Boolean.FALSE;
                case 'i' -> Integer.parseInt(this.until(';'));
                case 'l' -> Long.parseLong(this.until(';'));
                case 'd' -> Double.parseDouble(this.until(';'));
                case 'c' -> this.next();
                case 's' -> {
                    final int length = Integer.parseInt(this.until(':'));
                    if (length < 0 || this.at + length > this.text.length()) {
                        throw new IllegalArgumentException("text runs past the end");
                    }
                    final String read = this.text.substring(this.at, this.at + length);
                    this.at += length;
                    yield read;
                }
                case '[' -> {
                    final List<Object> list = new ArrayList<>();
                    while (this.peek() != ']') {
                        list.add(this.value(depth + 1));
                    }
                    this.at++;
                    yield list;
                }
                case '{' -> {
                    final Map<Object, Object> map = new LinkedHashMap<>();
                    while (this.peek() != '}') {
                        final Object key = this.value(depth + 1);
                        map.put(key, this.value(depth + 1));
                    }
                    this.at++;
                    yield map;
                }
                default -> throw new IllegalArgumentException("no value starts with " + tag);
            };
        }

        private char next() {
            if (this.at >= this.text.length()) {
                throw new IllegalArgumentException("ends too soon");
            }
            return this.text.charAt(this.at++);
        }

        private char peek() {
            if (this.at >= this.text.length()) {
                throw new IllegalArgumentException("ends too soon");
            }
            return this.text.charAt(this.at);
        }

        private String until(final char end) {
            final int stop = this.text.indexOf(end, this.at);
            if (stop < 0) {
                throw new IllegalArgumentException("no " + end);
            }
            final String read = this.text.substring(this.at, stop);
            this.at = stop + 1;
            return read;
        }
    }
}
