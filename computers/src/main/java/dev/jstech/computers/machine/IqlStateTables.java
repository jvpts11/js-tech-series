/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.jstechseries.network.NetworkTables;
import dev.jstech.core.jstechseries.network.StateColumn;
import dev.jstech.core.jstechseries.network.StateRow;
import dev.jstech.core.jstechseries.network.StateTable;
import dev.jstech.core.jstechseries.network.StateTables;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The state tables other mods declare, as the network's language reads them: {@code QUERY mymod:reactors WHERE
 * temperature > 90}, a row for each of their machines on the network, so a reactor is asked about as storage is and no
 * mod depends on another to be read. A table is named by its id, or by its id's path when one table alone has that
 * path; each has the columns its mod declares and {@link #AT}, where the machine stands.
 */
@TextHolder
public final class IqlStateTables {

    /** The column every state table has besides its own: where the machine stands. */
    public static final String AT = "at";

    private static final TextKey YES = TextKey.of("jsc.iql.table.yes", "yes");
    private static final TextKey NO = TextKey.of("jsc.iql.table.no", "no");

    private IqlStateTables() {
    }

    /**
     * The table {@code object} names: the one with that id, or the one whose id's path it is when no other shares
     * that path; null for none, or for a path more than one table has ({@link #sharing}).
     */
    @Nullable
    public static StateTable find(final String object) {
        final String name = object.toLowerCase(Locale.ROOT);
        if (name.contains(":")) {
            final ResourceLocation id = ResourceLocation.tryParse(name);
            return id == null ? null : StateTables.get(id).orElse(null);
        }
        final List<StateTable> found = withPath(name);
        return found.size() == 1 ? found.get(0) : null;
    }

    /** The ids of the tables whose path {@code object} is, when more than one has it; empty otherwise. */
    public static List<ResourceLocation> sharing(final String object) {
        final String name = object.toLowerCase(Locale.ROOT);
        if (name.contains(":")) {
            return List.of();
        }
        final List<StateTable> found = withPath(name);
        if (found.size() < 2) {
            return List.of();
        }
        final List<ResourceLocation> ids = new ArrayList<>(found.size());
        for (final StateTable table : found) {
            ids.add(table.id());
        }
        return ids;
    }

    /** The columns of the table {@code object} names, its own and then {@link #AT}; null when none has that name. */
    @Nullable
    public static List<String> columnsOf(final String object) {
        final StateTable table = find(object);
        return table == null ? null : columnsOf(table);
    }

    /** The columns of {@code table}, its own and then {@link #AT}. */
    public static List<String> columnsOf(final StateTable table) {
        final List<String> columns = new ArrayList<>(table.columns().size() + 1);
        for (final StateColumn column : table.columns()) {
            columns.add(column.name());
        }
        columns.add(AT);
        return columns;
    }

    /** What the grid heads {@code table}'s columns with: each name, its unit after it, then {@link #AT}. */
    public static List<String> headingsOf(final StateTable table) {
        final List<String> headings = new ArrayList<>(table.columns().size() + 1);
        for (final StateColumn column : table.columns()) {
            headings.add(column.unit().isEmpty() ? column.name() : column.name() + " (" + column.unit() + ")");
        }
        headings.add(AT);
        return headings;
    }

    /** The rows of {@code table} on {@code network}, a machine each, keyed by column: what a WHERE reads. */
    public static List<Map<String, Text>> rows(final ServerLevel level, final NetworkUuid network,
                                               final StateTable table) {
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final StateRow state : NetworkTables.rows(level, network, table)) {
            final Map<String, Text> row = new LinkedHashMap<>();
            for (final StateColumn column : table.columns()) {
                row.put(column.name(), cellOf(state.value(column.name())));
            }
            row.put(AT, Text.literal(BlockPos.of(state.pos()).toShortString()));
            rows.add(row);
        }
        return rows;
    }

    /** The tables {@code network}'s machines have rows in, in the order their mods declared them. */
    public static List<StateTable> tablesOf(final ServerLevel level, final NetworkUuid network) {
        return NetworkTables.tablesOf(level, network);
    }

    /*
     * A cell as a WHERE compares it and the grid shows it: a whole number without its point, a flag as yes or no, a
     * word as it is, and nothing for a value the machine did not give.
     */
    private static Text cellOf(@Nullable final Object value) {
        if (value instanceof Double number) {
            return Text.literal(number == Math.rint(number) && Math.abs(number) < 1e15
                    ? Long.toString(number.longValue()) : Double.toString(number));
        }
        if (value instanceof Boolean flag) {
            return (flag ? YES : NO).text();
        }
        return value == null ? Text.EMPTY : Text.literal(value.toString());
    }

    private static List<StateTable> withPath(final String path) {
        final List<StateTable> found = new ArrayList<>();
        for (final StateTable table : StateTables.all()) {
            if (table.id().getPath().equals(path)) {
                found.add(table);
            }
        }
        return found;
    }
}
