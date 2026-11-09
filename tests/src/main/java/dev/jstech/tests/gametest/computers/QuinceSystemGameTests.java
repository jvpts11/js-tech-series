/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.QuincePartitions;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TerminalHand;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quince's six systems (computers/docs/design/quince.md): each on the processors it was built for and at a desktop of
 * its own; the Quince System from its three floppies, asking for each; the classic systems' bombs, which stand until
 * the player restarts, and their programs opening in fixed blocks of memory; Quince OS X asking what to force quit
 * where a Linux would end something itself; and keg, which comes with the Standard.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class QuinceSystemGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final BlockPos BESIDE = new BlockPos(5, 2, 2);
    /* South of the computer, facing away from it: the drive links to the machine behind it. */
    private static final BlockPos DRIVE = new BlockPos(2, 2, 3);
    private static final String CALCULATOR = "jsc:calculator";
    /* As many runner ticks as a page of the copy may take before the test calls it stuck. */
    private static final int MOST_TICKS = 20_000;
    /* Longer than a Quince page would stand if it did not wait for the player, and than the memory's weighing. */
    private static final int PAST_THE_PAGE = 60;
    /* More counters than any machine here has the memory for. */
    private static final int MOST_COUNTERS = 256;
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

    private QuinceSystemGameTests() {
    }

    /** Each system goes on the processors it was built for, comes with a desktop of its own, and on no other. */
    @GameTest(template = ARENA)
    public static void quinceSystems_installOnTheProcessorsTheyAreBuiltFor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity russet = russet128k(helper, AT);
        assertInstalls(helper, russet, "quince_system", PanelStyle.QUINCE_CLASSIC);
        helper.assertFalse(russet.installOs(rl("quince_os_9")), "Quince OS 9 is built for the PowerPC alone");
        final PersonalComputerBlockEntity g4 = powerG4(helper, BESIDE);
        assertInstalls(helper, g4, "quince_os_9", PanelStyle.QUINCE_CLASSIC);
        assertInstalls(helper, g4, "quince_os_x", PanelStyle.QUINCE_X);
        helper.assertFalse(g4.installOs(rl("quinceos")), "quinceOS is built for Quince's own chips");
        final PersonalComputerBlockEntity white = qRussetWhite(helper, new BlockPos(8, 2, 2));
        assertInstalls(helper, white, "quince_os_x_server", PanelStyle.QUINCE_X);
        helper.assertFalse(white.installOs(rl("quince_os_9")), "an Integra machine takes no PowerPC system");
        final PersonalComputerBlockEntity colour = qRussetColour(helper, new BlockPos(11, 2, 2));
        assertInstalls(helper, colour, "quinceos", PanelStyle.QUINCE_X);
        helper.succeed();
    }

    /**
     * The Quince System comes on three floppies: the installer copies what is on the first, asks for the second while
     * the first is still in, takes the second once it goes in, then the third, and the system is on the disk.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void quinceSystem_installsFromItsThreeFloppiesAskingForEach(final GameTestHelper helper) {
        final PersonalComputerBlockEntity russet = russet128k(helper, AT);
        helper.setBlock(DRIVE, ComputingModule.FLOPPY_DRIVE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final MediaReaderBlockEntity drive = (MediaReaderBlockEntity) helper.getBlockEntity(DRIVE);
        final List<ItemStack> floppies = InstallerStacks.disksOf(OsRegistry.getOs(rl("quince_system")));
        helper.assertValueEqual(floppies.size(), 3, "the Quince System's floppies");
        drive.mediaSlot().setStackInSlot(0, floppies.getFirst().copy());
        russet.togglePower();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.absolutePos(AT).equals(drive.ownerPos()),
                        "the drive links to the Russet behind it"))
                .thenExecute(() -> {
                    helper.assertTrue(FirmwarePayloads.installOsFromReader(helper.getLevel(), russet, -1L, -1),
                            "the installer opens");
                    final InstallerFlow flow = russet.installer();
                    helper.assertTrue(flow != null && flow.page() == InstallerPage.WELCOME,
                            "on its Easy Install; got " + (flow == null ? "none" : flow.page()));
                    flow.next();
                    helper.assertTrue(flow.started(), "Install starts the copy");
                    for (int disk = 2; disk <= floppies.size(); disk++) {
                        copyUntil(helper, russet, InstallerPage.INSERT);
                        helper.assertValueEqual(flow.disk(), disk, "the disk asked for");
                        OsInstallRunner.tick(russet, helper.getLevel(), russet.getBlockPos());
                        helper.assertTrue(flow.page() == InstallerPage.INSERT && russet.installing() != null,
                                "the disk already read does not answer it");
                        drive.mediaSlot().setStackInSlot(0, floppies.get(disk - 1).copy());
                        OsInstallRunner.tick(russet, helper.getLevel(), russet.getBlockPos());
                        helper.assertTrue(flow.page() == InstallerPage.COPY,
                                "disk " + disk + " goes in and the copy goes on; got " + flow.page());
                    }
                    copyUntil(helper, russet, null);
                    helper.assertValueEqual(russet.installedOsId(), rl("quince_system"), "the system on the disk");
                })
                .thenSucceed();
    }

    /**
     * Quince OS 9 goes down on its bomb, naming the program that was running, and the bomb stands past any time a page
     * stands until the player restarts the machine, which then starts over.
     */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void crash_quinceOs9BombStandsUntilThePlayerRestarts(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_9");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4), "the machine comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(g4.crash(CrashCause.MEMORY, CrashPages.Moment.memory("Expedition", 12,
                            List.of())), "a running system crashes");
                    final CrashPage page = g4.crashPage();
                    helper.assertTrue(page != null && page.style() == CrashStyle.QUINCE_OS_9_BOMB
                                    && page.stopLine().equals("\"Expedition\"") && page.holdsOnKey(),
                            "on Quince OS 9's bomb, naming the program; got " + page);
                    helper.assertTrue(g4.crashHeld(), "and the bomb waits for the player");
                })
                .thenIdle(PAST_THE_PAGE)
                .thenExecute(() -> helper.assertTrue(g4.crashPage() != null && !g4.needsPost(),
                        "the bomb still stands"))
                .thenExecute(g4::crashKey)
                .thenWaitUntil(() -> helper.assertTrue(g4.crashPage() == null && g4.needsPost(),
                        "Restart starts the machine over"))
                .thenSucceed();
    }

    /** A window that finds no room on a classic system is refused, and the machine stays up. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void memoryShort_aClassicSystemRefusesTheWindowAndStaysUp(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_9");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4), "the machine comes up"))
                .thenExecute(() -> helper.assertTrue(!g4.memoryShort("Calculator", 0) && g4.crashPage() == null
                        && up(g4), "the window is refused and the machine stays up"))
                .thenSucceed();
    }

    /** On a classic system a program opens in the block its Get Info sets, whatever it would hold elsewhere. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void partition_aClassicProgramOpensInTheBlockItsGetInfoSets(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_9");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4), "the machine comes up"))
                .thenExecute(() -> {
                    g4.console().settings().setPartition("calculator", QuincePartitions.Partition.fixed(9));
                    g4.setOpenWindows(calculators(1));
                    helper.assertValueEqual(g4.ramLedger().usedMb(RamLedger.Kind.WINDOW), 9,
                            "the Calculator holds the block it was given");
                    g4.console().settings().setPartition("calculator", QuincePartitions.Partition.fixed(13));
                    g4.setOpenWindows(calculators(1));
                    helper.assertValueEqual(g4.ramLedger().usedMb(RamLedger.Kind.WINDOW), 13,
                            "and a bigger one once it is set");
                })
                .thenSucceed();
    }

    /**
     * Quince OS X out of memory ends nothing itself: asked for more than is free, the machine stays up with every
     * window and asks the player what to force quit, and the question stands while nothing is done, the program that
     * asked never in the memory to fit; once the player has made room, it closes.
     */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void outOfMemory_quinceOsXAsksWhatToForceQuit(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_x");
        final int windows = 3;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4), "the machine comes up"))
                .thenExecute(() -> {
                    g4.setOpenWindows(calculators(windows));
                    helper.assertTrue(!g4.memoryShort("counter.sgs", g4.ramLedger().freeMb() + 1),
                            "a program asking for more than is free gets no room");
                    helper.assertTrue(g4.askingForMemory() && g4.crashPage() == null,
                            "the machine asks rather than going down");
                    helper.assertValueEqual(g4.openWindows().size(), windows, "and nothing was ended for it");
                })
                .thenIdle(PAST_THE_PAGE)
                .thenExecute(() -> {
                    helper.assertTrue(g4.askingForMemory(), "the question stands while the player does nothing");
                    final List<String> chosen = new ArrayList<>();
                    for (final RamLedger.Entry entry : g4.ramLedger().entries()) {
                        if (entry.kind() == RamLedger.Kind.WINDOW) {
                            chosen.add(entry.token());
                        }
                    }
                    g4.forceQuit(chosen);
                    helper.assertTrue(g4.openWindows().isEmpty(), "what the player chose is ended");
                    helper.assertTrue(!g4.askingForMemory(), "and with room made the question closes");
                })
                .thenSucceed();
    }

    /** A program started at Quince OS X's prompt with no room for it is refused, and the machine asks the player. */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void programLaunch_onAFullQuinceOsXAsksAndDoesNotStart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_x");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4), "the machine comes up"))
                .thenExecute(() -> {
                    // Programs of the largest heap until what is left is less than one more asks for, each source
                    // built on the way in for the G4's own PowerPC.
                    for (int i = 0; i < MOST_COUNTERS && g4.ramLedger().fits(MachinePrograms.MAX_HEAP_MB); i++) {
                        final MachinePrograms.Started started = g4.programs().start("counter.sgs", COUNTER,
                                MachinePrograms.MAX_HEAP_MB, g4);
                        helper.assertTrue(started.ok(), "a counter starts while it fits: " + started.message());
                    }
                    helper.assertFalse(g4.ramLedger().fits(MachinePrograms.MAX_HEAP_MB), "the memory is full");
                    final int running = g4.programs().view().size();
                    final ProgramLauncher.Launch launch = ProgramLauncher.launch(g4, "/Users/player/counter.sgs",
                            asked -> ICliComputer.FsResult.ok(COUNTER), List.of(), IProgramParent.NONE,
                            ProgramPriority.MEDIUM, MachinePrograms.MAX_HEAP_MB);
                    helper.assertTrue(launch.refusal() == ProgramLauncher.Refusal.NO_MEMORY,
                            "the program does not start; got " + launch.refusal() + " " + launch.said().english());
                    helper.assertTrue(g4.askingForMemory() && g4.crashPage() == null,
                            "the machine asks rather than going down");
                    helper.assertValueEqual(g4.programs().view().size(), running, "and nothing was ended for it");
                })
                .thenSucceed();
    }

    /** keg came to Quince's systems with the Standard: the Legacy's Quince OS X has no such command. */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void keg_comesWithTheStandard(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_x");
        final PersonalComputerBlockEntity colour = running(helper, qRussetColour(helper, BESIDE), "quinceos");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4) && up(colour), "both machines come up"))
                .thenExecute(() -> {
                    final String legacy = new TerminalHand(g4, helper.getLevel()).glance("keg list");
                    helper.assertTrue(legacy.contains("command not found"),
                            "no keg on the Legacy's Quince OS X; got " + legacy.strip());
                    final String advanced = new TerminalHand(colour, helper.getLevel()).glance("keg list");
                    helper.assertTrue(!advanced.contains("command not found"),
                            "keg answers on quinceOS; got " + advanced.strip());
                })
                .thenSucceed();
    }

    /**
     * Every line a player types at Quince OS X's Terminal on a Russet Power G4, and at quinceOS's on a qRusset, answers
     * without an error, as every other system's do on the machines of its era (EraCommandsGameTests).
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void terminal_everyCommandOfQuinceOsXAndQuinceOsAnswersWithoutAnError(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = running(helper, powerG4(helper, AT), "quince_os_x");
        final PersonalComputerBlockEntity colour = running(helper, qRussetColour(helper, BESIDE), "quinceos");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(g4) && up(colour), "both machines come up"))
                .thenExecute(() -> {
                    final List<String> failures = new ArrayList<>(EraCommandsGameTests.failures(helper, g4,
                            "quince_os_x"));
                    failures.addAll(EraCommandsGameTests.failures(helper, colour, "quinceos"));
                    // These machines stand on no network, and net saying so is its right answer here.
                    failures.removeIf(line -> line.endsWith(": net: not on a network"));
                    helper.assertTrue(failures.isEmpty(), failures.size() + " lines answer with an error: "
                            + String.join(" | ", failures));
                })
                .thenSucceed();
    }

    /* Fails unless that system goes on the machine with a desktop of its own in that style. */
    private static void assertInstalls(final GameTestHelper helper, final PersonalComputerBlockEntity machine,
                                       final String system, final PanelStyle style) {
        helper.assertTrue(machine.installOs(rl(system)), system + " installs");
        final OsDef os = OsRegistry.getOs(rl(system));
        helper.assertTrue(os.bundledDesktop().isPresent() && os.bundledDesktop().get().equals(rl(system)),
                system + " comes with the desktop under its own id");
        final DesktopEnvironmentDef desktop = OsRegistry.getDesktop(rl(system));
        helper.assertTrue(desktop != null && desktop.panelStyle() == style,
                system + "'s desktop is drawn in " + style + "; got " + desktop);
    }

    /* Runs the copy until the installer stands on that page, or until it is over when the page is null. */
    private static void copyUntil(final GameTestHelper helper, final PersonalComputerBlockEntity machine,
                                  final InstallerPage page) {
        for (int i = 0; i < MOST_TICKS; i++) {
            final InstallerFlow flow = machine.installer();
            if (page == null ? machine.installing() == null : flow != null && flow.page() == page) {
                return;
            }
            OsInstallRunner.tick(machine, helper.getLevel(), machine.getBlockPos());
        }
        helper.fail("the copy never reached " + (page == null ? "its end" : page));
    }

    /* The machine with that system on its disk, switched on. */
    private static PersonalComputerBlockEntity running(final GameTestHelper helper,
                                                       final PersonalComputerBlockEntity machine,
                                                       final String system) {
        helper.assertTrue(machine.installOs(rl(system)), system + " installs");
        machine.togglePower();
        return machine;
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow(CALCULATOR, 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static boolean up(final PersonalComputerBlockEntity machine) {
        return machine.isRunning() && !machine.needsPost() && !machine.booting() && !machine.atBootMenu();
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static PersonalComputerBlockEntity russet128k(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(),
                QuinceItems.CPU_68000.get(), HardwareEra.VINTAGE, true);
    }

    private static PersonalComputerBlockEntity powerG4(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.RUSSET_POWER_G4.get(), QuinceItems.BOARD_POWER_G4.get(),
                QuinceItems.CPU_G4.get(), HardwareEra.LEGACY, true);
    }

    private static PersonalComputerBlockEntity qRussetWhite(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(), null,
                HardwareEra.TRANSITION, true);
    }

    private static PersonalComputerBlockEntity qRussetColour(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.QRUSSET_COLOURS.getFirst().get(),
                QuinceItems.BOARD_QRUSSET_COLOUR.get(), QuinceItems.CPU_Q1.get(), HardwareEra.ADVANCED, false);
    }

    /*
     * A machine assembled on {@code board}, switched off: the processor given, or the best of the board's era for
     * null, a memory module of the board's when asked, its era's supply and disk. Each part goes in the slot the
     * machine's own map has for it, a Quince case having no bays to count.
     */
    private static PersonalComputerBlockEntity built(final GameTestHelper helper, final BlockPos at, final Block block,
                                                     final Item board, final Item cpu, final Era era,
                                                     final boolean module) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(at, block);
        final PersonalComputerBlockEntity machine = world.blockEntity(at, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(cpu == null ? EraParts.cpuFor(board) : cpu));
        if (module) {
            hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        }
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(era)));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(EraParts.diskFor(era)));
        return machine;
    }
}
