/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a running program holds.
 *
 * <p>Numbers, bools and characters are the boxed ones Java already has, because they are values and
 * two of them that are equal are the same. Everything else is one of the kinds here, and every one
 * of those is a thing on the heap with a size and a life the program controls.
 */
public final class Values {

    private Values() {
    }

    /** An instance of a class the program declared, or of the object a lambda keeps things in. */
    public static final class Obj {

        private final String type;
        private final Map<String, Object> fields = new LinkedHashMap<>();

        public Obj(final String type) {
            this.type = type;
        }

        /** The name of the class this is one of. */
        public String type() {
            return this.type;
        }

        /** What that field holds. */
        public Object get(final String name) {
            return this.fields.get(name);
        }

        /** Puts something in that field. */
        public void set(final String name, final Object value) {
            this.fields.put(name, value);
        }

        /** Everything it holds, for writing the object down. */
        public Map<String, Object> all() {
            return new LinkedHashMap<>(this.fields);
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    /** A fixed run of values, all of the same type. */
    public static final class Arr {

        private final String element;
        private final Object[] values;

        public Arr(final String element, final int length) {
            this.element = element;
            this.values = new Object[length];
        }

        /** The type of what it holds. */
        public String element() {
            return this.element;
        }

        /** How many it holds. */
        public int length() {
            return this.values.length;
        }

        /** What is at that place. */
        public Object get(final int index, final int line) {
            this.check(index, line);
            return this.values[index];
        }

        /** Puts something at that place. */
        public void set(final int index, final Object value, final int line) {
            this.check(index, line);
            this.values[index] = value;
        }

        private void check(final int index, final int line) {
            if (index < 0 || index >= this.values.length) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                        "there is no place " + index + " in an array of " + this.values.length);
            }
        }

        /** Everything it holds, in order, for writing the array down. */
        public List<Object> all() {
            return new ArrayList<>(java.util.Arrays.asList(this.values));
        }
    }

    /** A run of values that grows. */
    public static final class ListValue {

        private final List<Object> items = new ArrayList<>();

        /** What it holds, in order. */
        public List<Object> items() {
            return this.items;
        }

        /** How many it holds. */
        public int size() {
            return this.items.size();
        }

        /** What is at that place. */
        public Object get(final int index, final int line) {
            this.check(index, line);
            return this.items.get(index);
        }

        /** Puts something at that place. */
        public void set(final int index, final Object value, final int line) {
            this.check(index, line);
            this.items.set(index, value);
        }

