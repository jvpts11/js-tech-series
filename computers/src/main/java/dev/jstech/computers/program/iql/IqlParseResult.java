/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.core.text.Text;

/**
 * The outcome of parsing one IQL statement: an {@link IqlOperation} to run, an {@link IqlDefinition} for
 * the IQL Engine to store/run (a CREATE/DROP/EXEC of a saved object), or an error message, read in the
 * player's language. Never an exception, so the surfaces (Command Prompt, the studio) print a clean
 * diagnostic instead of a stack trace.
 *
 * <p>{@code position} is the 0-based index of the token where parsing stopped (it is {@link #NO_POSITION}
 * when there is no meaningful spot, e.g. an empty statement). Exactly one of {@code operation}/
 * {@code definition}/{@code error} is non-null on a given result; use {@link #isDefinition()} to tell an
 * action from a Layer-2 definition.
 */
public record IqlParseResult(IqlOperation operation, IqlDefinition definition, IqlBusStatement bus,
                             IqlRedstoneStatement redstone, IqlCraftingStatement crafting, Text error,
                             int position) {

    /** Sentinel for "no particular token" (empty input, or a whole-statement error). */
    public static final int NO_POSITION = -1;

    public static IqlParseResult ok(final IqlOperation operation) {
        return new IqlParseResult(operation, null, null, null, null, null, NO_POSITION);
    }

    public static IqlParseResult okDefinition(final IqlDefinition definition) {
        return new IqlParseResult(null, definition, null, null, null, null, NO_POSITION);
    }

    /** A {@code SET BUS} statement, which sets one of a bus's settings. */
    public static IqlParseResult okBus(final IqlBusStatement bus) {
        return new IqlParseResult(null, null, bus, null, null, null, NO_POSITION);
    }

    /** A {@code SET REDSTONE} statement, which makes one of the computer's Redstone Interfaces read or emit. */
    public static IqlParseResult okRedstone(final IqlRedstoneStatement redstone) {
        return new IqlParseResult(null, null, null, redstone, null, null, NO_POSITION);
    }

    /** A statement that sets a Crafting Interface or a Crafting Input Router. */
    public static IqlParseResult okCrafting(final IqlCraftingStatement crafting) {
        return new IqlParseResult(null, null, null, null, crafting, null, NO_POSITION);
    }

    public static IqlParseResult error(final Text error, final int position) {
        return new IqlParseResult(null, null, null, null, null, error, position);
    }

    /** Whether this parsed to a statement that sets a part of the crafting network. */
    public boolean isCrafting() {
        return crafting != null;
    }

    /** Whether this parsed to a {@code SET BUS} statement. */
    public boolean isBus() {
        return bus != null;
    }

    /** Whether this parsed to a {@code SET REDSTONE} statement. */
    public boolean isRedstone() {
        return redstone != null;
    }

    /**
     * Whether this parsed to an action that reads rows, a QUERY or a COUNT, rather than one that changes something; a
     * setting or a definition reads nothing.
     */
    public boolean isRead() {
        return operation != null && (operation.verb() == IqlVerb.QUERY || operation.verb() == IqlVerb.COUNT);
    }

    public boolean ok() {
        return error == null;
    }

    /** Whether this parsed to a Layer-2 definition (CREATE/DROP/EXEC) rather than an immediate action. */
    public boolean isDefinition() {
        return definition != null;
    }
}
