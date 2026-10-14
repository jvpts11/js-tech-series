/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TerminalHand;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * FreeBSD's ports in many configurations, typed at the prompt with whatever a line leaves running moved along until
 * the prompt comes back. On a Personal Computer on the era's network and on a Mainframe of its own, from the Legacy to
 * the Advanced: the tree fetched and laid out, and every port in its index built with make install clean, each either
 * on the machine afterwards, built there, its work folder gone and its command answering, or refused in the ports'
 * words (marked as broken where the program is too new for the machine or belongs on another kind of machine). The
 * targets each do what they name; a port and the package of the same program meet in either order; a disk with no
 * room left takes no tree; and formatting the disk takes the builds with the system.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PortsMatrixGameTests {

    private static final String BENCH = "bench";
    private static final int TIMEOUT = 2_000;
    /** How long a computer may take to start on the system just put on its disk. */
    private static final int START_WITHIN = 600;
    private static final int WIDTH = 80;
    private static final int TRANSCRIPT = 800;
    private static final ResourceLocation FREEBSD = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "freebsd");
    /** Where the Mainframe of its own stands, well away from the era's network. */
    private static final BlockPos LONE_MAINFRAME = new BlockPos(30, 2, 30);
    private static final String INDEX = "usr/ports/INDEX-14";
    private static final String SCREENFETCH = "jsc:screenfetch";
    private static final String SCREENFETCH_PORT = "/usr/ports/sysutils/screenfetch";
    private static final String SCREENFETCH_BUILT = "usr/ports/sysutils/screenfetch/work/"
            + ".build_done.screenfetch._usr_local";
    private static final String VIM = "jsc:vim";
    private static final String VIM_PORT = "/usr/ports/editors/vim";
    /**
     * What the ports say when they will not build a port here, a Mainframe's own services among them: those come with
     * it, and its ports find them installed.
     */
    private static final List<String> REFUSALS = List.of("is marked as broken", "cannot install",
            "No space left on device", "is already installed");

    private PortsMatrixGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void legacy_everyPortIsBuiltOrRefusedOnBothMachines(final GameTestHelper helper) {
        everyPort(helper, EraKit.LEGACY);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void transition_everyPortIsBuiltOrRefusedOnBothMachines(final GameTestHelper helper) {
        everyPort(helper, EraKit.TRANSITION);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void standard_everyPortIsBuiltOrRefusedOnBothMachines(final GameTestHelper helper) {
        everyPort(helper, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void advanced_everyPortIsBuiltOrRefusedOnBothMachines(final GameTestHelper helper) {
        everyPort(helper, EraKit.ADVANCED);
    }

    /**
     * make alone builds and installs nothing; install installs and leaves the build; a second install is refused;
     * reinstall installs again; deinstall takes the program off; clean takes the build. A package installed first
     * leaves the port nothing to install, and a port built first leaves the package nothing to install.
     */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void standard_targetsDoWhatTheyNameAndPortsMeetPackagesEitherWay(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        onFreeBsd(helper, network, computer).thenExecute(() -> {
            final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
            hand.type("portsnap fetch extract");
            hand.type("cd " + SCREENFETCH_PORT);
            hand.yes("make");
            check(helper, hand, DiskFilesystem.exists(computer.systemDisk(), SCREENFETCH_BUILT)
                    && !installed(computer, SCREENFETCH), "make builds and installs nothing");
            hand.yes("make install");
            check(helper, hand, installed(computer, SCREENFETCH)
                    && DiskFilesystem.exists(computer.systemDisk(), SCREENFETCH_BUILT),
                    "make install installs and leaves the build");
            check(helper, hand, hand.yes("make install").contains("is already installed"),
                    "a second install is refused");
            hand.yes("make reinstall");
            check(helper, hand, installed(computer, SCREENFETCH)
                    && computer.console().builtFromSource(SCREENFETCH), "reinstall installs it again, built here");
            check(helper, hand, hand.yes("make deinstall").contains("Deinstalling for screenfetch")
                    && !installed(computer, SCREENFETCH), "deinstall takes the program off");
            hand.yes("make clean");
            check(helper, hand, !DiskFilesystem.exists(computer.systemDisk(), SCREENFETCH_BUILT),
                    "clean takes the build away");
            // The package first, then the port of the same program.
            hand.type("cd /");
            hand.yes("pkg install vim");
            check(helper, hand, installed(computer, VIM) && !computer.console().builtFromSource(VIM),
                    "pkg installs vim as a package");
            hand.type("cd " + VIM_PORT);
            check(helper, hand, hand.yes("make install clean").contains("is already installed"),
                    "the port finds the package's program already there");
            // The port first, then the package of the same program.
            hand.type("cd " + SCREENFETCH_PORT);
            hand.yes("make install clean");
            hand.type("cd /");
            hand.yes("pkg install screenfetch");
            check(helper, hand, installed(computer, SCREENFETCH)
                    && computer.console().builtFromSource(SCREENFETCH),
                    "the package leaves the program the port built as it was");
        }).thenSucceed();
    }

    /** A disk with no room left takes no tree, and says so. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void aFullDisk_takesNoTreeAndSaysSo(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        onFreeBsd(helper, network, computer).thenExecute(() -> {
            // Filled with stone by the item, and what is left under an item with water by the millibucket.
            final long free = DriveTable.freeWeightOf(computer.systemDisk());
            computer.localStore().insert(StorageKey.of(Items.COBBLESTONE), free / StorageKey.MB_EQ_PER_ITEM);
            computer.localStore().insert(StorageKey.of(new FluidStack(Fluids.WATER, 1)),
                    DriveTable.freeWeightOf(computer.systemDisk()));
            helper.assertTrue(DriveTable.freeWeightOf(computer.systemDisk()) == 0L, "the disk is full: "
                    + DriveTable.freeWeightOf(computer.systemDisk()) + " left");
            final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
            final String said = hand.type("portsnap fetch extract");
            check(helper, hand, said.contains("No space left on device"), "a full disk takes no tree");
        }).thenSucceed();
    }

    /** Formatting the system disk takes the tree and what was built from it with the system. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void format_takesTheTreeAndTheBuildsWithTheSystem(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        onFreeBsd(helper, network, computer).thenExecute(() -> {
            final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
            hand.type("portsnap fetch extract");
            hand.type("cd " + SCREENFETCH_PORT);
            hand.yes("make install clean");
            check(helper, hand, computer.console().builtFromSource(SCREENFETCH), "screenfetch is built here");
            computer.togglePower();
            helper.assertTrue(computer.formatDisk(0), "the system disk formats");
            helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD goes back on it");
            computer.togglePower();
            helper.assertFalse(installed(computer, SCREENFETCH) || computer.console().builtFromSource(SCREENFETCH),
                    "the format took the program and its build");
            helper.assertFalse(DiskFilesystem.exists(computer.systemDisk(), INDEX), "and the tree");
        }).thenSucceed();
    }

    /* Every port on the era's network's computer and on a Mainframe of its own, both running FreeBSD. */
    private static void everyPort(final GameTestHelper helper, final EraKit kit) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), kit);
        final PersonalComputerBlockEntity computer = network.computer();
        final MainframeBlockEntity lone = lone(helper, kit);
        onFreeBsd(helper, network, computer)
                .thenWaitUntil(() -> helper.assertTrue(lone.networkUuid() != null && lone.isMirrorActive(),
                        "the lone Mainframe to serve its own Mirror"))
                .thenExecute(() -> {
                    final List<String> failures = new ArrayList<>();
                    failures.addAll(ports(new TerminalHand(computer, helper.getLevel()), kit + " pc"));
                    failures.addAll(ports(new TerminalHand(lone, helper.getLevel()), kit + " mainframe"));
                    helper.assertTrue(failures.isEmpty(), failures.size() + " failed: " + String.join(" | ", failures));
                }).thenSucceed();
    }

    /* The tree fetched and laid out, then every port in its index built and installed, or refused. */
    private static List<String> ports(final TerminalHand hand, final String where) {
        final List<String> failures = new ArrayList<>();
        final AbstractComputerBlockEntity machine = hand.machine();
        hand.type("portsnap fetch extract");
        final String index = DiskFilesystem.read(machine.systemDisk(), INDEX).orElse("");
        if (index.isBlank()) {
            failures.add(where + ": no tree was laid out:\n" + hand.transcript(TRANSCRIPT));
            return failures;
        }
        final CliShell shell = CliCommands.newShell(ShellFamily.POSIX, WIDTH);
        for (final String line : index.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            final String dir = line.split("\\|")[1];
            final String name = dir.substring(dir.lastIndexOf('/') + 1);
            final ProgramSpec spec = programOf(name);
            hand.type("cd " + dir);
            final String made = hand.yes("make install clean");
            final boolean tooNew = spec != null && machine.displayEra() != null
                    && !OsGating.canInstall(spec.minEra(), machine.displayEra());
            if (spec == null || !installed(machine, spec.id().toString())) {
                if (REFUSALS.stream().noneMatch(made::contains)) {
                    failures.add(where + ": " + name + " was neither installed nor refused:\n"
                            + hand.transcript(TRANSCRIPT));
                } else if (tooNew && !made.contains("is marked as broken")) {
                    failures.add(where + ": " + name + " is too new for the machine, and is not marked as broken: "
                            + made.strip());
                }
                continue;
            }
            if (tooNew) {
                failures.add(where + ": " + name + " went onto a machine it is too new for");
            }
            if (!machine.console().builtFromSource(spec.id().toString())) {
                failures.add(where + ": " + name + " is not recorded as built here");
            }
            if (DiskFilesystem.exists(machine.systemDisk(), dir.substring(1) + "/work")) {
                failures.add(where + ": clean left " + name + "'s work folder");
            }
            if (shell.find(name) != null) {
                final String ran = hand.glance(name);
                if (ran.contains("command not found")) {
                    failures.add(where + ": " + name + " is built but its command is not found: " + ran.strip());
                }
                // Whatever the program left in front of the terminal is put away before the next one.
                machine.console().foreground().clear();
            }
        }
        hand.type("cd /");
        return failures;
    }

    /*
     * The network's computer switched off, its disk wiped and FreeBSD put on it, switched on again, with the Mirror
     * on the network's Mainframe; the sequence goes on once FreeBSD is up.
     */
    private static GameTestSequence onFreeBsd(final GameTestHelper helper, final EraNetwork network,
                                              final PersonalComputerBlockEntity computer) {
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        "the computer is up on the network"))
                .thenExecute(() -> {
                    network.mainframe().installMirror();
                    computer.togglePower();
                    computer.formatDisk(0);
                    helper.assertTrue(computer.installOs(FREEBSD), "FreeBSD installs on the computer");
                    computer.togglePower();
                })
                .thenExecuteFor(START_WITHIN / 10, () -> { })
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning()
                        && FREEBSD.equals(computer.installedOsId()), "the computer starts FreeBSD"));
    }

    /* A Mainframe of the era standing alone, switched on running FreeBSD and serving its own Mirror. */
    private static MainframeBlockEntity lone(final GameTestHelper helper, final EraKit kit) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(LONE_MAINFRAME, kit.mainframe());
        final MainframeBlockEntity mainframe = world.blockEntity(LONE_MAINFRAME, MainframeBlockEntity.class);
        final ItemStackHandler parts = mainframe.getInventory();
        final Item board = kit.mainframeBoard();
        parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(EraParts.cpuFor(board)));
        parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        parts.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START, new ItemStack(EraParts.diskFor(kit.era())));
        mainframe.togglePower();
        if (!mainframe.installOs(FREEBSD)) {
            throw new IllegalStateException("the lone " + kit + " Mainframe took no FreeBSD");
        }
        mainframe.installMirror();
        return mainframe;
    }

    /* The program a port of that name builds, or null when none goes by it. */
    private static ProgramSpec programOf(final String name) {
        for (final ProgramSpec spec : Programs.all()) {
            if (spec.commandName().equalsIgnoreCase(name)) {
                return spec;
            }
        }
        return null;
    }

    private static boolean installed(final AbstractComputerBlockEntity machine, final String id) {
        return machine.console() != null && machine.console().isInstalled(id);
    }

    /* Asserts the claim, showing what the terminal said on the way when it does not hold. */
    private static void check(final GameTestHelper helper, final TerminalHand hand, final boolean holds,
                              final String claim) {
        helper.assertTrue(holds, claim + "; the terminal went\n" + hand.transcript(TRANSCRIPT));
    }
}
