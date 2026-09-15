/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The one place a program is started from a file, whoever asks: the prompt, a program starting another, a program on
 * another machine, a ComputerCraft computer through a Gateway, or the desktop.
 *
 * <p>Every one of them goes the same way. Something installed has to run a file of that kind, the file has to be read,
 * the memory asked for is kept between the default and the most a program may have, and it has to fit in what the
 * machine has free. What comes back is the program's number or the reason it did not start, and each caller says it
 * in its own words.
 */
public final class ProgramLauncher {

    /** Why a program did not start. */
    public enum Refusal {
        /** Nothing installed runs a file of that kind; the file was not read. */
        NO_RUNNER,
        /** The file could not be read; the reader said why. */
        UNREADABLE,
        /** The memory it needs does not fit in what the machine has free. */
        NO_MEMORY,
        /** The language would not start what the file holds; it said why. */
        NOT_STARTED
    }

    /**
     * What came of a start: the number the program runs under and the name it is listed by, or why it did not start,
     * with what that needs to be told (the reader's or the language's message, the memory asked for and the memory
     * free).
     */
    public record Launch(int id, String name, Refusal refusal, String message, int roomMb, int freeMb) {

        /** Whether the program is running. */
        public boolean ok() {
            return this.refusal == null;
        }
    }

    private ProgramLauncher() {
    }

    /**
     * Starts the program in the file at {@code path} on {@code machine}.
     *
     * @param reader how the caller reads a file: from the machine's own disks, or from another machine's
     * @param heapMb the memory asked for, in megabytes; zero or less takes the default
     */
    public static Launch launch(final AbstractComputerBlockEntity machine, final String path,
                                final Function<String, ICliComputer.FsResult> reader, final List<String> args,
                                final IProgramParent parent, final ProgramPriority priority, final int heapMb) {
        final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        final String name = slash < 0 ? path : path.substring(slash + 1);
        final int dot = path.lastIndexOf('.');
        final String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!MachinePrograms.runs(extension)) {
            return new Launch(0, name, Refusal.NO_RUNNER, name + ": nothing installed runs a ." + extension, 0, 0);
        }
        final ICliComputer.FsResult file = reader.apply(path);
        if (!file.ok()) {
            return new Launch(0, name, Refusal.UNREADABLE, file.message(), 0, 0);
        }
        final int room = heapMb <= 0 ? MachinePrograms.DEFAULT_HEAP_MB : Math.min(heapMb, MachinePrograms.MAX_HEAP_MB);
        final RamLedger ledger = machine.ramLedger();
        if (!ledger.fits(room)) {
            return new Launch(0, name, Refusal.NO_MEMORY, "", room, ledger.freeMb());
        }
        final MachinePrograms.Started started =
                machine.programs().start(name, file.message(), room, machine, args, parent, priority);
        if (!started.ok()) {
            return new Launch(0, name, Refusal.NOT_STARTED, started.message(), room, 0);
        }
        machine.setChanged();
        return new Launch(started.id(), name, null, started.message(), room, 0);
    }
}
