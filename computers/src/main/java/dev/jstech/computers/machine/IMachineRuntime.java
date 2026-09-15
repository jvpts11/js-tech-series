/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.IProgramRuntime;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A program as a machine runs it: what every language gives a machine, what the machine keeps of every program (what it
 * wrote and the memory it may hold), and what the machine's program table asks of every program.
 *
 * <p>A program the machine runs from a listing is one of these already. One of any other language is wrapped once, when
 * it starts or comes back, in a runtime that holds the view its language was given and gives the table's answers for a
 * language that has none of them; after that, nothing asks what language a program is in.
 */
public interface IMachineRuntime extends ILanguageProcess, IProgramRuntime {

    @Override
    String name();

    /** What the program has written for a person to read, oldest kept line first. */
    List<String> console();

    /** How many lines it has written since it started, the ones no longer kept included. */
    long written();

    /** How many bytes it may hold at once. */
    long heapBytes();

    /** The language that runs the program, or null for one the machine runs from a listing itself. */
    @Nullable
    default IProgrammingLanguage language() {
        return null;
    }

    /**
     * Lets the program say goodbye out of at most {@code budget} instructions, and says how many it used. A runtime
     * that cannot tell is charged the whole budget.
     */
    default int farewell(final int budget) {
        this.onStop(budget);
        return budget;
    }

    /**
     * Whether the program has ended for good without saying so, answered true once. A program of the machine's own
     * language tells the machine the moment it ends; one of another language cannot, so the tick asks when it finds
     * the program over.
     */
    default boolean endedUnannounced() {
        return false;
    }
}
