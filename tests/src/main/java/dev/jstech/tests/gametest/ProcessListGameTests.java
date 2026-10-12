/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestShell;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for a Unix machine's process list and the signals {@code kill} sends: the system's own first process at
 * number 1 and its services before the programs, which are numbered after them; {@code kill 1} taking the system down
 * in each family's own words; a service's process ended until the machine starts over.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProcessListGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final BlockPos MAINFRAME = new BlockPos(2, 2, 2);
    /** A program that runs until something stops it. */
    private static final String COUNTER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Counter : IScript {
                int seen;
                public void OnInit() { seen = 0; }
                public void OnTick() { seen = seen + 1; Console.PrintLine("tick " + seen); }
                public void OnDestroy() { }
            }
            """;

    private ProcessListGameTests() {
    }

    /** ps lists the system's own first process, systemd on Debian, at number 1, then the programs after it. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void ps_listsTheSystemsFirstProcessBeforeThePrograms(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final MachinePrograms.Started started = pc.programs().start("counter.sgs", COUNTER, 1, pc);
                    helper.assertTrue(started.ok() && started.id() >= MachinePrograms.FIRST_PID,
                            "a program is numbered after the system's own; got " + started.id());
                    final List<String> ps = TestShell.shell(helper, pc, "ps");
                    helper.assertTrue(ps.size() >= 3 && ps.get(1).matches("1\\s+systemd\\s+/sbin/init\\s+running.*"),
                            "the first process, then the program; got " + ps);
                    helper.assertTrue(ps.getLast().startsWith(String.valueOf(started.id())),
                            "the program comes last, under its own number; got " + ps.getLast());
                })
                .thenSucceed();
    }

    /** Gentoo comes up under OpenRC, and FreeBSD and UNIX have always had one: their first process is plain init. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void ps_namesTheFirstProcessInitWhereTheSystemStartsNoSystemd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "gentoo");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final List<String> ps = TestShell.shell(helper, pc, "ps");
                    helper.assertTrue(ps.size() >= 2 && ps.get(1).matches("1\\s+init\\s+/sbin/init.*"),
                            "Gentoo's first process is init; got " + ps);
                })
                .thenSucceed();
    }

    /** kill -9 1 on Linux ends init, and the kernel panics saying so, with the signal it died of. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void kill_nineToTheFirstProcessPanicsLinuxWithItsExitCode(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    TestShell.shell(helper, pc, "kill -9 1");
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.lines().stream().anyMatch(line ->
                                    line.endsWith("Kernel panic - not syncing: Attempted to kill init!"
                                            + " exitcode=0x00000009")),
                            "Linux panics on its init killed; got " + page);
                    helper.assertTrue(page.lines().getFirst().matches("player@\\S+:~\\$ kill -9 1"),
                            "the line that did it is on the page, at the prompt it was typed at; got "
                                    + page.lines().getFirst());
                })
                .thenSucceed();
    }

    /** A plain kill 1 on FreeBSD sends TERM to init, and FreeBSD goes nowhere without it. */
    @GameTest(template = ARENA, timeoutTicks = 1500)
    public static void kill_toTheFirstProcessTakesFreeBsdNowhere(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "freebsd");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    TestShell.shell(helper, pc, "kill 1");
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.lines().contains("init died (signal 15, exit 0)")
                                    && page.lines().contains("panic: Going nowhere without my init!"),
                            "FreeBSD's own panic, with TERM; got " + page);
                })
                .thenSucceed();
    }

    /** kill reaches a program by the number ps gives it, with any signal, and refuses a signal there is not. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void kill_reachesAProgramByItsNumberAndRefusesAnUnknownSignal(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final MachinePrograms.Started started = pc.programs().start("counter.sgs", COUNTER, 1, pc);
                    final List<String> refused = TestShell.shell(helper, pc, "kill -FOO " + started.id());
                    helper.assertTrue(TestShell.says(refused, "kill: -FOO: invalid signal specification")
                            && pc.programs().byId(started.id()) != null, "an unknown signal reaches nothing; got "
                            + refused);
                    final List<String> said = TestShell.shell(helper, pc, "kill -SIGKILL " + started.id());
                    helper.assertTrue(pc.programs().byId(started.id()) == null && pc.crashPage() == null,
                            "the program stops and the system stays up; got " + said);
                    helper.assertTrue(TestShell.says(TestShell.shell(helper, pc, "kill -l"), "9) SIGKILL"),
                            "kill -l lists the signals");
                })
                .thenSucceed();
    }

    /**
     * A Mainframe's network engine is a service with a process of its own: ps lists it, kill ends it and the network
     * has no engine, until the Mainframe starts over and the engine comes back with it.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void kill_endsAServiceUntilTheMachineStartsOver(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper)
                .placeMainframeWithSystem(MAINFRAME, ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
        final String[] engine = new String[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(mainframe), "the Mainframe comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(mainframe.runningEngine() != null, "its engine runs");
                    final List<String> ps = TestShell.shell(helper, mainframe, "ps");
                    final String line = ps.stream().filter(one -> one.contains("/usr/sbin/")).findFirst().orElse("");
                    helper.assertTrue(!line.isEmpty(), "ps lists a service; got " + ps);
                    engine[0] = line.split("\\s+")[0];
                    final List<String> said = TestShell.shell(helper, mainframe, "kill " + engine[0]);
                    helper.assertTrue(TestShell.says(said, "stopped " + engine[0]), "kill says so; got " + said);
                    helper.assertTrue(mainframe.runningEngine() == null, "and the network has no engine");
                    helper.assertTrue(!TestShell.says(TestShell.shell(helper, mainframe, "ps"), "/usr/sbin/"),
                            "nor does ps list it any more");
                    mainframe.restart();
                })
                .thenWaitUntil(() -> helper.assertTrue(up(mainframe), "the Mainframe comes back"))
                .thenExecute(() -> helper.assertTrue(mainframe.runningEngine() != null,
                        "and its engine with it"))
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity upWith(final GameTestHelper helper, final String system) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC,
                ResourceLocation.fromNamespaceAndPath("jsc", system));
    }

    private static boolean up(final AbstractComputerBlockEntity machine) {
        return machine.isRunning() && !machine.needsPost() && !machine.booting() && !machine.atBootMenu();
    }
}
