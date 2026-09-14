/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The watches a process has set on what its network holds, and the numbers they are known by.
 *
 * <p>Watches go off in the order they were set. The things they watch are kept beside them, each once, so whatever
 * looks the world up for the process asks about each thing once however many watches are waiting on it.
 */
final class ProgramWatches {

    /** Told of each watch that goes off, with the reading it had and the one that set it off. */
    @FunctionalInterface
    interface IFired {
        void fired(Watch watch, long before, long now, boolean first);
    }

    /** One thing a program asked to be told about. */
    static final class Watch {
        private final int id;
        private final String item;
        private final Process.Watching kind;
        private final long threshold;
        private final Values.DelegateValue handler;
        private final Values.Obj token;
        private long last;
        private boolean armed;
        private boolean seen;

        private Watch(final int id, final String item, final Process.Watching kind, final long threshold,
                      final Values.DelegateValue handler, final Values.Obj token) {
            this.id = id;
            this.item = item;
            this.kind = kind;
            this.threshold = threshold;
            this.handler = handler;
            this.token = token;
            this.armed = true;
        }

        int id() {
            return this.id;
        }

        String item() {
            return this.item;
        }

        Process.Watching kind() {
            return this.kind;
        }

        long threshold() {
            return this.threshold;
        }

        Values.DelegateValue handler() {
            return this.handler;
        }

        /** What the program holds to stop the watch; letting go of it stops it. */
        Values.Obj token() {
            return this.token;
        }

        /** The number the last reading gave. */
        long last() {
            return this.last;
        }

        /** Whether a watch on a number goes off the next time the reading crosses it. */
        boolean armed() {
            return this.armed;
        }

        /** Whether the watch has had its first reading. */
        boolean seen() {
            return this.seen;
        }
    }

    private final List<Watch> watches = new ArrayList<>();
    /** Each thing watched, once, in the order the watches kept first name it. */
    private final Set<String> items = new LinkedHashSet<>();
    /** What {@link #watching} hands out until the things watched change; null when it has to be made again. */
    private List<String> named = List.of();
    private int nextId = 1;

    /** The number the next watch set is known by. */
    int nextId() {
        return this.nextId;
    }

    /** Keeps a watch under the next number. */
    void add(final String item, final Process.Watching kind, final long threshold,
             final Values.DelegateValue handler, final Values.Obj token) {
        this.watches.add(new Watch(this.nextId++, item, kind, threshold, handler, token));
        this.items.add(item);
        this.named = null;
    }

    /**
     * Everything being watched, each once, in the order the watches kept first name it.
     *
     * <p>The machine asks on every tick, so the same unmodifiable list is handed out until a watch is set or dropped.
     */
    List<String> watching() {
        if (this.named == null) {
            this.named = List.copyOf(this.items);
        }
        return this.named;
    }

    /** The watches kept, in the order they were set, for the save. */
    List<Watch> all() {
        return Collections.unmodifiableList(this.watches);
    }

    /**
     * Takes in what the world now holds and tells {@code fired} of every watch that goes off, in the order they were
     * set.
     *
     * <p>A watch whose token {@code freed} says was let go is dropped first and never goes off. Until then it is still
     * kept and its thing still named, which costs at most one more look. A watch whose thing is not in the totals is
     * not read at all.
     */
    void deliver(final Map<String, Long> totals, final Predicate<Values.Obj> freed, final IFired fired) {
        if (this.watches.removeIf(watch -> freed.test(watch.token))) {
            this.items.clear();
            this.named = null;
            for (final Watch watch : this.watches) {
                this.items.add(watch.item);
            }
        }
        for (final Watch watch : this.watches) {
            final Long now = totals.get(watch.item);
            if (now == null) {
                continue;
            }
            final long before = watch.last;
            final boolean first = !watch.seen;
            watch.last = now;
            watch.seen = true;
            if (fires(watch, before, now, first)) {
                fired.fired(watch, before, now, first);
            }
        }
    }

    /** Puts back a watch the program had when it was saved, and numbers on from past it. */
    void restore(final Snapshot.WatchShot written, final Values.DelegateValue handler, final Values.Obj token) {
        final Watch watch = new Watch(written.id(), written.item(), Process.Watching.named(written.kind()),
                written.threshold(), handler, token);
        watch.last = written.last();
        watch.armed = written.armed();
        watch.seen = written.seen();
        this.watches.add(watch);
        this.items.add(watch.item);
        this.named = null;
        this.nextId = Math.max(this.nextId, written.id() + 1);
    }

    /**
     * Whether that watch goes off.
     *
     * <p>A threshold watch fires on the crossing, not on the state: a program told once that the iron
     * is low should not be told again every tick that it is still low. It rearms when the number goes
     * back the other way. The first look is only ever a reading, never a crossing, because a program
     * that starts up with the iron already low has not just seen it fall.
     */
    private static boolean fires(final Watch watch, final long before, final long now, final boolean first) {
        return switch (watch.kind) {
            case CHANGE -> !first && before != now;
            case BELOW -> {
                if (now > watch.threshold) {
                    watch.armed = true;
                    yield false;
                }
                final boolean go = watch.armed && !first;
                watch.armed = false;
                yield go;
            }
            case ABOVE -> {
                if (now < watch.threshold) {
                    watch.armed = true;
                    yield false;
                }
                final boolean go = watch.armed && !first;
                watch.armed = false;
                yield go;
            }
        };
    }
}
