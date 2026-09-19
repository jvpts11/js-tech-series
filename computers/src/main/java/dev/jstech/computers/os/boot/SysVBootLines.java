/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.install.Installers;

/**
 * What UNIX System V says on its way up and on its way down.
 *
 * <p>It is a quiet system. It signs itself, counts the memory in bytes the way it always did, checks its root
 * filesystem, and says it is ready; stopping, it warns whoever is on the console in capitals, changes run level
 * and ends on one short sentence. Every figure is this machine's: the memory is the memory that is in it, and
 * what it keeps for itself is taken off before it says what is left.
 */
final class SysVBootLines {

    /** What the kernel and its buffers hold back, which is why less is available than is there. */
    private static final long KEPT_BYTES = 524_288L;

    /** A megabyte as that system counted one when it printed memory. */
    private static final long BYTES_PER_MB = 1_024_000L;

    private SysVBootLines() {
    }

    /** The banner, the memory, the root filesystem checked, and the word that it is ready. */
    static BootSequence up(final IOsHost machine, final OsDef system) {
        final long total = machine.ramTotalMb() * BYTES_PER_MB;
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Booting the UNIX System...")
                .subtitle("");
        out.line(system.displayName() + " Release " + KernelNames.SYSTEM_V_RELEASE);
        out.line("Copyright (c) 1984, 1986, 1987 " + system.house().name());
        out.line("All Rights Reserved");
        out.line("Total real memory     = " + total);
        out.line("Available memory      = " + Math.max(0L, total - KEPT_BYTES));
        out.line("The system is coming up.  Please wait.");
        out.line("/dev/root: clean");
        out.line("The system is ready.");
        return out.build();
    }

    /**
     * The same machine stopping: the warning to the console, the run level, and the last sentence.
     *
     * @param restarting the machine is coming straight back up, which is run level 6 and not 0
     */
    static BootSequence down(final IOsHost machine, final boolean restarting) {
        final BootSequence.Builder out = new BootSequence.Builder().title("").subtitle("");
        out.line("Broadcast Message from player (console) on " + Installers.hostName(machine));
        out.line("THE SYSTEM IS BEING SHUT DOWN NOW ! ! !");
        out.line("Log off now or risk your files being damaged.");
        out.line("INIT: New run level: " + (restarting ? "6" : "0"));
        out.line("The system is coming down.  Please wait.");
        out.line("System services are now being stopped.");
        out.line(restarting ? "The system is being restarted." : "The system is down.");
        return out.build();
    }
}
