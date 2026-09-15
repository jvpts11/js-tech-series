/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IMemberSpec;
import java.util.List;

/**
 * The calls and values a program reaches in the world, as the machine it runs on answers them.
 *
 * <p>When the process is made, its host is asked once, for each place the program gave such a call or value, what
 * answers it; a host that answers one by its name instead leaves that place empty. Answering one charges what the
 * system declares it costs, counted by the rows it gives back and the bytes it says it moved, on top of the instruction
 * that reaches for it.
 */
final class WorldCalls implements IWorldCall {

    /** What reading a value of the world hands over: nothing. */
    static final Object[] NOTHING = new Object[0];

    private final Process process;
    /** The class the program was started from, which is who the world is told asked. */
    private final String caller;
    private final IMemberSpec[] declared;
    private final IWorldFunction[] bound;
    /** How many bytes the call being answered has said it read or wrote. */
    private long moved;

    WorldCalls(final Process process, final ProgramImage program, final IHost host) {
        this.process = process;
        this.caller = program.entryPoint() == null ? "" : program.entryPoint();
        final List<IMemberSpec> calls = program.worldCalls();
        this.declared = calls.toArray(new IMemberSpec[0]);
        this.bound = new IWorldFunction[this.declared.length];
        for (int i = 0; i < this.bound.length; i++) {
            this.bound[i] = host.bind(this.declared[i].id());
        }
    }

    /** Whether the host answers the call or value at that place with a function, rather than by its name. */
    boolean binds(final int place) {
        return this.bound[place] != null;
    }

    /**
     * Answers the call or value at that place and charges it. What comes back is still the machine's until the caller
     * makes it the program's to hold.
     */
    Object answer(final int place, final Object target, final Object[] arguments, final int line) {
        this.moved = 0;
        final Object answer = this.bound[place].call(this, target, arguments, line);
        final int rows = answer instanceof Values.ListValue list ? list.size() : 0;
        // What a call is declared to cost comes on top of the instruction that makes it, as for every other call.
        this.process.charge(this.declared[place].cost().at(rows, this.moved));
        return answer;
    }

    /** Counts what the call being answered read or wrote, towards its price. */
    @Override
    public void moved(final long bytes) {
        this.moved += Math.max(0, bytes);
    }

    @Override
    public String caller() {
        return this.caller;
    }
}
