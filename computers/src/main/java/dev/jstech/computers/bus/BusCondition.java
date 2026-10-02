/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import dev.jstech.core.id.IStableId;

import java.util.Objects;

/**
 * What a bus of the Standard or later waits for before it moves: the network holding less than so many of an item or
 * of a tag, the hours of the day, or another bus having finished.
 *
 * @param kind     which of the three it is
 * @param subject  for a stock: the item's id, or a tag's id after a {@code #}; for {@link Kind#AFTER}: the bus's name
 * @param below    for a stock: the count the network must hold less of
 * @param fromHour for the hours: the hour it starts at, 0 to 23
 * @param toHour   for the hours: the hour it ends at, 0 to 23, which may be past midnight
 */
public record BusCondition(Kind kind, String subject, long below, int fromHour, int toHour) {

    /** The longest a subject may be: an item's id, a tag's, or a bus's name. */
    public static final int MAX_SUBJECT = 64;

    public BusCondition {
        Objects.requireNonNull(kind, "kind must not be null");
        subject = subject == null ? "" : subject;
        fromHour = Math.floorMod(fromHour, 24);
        toHour = Math.floorMod(toHour, 24);
        below = Math.max(0L, below);
    }

    /** Only while the network holds fewer than {@code below} of {@code subject} (an item id, or a tag after #). */
    public static BusCondition stock(final String subject, final long below) {
        return new BusCondition(Kind.STOCK, subject, below, 0, 0);
    }

    /** Only between {@code from} and {@code to}, hours of the day, past midnight when {@code to} comes first. */
    public static BusCondition hours(final int from, final int to) {
        return new BusCondition(Kind.HOURS, "", 0L, from, to);
    }

    /** Only after the bus named {@code bus} has nothing more to move. */
    public static BusCondition after(final String bus) {
        return new BusCondition(Kind.AFTER, bus, 0L, 0, 0);
    }

    /** Whether a stock condition names a tag rather than an item. */
    public boolean onTag() {
        return kind == Kind.STOCK && subject.startsWith("#");
    }

    /** For the hours: whether {@code hour} of the day falls between them, past midnight as well. */
    public boolean holdsAt(final int hour) {
        if (fromHour == toHour) {
            return true;
        }
        return fromHour < toHour ? hour >= fromHour && hour < toHour : hour >= fromHour || hour < toHour;
    }

    /** The three kinds of condition, by an id kept as they are saved. */
    public enum Kind implements IStableId {
        STOCK(0),
        HOURS(1),
        AFTER(2);

        private final int id;

        Kind(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }
}
