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
                                                      ProgramPriority priority, long startedAt) {

    /**
     * What a program is listed as when it gave itself no name: the runtime that runs it, the way an interpreted program
     * shows up under its interpreter on any machine.
     */
    public static final String RUNTIME_NAME = "sigma";

    /** The start of a program nobody knows the start of: one read from a save that never said. */
    public static final long STARTED_UNKNOWN = -1L;

    public ProgramEntry {
        parent = parent == null ? IProgramParent.NONE : parent;
        args = args == null ? List.of() : List.copyOf(args);
        priority = priority == null ? ProgramPriority.MEDIUM : priority;
    }

    /** The same, for a program whose start is not known. */
    public ProgramEntry(final int id, final String file, final String binary, final int heapMb, final R process,
                        final IProgramParent parent, final List<String> args, final ProgramPriority priority) {
        this(id, file, binary, heapMb, process, parent, args, priority, STARTED_UNKNOWN);
    }

    /** Whether it has run without stopping for at least {@code ticks} by the game tick {@code now}. */
    public boolean upFor(final long now, final long ticks) {
        return this.startedAt != STARTED_UNKNOWN && now - this.startedAt >= ticks;
    }

    /** What the machine lists it as: the name the program gave itself, or the runtime's that runs it. */
    public String name() {
        final String own = this.process.name();
        return own != null && !own.isBlank() ? own : RUNTIME_NAME;
    }
}
