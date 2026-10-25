/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The whole of a machine that the IQL Engine needs: one way to read, one way to act.
 *
 * <p>A statement either asks what the network holds or asks it to do something, so those are the two things the
 * engine is handed. It is given this rather than a computer because an engine that declares a computer declares
 * far more than it uses, and whoever reads it cannot tell what it touches.
 */
public interface IIqlView {

    /**
     * The rows a {@code QUERY <object>} brings back.
     *
     * @param object the name of what is being asked about
     * @param where  the condition a row has to meet, or null for every row
     * @param server a server name to scope the read to, or {@code ""} for the whole network
     * @param limit  the largest number of rows to hand back
     */
    List<ICliComputer.StoredItem> queryObject(String object, IIqlCondition where, String server, int limit);

    /**
     * The same read with every column of the table, sorted by {@code orderBy} when there is one: what a studio's
     * grid shows. {@link IqlTable#NONE} where the machine keeps no tables.
     */
    default IqlTable queryTable(final String object, final IIqlCondition where, final String server, final int limit,
                                final String orderBy, final boolean descending) {
        return IqlTable.NONE;
    }

    /**
     * Why a {@code QUERY} of {@code object} has nothing to answer, such as no table by that name; null when it has, or
     * where the machine cannot tell.
     */
    @Nullable
    default Text tableProblem(final String object) {
        return null;
    }

    /** Carries out a statement that changes something, and says how it went. */
    ICliComputer.OpResult execute(IqlOperation operation);

    /**
     * Makes one of this machine's own Redstone Interfaces read or emit, marked with {@code by}, and says how it went.
     * An interface is the machine's, not the network's, so the machine that runs the statement is the one asked.
     */
    ICliComputer.OpResult setRedstone(IqlRedstoneStatement statement, String by);
}