        private void check(final int index, final int line) {
            if (index < 0 || index >= this.items.size()) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                        "there is no place " + index + " in a list of " + this.items.size());
            }
        }

        /** What the list costs: its own header and a reference for each thing in it. */
        public long bytes() {
            return Heap.HEADER + (long) Heap.REFERENCE * this.items.size();
        }
    }

    /** Values reached by a key. */
    public static final class MapValue {

        private final Map<Object, Object> entries = new LinkedHashMap<>();

        /** What it holds, by key. */
        public Map<Object, Object> entries() {
            return this.entries;
        }

        /** What the map costs: its own header and a pair of references for each entry. */
        public long bytes() {
            return Heap.HEADER + 2L * Heap.REFERENCE * this.entries.size();
        }
    }

    /**
     * A Lua table: values reached by any key, with the run of whole numbers from one kept apart.
     *
     * <p>Keys that mean the same thing are the same key: a real number that is whole is kept as the
     * whole number, and there is no minus zero. The run from one upwards lives in an array, so a table
     * used as a list costs and behaves like one; everything else is kept in the order it was put in,
     * which is the order {@code next} walks it in. Taking a key out leaves its place behind until the
     * next new key comes, so a walk that takes keys out on the way does not lose its footing.
     */
    public static final class Table {

        private static final Object[] NONE = new Object[0];

        private Object[] array = NONE;
        private int count;
        private final List<Object> keys = new ArrayList<>();
        private final List<Object> values = new ArrayList<>();
        private final Map<Object, Integer> where = new java.util.HashMap<>();
        private int holes;
        private Table metatable;

        /** The key as the table keeps it: a whole real is the whole number, and minus zero is zero. */
        public static Object normalise(final Object key) {
            if (key instanceof Double real) {
                if (real == Math.rint(real) && !real.isInfinite()
                        && real >= Long.MIN_VALUE && real <= Long.MAX_VALUE) {
                    return real.longValue();
                }
                return real;
            }
            if (key instanceof Integer whole) {
                return whole.longValue();
            }
            if (key instanceof Float real) {
                return normalise(real.doubleValue());
            }
            return key;
        }

        /** What the key holds, or null. Nothing here consults a metatable. */
        public Object get(final Object key) {
            final Object at = normalise(key);
            if (at instanceof Long index && index >= 1 && index <= this.count) {
                return this.array[(int) (index - 1)];
            }
            final Integer place = this.where.get(at);
            return place == null ? null : this.values.get(place);
        }

        /** Puts a value under a key; null takes the key out. Nothing here consults a metatable. */
        public void put(final Object key, final Object value) {
            final Object at = normalise(key);
            if (at instanceof Long index && index >= 1 && index <= this.count + 1) {
                if (index == this.count + 1) {
                    if (value == null) {
                        return;
                    }
                    this.append(value);
                    this.migrate();
                    return;
                }
                this.array[(int) (index - 1)] = value;
                if (value == null && index == this.count) {
                    while (this.count > 0 && this.array[this.count - 1] == null) {
                        this.count--;
                    }
                }
                return;
            }
            final Integer place = this.where.get(at);
            if (place != null) {
                if (value == null && this.values.get(place) != null) {
                    this.holes++;
                } else if (value != null && this.values.get(place) == null) {
                    this.holes--;
                }
                this.values.set(place, value);
                return;
            }
            if (value == null) {
                return;
            }
            if (this.holes > 0 && this.holes * 2 >= this.keys.size()) {
                this.compact();
            }
            this.where.put(at, this.keys.size());
            this.keys.add(at);
            this.values.add(value);
        }

        private void append(final Object value) {
            if (this.count == this.array.length) {
                this.array = java.util.Arrays.copyOf(this.array, Math.max(4, this.array.length * 2));
            }
            this.array[this.count++] = value;
        }

        /* A key that was kept apart because the run had not reached it yet joins the run once it does. */
        private void migrate() {
            while (true) {
                final Long next = (long) this.count + 1;
                final Integer place = this.where.get(next);
                if (place == null || this.values.get(place) == null) {
                    return;
                }
                this.append(this.values.get(place));
                this.values.set(place, null);
                this.holes++;
            }
        }

        private void compact() {
            final List<Object> liveKeys = new ArrayList<>();
            final List<Object> liveValues = new ArrayList<>();
            this.where.clear();
            for (int i = 0; i < this.keys.size(); i++) {
                if (this.values.get(i) != null) {
                    this.where.put(this.keys.get(i), liveKeys.size());
                    liveKeys.add(this.keys.get(i));
                    liveValues.add(this.values.get(i));
                }
            }
            this.keys.clear();
            this.keys.addAll(liveKeys);
            this.values.clear();
            this.values.addAll(liveValues);
            this.holes = 0;
        }

        /**
         * A border: a whole number n where n holds something and n + 1 holds nothing, or zero.
         *
         * <p>The end of the run is one when the run ends cleanly; when its last place has been emptied,
         * the border is looked for inside the run, and when the key past the run was kept apart, the
         * run carries on through those.
         */
        public long length() {
            if (this.count > 0 && this.array[this.count - 1] == null) {
                int low = 0;
                int high = this.count;
                while (high - low > 1) {
                    final int middle = (low + high) / 2;
                    if (this.array[middle - 1] == null) {
                        high = middle;
                    } else {
                        low = middle;
                    }
                }
                return low;
            }
            long border = this.count;
            while (this.get(border + 1) != null) {
                border++;
            }
            return border;
        }

        /**
         * The key that comes after this one, or null at the end; the first key for a null key.
         *
         * <p>Returns null for a key the table does not know, which is the caller's to complain about.
         */
        public Object nextKey(final Object key) {
            final Object at = normalise(key);
            int index;
            if (at == null) {
                index = 0;
            } else if (at instanceof Long whole && whole >= 1 && whole <= this.count) {
                index = (int) (whole - 1) + 1;
            } else {
                final Integer place = this.where.get(at);
                if (place == null) {
                    return null;
                }
                index = this.count + place + 1;
            }
            for (; index < this.count; index++) {
                if (this.array[index] != null) {
                    return (long) (index + 1);
                }
            }
            for (int place = index - this.count; place < this.keys.size(); place++) {
                if (this.values.get(place) != null) {
                    return this.keys.get(place);
                }
            }
            return null;
        }

        /** Whether this key is one the table knows, even if it holds nothing right now. */
        public boolean knows(final Object key) {
            final Object at = normalise(key);
            if (at instanceof Long whole && whole >= 1 && whole <= this.count) {
                return true;
            }
            return this.where.containsKey(at);
        }

        /** How far the run from one reaches. */
        public int runLength() {
            return this.count;
        }

        /** One place in the run, from zero. */
        public Object inRun(final int index) {
            return this.array[index];
        }

        /** Every key kept apart, holes included, in the order they were put in. */
        public List<Object> apartKeys() {
            return this.keys;
        }

        /** What each key kept apart holds, lining up with {@link #apartKeys()}. */
        public List<Object> apartValues() {
            return this.values;
        }

        public Table metatable() {
            return this.metatable;
        }

        public void setMetatable(final Table metatable) {
            this.metatable = metatable;
        }

        /** What it costs: its header, a place for every slot of the run and three for every other key. */
        public long bytes() {
            return Heap.HEADER + (long) Heap.REFERENCE * this.array.length
                    + 3L * Heap.REFERENCE * this.keys.size() + Heap.REFERENCE;
        }
    }

    /** One method bound to the object it belongs to. */
    public record Bound(Object target, String owner, String method, List<String> parameters,
                        String returns) {

        public Bound {
            parameters = List.copyOf(parameters);
        }
    }

    /**
     * A handler, or a run of them.
     *
     * <p>Joining two delegates makes a third that calls both in turn, which is what lets an event
     * have more than one listener without the event itself knowing how many.
     */
    public static final class DelegateValue {

        private final String type;
        private final List<Bound> chain;

        public DelegateValue(final String type, final List<Bound> chain) {
            this.type = type;
            this.chain = List.copyOf(chain);
        }

        /** The delegate type this is one of. */
        public String type() {
            return this.type;
        }

        /** The methods it calls, in order. */
        public List<Bound> chain() {
            return this.chain;
        }

        /** What it costs: its header, and a target and a method for each one in the run. */
        public long bytes() {
            return Heap.HEADER + 2L * Heap.REFERENCE * this.chain.size();
        }
    }
}
