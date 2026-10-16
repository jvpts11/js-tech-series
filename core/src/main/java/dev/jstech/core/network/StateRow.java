/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.uuid.NodeUuid;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's row of a state table, as it was when read: which member of the network it is, where it stands, and its
 * value in each column, or none where it had nothing to say.
 */
public final class StateRow {

    private final StateTable table;
    private final NodeUuid member;
    private final long pos;
    private final @Nullable Object[] values;

    private StateRow(final StateTable table, final NodeUuid member, final long pos, final @Nullable Object[] values) {
        this.table = table;
        this.member = member;
        this.pos = pos;
        this.values = values;
    }

    /** The table it is a row of. */
    public StateTable table() {
        return this.table;
    }

    /** The member of the network whose row it is. */
    public NodeUuid member() {
        return this.member;
    }

    /** Where the member stands, as the game packs a block position into a number. */
    public long pos() {
        return this.pos;
    }

    /** The number in {@code column}, if the column holds numbers and the member gave one. */
    public OptionalDouble number(final String column) {
        return value(column) instanceof Double number ? OptionalDouble.of(number) : OptionalDouble.empty();
    }

    /** The text in {@code column}, if the column holds text and the member gave some. */
    public Optional<String> text(final String column) {
        return value(column) instanceof String text ? Optional.of(text) : Optional.empty();
    }

    /** The yes or no in {@code column}, if the column holds flags and the member gave one. */
    public Optional<Boolean> flag(final String column) {
        return value(column) instanceof Boolean flag ? Optional.of(flag) : Optional.empty();
    }

    /** The value in {@code column}: a {@code Double}, a {@code String} or a {@code Boolean}; none where none. */
    public @Nullable Object value(final String column) {
        final int index = this.table.indexOf(column);
        return index < 0 ? null : this.values[index];
    }

    /** Writes a member's row: each value set in its column, of the kind its column holds. */
    public static final class Writer {

        private final StateTable table;
        private final @Nullable Object[] values;

        /** Starts an empty row of {@code table}; the Core hands one to each member it reads. */
        public Writer(final StateTable table) {
            this.table = Objects.requireNonNull(table, "table");
            this.values = new Object[table.columns().size()];
        }

        /** Sets the number in {@code column}, a column of numbers, in its unit. */
        public Writer set(final String column, final double value) {
            return put(column, StateValueKind.NUMBER, value);
        }

        /** Sets the text in {@code column}, a column of text. */
        public Writer set(final String column, final String value) {
            return put(column, StateValueKind.TEXT, Objects.requireNonNull(value, "value"));
        }

        /** Sets the yes or no in {@code column}, a column of flags. */
        public Writer set(final String column, final boolean value) {
            return put(column, StateValueKind.FLAG, value);
        }

        /** The row written, as the member {@code member} at {@code pos}. */
        public StateRow done(final NodeUuid member, final long pos) {
            return new StateRow(this.table, Objects.requireNonNull(member, "member"), pos,
                    Arrays.copyOf(this.values, this.values.length));
        }

        private Writer put(final String column, final StateValueKind kind, final Object value) {
            final int index = this.table.indexOf(column);
            if (index < 0) {
                throw new IllegalArgumentException("the table " + this.table.id() + " has no column " + column);
            }
            if (this.table.columns().get(index).kind() != kind) {
                throw new IllegalArgumentException("the column " + column + " of " + this.table.id()
                        + " holds another kind of value");
            }
            this.values[index] = value;
            return this;
        }
    }
}
