/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * What a call to the world tells the runtime about itself while it is answered, for what it is charged: a call priced
 * by how much it reads or writes says how much that was.
 */
@FunctionalInterface
public interface IWorldCall {

    /** Counts that many bytes read or written towards what the call is charged. */
    void moved(long bytes);

    /**
     * Counts the rows a call brought back inside what it answers towards what it is charged, for a call whose answer
     * is a record holding them rather than the list of them itself.
     */
    default void rows(final int count) {
    }

    /**
     * The name of the class the asking program was started from, for what the world writes down of who asked: a base
     * runs many programs at once, and a record that only says "a program" is one a player cannot act on.
     */
    default String caller() {
        return "";
    }
}
