/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The peripherals an owner has linked, each by its packed position with the kind of port it takes, in the order they
 * were linked. When the owner has fewer ports of a kind than it has peripherals of that kind, as when a card is taken
 * out, the ones linked first keep theirs.
 *
 * <p>It is saved as two arrays of the same length, the positions and the kinds' numbers, which the owner writes where
 * it keeps the rest of its state.
 */
public final class PeripheralPorts {

    private final Map<Long, PortKind> linked = new LinkedHashMap<>();

    /**
     * The linked positions, in the order they were linked: a live view, so taking one out of it unlinks it; a link is
     * only made through {@link #link}.
     */
    public Set<Long> endpoints() {
        return this.linked.keySet();
    }

    /** Links the peripheral at {@code pos} on a port of {@code kind}; whether that changed anything. */
    public boolean link(final long pos, final PortKind kind) {
        return this.linked.put(pos, kind) != kind;
    }

    /** Unlinks the peripheral at {@code pos}; whether it was linked. */
    public boolean unlink(final long pos) {
        return this.linked.remove(pos) != null;
    }

    /** The kind of port the peripheral at {@code pos} takes, or null when it is not linked. */
    public @Nullable PortKind kindOf(final long pos) {
        return this.linked.get(pos);
    }

    /** How many linked peripherals take a port of {@code kind}. */
    public int inUse(final PortKind kind) {
        int count = 0;
        for (final PortKind taken : this.linked.values()) {
            if (taken == kind) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether the peripheral at {@code pos} is linked and among the first {@code ports} of its kind to have been: the
     * ones that keep a port when there are fewer ports than peripherals.
     */
    public boolean holds(final long pos, final int ports) {
        final PortKind kind = this.linked.get(pos);
        if (kind == null) {
            return false;
        }
        int before = 0;
        for (final Map.Entry<Long, PortKind> entry : this.linked.entrySet()) {
            if (entry.getKey() == pos) {
                return before < ports;
            }
            if (entry.getValue() == kind) {
                before++;
            }
        }
        return false;
    }

    /** The linked positions, in order, for saving beside {@link #kindIds()}. */
    public long[] positions() {
        final long[] out = new long[this.linked.size()];
        int i = 0;
        for (final long pos : this.linked.keySet()) {
            out[i++] = pos;
        }
        return out;
    }

    /** The kinds' numbers, in the order of {@link #positions()}. */
    public int[] kindIds() {
        final int[] out = new int[this.linked.size()];
        int i = 0;
        for (final PortKind kind : this.linked.values()) {
            out[i++] = kind.id();
        }
        return out;
    }

    /**
     * Puts back what {@link #positions()} and {@link #kindIds()} saved. A position saved without a kind it can read
     * is left out: its peripheral finds itself unlinked and links again, taking the port of its own kind.
     */
    public void restore(final long[] positions, final int[] kindIds) {
        this.linked.clear();
        for (int i = 0; i < positions.length && i < kindIds.length; i++) {
            final PortKind kind = PortKind.byId(kindIds[i]);
            if (kind != null) {
                this.linked.put(positions[i], kind);
            }
        }
    }
}
