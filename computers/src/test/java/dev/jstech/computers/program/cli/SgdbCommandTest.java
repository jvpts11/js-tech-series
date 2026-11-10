/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SgdbCommandTest {

    private CliShell shell;
    private Fake computer;

    private static final String PLANT = String.join("\n",
            "using System.IO.*;",
            "namespace Plant;",
            "class Plant {",
            "    static int Twice(int n) {",
            "        return n * 2;",
            "    }",
            "",
            "    static void Main() {",
            "        long logs = 41;",
            "        Console.PrintLine(\"\" + Twice(21));",
            "    }",
            "}");

    @BeforeEach
    void setUp() {
        this.shell = new CliShell(BuiltinCommands.all(), 60);
        this.computer = new Fake();
        this.computer.files.put("Plant.sgs", PLANT);
        this.computer.installed.add(new ICliComputer.ProgramInfo("sgsc", SigmaCommands.COMPILER));
        this.shell.run("sgsc --debug Plant.sgs", this.computer);
    }

    @Test
    void sgdb_isNoCommandUntilTheCompilerIsInstalled() {
        this.computer.installed.clear();
        assertTrue(this.said("sgdb Plant.asm").contains("command not found"));
    }

    @Test
    void sgdb_holdsTheTerminalAtItsOwnPrompt() {
        final CliShell.Response opened = this.shell.run("sgdb Plant.asm", this.computer);
        assertTrue(text(opened.lines()).contains("sgdb 3, the Σ debugger."), text(opened.lines()));
        final ITtyProcess sgdb = opened.started();
        assertNotNull(sgdb);
        assertEquals("(sgdb) ", sgdb.asking().text().text());
    }

    @Test
    void break_landsOnTheNextLineWithCodeAndIsListedAndDeleted() {
        final ITtyProcess sgdb = this.sgdb();
        assertEquals("Breakpoint 1 at Plant.sgs, line 9.\n", answer(sgdb, "break 7"));
        assertEquals("Breakpoint 2 at Plant.sgs, line 5.\n", answer(sgdb, "b Plant.sgs:5"));
        assertEquals("No line 99 in file \"Plant.sgs\".\n", answer(sgdb, "break 99"));
        assertEquals("Num  Where\n1    Plant.sgs:9\n2    Plant.sgs:5\n", answer(sgdb, "info breakpoints"));
        assertEquals("", answer(sgdb, "delete 1"));
        assertEquals("No breakpoint number 1.\n", answer(sgdb, "delete 1"));
        assertEquals("", answer(sgdb, "delete"));
        assertEquals("No breakpoints.\n", answer(sgdb, "info breakpoints"));
    }

    @Test
    void commands_beforeTheProgramRunsSayItIsNotRunning() {
        final ITtyProcess sgdb = this.sgdb();
        assertEquals("The program is not being run.\n", answer(sgdb, "continue"));
        assertEquals("The program is not being run.\n", answer(sgdb, "step"));
        assertEquals("No stack.\n", answer(sgdb, "print logs"));
        assertEquals("No stack.\n", answer(sgdb, "backtrace"));
        assertEquals("Undefined command: \"jump\". Try \"help\".\n", answer(sgdb, "jump"));
        assertTrue(answer(sgdb, "help").startsWith("List of commands:\nbreak [file:]line"));
    }

    @Test
    void run_onAComputerWithNoRuntimeSaysSo() {
        assertEquals("not installed\n", answer(this.sgdb(), "run"));
    }

    @Test
    void quit_andCtrlCAtThePromptLetTheTerminalGo() {
        final ITtyProcess quit = this.sgdb();
        answer(quit, "quit");
        assertTrue(quit.over());
        assertNull(quit.asking());
        final ITtyProcess interrupted = this.sgdb();
        interrupted.interrupt(0, new Lines());
        assertTrue(interrupted.over());
    }

    @Test
    void sgdb_onAListingWithoutLinesSaysHowToBuildOne() {
        this.shell.run("sgsc Plant.sgs", this.computer);
        assertTrue(this.said("sgdb Plant.asm").contains("Plant.asm has no lines for the debugger: build it with sgsc"
                + " --debug"));
    }

    @Test
    void sgdb_onAFileThatIsNoListingRefusesIt() {
        assertTrue(this.said("sgdb Plant.sgs").contains("Plant.sgs is not a listing a machine runs"));
        assertNull(this.shell.run("sgdb Plant.sgs", this.computer).started());
    }

    private ITtyProcess sgdb() {
        return this.shell.run("sgdb Plant.asm", this.computer).started();
    }

    private String said(final String line) {
        return text(this.shell.run(line, this.computer).lines());
    }

    private static String answer(final ITtyProcess tool, final String line) {
        final Lines out = new Lines();
        tool.answer(line, 0, out);
        return text(out.lines);
    }

    private static String text(final List<CliLine> lines) {
        final StringBuilder text = new StringBuilder();
        for (final CliLine line : lines) {
            text.append(line.text()).append('\n');
        }
        return text.toString();
    }

    /** What a tool printed, kept in order. */
    private static final class Lines implements ITtySink {

        private final List<CliLine> lines = new ArrayList<>();

        @Override
        public void line(final CliLine line) {
            this.lines.add(line);
        }

        @Override
        public void redraw(final CliLine line) {
            this.lines.add(line);
        }
    }

    /** A computer with files and installed programs, and no runtime to run anything on. */
    private static final class Fake implements ICliComputer {

        private final Map<String, String> files = new LinkedHashMap<>();
        private final List<ProgramInfo> installed = new ArrayList<>();

        @Override
        public String name() {
            return "TEST-PC";
        }

        @Override
        public Platform platform() {
            return Platform.LINUX;
        }

        @Override
        public Era era() {
            return HardwareEra.STANDARD;
        }

        @Override
        public List<ProgramInfo> programs() {
            return List.copyOf(this.installed);
        }

        @Override
        public FsResult readFile(final String path) {
            final String held = this.files.get(path);
            return held == null ? FsResult.fail("no such file: " + path) : FsResult.content(held);
        }

        @Override
        public FsResult writeFile(final String path, final String content) {
            this.files.put(path, content);
            return FsResult.ok("wrote " + path);
        }
    }
}
