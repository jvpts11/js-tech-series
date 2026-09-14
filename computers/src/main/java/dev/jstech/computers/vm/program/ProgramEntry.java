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
 * One program a machine is running: the number it lists it under, the file it was started from and the text that file
 * held, the memory it was given, what runs it, which program started it, what it was started with, and how urgently it
 * runs.
 *
 * @param <R> what runs the program, as the machine holds it
 */
public record ProgramEntry<R extends IProgramRuntime>(int id, String file, String binary, int heapMb, R process,
                                                      IProgramParent parent, List<String> args,
                                                      ProgramPriority priority) {

    /**
     * What a program is listed as when it gave itself no name: the runtime that runs it, the way an interpreted program
     * shows up under its interpreter on any machine.
     */
    public static final String RUNTIME_NAME = "sigma";

    public ProgramEntry {
        parent = parent == null ? IProgramParent.NONE : parent;
        args = args == null ? List.of() : List.copyOf(args);
        priority = priority == null ? ProgramPriority.MEDIUM : priority;
    }

    /** What the machine lists it as: the name the program gave itself, or the runtime's that runs it. */
    public String name() {
        final String own = this.process.name();
        return own != null && !own.isBlank() ? own : RUNTIME_NAME;
    }
}
