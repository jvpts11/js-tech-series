/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A table of the state of a mod's machines, as a data network reads it: a row for each machine on the network that
 * has one, a column for each value it shows. A mod declares its tables once, while the game loads
 * ({@link StateTables#declare}), and its machines fill their rows ({@link INetworkMember#fill}); a query of the
 * network, J's Computers' SELECT among them, reads a reactor or a satellite the way it reads anything else, and no
 * mod depends on another to be read.
 *
 * <pre>{@code
 * public static final StateTable REACTORS = StateTables.declare(StateTable.builder(
 *                 ResourceLocation.fromNamespaceAndPath("mymod", "reactors"), MyTexts.REACTORS)
 *         .number("temperature", MyTexts.TEMPERATURE, "°C")
 *         .number("power", MyTexts.POWER, "W")
 *         .flag("running", MyTexts.RUNNING)
 *         .build());
 * }</pre>
 */
public final class StateTable {

    private final ResourceLocation id;
    private final TextKey title;
    private final List<StateColumn> columns;
    private final Map<String, Integer> indexes;

    private StateTable(final Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.columns = List.copyOf(builder.columns);
        final Map<String, Integer> byName = new HashMap<>();
        for (int i = 0; i < this.columns.size(); i++) {
            byName.put(this.columns.get(i).name(), i);
        }
        this.indexes = Map.copyOf(byName);
    }

    /** Starts the table {@code id}, its name a query reads it by, under {@code title}, the heading a screen shows. */
    public static Builder builder(final ResourceLocation id, final TextKey title) {
        return new Builder(id, title);
    }

    /** The name a query reads it by: {@code mymod:reactors}. */
    public ResourceLocation id() {
        return this.id;
    }

    /** The heading a screen shows over it, in the player's language. */
    public TextKey title() {
        return this.title;
    }

    /** Its columns, in the order a screen shows them. */
    public List<StateColumn> columns() {
        return this.columns;
    }

    /** The column named {@code name}, if it has one. */
    public Optional<StateColumn> column(final String name) {
        final Integer index = this.indexes.get(name);
        return index == null ? Optional.empty() : Optional.of(this.columns.get(index));
    }

    /** Where among its columns the one named {@code name} stands; -1 when it has none of that name. */
    public int indexOf(final String name) {
        final Integer index = this.indexes.get(name);
        return index == null ? -1 : index;
    }

    /** Declares a state table. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey title;
        private final List<StateColumn> columns = new ArrayList<>();

        private Builder(final ResourceLocation id, final TextKey title) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title");
        }

        /** A column of numbers in {@code unit}, its symbol, or none when empty. */
        public Builder number(final String name, final TextKey title, final String unit) {
            return add(new StateColumn(name, title, StateValueKind.NUMBER, unit));
        }

        /** A column of text. */
        public Builder text(final String name, final TextKey title) {
            return add(new StateColumn(name, title, StateValueKind.TEXT, ""));
        }

        /** A column of yes or no. */
        public Builder flag(final String name, final TextKey title) {
            return add(new StateColumn(name, title, StateValueKind.FLAG, ""));
        }

        public StateTable build() {
            if (this.columns.isEmpty()) {
                throw new IllegalStateException("the table " + this.id + " has no columns");
            }
            return new StateTable(this);
        }

        private Builder add(final StateColumn column) {
            for (final StateColumn other : this.columns) {
                if (other.name().equals(column.name())) {
                    throw new IllegalArgumentException("the table " + this.id + " has two columns named "
                            + column.name());
                }
            }
            this.columns.add(column);
            return this;
        }
    }
}
