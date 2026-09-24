/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * The small answers a machine gives about itself, said once and worn by every family.
 *
 * <p>What is running, how to stop one of them, and where a command came from are one question each however
 * they are asked. Three shells ask them here under three sets of words, so the answers live apart from any
 * one shell's verbs: {@code ps} and {@code tasklist} and {@code tasklist} again on the appliance are three
 * faces over the list below, never three listings that drift apart.
 */
@TextHolder
final class MachineFacts {

    /** How wide each column of a process listing is. */
    private static final int PID_W = 7;
    private static final int NAME_W = 14;
    private static final int FILE_W = 24;
    private static final int STATE_W = 10;
    private static final int MEM_W = 10;

    private static final TextKey NO_TASKS = TextKey.of("jsc.cli.facts.no_tasks", "No tasks are running.");
    private static final TextKey NO_PROCESSES = TextKey.of("jsc.cli.facts.no_processes", "no processes");
    private static final TextKey NOTHING_RUNNING = TextKey.of("jsc.cli.facts.nothing_running", "nothing is running");

    /* The heads of a process listing's columns. */
    private static final TextKey PID = TextKey.of("jsc.cli.facts.col.pid", "PID");
    private static final TextKey NAME = TextKey.of("jsc.cli.facts.col.name", "NAME");
    private static final TextKey FILE = TextKey.of("jsc.cli.facts.col.file", "FILE");
    private static final TextKey STATE = TextKey.of("jsc.cli.facts.col.state", "STATE");
    private static final TextKey MEM = TextKey.of("jsc.cli.facts.col.mem", "MEM");

    private static final TextKey BAD_TASK = TextKey.of("jsc.cli.facts.bad_task", "/PID takes the number of a task");
    private static final TextKey BAD_PID = TextKey.of("jsc.cli.facts.bad_pid", "not a process id");
    private static final TextKey BAD_THING =
            TextKey.of("jsc.cli.facts.bad_thing", "say the number of the thing to stop");

    private static final TextKey NOT_FOUND_DOS =
            TextKey.of("jsc.cli.facts.not_found_dos", "INFO: Could not find \"%s\".");
    private static final TextKey NOT_FOUND = TextKey.of("jsc.cli.facts.not_found", "%s not found");
    private static final TextKey NO_SUCH_COMMAND =
            TextKey.of("jsc.cli.facts.no_such_command", "this machine has no command called %s");
    private static final TextKey THE_SYSTEM = TextKey.of("jsc.cli.facts.the_system", "the system");

    private MachineFacts() {
    }

    /** The processes a machine is running, with what each is holding. */
    static void printProcesses(final CliContext ctx, final ShellFamily family) {
        final List<ICliComputer.SigmaProcess> running = ctx.computer().sigmaProcesses();
        if (running.isEmpty()) {
            ctx.out().dim(switch (family) {
                case DOS -> NO_TASKS;
                case POSIX -> NO_PROCESSES;
                case NET -> NOTHING_RUNNING;
            });
            return;
        }
        /*
         * The file as well as the name: a program that gave itself no name is listed under the runtime that
         * runs it, and then the file is the only thing telling two of them apart. The heads are laid out once they
         * are in the reader's language, so a longer word still leaves the next column where it was.
         */
        final String indent = family == ShellFamily.POSIX ? "  " : "";
        final int nameAt = PID_W;
        final int fileAt = nameAt + NAME_W;
        final int stateAt = fileAt + FILE_W;
        final int end = stateAt + STATE_W + MEM_W;
        ctx.out().line(CliLine.of(new CliSpan(Text.literal(indent), CliStyle.HEADER),
                new CliSpan(PID.text(), CliStyle.HEADER), CliSpan.pad(nameAt),
                new CliSpan(NAME.text(), CliStyle.HEADER), CliSpan.pad(fileAt),
                new CliSpan(FILE.text(), CliStyle.HEADER), CliSpan.pad(stateAt),
                new CliSpan(STATE.text(), CliStyle.HEADER),
                new CliSpan(Text.EMPTY, CliStyle.PLAIN, new CliSpan.Fill(end, true, true)),
                new CliSpan(MEM.text(), CliStyle.HEADER)));
        for (final ICliComputer.SigmaProcess one : running) {
            ctx.out().line(CliText.pad(String.valueOf(one.id()), PID_W)
                    + CliText.pad(one.name(), NAME_W) + CliText.pad(one.file(), FILE_W)
                    + CliText.pad(one.state(), STATE_W)
                    + CliText.padLeft(RamLedger.heldLabel(one.heldBytes()), MEM_W));
        }
    }

    /** Stops the process of that number, in whichever family's words. */
    static void stopProcess(final CliContext ctx, final String number, final ShellFamily family) {
        final int id = whole(number);
        if (id <= 0) {
            ctx.out().error(switch (family) {
                case DOS -> CliTexts.SAID_BY.with("taskkill", BAD_TASK);
                case POSIX -> CliTexts.SAID_BY.with("kill", BAD_PID);
                case NET -> CliTexts.SAID_BY.with("end", BAD_THING);
            });
            return;
        }
        final ICliComputer.OpResult result = ctx.computer().stopSigma(id);
        ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
    }

    /** Where a command lives, which for a machine like this is which package put it there. */
    static void whereIs(final CliContext ctx, final String name, final ShellFamily family) {
        final ICliCommand found = ctx.shell().find(name);
        if (found == null || !found.available(ctx.computer())) {
            ctx.out().error(switch (family) {
                case DOS -> NOT_FOUND_DOS.with(name);
                case POSIX -> NOT_FOUND.with(name);
                case NET -> NO_SUCH_COMMAND.with(name);
            });
            return;
        }
        final Text path = found.scope().fromAPackage()
                ? Text.literal(found.scope().packageId()) : THE_SYSTEM.text();
        ctx.out().row(Text.literal(found.name()), path);
    }

    /**
     * A number typed at a prompt, or {@code -1} when it was not one.
     *
     * <p>The per cent sign goes because one family writes a job as {@code %1} and means the number after it.
     */
    static int whole(final String text) {
        try {
            return Integer.parseInt(text.trim().replace("%", ""));
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }
}
