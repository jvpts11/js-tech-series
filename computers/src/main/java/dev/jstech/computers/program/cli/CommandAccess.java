/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.CommandScope.Need;

/**
 * Whether a machine has a command at all, asked of that machine against what the command declares.
 *
 * <p>One answer serves every question a player can ask: the list of commands, the manual, what a half-typed name
 * completes to, and whether a word typed at the prompt is found. A machine therefore never names something it cannot
 * do, and never refuses something it has just listed.
 *
 * <p>The asking is as free of the game as what is asked: a machine is only ever read through the words it answers
 * with, so a computer written for a test is enough to ask this of.
 */
public final class CommandAccess {

    private CommandAccess() {
    }

    /** Whether {@code computer} has a command of that scope. */
    public static boolean allows(final CommandScope scope, final ICliComputer computer) {
        if (!scope.onSystem(computer.platform(), computer.osEdition())) {
            return false;
        }
        if (!computer.hostIs(scope.hostScope())) {
            return false;
        }
        if (scope.manager() != null && computer.packageManager() != scope.manager()) {
            return false;
        }
        if (scope.fromAPackage() && !installed(computer, scope.packageId())) {
            return false;
        }
        return hasWhatItNeeds(scope, computer);
    }

    /** Whether the machine has everything the command needs of it. */
    private static boolean hasWhatItNeeds(final CommandScope scope, final ICliComputer computer) {
        if (scope.needs(Need.FILES) && !computer.hasFiles()) {
            return false;
        }
        if (scope.needs(Need.NETWORK) && !computer.onNetwork()) {
            return false;
        }
        return !scope.needs(Need.PORTS) || computer.hasPorts();
    }

    /** Whether that package is on the machine, by the id the machine lists its programs under. */
    private static boolean installed(final ICliComputer computer, final String packageId) {
        for (final ICliComputer.ProgramInfo program : computer.programs()) {
            if (packageId.equalsIgnoreCase(program.id())) {
                return true;
            }
        }
        return false;
    }
}
