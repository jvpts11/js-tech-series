/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SigmaCommandsTest {

    private static final String SCRIPT = """
            using System.*;
            using System.IO.*;
            namespace Tests;
            class Monitor : IScript {
                public void OnInit() { }
                public void OnTick() { Console.PrintLine("hello"); }
                public void OnDestroy() { }
            }
            """;

    /** The same program in the smaller language: one library, and the class a script stands on. */
    private static final String SUBSET = """
            using Standard.*;
            namespace Tests;
            class Monitor : Script {
                public override void OnTick() { Console.PrintLine("hello"); }
            }
            """;

    private CliShell shell;
    private Fake computer;

    @BeforeEach
    void setUp() {
        this.shell = new CliShell(BuiltinCommands.all(), 60);
        this.computer = new Fake();
    }

    private String run(final String line) {
        final StringBuilder text = new StringBuilder();
        for (final CliLine written : this.shell.run(line, this.computer).lines()) {
            text.append(written.text()).append('\n');
        }
        return text.toString();
    }

    private boolean errored(final String line) {
        return this.shell.run(line, this.computer).lines().stream()
                .anyMatch(written -> written.style() == CliStyle.ERROR);
    }

    @Test
    void run_hasNoSigmaVerbsUntilThePackagesAreInstalled() {
        this.computer.files.put("Monitor.sgs", SCRIPT);
        assertTrue(this.run("sgsc Monitor.sgs").contains("command not found"));
        assertTrue(this.run("sigma ps").contains("command not found"));
    }

    /** The smaller language's compiler is its own package, and brings its own verb with it. */
    @Test
    void scc_isNotThereUntilItsOwnPackageIs() {
        this.computer.files.put("Watch.sg", SUBSET);
        assertTrue(this.run("scc Watch.sg").contains("command not found"));
        this.computer.add(SigmaCommands.SUBSET_COMPILER);
        assertTrue(this.run("scc Watch.sg").contains("wrote Watch.asm"));
    }

    /** One compiler, two languages: what comes out is the listing, whichever verb was typed. */
    @Test
    void scc_writesTheSameListingSgscWouldForTheSameProgram() {
        this.computer.add(SigmaCommands.SUBSET_COMPILER);
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Watch.sg", SUBSET);
        this.computer.files.put("Watch.sgs", SUBSET);
        this.run("scc Watch.sg -o small.asm");
        this.run("sgsc Watch.sgs -o big.asm");
        assertEquals(this.computer.files.get("big.asm"), this.computer.files.get("small.asm"));
    }

    @Test
    void scc_refusesWhatIsOutsideTheSmallerLanguage() {
        this.computer.add(SigmaCommands.SUBSET_COMPILER);
        this.computer.files.put("Watch.sg", """
                using Standard.*;
                namespace Tests;
                class Watch : Script {
                    public override void OnTick() { int[] n = new int[2]; foreach (int x in n) { } }
                }
                """);
        assertTrue(this.errored("scc Watch.sg"));
        assertFalse(this.computer.files.containsKey("Watch.asm"));
    }

    /** The oldest machines run the smaller language only, so the bigger one cannot be built for them. */
    @Test
    void sgsc_willNotBuildForTheArchitectureThatRunsSigmaOnly() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        assertTrue(this.run("sgsc Monitor.sgs --arch x86-16").contains("scc"));
        assertFalse(this.computer.files.containsKey("Monitor.asm"));
    }

    @Test
    void scc_buildsForTheArchitectureThatRunsSigmaOnly() {
        this.computer.add(SigmaCommands.SUBSET_COMPILER);
        this.computer.files.put("Watch.sg", SUBSET);
        assertTrue(this.run("scc Watch.sg --arch x86-16").contains("wrote Watch.asm"));
    }

    @Test
    void sgpack_isNotThereUntilTheRuntimeIs() {
        assertTrue(this.run("sgpack init").contains("command not found"));
    }

    @Test
    void sgpackInit_writesAManifestThatIsAlreadyValid() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sgpack init stockwatch").contains("wrote package.cpk"));
        final dev.jstech.computers.sigma.pack.Manifest made =
                dev.jstech.computers.sigma.pack.Manifest.read(this.computer.files.get("package.cpk"));
        assertEquals("stockwatch", made.name());
        assertEquals("stockwatch.asm", made.entry());
        assertTrue(made.problems().isEmpty(), () -> made.problems().toString());
    }

    @Test
    void sgpackInit_refusesToPaveOverOneThatIsAlreadyThere() {
        this.computer.add(SigmaCommands.RUNTIME);
        this.computer.files.put("package.cpk", "name: mine\n");
        assertTrue(this.errored("sgpack init other"));
        // Whatever the player had written in it is still there.
        assertEquals("name: mine\n", this.computer.files.get("package.cpk"));
    }

    @Test
    void sgpackBuild_putsEveryNamedFileIntoOnePieceOfReadableText() {
        this.computer.add(SigmaCommands.RUNTIME);
        this.computer.files.put("package.cpk", """
                name: stockwatch
                version: 1.0.0
                house: jvpts11
                entry: stockwatch.asm
                icon: bell
                ram: 1
                file: stockwatch.asm
                file: readme.txt
                """);
        this.computer.files.put("stockwatch.asm", ".asm 1\n.start Watcher script\n");
        this.computer.files.put("readme.txt", "Watches the iron.\n");
        assertTrue(this.run("sgpack build").contains("built stockwatch-1.0.0.cpk"));
        final String packed = this.computer.files.get("stockwatch-1.0.0.cpk");
        assertTrue(packed.startsWith(".pkg 1\n"), packed);
        assertTrue(packed.contains(".start Watcher script"), packed);
        assertTrue(packed.contains("Watches the iron."), packed);
    }

    @Test
    void sgpackBuild_saysWhichFileIsMissingRatherThanBuildingHalfAPackage() {
        this.computer.add(SigmaCommands.RUNTIME);
        this.computer.files.put("package.cpk", """
                name: stockwatch
                version: 1.0.0
                entry: stockwatch.asm
                file: stockwatch.asm
                file: readme.txt
                """);
        this.computer.files.put("stockwatch.asm", ".asm 1\n");
        assertTrue(this.run("sgpack build").contains("readme.txt"));
        assertFalse(this.computer.files.containsKey("stockwatch-1.0.0.cpk"),
                "nothing half-built is left behind");
    }

    @Test
    void sgpackBuild_namesEveryLineOfTheManifestThatNeedsFixing() {
        this.computer.add(SigmaCommands.RUNTIME);
        this.computer.files.put("package.cpk", """
                name: Stock Watch
                version: 1
                entry: watch.sgs
                icon: sparkle
                ram: 0
                file: watch.sgs
                """);
        this.computer.files.put("watch.sgs", "class W { }\n");
        final String said = this.run("sgpack build");
        assertTrue(said.contains("name:"), said);
        assertTrue(said.contains("version:"), said);
        assertTrue(said.contains("icon:"), said);
    }

    @Test
    void sgpackBuild_asksForAManifestWhenThereIsNone() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sgpack build").contains("sgpack init"));
    }

    @Test
    void compile_writesTheAssemblyBesideTheSource() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        assertTrue(this.run("sgsc Monitor.sgs").contains("wrote Monitor.asm"));
        assertTrue(this.computer.files.get("Monitor.asm").startsWith(".asm 3"));
        assertTrue(this.computer.files.get("Monitor.asm").contains(".arch jsc:x86"));
        assertTrue(this.computer.files.get("Monitor.asm").contains(".start Tests.Monitor"));
    }

    @Test
    void compile_leftAlone_buildsForTheOldestArchitectureThatRunsIt() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        this.run("sgsc Monitor.sgs");
        assertTrue(this.computer.files.get("Monitor.asm").contains(".arch jsc:x86\n"),
                "a program nobody asked anything of runs on every machine it could have: "
                        + this.computer.files.get("Monitor.asm"));
    }

    @Test
    void compile_withAnArchitectureAsked_buildsForThatOne() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        this.run("sgsc Monitor.sgs --arch jsc:x86_64");
        assertTrue(this.computer.files.get("Monitor.asm").contains(".arch jsc:x86_64"));
    }

    @Test
    void compile_withAnArchitectureAskedByName_buildsForThatOne() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        this.run("sgsc Monitor.sgs --arch x86-64");
        assertTrue(this.computer.files.get("Monitor.asm").contains(".arch jsc:x86_64"));
    }

    @Test
    void compile_withAnArchitectureNothingAnswersTo_writesNothing() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        final String said = this.run("sgsc Monitor.sgs --arch risc");
        assertTrue(said.contains("no architecture is called 'risc'"), said);
        assertTrue(said.contains("x86-64"), "it says what there is instead: " + said);
        assertFalse(this.computer.files.containsKey("Monitor.asm"), "and nothing was written");
    }

    @Test
    void compile_saysHowToRunWhatItWrote() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        assertTrue(this.run("sgsc Monitor.sgs").contains("run it with: sigma run Monitor.asm"));
        assertFalse(this.run("sgsc Missing.sgs").contains("run it with"));
    }

    @Test
    void compile_writesWhereItWasToldTo() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Monitor.sgs", SCRIPT);
        assertTrue(this.run("sgsc Monitor.sgs -o build.asm").contains("wrote build.asm"));
        assertTrue(this.computer.files.containsKey("build.asm"));
        assertFalse(this.computer.files.containsKey("Monitor.asm"));
    }

    @Test
    void compile_saysWhatIsWrongAndWritesNothing() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("Bad.sgs", "class C { void M() { int n = \"text\"; } }\n");
        final String out = this.run("sgsc Bad.sgs");
        assertTrue(out.contains("Bad.sgs("), out);
        assertTrue(out.contains("nothing was written"), out);
        assertFalse(this.computer.files.containsKey("Bad.asm"));
    }

    @Test
    void compile_namesTheFileTheMistakeIsIn() {
        this.computer.add(SigmaCommands.COMPILER);
        this.computer.files.put("disk/Bad.sgs", "class C { void M() { nope(); } }\n");
        assertTrue(this.run("sgsc disk/Bad.sgs").contains("Bad.sgs("));
    }

    @Test
    void compile_saysSoWhenThereIsNoSuchFile() {
        this.computer.add(SigmaCommands.COMPILER);
        assertTrue(this.run("sgsc Missing.sgs").contains("no such file"));
    }

    @Test
    void compile_asksForAFileWhenGivenNone() {
        this.computer.add(SigmaCommands.COMPILER);
        assertTrue(this.run("sgsc").contains("usage: sgsc"));
    }

    @Test
    void sigma_listsWhatIsRunningAndSaysWhenNothingIs() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sigma ps").contains("no Σ# programs are running"));
        this.computer.running.add(new ICliComputer.SigmaProcess(1, "Monitor", "running", 2048, 65536));
        final String out = this.run("sigma ps");
        assertTrue(out.contains("Monitor"), out);
        assertTrue(out.contains("2 KB of 64 KB"), out);
    }

    @Test
    void sigma_startsAProgramAndHandsOnTheRoomItWasAskedFor() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sigma run Monitor.asm --heap 16M").contains("started"));
        assertEquals("Monitor.asm", this.computer.started);
        assertEquals(16, this.computer.startedHeap);
    }

    @Test
    void sigma_letsTheComputerDecideTheRoomWhenNoneIsAsked() {
        this.computer.add(SigmaCommands.RUNTIME);
        this.run("sigma run Monitor.asm");
        assertEquals(0, this.computer.startedHeap);
    }

    @Test
    void sigma_stopsOneByTheNumberItWasListedUnder() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sigma stop 3").contains("stopped 3"));
        assertEquals(3, this.computer.stopped);
        assertTrue(errored("sigma stop"));
    }

    @Test
    void sigma_saysHowItIsUsedWhenGivenSomethingElse() {
        this.computer.add(SigmaCommands.RUNTIME);
        assertTrue(this.run("sigma frobnicate").contains("usage: sigma"));
    }

    /**
     * A computer with only the parts the Σ# commands reach: a name to sign a package with, a disk, the programs it has
     * installed, and a note of what it was asked to run.
     */
    private static final class Fake implements ICliComputer {

        private final Map<String, String> files = new LinkedHashMap<>();
        private final List<ProgramInfo> installed = new ArrayList<>();
        private final List<SigmaProcess> running = new ArrayList<>();
        private String started;
        private int startedHeap = -1;
        private int stopped = -1;

        void add(final String id) {
            this.installed.add(new ProgramInfo(id.substring(id.indexOf(':') + 1), id));
        }

        @Override public String name() {
            return "TEST-PC";
        }

        @Override public List<ProgramInfo> programs() {
            return List.copyOf(this.installed);
        }

        @Override public FsResult readFile(final String path) {
            final String held = this.files.get(path);
            return held == null ? FsResult.fail("no such file: " + path) : FsResult.content(held);
        }

        @Override public FsResult writeFile(final String path, final String content) {
            this.files.put(path, content);
            return FsResult.ok("wrote " + path);
        }

        @Override public List<SigmaProcess> sigmaProcesses() {
            return List.copyOf(this.running);
        }

        @Override public OpResult startSigma(final String path, final int heapMb) {
            this.started = path;
            this.startedHeap = heapMb;
            return OpResult.ok("started " + path);
        }

        @Override public OpResult stopSigma(final int id) {
            this.stopped = id;
            return OpResult.ok("stopped " + id);
        }
    }
}
