/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.List;

/**
 * A running program as a machine's program table knows it, whatever runs it.
 *
 * <p>The table and the machine reach every program through this and never ask what language it is in. What only the
 * machine's own language can do (be told a message, a Gateway's message or a player's click, show windows, end with a
 * code of its own) has an answer here for every program: a program whose language has none of it takes a message and
 * does nothing with it, hears nothing else, shows nothing, and ends with one for a halt and zero otherwise.
 */
public interface IProgramRuntime {

    /** The name the program gave itself, or empty when it gave none. */
    String name();

    /** How the program ended: the code it gave, one for a halt, zero otherwise. */
    int exitCode();

    /** Hands the program a line from another program on the machine; false when it cannot take it. */
    default boolean deliverMessage(final int from, final String text, final long tick) {
        return true;
    }

    /** Hands the program what a ComputerCraft computer said through a Gateway; false when it did not hear it. */
    default boolean deliverGatewayMessage(final int from, final String text, final long tick) {
        return false;
    }

    /** Tells the program what a player did to one of its widgets; false when no part of it heard. */
    default boolean deliverUiEvent(final long window, final long widget, final String kind,
                                   final List<Object> values) {
        return false;
    }

    /** The windows the program has open, in the order it opened them. */
    default List<Values.Obj> windows() {
        return List.of();
    }

    /** Tells the program that another program has ended, so whatever of it waits on that one runs again. */
    default void programEnded(final int program) {
    }
}
