/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.ProgramEntry;

/**
 * One program on a machine as a screen reads it: the Task Manager, the prompt's process list, a program asking what
 * else is running, and the memory ledger.
 *
 * <p>It reads through to the program when asked, so what it says is current, but it offers no way to reach the
 * program itself: nothing holding one can step it, stop it or hand it anything. Each part is read only when it is
 * asked for, so a reader that wants a name and a memory size pays for nothing else.
 */
public final class ProgramView {

    private final ProgramEntry<IMachineRuntime> entry;

    ProgramView(final ProgramEntry<IMachineRuntime> entry) {
        this.entry = entry;
    }

    /** The number the machine lists it under. */
    public int id() {
        return this.entry.id();
    }

    /** What the machine lists it as: the name it gave itself, or the runtime's that runs it. */
    public String name() {
        return this.entry.name();
    }

    /** The file it was started from. */
    public String file() {
        return this.entry.file();
    }

    /** The memory it was given, in megabytes. */
    public int heapMb() {
        return this.entry.heapMb();
    }

    /** How its state reads to a person; see {@link MachinePrograms#stateOf}. */
    public String state() {
        return MachinePrograms.stateOf(this.entry.process());
    }

    /** How many bytes it is holding. */
    public long heldBytes() {
        return this.entry.process().heldBytes();
    }

    /** How many bytes it was allowed. */
    public long heapBytes() {
        return this.entry.process().heapBytes();
    }
}
