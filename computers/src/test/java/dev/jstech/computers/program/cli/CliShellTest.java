/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.core.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliShellTest {

    private CliShell shell;
    private FakeComputer computer;

    @BeforeEach
    void setUp() {
        shell = new CliShell(BuiltinCommands.all(), 52);
        computer = new FakeComputer();
    }

    private String joined(final String line) {
        final StringBuilder sb = new StringBuilder();
        for (final CliLine cliLine : shell.run(line, computer).lines()) {
            sb.append(cliLine.text()).append('\n');
        }
        return sb.toString();
    }

    private boolean anyStyle(final String line, final CliStyle style) {
        return shell.run(line, computer).lines().stream().anyMatch(l -> l.style() == style);
    }

    @Test
    void run_emptyLineProducesNoOutput() {
        assertTrue(shell.run("   ", computer).lines().isEmpty());
    }

    @Test
    void run_unknownCommandReportsError() {
        final CliShell.Response response = shell.run("frobnicate now", computer);
        assertTrue(response.lines().stream().anyMatch(l -> l.style() == CliStyle.ERROR));
        assertTrue(response.lines().get(0).text().contains("frobnicate"));
    }

    /** MC-DOS runs a listing by its own name, no extension, found in the current directory. */
    @Test
    void run_dosRunsAListingByNameFromTheCurrentDirectory() {
        computer.hasFiles = true;
        computer.files.put("hello.asm", ".asm 1\n");
        shell.run("hello", computer);
        assertEquals("hello.asm", computer.startedListing);
    }

    /** With nothing in the current directory, MC-DOS falls back to the PATH, and the extension is never typed. */
    @Test
    void run_dosFallsBackToThePathWhenNothingIsInTheCurrentDirectory() {
        computer.hasFiles = true;
        computer.variables.put("PATH", "C:\\TOOLS");
        computer.files.put("C:\\TOOLS\\hello.asm", ".asm 1\n");
        shell.run("hello", computer);
        assertEquals("C:\\TOOLS\\hello.asm", computer.startedListing);
    }

    /** A word neither a command nor a listing anywhere answers to is an unknown command on MC-DOS. */
    @Test
    void run_dosUnresolvedNameKeepsTheGenericNotFound() {
        computer.hasFiles = true;
        final String said = joined("nosuchthing");
        assertTrue(said.contains("command not found: nosuchthing"), said);
    }

    /** UNIX runs a listing named with a slash exactly where it is written, current directory included. */
    @Test
    void run_unixRunsAnExplicitPathWithoutSearchingThePath() {
        computer.hasFiles = true;
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.files.put("./hello.asm", ".asm 1\n");
        shell.run("./hello", computer);
        assertEquals("./hello.asm", computer.startedListing);
    }

    /** A bare name on UNIX is never found beside the prompt: only a PATH directory answers to it. */
    @Test
    void run_unixBareNameIsNeverFoundInTheCurrentDirectory() {
        computer.hasFiles = true;
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.files.put("hello.asm", ".asm 1\n");
        final String said = joined("hello");
        assertTrue(said.contains("hello: not found"), said);
        assertFalse(said.contains("command not found"), said);
    }

    /** A bare name on UNIX is found once its directory is on the PATH. */
    @Test
    void run_unixBareNameIsFoundOnThePath() {
        computer.hasFiles = true;
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.variables.put("PATH", "/tools");
        computer.files.put("/tools/hello.asm", ".asm 1\n");
        shell.run("hello", computer);
        assertEquals("/tools/hello.asm", computer.startedListing);
    }

    /** A name this system has no command for runs by name when a listing answers to it, even if a package has one. */
    @Test
    void run_dosRunsAListingNamedLikeARegisteredButUnavailableCommand() {
        computer.hasFiles = true;
        // Nothing here installed the scc package, so the registered command is not available on this machine.
        assertFalse(shell.find("scc").available(computer), "scc must be unavailable here");
        computer.files.put("scc.asm", ".asm 1\n");
        shell.run("scc", computer);
        assertEquals("scc.asm", computer.startedListing);
    }

    /** A machine with no processor of its own to run a program on never blames the runtime for it. */
    @Test
    void run_byNameDeclinesWithoutSigmaWordingWhenTheHostCannotRunPrograms() {
        computer.hasFiles = true;
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.canRunPrograms = false;
        computer.files.put("./hello.asm", ".asm 1\n");
        final String said = joined("./hello");
        assertTrue(said.contains("hello: not found"), said);
        assertFalse(said.contains("sigma:"), said);
    }

    /** The by-name not-found line reads plain, not the shell's usual red error line. */
    @Test
    void run_unixByNameNotFoundPrintsInPlainStyleNotError() {
        computer.hasFiles = true;
        computer.platform = Platform.UNIX;
        computer.shellFamily = ShellFamily.POSIX;
        computer.files.put("hello.asm", ".asm 1\n");
        assertFalse(anyStyle("hello", CliStyle.ERROR), "hello: not found must not be the shell's error style");
    }

    @Test
    void run_helpListsEveryRegisteredCommand() {
        final String out = joined("help");
        assertTrue(out.contains("iql"));
        assertTrue(out.contains("interac"));
        assertTrue(out.contains("net"));
    }

    @Test
    void run_helpForOneCommandShowsItsUsage() {
        assertTrue(joined("help operation").contains("<statement>"));
    }

    @Test
    void run_aliasResolvesToTheSameCommand() {
        // 'op' is an alias of operation; with no statement both print the same usage error.
        assertEquals(joined("operation"), joined("op"));
    }

    @Test
    void run_echoPrintsTheRest() {
        assertEquals("hello world\n", joined("echo hello world"));
    }

    @Test
    void run_clsSetsTheClearFlag() {
        assertTrue(shell.run("cls", computer).clearScreen());
        assertFalse(shell.run("echo x", computer).clearScreen());
    }

    @Test
    void run_bareDriveQualifierIsHandledAsADriveSwitch() {
        // "D:" must be routed to the drive switch, not reported as an unknown command.
        final CliShell.Response response = shell.run("D:", computer);
        assertFalse(response.lines().stream().anyMatch(l -> l.text().contains("command not found")),
                "a bare drive qualifier must not be treated as an unknown command");
    }

    @Test
    void run_unixAliasesAreNotRegistered() {
        // The shell uses DOS syntax; the old Unix aliases must not resolve to any command.
        for (final String unix : List.of("ls", "cat", "rm", "clear", "man", "ps", "locate")) {
            assertTrue(shell.find(unix) == null, unix + " must not be a registered command or alias");
        }
        // The DOS verbs must be present.
        for (final String dos : List.of("dir", "cd", "cls", "type", "del", "mkdir", "md",
                "rmdir", "rd", "copy", "move", "ren")) {
            assertFalse(shell.find(dos) == null, dos + " must be a registered command");
        }
    }

    @Test
    void run_statusReflectsTheComputerState() {
        computer.running = true;
        assertTrue(anyStyle("status", CliStyle.OK));
        computer.running = false;
        assertTrue(anyStyle("status", CliStyle.ERROR));
    }

    @Test
    void run_operationOffNetworkIsNotOfferedAtAll() {
        computer.onNetwork = false;
        assertFalse(shell.find("operation").available(computer),
                "a machine with no network under it has nothing to ask the network for");
        assertTrue(anyStyle("operation query items", CliStyle.ERROR));
    }

    @Test
    void run_operationParsesAndExecutesEffectingVerb() {
        computer.onNetwork = true;
        joined("operation select 64 cobblestone");
        assertEquals("execute(SELECT,cobblestone,64)", computer.lastCall);
    }

    @Test
    void run_operationQueryReadsStorageInsteadOfExecuting() {
        computer.onNetwork = true;
        computer.stock.add(new ICliComputer.StoredItem("cobblestone", 100));
        final String out = joined("operation query items");
        assertTrue(out.contains("cobblestone"));
    }

    @Test
    void run_operationReportsSyntaxErrors() {
        assertTrue(anyStyle("operation frobnicate 1 thing", CliStyle.ERROR));
    }

    @Test
    void run_installDelegatesToTheComputer() {
        joined("install nms");
        assertEquals("install(nms)", computer.lastCall);
    }

    @Test
    void run_maintenanceDelegatesToTheComputer() {
        joined("vacuum");
        assertEquals("maintenance(VACUUM)", computer.lastCall);
    }

    @Test
    void run_commandNeverThrowsEvenIfTheComputerDoes() {
        computer.explode = true;
        final CliShell.Response response = shell.run("status", computer);
        assertTrue(response.lines().stream().anyMatch(l -> l.style() == CliStyle.ERROR));
    }

    /**
     * An in-memory computer holding only the parts these commands reach: what it is, the network it reads, the work it
     * is asked for and the programs it has. It records the last effecting call so tests can assert on it.
     *
     * <p>A Mainframe running MC-NET, because that is the machine every command under test is offered on: the DOS
     * verbs belong to that family of systems, and the upkeep of the storage index belongs to the machine that keeps it.
     */
    private static final class FakeComputer implements ICliComputer {
        boolean running;
        boolean onNetwork = true;
        boolean explode;
        String lastCall = "";
        final List<StoredItem> stock = new ArrayList<>();
        /*
         * The shell these tests drive is the DOS command set by default, so the machine under it says it is a
         * system that speaks DOS. A test of the by-name run on another family flips both together.
         */
        Platform platform = Platform.MC_DOS;
        ShellFamily shellFamily = ShellFamily.DOS;
        boolean hasFiles;
        boolean canRunPrograms = true;
        final Map<String, String> files = new LinkedHashMap<>();
        final Map<String, String> variables = new LinkedHashMap<>();
        String startedListing;

        @Override public String name() {
            return "TEST-MF";
        }

        @Override public String type() {
            return "Mainframe";
        }

        @Override public Platform platform() {
            return this.platform;
        }

        @Override public ShellFamily shellFamily() {
            return this.shellFamily;
        }

        @Override public boolean hasFiles() {
            return this.hasFiles;
        }

        @Override public boolean canRunPrograms() {
            return this.canRunPrograms;
        }

        @Override public Map<String, String> shellVariables() {
            return Map.copyOf(this.variables);
        }

        @Override public FsResult readFile(final String path) {
            final String held = this.files.get(path);
            return held == null ? FsResult.fail("no such file: " + path) : FsResult.content(held);
        }

        @Override public FsResult writeFile(final String path, final String content) {
            this.files.put(path, content);
            return FsResult.ok("wrote " + path);
        }

        @Override public OpResult startSigma(final String path, final int heapMb) {
            this.startedListing = path;
            return OpResult.ok("started " + path);
        }

        @Override public boolean hostIs(final HostScope scope) {
            return scope == HostScope.ANY || scope == HostScope.MAINFRAME;
        }

        @Override public String nodeId() {
            return "abc123";
        }

        @Override public boolean running() {
            if (explode) {
                throw new IllegalStateException("boom");
            }
            return running;
        }

        @Override public long cpuCapacity() {
            return 1000;
        }

        @Override public long ramBuffer() {
            return 8192;
        }

        @Override public boolean onNetwork() {
            return onNetwork;
        }

        @Override public String networkId() {
            return "net7f3a";
        }

        @Override public NetSummary network() {
            return new NetSummary(onNetwork, 2, 1, 0, stock.size(), true);
        }

        @Override public List<StoredItem> query(
                final dev.jstech.computers.program.iql.IIqlCondition where,
                final String server, final int limit) {
            final String filter = dev.jstech.computers.program.iql.IIqlCondition
                    .itemNameFilter(where).toLowerCase();
            final List<StoredItem> out = new ArrayList<>();
            for (final StoredItem item : stock) {
                if (filter.isEmpty() || item.name().english().toLowerCase().contains(filter)) {
                    out.add(item);
                }
            }
            return out;
        }

        @Override public List<StoredItem> queryObject(final String object,
                final dev.jstech.computers.program.iql.IIqlCondition where,
                final String server, final int limit) {
            return object.equalsIgnoreCase("items") ? query(where, server, limit) : List.of();
        }

        @Override public OpResult maintenance(final IqlVerb action) {
            lastCall = "maintenance(" + action + ")";
            return OpResult.ok("done");
        }

        @Override public List<ProgramInfo> programs() {
            return List.of(new ProgramInfo("cmd", "jsc:command_prompt"));
        }

        @Override public OpResult install(final String programId) {
            lastCall = "install(" + programId + ")";
            return OpResult.ok("installed " + programId);
        }

        @Override public OpResult execute(
                final dev.jstech.computers.program.iql.IqlOperation operation) {
            lastCall = "execute(" + operation.verb() + "," + operation.item() + "," + operation.quantity() + ")";
            return OpResult.ok("queued " + operation.verb());
        }

        /** Answers a whole statement as an engine would: a read from the stock, an action through execute. */
        @Override public StatementResult runStatement(final String statement, final int rowLimit) {
            final IqlParseResult parsed = IqlParser.tryParse(statement);
            final IqlOperation op = parsed.operation();
            if (op.verb() == IqlVerb.QUERY || op.verb() == IqlVerb.COUNT) {
                final List<StoredItem> rows = queryObject(op.item(), op.where(), "", rowLimit);
                return new StatementResult(true, Text.literal(rows.size() + " rows"), rows);
            }
            final OpResult result = execute(op);
            return new StatementResult(result.ok(), result.message(), List.of());
        }
    }
}
