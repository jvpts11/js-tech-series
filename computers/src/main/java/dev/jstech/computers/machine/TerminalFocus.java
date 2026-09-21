/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramTable;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.nbt.CompoundTag;

/**
 * A machine's terminal as its programs see it: which program is in front of it, what that program printed that the
 * terminal has not shown yet, and where a typed line goes.
 *
 * <p>A machine has one prompt, so it has at most one program in front of it. That program keeps its place in the
 * list after it returns, because what it printed last is not read until the terminal has had its turn; every other
 * finished program is cleared away as soon as it is done.
 */
final class TerminalFocus {

    private static final String HELD = "held";
    private static final String SHOWN = "shown";

    private final ProgramTable<IMachineRuntime> table;
    private final IntPredicate stopper;
    private int held;
    private long shown;

    /**
     * @param table   the machine's programs
     * @param stopper how the machine stops one of them, farewell included
     */
    TerminalFocus(final ProgramTable<IMachineRuntime> table, final IntPredicate stopper) {
        this.table = table;
        this.stopper = stopper;
    }

    /** The program the terminal is holding, or 0. */
    int held() {
        return this.held;
    }

    /** Whether the terminal is holding the program of that number. */
    boolean holds(final int id) {
        return this.held == id;
    }

    /** Says the terminal is now waiting on that program. */
    void hold(final int id) {
        final ProgramEntry<IMachineRuntime> before = this.table.byId(this.held);
        if (before != null && before.id() != id && !before.process().isService()) {
            /*
             * The terminal is one, and a program that loses it can never read from it again: what is
             * typed goes to the program in front. Left alone it would wait for ever at no cost and some
             * memory, listed as running, so it is stopped the moment the terminal moves on.
             */
            this.stopper.test(before.id());
        }
        this.held = id;
        this.shown = 0;
    }

    /** Hands a line typed at the terminal to the program it is holding; false when it holds none. */
    boolean offerInput(final String line) {
        final ProgramEntry<IMachineRuntime> one = this.table.byId(this.held);
        if (one == null) {
            return false;
        }
        one.process().offerInput(line);
        return true;
    }

    /** Lets the terminal go, clearing the program away if it had already finished. */
    void release() {
        final ProgramEntry<IMachineRuntime> one = this.table.byId(this.held);
        this.held = 0;
        if (one != null && !MachinePrograms.running(one.process())) {
            this.table.remove(one.id());
        }
    }

    /**
     * What the held program has printed since this was last asked, and never the same line twice.
     *
     * <p>A program that printed more than its console keeps while nobody was looking has scrolled: what
     * fell off the end is gone, the way it is gone from any terminal nobody was watching.
     */
    List<String> unseen() {
        final ProgramEntry<IMachineRuntime> one = this.table.byId(this.held);
        if (one == null) {
            return List.of();
        }
        final List<String> kept = one.process().console();
        final long written = one.process().written();
        final int fresh = (int) Math.min(written - this.shown, kept.size());
        this.shown = written;
        return fresh <= 0 ? List.of() : List.copyOf(kept.subList(kept.size() - fresh, kept.size()));
    }

    /** Lets go of a program that has been stopped, if the terminal was holding it. */
    void forget(final int id) {
        if (this.held == id) {
            this.held = 0;
        }
    }

    /**
     * Lets go of a held program the machine does not have, as after a load that could not bring it back: its
     * language gone, or its saved state refused. Held, it would take every line typed at the prompt into nothing.
     */
    void letGoOfMissing() {
        if (this.held != 0 && this.table.byId(this.held) == null) {
            this.held = 0;
            this.shown = 0;
        }
    }

    /** Writes the terminal's place down beside the machine's programs. */
    void save(final CompoundTag tag) {
        tag.putInt(HELD, this.held);
        tag.putLong(SHOWN, this.shown);
    }

    /** Reads the terminal's place back. */
    void load(final CompoundTag tag) {
        this.held = tag.getInt(HELD);
        this.shown = tag.getLong(SHOWN);
    }
}
