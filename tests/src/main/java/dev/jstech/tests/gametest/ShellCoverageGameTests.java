/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The shell commands whose machine side nothing else exercised, run on a real network the way a player types them:
 * finding and holding stock, describing the machine and the network, the Mainframe's services, the other machines
 * a remote shell reaches, and the Frames package manager's update; and the machine's shell answering every member of
 * what a command reaches itself.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShellCoverageGameTests {

    private ShellCoverageGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    /** A program that names Sound, which came in the second version of Σ#. */
    private static final String BEEPER = "using System.IO.*; using System.Sound.*; namespace T; "
            + "class Beeper { static void Main() { Sound.Beep(880, 200); } }";

    /** The Mainframe with a rack holding 640 oak logs, and two personal computers off one router. */
    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity lab,
                         PersonalComputerBlockEntity desk) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(2, 2, 1));
        rack.getServerStorage(0).insert(Items.OAK_LOG, 640);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity desk = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        desk.console().setComputerName("desk");
        return new Fleet(mainframe, lab, desk);
    }

    /*
     * Every member of what a command reaches has a default that answers as a computer without that part would, so a
     * computer made for a test writes only what its commands use. The machine's own shell must never be one that
     * leans on such a default, or a command would quietly be told the machine has nothing.
     */
    @GameTest(template = ARENA)
    public static void serverShell_answersEveryMemberOfWhatACommandReachesItself(final GameTestHelper helper) {
        final List<String> leaning = new ArrayList<>();
        for (final Method member : ICliComputer.class.getMethods()) {
            if (Modifier.isStatic(member.getModifiers())) {
                continue;
            }
            try {
                final Method answered = ServerCliComputer.class.getMethod(member.getName(), member.getParameterTypes());
                if (answered.getDeclaringClass() != ServerCliComputer.class) {
                    leaning.add(member.getName());
                }
            } catch (final NoSuchMethodException missing) {
                leaning.add(member.getName());
            }
        }
        helper.assertTrue(leaning.isEmpty(), "the machine's shell answers every member itself; it leans on " + leaning);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void storage_findLockAndUnlockThroughTheShell(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> found = shell(helper, fleet.lab(), "interac where minecraft:oak_log");
                    helper.assertTrue(says(found, "640"), "it names where the 640 logs are; got " + found);

                    final List<String> locked = shell(helper, fleet.lab(), "interac lock minecraft:oak_log");
                    helper.assertTrue(says(locked, "LOCK held") && says(locked, "Oak Log"),
                            "lock holds the logs; got " + locked);
                    final List<String> held = shell(helper, fleet.lab(), "interac locks");
                    helper.assertTrue(says(held, "Oak Log"), "locks lists them; got " + held);

                    final List<String> released = shell(helper, fleet.lab(), "interac unlock minecraft:oak_log");
                    helper.assertTrue(says(released, "UNLOCK released"), "unlock lets them go; got " + released);
                    final List<String> none = shell(helper, fleet.lab(), "interac locks");
                    helper.assertTrue(says(none, "no items are locked"), "and nothing is held after; got " + none);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void machine_netProgramsAndDevicesDescribeIt(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        // A monitor against the lab computer, which links to it as a peripheral.
        TestWorldBuilder.forGameTest(helper).placeMonitor(new BlockPos(6, 2, 2), Direction.EAST);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> net = shell(helper, fleet.lab(), "net");
                    helper.assertTrue(says(net, "mainframe") && says(net, "present") && says(net, "servers"),
                            "net summarises the network with its Mainframe; got " + net);
                    final List<String> programs = shell(helper, fleet.lab(), "programs");
                    helper.assertTrue(says(programs, "cmd"), "programs lists what the system ships; got " + programs);
                })
                .thenWaitUntil(() -> helper.assertTrue(says(shell(helper, fleet.lab(), "devices"), " @ "),
                        "devices lists the linked monitor"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void services_iqlEngineAndMirrorAnswerThroughTheShell(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    // The engine comes with the Mainframe; the Mirror is put on by hand.
                    fleet.mainframe().installMirror();

                    final List<String> elsewhere = shell(helper, fleet.lab(), "iqlengine status");
                    helper.assertTrue(says(elsewhere, "command not found"),
                            "a service of the network is worked from the machine that runs it; got " + elsewhere);

                    final List<String> status = shell(helper, fleet.mainframe(), "iqlengine status");
                    helper.assertTrue(says(status, "Midsoft IQL Server: running"), "the engine runs; got " + status);
                    final List<String> stopped = shell(helper, fleet.mainframe(), "iqlengine stop");
                    helper.assertTrue(says(stopped, "Midsoft IQL Server stopped"), "it stops; got " + stopped);
                    final List<String> started = shell(helper, fleet.mainframe(), "iqlengine start");
                    helper.assertTrue(says(started, "Midsoft IQL Server started"), "and starts again; got " + started);

                    final List<String> services = shell(helper, fleet.mainframe(), "services");
                    helper.assertTrue(says(services, "Midsoft IQL Server") && says(services, "running")
                                    && says(services, "Mirror") && says(services, "serving"),
                            "services lists both with their state; got " + services);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void remote_sshListsTheOtherMachinesAndHostnameIsTheName(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> hosts = shell(helper, fleet.lab(), "ssh");
                    helper.assertTrue(says(hosts, "Reachable hosts:") && says(hosts, "desk"),
                            "ssh with no host lists the other machines; got " + hosts);
                    final String hostname = new ServerCliComputer(fleet.lab(), helper.getLevel()).hostname();
                    helper.assertTrue("lab".equals(hostname), "the host name is the computer's name; got " + hostname);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void packages_pckmgrUpdateAnswersThroughTheMirror(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    fleet.mainframe().installMirror();
                    final List<String> updated = shell(helper, fleet.lab(), "pckmgr update");
                    helper.assertTrue(!says(updated, "could not resolve")
                                    && (says(updated, "up to date") || says(updated, "Updated")),
                            "pckmgr update reaches the Mirror and reports; got " + updated);
                })
                .thenSucceed();
    }

    /**
     * The version of the language lives in the compiler's package: a machine whose sgsc is still 1.0 knows Σ# 1 and
     * is refused what came in 2, and an upgrade through the Mirror brings the compiler, and with it the language, up.
     */
    @GameTest(template = ARENA)
    public static void packages_upgradeBringsTheCompilerUpToTheNewerLanguage(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    fleet.lab().console().install(SigmaCommands.COMPILER);
                    fleet.lab().console().setInstalledVersion(SigmaCommands.COMPILER, "1.0");
                    new ServerCliComputer(fleet.lab(), helper.getLevel()).writeFile("Beeper.sgs", BEEPER);
                    final List<String> old = shell(helper, fleet.lab(), "sgsc Beeper.sgs");
                    helper.assertTrue(says(old, "Σ# Compiler 1.0") && says(old, "'Sound' needs Σ# 2"),
                            "an sgsc of 1.0 knows Σ# 1; got " + old);
                    fleet.mainframe().installMirror();
                    final List<String> upgraded = shell(helper, fleet.lab(), "pckmgr upgrade");
                    helper.assertTrue(says(upgraded, "Setting up sgsc (2.0)"),
                            "the upgrade brings the compiler to 2.0; got " + upgraded);
                    final List<String> built = shell(helper, fleet.lab(), "sgsc Beeper.sgs");
                    helper.assertTrue(says(built, "Σ# Compiler 2.0") && says(built, "wrote Beeper.asm"),
                            "and the program builds with Σ# 2; got " + built);
                })
                .thenSucceed();
    }

    /** The upgrade verb of a distribution's own manager does the same, where its update verb only reads the lists. */
    @GameTest(template = ARENA)
    public static void packages_aptUpgradeBringsPackagesUpAndAptUpdateOnlyReads(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(new BlockPos(4, 2, 4), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity linux = world.placeRunningPersonalComputer(new BlockPos(5, 2, 4),
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu"));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    fleet.mainframe().installMirror();
                    linux.console().install(SigmaCommands.COMPILER);
                    linux.console().setInstalledVersion(SigmaCommands.COMPILER, "1.0");
                    final ServerCliComputer cli = new ServerCliComputer(linux, helper.getLevel());
                    final CliShell posix = CliCommands.shellFor(cli, 80);
                    posix.run("apt update", cli);
                    helper.assertTrue("1.0".equals(linux.console().installedVersion(SigmaCommands.COMPILER)),
                            "apt update reads the lists and installs nothing");
                    final List<CliLine> upgraded = posix.run("apt upgrade", cli).lines();
                    helper.assertTrue("2.0".equals(linux.console().installedVersion(SigmaCommands.COMPILER)),
                            "apt upgrade brings sgsc up; got " + upgraded.stream().map(CliLine::text).toList());
                })
                .thenSucceed();
    }

    /**
     * What a machine prints fits the glass it was told it is writing for.
     *
     * <p>A terminal window on a desktop is narrower than a monitor, and it tells the machine so. A listing
     * laid out for a monitor and read in such a window folds every line in half, which is what a directory
     * listing looked like until the width travelled with the line. So each of these is asked for on a narrow
     * glass and has to come back fitting it.
     */
    @GameTest(template = ARENA)
    public static void aListing_fitsTheGlassItWasWrittenFor(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    for (final String command : new String[] {"dir", "dir C:\\", "mem", "tasklist", "tree"}) {
                        narrow(helper, fleet.lab(), command);
                    }
                })
                .thenSucceed();
    }

    /** The same for the other family, whose listings are its own. */
    @GameTest(template = ARENA)
    public static void aListing_fitsTheGlassOnTheOtherFamilyToo(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    fleet.desk().installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian"));
                    for (final String command : new String[] {"ls -l", "ls -l /", "df", "ps"}) {
                        narrow(helper, fleet.desk(), command);
                    }
                })
                .thenSucceed();
    }

    /** How wide a terminal window on a desktop is, which is narrower than a monitor and the case that broke. */
    private static final int NARROW = 52;

    /** Runs a command on a narrow glass and fails on the first line that runs past it. */
    private static void narrow(final GameTestHelper helper, final IComputerTerminalHost on,
                               final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        for (final CliLine line : CliCommands.newShell(NARROW).run(command, computer).lines()) {
            helper.assertTrue(line.text().length() <= NARROW,
                    "'" + command + "' wrote " + line.text().length() + " columns onto a glass of "
                            + NARROW + ": [" + line.text() + "]");
        }
    }

    private static List<String> shell(final GameTestHelper helper, final IComputerTerminalHost on,
                                      final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        final List<String> out = new ArrayList<>();
        for (final CliLine line : CliCommands.newShell(80).run(command, computer).lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }
}
