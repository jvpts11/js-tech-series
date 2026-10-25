/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;

/**
 * The rows a member writes of a state table ({@link INetworkMember#fillRows}): most write one, of themselves, where
 * they stand; a member that reads other machines, as an industrial controller reads the machines on its cable, writes
 * one for each, each where that machine stands, so the table's {@code at} says where each one is.
 */
@ApiStatus.Experimental
public final class StateRows {

    private final StateTable table;
    private final NodeUuid member;
    private final long own;
    private final List<StateRow.Writer> writers = new ArrayList<>();
    private final List<Long> places = new ArrayList<>();

    /* The rows of {@code table} the member {@code member}, standing at {@code own}, writes; the Core makes them. */
    StateRows(final StateTable table, final NodeUuid member, final long own) {
        this.table = Objects.requireNonNull(table, "table");
        this.member = Objects.requireNonNull(member, "member");
        this.own = own;
    }

    /** A row of the member itself, where it stands. */
    public StateRow.Writer own() {
        return row(this.own);
    }

    /** A row of a machine the member reads, standing at {@code where}. */
    public StateRow.Writer at(final BlockPos where) {
        return row(where.asLong());
    }

    /* The rows written, in the order they were begun. */
    List<StateRow> done() {
        final List<StateRow> rows = new ArrayList<>(this.writers.size());
        for (int i = 0; i < this.writers.size(); i++) {
            rows.add(this.writers.get(i).done(this.member, this.places.get(i)));
        }
        return rows;
    }

    private StateRow.Writer row(final long place) {
        final StateRow.Writer writer = new StateRow.Writer(this.table);
        this.writers.add(writer);
        this.places.add(place);
        return writer;
    }
}
