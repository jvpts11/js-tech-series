/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The peripherals an owner has linked, each by its packed position with the kind of port it takes, in the order they
 * were linked. A peripheral linked through an {@link IPeripheralHub} takes one of the hub's ports, not one of the
 * owner's, and is kept with the hub's position. When the owner has fewer ports of a kind than it has peripherals of
 * that kind on its own ports, as when a card is taken out, the ones linked first keep theirs.
 *
 * <p>It is saved as three arrays of the same length, the positions, the kinds' numbers and the hubs, which the owner
 * writes where it keeps the rest of its state.
 */
public final class PeripheralPorts {

    private final Map<Long, PortKind> linked = new LinkedHashMap<>();
    /**
     * The hub each peripheral linked through one hangs from; a peripheral on the owner's own port is not here. One
     * taken out through {@link #endpoints()} may stay behind, so every read asks {@link #linked} first.
     */
    private final Map<Long, Long> hubs = new HashMap<>();

    /**
     * The linked positions, in the order they were linked: a live view, so taking one out of it unlinks it; a link is
     * only made through {@link #link} or {@link #linkThrough}.
     */
    public Set<Long> endpoints() {
        return this.linked.keySet();
    }

    /** Links the peripheral at {@code pos} on one of the owner's own ports of {@code kind}; whether that changed. */
    public boolean link(final long pos, final PortKind kind) {
        final boolean wasHubbed = this.hubs.remove(pos) != null;
        return this.linked.put(pos, kind) != kind || wasHubbed;
    }

    /** Links the peripheral at {@code pos} on a port of {@code kind} of the hub at {@code hub}; whether it changed. */
    public boolean linkThrough(final long pos, final PortKind kind, final long hub) {
        final Long before = this.hubs.put(pos, hub);
        return this.linked.put(pos, kind) != kind || before == null || before != hub;
    }

    /** Unlinks the peripheral at {@code pos}; whether it was linked. */
    public boolean unlink(final long pos) {
        this.hubs.remove(pos);
        return this.linked.remove(pos) != null;
    }

    /** The kind of port the peripheral at {@code pos} takes, or null when it is not linked. */
    public @Nullable PortKind kindOf(final long pos) {
        return this.linked.get(pos);
    }

    /** The hub the peripheral at {@code pos} hangs from; empty when it is on the owner's own port or not linked. */
    public OptionalLong hubOf(final long pos) {
        final Long hub = this.linked.containsKey(pos) ? this.hubs.get(pos) : null;
        return hub == null ? OptionalLong.empty() : OptionalLong.of(hub);
    }

    /** How many linked peripherals take one of the owner's own ports of {@code kind}. */
    public int inUse(final PortKind kind) {
        int count = 0;
        for (final Map.Entry<Long, PortKind> entry : this.linked.entrySet()) {
            if (entry.getValue() == kind && !this.hubs.containsKey(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    /** How many linked peripherals take a port of the hub at {@code hub}. */
    public int inUseThrough(final long hub) {
        int count = 0;
        for (final Map.Entry<Long, Long> entry : this.hubs.entrySet()) {
            if (entry.getValue() == hub && this.linked.containsKey(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether the peripheral at {@code pos} is linked and still has its port. On the owner's own ports, that is being
     * among the first {@code ports} of its kind to have been linked there, the ones that keep a port when there are
     * fewer ports than peripherals. A peripheral on a hub's port keeps it while the hub stands, which its way to the
     * owner shows.
     */
    public boolean holds(final long pos, final int ports) {
        final PortKind kind = this.linked.get(pos);
        if (kind == null) {
            return false;
        }
        if (this.hubs.containsKey(pos)) {
            return true;
        }
        int before = 0;
        for (final Map.Entry<Long, PortKind> entry : this.linked.entrySet()) {
            if (entry.getKey() == pos) {
                return before < ports;
            }
            if (entry.getValue() == kind && !this.hubs.containsKey(entry.getKey())) {
                before++;
            }
        }
        return false;
    }

    /** The linked positions, in order, for saving beside {@link #kindIds()} and {@link #hubPositions()}. */
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
     * The hubs, in the order of {@link #positions()}. A peripheral on the owner's own port is saved with its own
     * position, which no hub can have, as a peripheral never hangs from itself.
     */
    public long[] hubPositions() {
        final long[] out = new long[this.linked.size()];
        int i = 0;
        for (final long pos : this.linked.keySet()) {
            out[i++] = this.hubs.getOrDefault(pos, pos);
        }
        return out;
    }

    /**
     * Puts back what {@link #positions()} and {@link #kindIds()} saved, every peripheral on the owner's own ports. A
     * position saved without a kind it can read is left out: its peripheral finds itself unlinked and links again,
     * taking the port of its own kind.
     */
    public void restore(final long[] positions, final int[] kindIds) {
        restore(positions, kindIds, new long[0]);
    }

    /**
     * Puts back what {@link #positions()}, {@link #kindIds()} and {@link #hubPositions()} saved. A save from before
     * the hubs has no hubs, and its peripherals stand on the owner's own ports until their ways are checked.
     */
    public void restore(final long[] positions, final int[] kindIds, final long[] hubPositions) {
        this.linked.clear();
        this.hubs.clear();
        for (int i = 0; i < positions.length && i < kindIds.length; i++) {
            final PortKind kind = PortKind.byId(kindIds[i]);
            if (kind != null) {
                this.linked.put(positions[i], kind);
                if (i < hubPositions.length && hubPositions[i] != positions[i]) {
                    this.hubs.put(positions[i], hubPositions[i]);
                }
            }
        }
    }
}
