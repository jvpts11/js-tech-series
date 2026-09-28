/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliComputer.FsResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShRunnerTest {

    @Test
    void expand_unixGetsThePathTheByNameSearchUses() {
        final Fake computer = new Fake();
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        assertEquals("/bin:/usr/bin", ShRunner.expand(computer, List.of("$PATH")).getFirst());
    }

    /** FreeBSD installs its programs where its own tree says, not under the bare guess UNIX answers $PATH with. */
    @Test
    void expand_freeBsdGetsNoPathFromThisBuiltIn() {
        final Fake computer = new Fake();
        computer.platform = Platform.FREEBSD;
        computer.shellFamily = ShellFamily.POSIX;
        assertEquals("", ShRunner.expand(computer, List.of("$PATH")).getFirst());
    }

    /**
     * A DOS machine's own PATH is read from its disk, so a line with nothing between per cent signs must never
     * touch it: "cls" or "ver" would otherwise seek a hard drive for a value nothing on the line ever uses.
     */
    @Test
    void expand_dosLineWithNoPercentSign_neverReadsTheDisk() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        ShRunner.expand(computer, List.of("ver"));
        assertEquals(0, computer.filesRead, "no token names %PATH%, so nothing reads a file");
    }

    /** A line that does name %PATH% expands it correctly all the same, at the cost of the one read that needs it. */
    @Test
    void expand_dosLineNamingPath_expandsItAndReadsTheDiskOnce() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        assertEquals("C:\\DOS", ShRunner.expand(computer, List.of("%PATH%")).getFirst());
        assertEquals(1, computer.filesRead, "the one token naming %PATH% reads AUTOEXEC.BAT once");
    }

    /** A computer with only what expanding a shell word reads. */
    private static final class Fake implements ICliComputer {

        private Platform platform = Platform.LINUX;
        private ShellFamily shellFamily = ShellFamily.POSIX;
        private int filesRead;

        @Override public Platform platform() {
            return this.platform;
        }

        @Override public ShellFamily shellFamily() {
            return this.shellFamily;
        }

        @Override public FsResult readFile(final String path) {
            this.filesRead++;
            return FsResult.noOs();
        }
    }
}
