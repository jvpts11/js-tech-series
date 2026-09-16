/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;

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

    /** Carries out a statement that changes something, and says how it went. */
    ICliComputer.OpResult execute(IqlOperation operation);
}
