/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.fs.McDosTree;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ByNameProgramTest {

    @Test
    void defaultPath_mcDosReadsThePathLineOfTheMachinesOwnAutoexec() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        computer.autoexec(List.of("SCC", "VIM"));
        assertEquals("C:\\DOS;C:\\SCC;C:\\VIM", ByNameProgram.defaultPath(computer));
    }

    @Test
    void defaultPath_mcDosWithNothingInstalled_isJustTheToolsDirectory() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        computer.autoexec(List.of());
        assertEquals("C:\\DOS", ByNameProgram.defaultPath(computer));
    }

    /**
     * A computer kept for another test, with no {@code AUTOEXEC.BAT} of its own, answers with the tools
     * directory all the same.
     */
    @Test
    void defaultPath_mcDosWithNoAutoexecOfItsOwn_fallsBackToTheToolsDirectory() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        assertEquals("C:\\DOS", ByNameProgram.defaultPath(computer));
    }

    @Test
    void defaultPath_posixListsTheTreesOwnBinDirectories() {
        final Fake computer = new Fake();
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        assertEquals("/bin:/usr/bin", ByNameProgram.defaultPath(computer));
    }

    @Test
    void pathDirectories_splitsTheDefaultOnTheFamilysOwnSeparator() {
        final Fake dos = new Fake();
        dos.platform = Platform.MC_DOS;
        dos.shellFamily = ShellFamily.DOS;
        assertEquals(List.of("C:\\DOS"), ByNameProgram.pathDirectories(dos));

        final Fake unix = new Fake();
        unix.platform = Platform.UNIX;
        unix.shellFamily = ShellFamily.POSIX;
        assertEquals(List.of("/bin", "/usr/bin"), ByNameProgram.pathDirectories(unix));
    }

    @Test
    void pathDirectories_whatThePlayerSetWinsOverTheDefault() {
        final Fake computer = new Fake();
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.variables.put("PATH", "/opt/tools:/home/player/bin");
        assertEquals(List.of("/opt/tools", "/home/player/bin"), ByNameProgram.pathDirectories(computer));
    }

    /** MC-DOS finds a listing by name however it was typed, since every command at that prompt is case-blind. */
    @Test
    void tryRun_mcDosFindsAListingInTheCurrentDirectoryRegardlessOfCase() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        computer.files.put("hello.asm", ".asm 1\n");
        ByNameProgram.tryRun("HELLO", List.of(), computer, new CliOutput(52));
        assertEquals("hello.asm", computer.startedPath);
    }

    /** The same is true of a listing found on the PATH rather than beside the prompt. */
    @Test
    void tryRun_mcDosFindsAListingOnThePathRegardlessOfCase() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        computer.variables.put("PATH", "C:\\TOOLS");
        computer.files.put("C:\\TOOLS\\hello.asm", ".asm 1\n");
        ByNameProgram.tryRun("HELLO", List.of(), computer, new CliOutput(52));
        assertEquals("C:\\TOOLS\\hello.asm", computer.startedPath);
    }

    /** With a listing of the same name in both places, the one beside the prompt is the one that runs. */
    @Test
    void tryRun_mcDosPrefersTheCurrentDirectoryOverThePath() {
        final Fake computer = new Fake();
        computer.platform = Platform.MC_DOS;
        computer.shellFamily = ShellFamily.DOS;
        computer.variables.put("PATH", "C:\\TOOLS");
        computer.files.put("hello.asm", "current\n");
        computer.files.put("C:\\TOOLS\\hello.asm", "path\n");
        ByNameProgram.tryRun("hello", List.of(), computer, new CliOutput(52));
        assertEquals("hello.asm", computer.startedPath);
    }

    /** A computer with only what the PATH default and the search need to read. */
    private static final class Fake implements ICliComputer {

        private final List<ProgramInfo> installed = new ArrayList<>();
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final Map<String, String> files = new LinkedHashMap<>();
        private String autoexec;
        private String startedPath;
        private Platform platform = Platform.LINUX;
        private ShellFamily shellFamily = ShellFamily.POSIX;

        @Override public Platform platform() {
            return this.platform;
        }

        @Override public ShellFamily shellFamily() {
            return this.shellFamily;
        }

        @Override public List<ProgramInfo> programs() {
            return List.copyOf(this.installed);
        }

        @Override public Map<String, String> shellVariables() {
            return Map.copyOf(this.variables);
        }

        @Override public boolean hasFiles() {
            return true;
        }

        @Override public FsResult readFile(final String path) {
            if (this.autoexec != null && path.equalsIgnoreCase("C:\\" + McDosTree.AUTOEXEC)) {
                return FsResult.content(this.autoexec);
            }
            final String held = this.files.get(path);
            return held == null ? FsResult.fail("no such file: " + path) : FsResult.content(held);
        }

        @Override public List<String> fileNames() {
            return this.namesIn("");
        }

        @Override public FsResult listDisk(final String dir) {
            final List<FsEntry> entries = new ArrayList<>();
            for (final String name : this.namesIn(dir)) {
                entries.add(new FsEntry(name, "", 0L, false, false, 0L));
            }
            return FsResult.listing(entries);
        }

        @Override public OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
            this.startedPath = path;
            return OpResult.ok("started " + path);
        }

        /** Sets this machine's own AUTOEXEC.BAT, as MC-DOS writes it for these installed programs' directories. */
        void autoexec(final List<String> programs) {
            this.autoexec = McDosTree.text(new McDosTree.Facts("MC-DOS", "", "MCDOS.SYS", programs),
                    McDosTree.AUTOEXEC).orElseThrow();
        }

        /** The bare names directly inside {@code dir} ({@code ""} for the current directory), from {@link #files}. */
        private List<String> namesIn(final String dir) {
            final String prefix = dir == null || dir.isEmpty() ? "" : dir + "\\";
            final List<String> names = new ArrayList<>();
            for (final String path : this.files.keySet()) {
                if (path.regionMatches(true, 0, prefix, 0, prefix.length())
                        && path.indexOf('\\', prefix.length()) < 0) {
                    names.add(path.substring(prefix.length()));
                }
            }
            return names;
        }
    }
}
