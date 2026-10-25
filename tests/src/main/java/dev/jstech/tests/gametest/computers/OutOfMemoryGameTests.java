/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GameTests for a Unix kernel running out of memory: it ends what holds the most, logs each one as the real kernel
 * does, keeps the machine up, and panics only once nothing is left to end.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OutOfMemoryGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final String CALCULATOR = "jsc:calculator";
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

    private OutOfMemoryGameTests() {
    }

    /**
     * Linux handed more windows than its memory holds keeps every window it can: the latest go, one by one, until the
     * rest fit, each logged as the kernel logs a process it killed, and the machine stays up.
     */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void overcommit_linuxEndsTheLatestWindowsUntilTheRestFit(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    pc.setOpenWindows(calculators(OpenWindow.MAX));
                    pc.weighMemory();
                    final RamLedger ledger = pc.ramLedger();
                    helper.assertTrue(ledger.usedMb() <= ledger.totalMb(), "what is left fits; holding "
                            + ledger.usedMb() + " of " + ledger.totalMb() + " MB");
                    final List<OpenWindow> left = pc.openWindows();
                    helper.assertTrue(!left.isEmpty() && left.size() < OpenWindow.MAX,
                            "some windows went and some stayed; " + left.size() + " stayed");
                    helper.assertTrue(left.getLast().x() == 10 + left.size() - 1,
                            "the windows that stayed are the first ones opened");
                    final List<String> log = pc.memoryLog();
                    helper.assertTrue(log.size() == OpenWindow.MAX - left.size(), "one line for each window ended");
                    helper.assertTrue(log.getFirst().matches("\\[ *\\d+\\.\\d{6}] Out of memory: Killed process \\d+"
                            + " \\(calc\\) total-vm:\\d+kB, anon-rss:\\d+kB, UID:0 pgtables:\\d+kB oom_score_adj:0"),
                            "in the kernel's own words; got " + log.getFirst());
                    helper.assertTrue(pc.crashPage() == null && pc.isRunning(), "and the machine stays up");
                })
                .thenSucceed();
    }

    /** Of what a machine runs, the kernel ends what holds the most: a program heavier than every window goes first. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void overcommit_endsTheProgramHoldingTheMost(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, "debian");
        final int[] counter = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final MachinePrograms.Started started = pc.programs().start("counter.sgs", COUNTER,
                            MachinePrograms.MAX_HEAP_MB, pc);
                    helper.assertTrue(started.ok(), "the counter starts: " + started.message());
                    counter[0] = started.id();
                    // Calculators up to a little past what is left beside the counter: the counter holds the most.
                    final RamLedger ledger = pc.ramLedger();
                    final int each = windowMb(pc);
                    final int windows = Math.min(OpenWindow.MAX, ledger.freeMb() / each + 1);
                    pc.setOpenWindows(calculators(windows));
                    pc.weighMemory();
                    helper.assertTrue(pc.programs().byId(counter[0]) == null, "the program holding the most is ended");
                    helper.assertTrue(pc.openWindows().size() == windows, "and every window stays");
                    // A program that gave itself no name goes by its runtime's, as ps lists it.
                    helper.assertTrue(pc.memoryLog().size() == 1 && pc.memoryLog().getFirst()
                                    .contains("Killed process " + counter[0] + " (sigma)"),
                            "the line names the program by its own number; got " + pc.memoryLog());
                })
                .thenSucceed();
    }

    /** A program started on a full Linux makes its own room: the kernel ends what holds the most, and it starts. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void programLaunch_linuxEndsWhatHoldsTheMostToMakeRoom(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final int each = windowMb(pc);
                    // As many Calculators as leave less free than the program asks for.
                    final int windows = Math.min(OpenWindow.MAX, (pc.ramLedger().freeMb() - 8) / each);
                    pc.setOpenWindows(calculators(windows));
                    final ProgramLauncher.Launch launch = ProgramLauncher.launch(pc, "/home/player/counter.sgs",
                            asked -> ICliComputer.FsResult.ok(COUNTER), List.of(), IProgramParent.NONE,
                            ProgramPriority.MEDIUM, 16);
                    helper.assertTrue(launch.ok(), "the program starts; got " + launch.refusal());
                    helper.assertTrue(pc.openWindows().size() < windows, "a window went to make its room");
                    helper.assertTrue(!pc.memoryLog().isEmpty(), "and the kernel logged it");
                })
                .thenSucceed();
    }

    /** FreeBSD logs a process it ended in its own words. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void overcommit_freeBsdSaysItInItsOwnWords(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, "freebsd");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    pc.setOpenWindows(calculators(OpenWindow.MAX));
                    pc.weighMemory();
                    helper.assertTrue(!pc.memoryLog().isEmpty() && pc.memoryLog().getFirst()
                                    .matches("pid \\d+ \\(calc\\), jid 0, uid 0, was killed: out of swap space"),
                            "FreeBSD's line; got " + pc.memoryLog());
                })
                .thenSucceed();
    }

    /**
     * {@code dmesg} reads the kernel's log: what it said of itself as it came up, then what it ended; a machine that
     * starts over begins its log again.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void dmesg_readsTheBootThenWhatWasEndedAndARestartClearsIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, "debian");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    pc.setOpenWindows(calculators(OpenWindow.MAX));
                    pc.weighMemory();
                    final List<String> log = new ServerCliComputer(pc, level(helper)).kernelLog();
                    helper.assertTrue(log.getFirst().matches("\\[ +0\\.000000] Linux version .*"),
                            "the log opens on the kernel naming itself; got " + log.getFirst());
                    helper.assertTrue(log.getLast().contains("Out of memory: Killed process"),
                            "and ends on what it ended; got " + log.getLast());
                    pc.restart();
                })
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes back"))
                .thenExecute(() -> helper.assertTrue(pc.memoryLog().isEmpty(),
                        "a machine that started over begins its log again; got " + pc.memoryLog()))
                .thenSucceed();
    }

    /**
     * With nothing left to end and still no room, the kernel panics: UNIX on a Vintage machine with four megabytes,
     * asked for sixteen, goes down on its own page.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void programLaunch_unixPanicsWhenNothingIsLeftToEnd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = vintageUnix(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the machine comes up"))
                .thenExecute(() -> {
                    final ProgramLauncher.Launch launch = ProgramLauncher.launch(pc, "/usr/player/counter.sgs",
                            asked -> ICliComputer.FsResult.ok(COUNTER), List.of(), IProgramParent.NONE,
                            ProgramPriority.MEDIUM, 16);
                    helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.NO_MEMORY,
                            "the program does not start; got " + launch.refusal());
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.lines().contains("PANIC: kmem_alloc: out of kernel memory"),
                            "UNIX panics; got " + page);
                })
                .thenSucceed();
    }

    /* A Legacy computer with the least memory its board takes, 128 MB, running that system. */
    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final String system) {
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
                new ItemStack(EraParts.cardFor(board, GpuSpec.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(HardwareEra.LEGACY)));
        if (!pc.installOs(ResourceLocation.fromNamespaceAndPath("jsc", system))) {
            throw new IllegalStateException("could not install " + system + " on the Legacy computer");
        }
        pc.togglePower();
        return pc;
    }

    /* A Vintage computer with a single 4 MB stick, running UNIX System V. */
    private static PersonalComputerBlockEntity vintageUnix(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(PC, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = world.blockEntity(PC, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(HardwareEra.VINTAGE)));
        if (!pc.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "unix"))) {
            throw new IllegalStateException("could not install UNIX on the Vintage computer");
        }
        pc.togglePower();
        return pc;
    }

    /* What one Calculator window holds on that machine's system. */
    private static int windowMb(final PersonalComputerBlockEntity pc) {
        final RamLedger before = pc.ramLedger();
        final List<OpenWindow> kept = new ArrayList<>(pc.openWindows());
        pc.setOpenWindows(calculators(1));
        final int each = pc.ramLedger().usedMb() - before.usedMb() + before.usedMb(RamLedger.Kind.WINDOW);
        pc.setOpenWindows(kept);
        return Math.max(1, each);
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow(CALCULATOR, 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static ServerLevel level(final GameTestHelper helper) {
        return helper.getLevel();
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }
}
