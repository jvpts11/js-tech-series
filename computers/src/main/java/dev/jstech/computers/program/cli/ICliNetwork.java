/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.iql.IIqlCondition;
import java.util.List;

/**
 * What a command reads of the data network its computer is on: whether there is one, what it holds and where, and its
 * servers.
 *
 * <p>Every member answers as a computer on no network would.
 */
public interface ICliNetwork {

    /** Whether the computer is on a data network. */
    default boolean onNetwork() {
        return false;
    }

    /** A short identifier for the network this computer is on, or {@code ""} when unlinked. */
    default String networkId() {
        return "";
    }

    /** Whether this computer is itself the network's Mainframe (gates maintenance commands). */
    default boolean isMainframe() {
        return false;
    }

    /** Counts that describe the network at a glance. */
    default ICliComputer.NetSummary network() {
        return new ICliComputer.NetSummary(false, 0, 0, 0, 0, false);
    }

    /**
     * Items the network holds, optionally filtered by a condition and scoped to a single server by name.
     *
     * @param where  the condition an item has to meet, or null for every item
     * @param server a server name to scope the read to, or {@code ""} for the whole network
     * @param limit  the maximum number of rows to return
     */
    default List<ICliComputer.StoredItem> query(final IIqlCondition where,
                                                final String server, final int limit) {
        return List.of();
    }

    /**
     * Rows for a {@code QUERY <object>} against a schema object: {@code items} (name to quantity),
     * {@code servers} (name to items stored), {@code operations} (description to progress). An object that is
     * not wired yet returns an empty list. The label/value pair is what the studio grid and the CLI render.
     *
     * @param object the schema object name (e.g. {@code "items"}, {@code "servers"}, {@code "operations"})
     * @param where  the condition a row has to meet, or null for every row
     * @param server a server name to scope the read to, or {@code ""} for the whole network
     * @param limit  the maximum number of rows to return
     */
    default List<ICliComputer.StoredItem> queryObject(final String object,
                                                      final IIqlCondition where,
                                                      final String server, final int limit) {
        return List.of();
    }

    /** Which servers hold the named item and how much each has. */
    default List<ICliComputer.Holding> find(final String item) {
        return List.of();
    }

    /** Every server on the network, with what each is holding; empty when this machine is on none. */
    default List<ICliComputer.ServerUse> servers() {
        return List.of();
    }

    /** What the whole network is holding, and what it could hold. */
    default ICliComputer.ServerUse networkUse() {
        return new ICliComputer.ServerUse("", 0L, 0L);
    }

    /**
     * What a name a player typed could stand for, best first: the one thing it names, or the several it fits.
     *
     * <p>A command that takes an item takes it the way a person says it, so this is where {@code "oak log"}
     * becomes something a machine can act on, and where a name that fits several stays several.
     */
    default List<ICliComputer.ItemMatch> matching(final String text) {
        return List.of();
    }

    /** Everything known about one thing, by the id a match gave: where it is, what makes it, what it makes. */
    default ICliComputer.ItemDetail itemDetail(final String id) {
        return new ICliComputer.ItemDetail("", id, 0L, List.of(), List.of(), List.of());
    }
}
