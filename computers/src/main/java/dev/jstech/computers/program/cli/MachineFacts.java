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
import java.util.List;

/**
 * The small answers a machine gives about itself, said once and worn by every family.
 *
 * <p>What is running, how to stop one of them, and where a command came from are one question each however
 * they are asked. Three shells ask them here under three sets of words, so the answers live apart from any
 * one shell's verbs: {@code ps} and {@code tasklist} and {@code tasklist} again on the appliance are three
 * faces over the list below, never three listings that drift apart.
 */
final class MachineFacts {

    /** How wide each column of a process listing is. */
    private static final int PID_W = 7;
    private static final int NAME_W = 14;
    private static final int FILE_W = 24;
    private static final int STATE_W = 10;
    private static final int MEM_W = 10;

    private MachineFacts() {
    }

    /** The processes a machine is running, with what each is holding. */
    static void printProcesses(final CliContext ctx, final ShellFamily family) {
        final List<ICliComputer.SigmaProcess> running = ctx.computer().sigmaProcesses();
        if (running.isEmpty()) {
            ctx.out().dim(switch (family) {
                case DOS -> "No tasks are running.";
                case POSIX -> "no processes";
                case NET -> "nothing is running";
            });
            return;
        }
        /*
         * The file as well as the name: a program that gave itself no name is listed under the runtime that
         * runs it, and then the file is the only thing telling two of them apart.
         */
        ctx.out().header(CliText.pad(family == ShellFamily.POSIX ? "  PID" : "PID", PID_W)
                + CliText.pad("NAME", NAME_W) + CliText.pad("FILE", FILE_W)
                + CliText.pad("STATE", STATE_W) + CliText.padLeft("MEM", MEM_W));
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
                case DOS -> "taskkill: /PID takes the number of a task";
                case POSIX -> "kill: not a process id";
                case NET -> "end: say the number of the thing to stop";
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
                case DOS -> "INFO: Could not find \"" + name + "\".";
                case POSIX -> name + " not found";
                case NET -> "this machine has no command called " + name;
            });
            return;
        }
        final String path = found.scope().fromAPackage() ? found.scope().packageId() : "the system";
        ctx.out().row(found.name(), path);
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
