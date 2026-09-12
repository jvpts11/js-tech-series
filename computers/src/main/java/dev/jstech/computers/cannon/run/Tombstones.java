/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * The things a program has freed, remembered by identity for exactly as long as something still reaches
 * them.
 *
 * <p>A freed thing has to stay recognisable while the program holds another name for it, because using it
 * through that name is a mistake the machine must catch. It must not be kept a moment longer, or a program
 * that allocates and frees for days would hold on to everything it ever made. So each one is held weakly:
 * the program's own reference is what keeps it here, and once nothing does, the collector takes it and it
 * is swept out of this set. Two pieces of text that read the same are still two things, which is why this
 * goes by identity and never by equality.
 *
 * <p>Asking is the hot path, since every reach into an object asks, so it allocates nothing: the table is
 * chained by the identity hash, and a program that never freed anything answers without looking at all.
 */
final class Tombstones {

    private static final int FIRST_CAPACITY = 16;

    /** One freed thing, chained to the next whose identity lands in the same bucket. */
    private static final class Stone extends WeakReference<Object> {

        private final int hash;
        private Stone next;

        Stone(final Object thing, final int hash, final ReferenceQueue<Object> queue) {
            super(thing, queue);
            this.hash = hash;
        }
    }

    private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
    private Stone[] buckets = new Stone[FIRST_CAPACITY];
    private int size;

    /** Remembers a freed thing; remembering it twice changes nothing. */
    void add(final Object thing) {
        this.sweep();
        if (this.contains(thing)) {
            return;
        }
        if (this.size >= this.buckets.length - (this.buckets.length >> 2)) {
            this.grow();
        }
        final int hash = System.identityHashCode(thing);
        final int at = hash & (this.buckets.length - 1);
        final Stone stone = new Stone(thing, hash, this.collected);
        stone.next = this.buckets[at];
        this.buckets[at] = stone;
        this.size++;
    }

    /** Whether this very thing was freed. */
    boolean contains(final Object thing) {
        if (this.size == 0 || thing == null) {
            return false;
        }
        final int hash = System.identityHashCode(thing);
        for (Stone stone = this.buckets[hash & (this.buckets.length - 1)]; stone != null; stone = stone.next) {
            if (stone.hash == hash && stone.get() == thing) {
                return true;
            }
        }
        return false;
    }

    /** How many are remembered, counting any the collector has taken that have not been swept out yet. */
    int size() {
        return this.size;
    }

    /* Takes out what the collector has taken since the last look. */
    private void sweep() {
        for (Reference<?> gone = this.collected.poll(); gone != null; gone = this.collected.poll()) {
            this.unlink((Stone) gone);
        }
    }

    private void unlink(final Stone stone) {
        final int at = stone.hash & (this.buckets.length - 1);
        Stone previous = null;
        for (Stone each = this.buckets[at]; each != null; previous = each, each = each.next) {
            if (each == stone) {
                if (previous == null) {
                    this.buckets[at] = each.next;
                } else {
                    previous.next = each.next;
                }
                this.size--;
                return;
            }
        }
    }

    private void grow() {
        final Stone[] old = this.buckets;
        this.buckets = new Stone[old.length << 1];
        for (Stone head : old) {
            while (head != null) {
                final Stone next = head.next;
                final int at = head.hash & (this.buckets.length - 1);
                head.next = this.buckets[at];
                this.buckets[at] = head;
                head = next;
            }
        }
    }
}
