/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.api.language.ILanguageProcess;
import dev.jstech.computers.vm.program.Debugger;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * A program a debugger started on a machine, and the debugger attached to it.
 *
 * <p>The sgdb at a prompt and the debuggers of Virtual Studio and Virtual Studio Code start their program this way and
 * talk to it through this: what it printed since they last looked, whether it waits for a line, whether it has ended
 * and how. It runs as any program does, on the machine's own clock and out of its own credits, and is in the machine's
 * list of programs like any other, so the Task Manager shows it and can end it.
 *
 * <p>The program belongs to its debugger. It is not written down with the world, and it ends when the debugger lets it
 * go: a debugger quit, a debugging session stopped, or a window closed in the middle of one ends it at once, with no
 * farewell, as stopping a debugger ends what it was debugging.
 */
@TextHolder
public final class DebugSession {

    private final MachinePrograms programs;
    private final SigmaProgram program;
    private final int id;
    private final long startedAt;
    /** How many of the lines the program wrote have been handed on. */
    private long handed;

    /* The file that is not a Σ or Σ# program, whatever else it is. */
    private static final TextKey NOT_SIGMA = TextKey.of("jsc.machine.debug_session.not_sigma",
            "%s is not a Σ or Σ# program, and only those can be debugged");
    private static final TextKey NO_PROGRAMS = TextKey.of("jsc.machine.debug_session.no_programs",
            "this machine runs no programs");

    private DebugSession(final MachinePrograms programs, final SigmaProgram program, final int id,
                         final long startedAt) {
        this.programs = programs;
        this.program = program;
        this.id = id;
        this.startedAt = startedAt;
    }

    /** The number the program runs under on its machine. */
    public int id() {
        return this.id;
    }

    /** The debugger attached to the program. */
    public Debugger debugger() {
        return this.program.process().debugger();
    }

    /** Whether the program is over: it returned, it was stopped, or it halted. */
    public boolean ended() {
        return this.programs.byId(this.id) == null || !MachinePrograms.running(this.program);
    }

    /** The code the program ended with, or 0 while it runs or when it ended with none. */
    public int exitCode() {
        return this.program.exitCode();
    }

    /** Whether the program stopped on a mistake, rather than returning or being stopped. */
    public boolean halted() {
        return this.program.state() == ILanguageProcess.State.HALTED;
    }

    /** What a program that halted said as it stopped. */
    public Text haltMessage() {
        return this.program.message();
    }

    /** What the program has written since this was last asked, in English, oldest first. */
    public List<String> output() {
        final long written = this.program.written();
        final List<String> kept = this.program.console();
        final long fresh = Math.min(written - this.handed, kept.size());
        this.handed = written;
        return fresh <= 0 ? List.of() : new ArrayList<>(kept.subList((int) (kept.size() - fresh), kept.size()));
    }

    /** Whether the program is waiting for a line to be typed. */
    public boolean waitingForInput() {
        return this.program.waitingForInput();
    }

    /** Hands the program a typed line. */
    public void offerInput(final String line) {
        this.program.offerInput(line);
    }

    /** How many ticks the program has been running. */
    public long elapsed(final long now) {
        return Math.max(0, now - this.startedAt);
    }

    /** How many bytes of its memory the program holds now. */
    public long memoryBytes() {
        return this.program.heldBytes();
    }

    /** Lets the program go, which ends it at once, with no farewell. */
    public void end() {
        if (this.programs.byId(this.id) != null) {
            this.programs.stop(this.id, false);
        }
    }

    /**
     * Starts the program at {@code path} on the machine a service runs programs for, with a debugger attached before it
     * runs a single instruction.
     */
    static Started start(final ProgramService service, final MachinePrograms programs, final String path,
                         final List<String> arguments, final long now) {
        if (programs == null) {
            return Started.refused(NO_PROGRAMS.text());
        }
        final ProgramLauncher.Launch launch = service.start(path, arguments, IProgramParent.NONE,
                ProgramPriority.MEDIUM);
        if (!launch.ok()) {
            return Started.refused(launch.said());
        }
        final ProgramEntry<IMachineRuntime> one = programs.byId(launch.id());
        if (one == null || !(one.process() instanceof SigmaProgram sigma)) {
            if (one != null) {
                programs.stop(launch.id(), false);
            }
            return Started.refused(NOT_SIGMA.with(launch.name()));
        }
        sigma.process().debug();
        return new Started(new DebugSession(programs, sigma, launch.id(), now), null);
    }

    /** A session that started, or why none did. */
    public record Started(DebugSession session, Text refusal) {

        /** Whether the program started under the debugger. */
        public boolean ok() {
            return this.session != null;
        }

        static Started refused(final Text why) {
            return new Started(null, why);
        }
    }
}
