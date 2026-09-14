/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.ProgramTable;
import dev.jstech.core.language.ILanguageProcess;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * One machine's tick, handed out to its programs.
 *
 * <p>What the programs watch is looked up once a thing, and only on a tick where some program watches something, and
 * handed only to the programs that watch it; what the machine owes for work done outside its programs comes off the
 * top; a terminal program nothing can answer any more and a finished one nobody waits for are cleared away;
 * everything still going is stepped by the {@link Scheduler}.
 */
final class ProgramTicker {

    private final ProgramTable<IMachineRuntime> table;
    private final TerminalFocus focus;
    private final Scheduler scheduler = new Scheduler();
    /** What the machine still owes for work done on its behalf outside its programs. */
    private int owed;

    ProgramTicker(final ProgramTable<IMachineRuntime> table, final TerminalFocus focus) {
        this.table = table;
        this.focus = focus;
    }

    /** Adds to what the machine owes; see {@link MachinePrograms#owe}. */
    void owe(final int credits) {
        this.owed = (int) Math.min(Integer.MAX_VALUE, this.owed + (long) Math.max(0, credits));
    }

    /** What the machine still owes for work done outside its programs. */
    int owed() {
        return this.owed;
    }

    /** Runs one tick; see {@link MachinePrograms#tick(int, long, ToLongFunction, Predicate)}. */
    void tick(final int credits, final long deadline, final ToLongFunction<String> stock,
              final Predicate<IProgramParent.Remote> waiting) {
        if (stock != null && !this.table.isEmpty()) {
            this.deliverWatched(stock);
        }
        // What was spent on the machine's behalf outside its programs comes off the top first.
        final int available = Math.max(0, credits - this.owed);
        this.owed = Math.max(0, this.owed - Math.max(0, credits));
        if (this.table.isEmpty() || available <= 0) {
            return;
        }
        final List<ProgramEntry<IMachineRuntime>> ready = new ArrayList<>();
        final List<ProgramEntry<IMachineRuntime>> done = new ArrayList<>();
        this.sort(waiting, ready, done);
        this.table.removeAll(done);
        if (ready.isEmpty()) {
            return;
        }
        final List<Slot> slots = new ArrayList<>(ready.size());
        for (final ProgramEntry<IMachineRuntime> one : ready) {
            slots.add(new Slot(one.process(), one.priority() == ProgramPriority.LOW));
        }
        this.scheduler.run(slots, available, System::nanoTime, deadline);
    }

    /**
     * Looks up everything the programs watch, each thing once, and hands the answers to the programs watching. On a
     * machine whose programs watch nothing, nothing is built.
     */
    private void deliverWatched(final ToLongFunction<String> stock) {
        Map<String, Long> totals = null;
        Function<String, Long> look = null;
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            final List<String> items = one.process().watching();
            for (int i = 0; i < items.size(); i++) {
                if (totals == null) {
                    totals = new LinkedHashMap<>();
                    look = stock::applyAsLong;
                }
                totals.computeIfAbsent(items.get(i), look);
            }
        }
        if (totals == null) {
            return;
        }
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            if (!one.process().watching().isEmpty()) {
                one.process().deliver(totals);
            }
        }
    }

    /** Sorts the programs into those that get a share of this tick and those that leave the machine now. */
    private void sort(final Predicate<IProgramParent.Remote> waiting, final List<ProgramEntry<IMachineRuntime>> ready,
                      final List<ProgramEntry<IMachineRuntime>> done) {
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            final ILanguageProcess.State state = one.process().state();
            if (!one.process().isService() && !this.focus.holds(one.id()) && one.process().waitingForInput()) {
                /*
                 * A terminal program stopped on a read with no terminal in front of it: only the program
                 * in front gets what is typed, so nothing can ever reach this one. However it came to be
                 * here, it is stopped rather than kept for ever as something the machine is running.
                 */
                one.process().onStop(MachinePrograms.FAREWELL);
                done.add(one);
                continue;
            }
            if (state == ILanguageProcess.State.HALTED || state == ILanguageProcess.State.FINISHED) {
                /*
                 * A program that runs at a terminal is done when it returns, and is asked nothing more;
                 * one that stays up is asked again. Either way, a finished terminal program only leaves
                 * once whoever was waiting on it has read it.
                 */
                if (!one.process().isService() && !this.focus.holds(one.id())) {
                    /*
                     * One started by another program, on this machine or on another, keeps its output and
                     * its exit code for that program to read, and goes when it goes.
                     */
                    if (!this.awaited(one, waiting)) {
                        done.add(one);
                    }
                } else if (one.process().isService() && state == ILanguageProcess.State.FINISHED) {
                    one.process().onTick();
                    ready.add(one);
                }
                continue;
            }
            ready.add(one);
        }
    }

    /** Whether the program that started this one is still there to read what it left. */
    private boolean awaited(final ProgramEntry<IMachineRuntime> one, final Predicate<IProgramParent.Remote> waiting) {
        return switch (one.parent()) {
            case IProgramParent.Remote remote -> waiting.test(remote);
            case IProgramParent.Local local -> this.table.byId(local.program()) != null;
            case IProgramParent.None none -> false;
        };
    }

    /** One program as the tick deals it out: what it runs and whether it may be passed over. */
    private record Slot(ILanguageProcess process, boolean low) implements Scheduler.ISlot {

        @Override
        public int step(final int budget) {
            return this.process.step(budget);
        }
    }
}
