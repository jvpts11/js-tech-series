/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for a system crashing as the machine's own state: the page it goes down on, what dies with it, and how
 * the machine comes out of it, starting over from its self-test.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CrashGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
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

    private CrashGameTests() {
    }

    /**
     * Frames 95 goes down on its blue page: the program it was running dies, its session ends, and once the page has
     * stood its time the machine owes its self-test, which is how it starts over.
     */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void crash_frames95GoesDownOnItsPageAndStartsOverFromItsSelfTest(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "frames_95");
        final long[] session = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final MachinePrograms.Started started = pc.programs().start("counter.sgs", COUNTER, 1, pc);
                    helper.assertTrue(started.ok(), "the counter starts: " + started.message());
                    session[0] = pc.console().session();
                    helper.assertTrue(pc.crash(CrashCause.MEMORY, CrashPages.Moment.memory("Paint", 0, List.of())),
                            "a running system crashes");
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.style() == CrashStyle.FRAMES_95 && !page.red(),
                            "on Frames 95's blue page; got " + page);
                    helper.assertTrue(pc.programs().isEmpty(), "the program it ran dies with it");
                    helper.assertTrue(pc.console().session() != session[0], "its session ends");
                    helper.assertTrue(pc.goingDown(), "and it is on its way down");
                    helper.assertTrue(!pc.crash(CrashCause.MEMORY, CrashPages.Moment.memory("Paint", 0, List.of())),
                            "a system already down does not crash again");
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.crashPage() == null && pc.needsPost(),
                        "the page stands its time, and then the machine starts over from its self-test"))
                .thenSucceed();
    }

    /** A window that no longer fits on a Frames desktop brings the machine down onto its page. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void memoryShort_bringsAFramesMachineDown(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "frames_95");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    pc.memoryShort("Calculator");
                    helper.assertTrue(pc.crashPage() != null, "the window that did not fit brings it down");
                })
                .thenSucceed();
    }

    /** The red page of a fault of the hardware, with the part's name on the newer editions' page. */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void crash_hardwarePutsTheRedPageUpNamingThePart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "frames_10");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(pc.crash(CrashCause.HARDWARE,
                            CrashPages.Moment.hardware("Stratix Layer DDR3-8192")), "a running system crashes");
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.style() == CrashStyle.FRAMES_10 && page.red()
                                    && page.stopCode().equals("WHEA_UNCORRECTABLE_ERROR")
                                    && page.failedPart().equals("Stratix Layer DDR3-8192"),
                            "on the red page naming the part; got " + page);
                })
                .thenSucceed();
    }

    /**
     * FreeBSD's panic offers to stop its automatic reboot: a key stops it and the machine waits past the fifteen
     * seconds, and the next key reboots it.
     */
    @GameTest(template = ARENA, timeoutTicks = 1500)
    public static void crash_freeBsdHoldsItsRebootOnAKeyAndRebootsOnTheNext(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "freebsd");
        final int[] total = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(pc.crash(CrashCause.INIT_KILLED, CrashPages.Moment.initKilled("kill 1", 15)),
                            "killing its first process brings it down");
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.holdsOnKey()
                                    && page.lines().contains("panic: Going nowhere without my init!"),
                            "on FreeBSD's own panic; got " + page);
                    total[0] = page.totalTicks();
                    pc.crashKey();
                    helper.assertTrue(pc.crashHeld(), "a key stops the reboot");
                })
                .thenIdle(320)
                .thenExecute(() -> helper.assertTrue(pc.crashPage() != null && pc.goingDown() && !pc.needsPost(),
                        "past the " + total[0] + " ticks the page still stands and the machine waits"))
                .thenExecute(pc::crashKey)
                .thenWaitUntil(() -> helper.assertTrue(pc.crashPage() == null && pc.needsPost(),
                        "the next key reboots it"))
                .thenSucceed();
    }

    /** The earliest systems have no crash page: MC-DOS runs one program at a time and has nothing to bring down. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void crash_earliestSystemsHaveNoPage(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "mc_dos");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> helper.assertTrue(!pc.crash(CrashCause.MEMORY,
                        CrashPages.Moment.memory("EDIT", 0, List.of())) && pc.crashPage() == null,
                        "MC-DOS does not crash onto a page"))
                .thenSucceed();
    }

    /** A machine still testing itself has no system up to crash. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void crash_refusedBeforeTheSystemIsUp(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = upWith(helper, "frames_xp");
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(pc.needsPost() && !pc.crash(CrashCause.MEMORY,
                        CrashPages.Moment.memory("Paint", 0, List.of())), "no system is up to crash"))
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity upWith(final GameTestHelper helper, final String system) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC,
                ResourceLocation.fromNamespaceAndPath("jsc", system));
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }
}
