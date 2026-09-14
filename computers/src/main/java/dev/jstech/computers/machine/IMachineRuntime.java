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

/**
 * A program as a machine runs it: what every language gives a machine, and what the machine's program table asks of
 * every program.
 *
 * <p>A program of the machine's own language is one of these already. One of any other language is wrapped once, when
 * it starts or comes back, in a runtime that gives the table's answers for a language that has none of them; after
 * that, nothing asks what language a program is in.
 */
public interface IMachineRuntime extends ILanguageProcess, IProgramRuntime {

    @Override
    String name();

    /**
     * Lets the program say goodbye out of at most {@code budget} instructions, and says how many it used. A runtime
     * that cannot tell is charged the whole budget.
     */
    default int farewell(final int budget) {
        this.onStop(budget);
        return budget;
    }

    /** What runs a program a language started or brought back. */
    static IMachineRuntime of(final ILanguageProcess process) {
        return process instanceof IMachineRuntime runtime ? runtime : new HostedRuntime(process);
    }
}
