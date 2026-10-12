/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

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

    /** A program started at the prompt of a Frames with no room left for it brings the machine down. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void programLaunch_onAFullFramesBringsItDown(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacyFrames95(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    // Calculators that leave it 4 of its 128 MB: the program asks for more than that.
                    pc.setOpenWindows(calculators(27));
                    final ProgramLauncher.Launch launch = ProgramLauncher.launch(pc, "C:\\progs\\counter.sgs",
                            asked -> ICliComputer.FsResult.ok(COUNTER), List.of(), IProgramParent.NONE,
                            ProgramPriority.MEDIUM, 16);
                    helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.NO_MEMORY,
                            "the program does not fit; got " + launch.refusal());
                    helper.assertTrue(pc.crashPage() != null, "and Frames goes down rather than refuse it");
                })
                .thenSucceed();
    }

    /** A Frames that came to hold more than it has, whatever took the memory, goes down within a second. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void overcommit_bringsAFramesMachineDownWithinASecond(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacyFrames95(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> pc.setOpenWindows(calculators(OpenWindow.MAX)))
                .thenExecuteAfter(25, () -> helper.assertTrue(pc.crashPage() != null,
                        "holding 144 of its 128 MB, the machine goes down"))
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

    /* A Legacy computer with the least memory its board takes, 128 MB, running Frames 95. */
    private static PersonalComputerBlockEntity legacyFrames95(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(PC, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = world.blockEntity(PC, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = pc.getHardware();
        final Item board = HardwareItems.MOTHERBOARD_ATX_LEGACY_S370.get();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SDRAM_128.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(EraParts.psuFor(HardwareEra.LEGACY)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(HardwareEra.LEGACY)));
        if (!pc.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "frames_95"))) {
            throw new IllegalStateException("could not install Frames 95 on the Legacy computer");
        }
        pc.togglePower();
        return pc;
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow("jsc:calculator", 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static PersonalComputerBlockEntity upWith(final GameTestHelper helper, final String system) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC,
                ResourceLocation.fromNamespaceAndPath("jsc", system));
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }
}
