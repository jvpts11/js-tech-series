/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The shell commands whose machine side nothing else exercised, run on a real network the way a player types them:
 * finding and holding stock, describing the machine and the network, the Mainframe's services, the other machines
 * a remote shell reaches, and the Frames package manager's update.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShellCoverageGameTests {

    private ShellCoverageGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;

    /** The Mainframe with a rack holding 640 oak logs, and two personal computers off one router. */
    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity lab,
                         PersonalComputerBlockEntity desk) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(2, 2, 1));
        rack.getServerStorage(0).insert(Items.OAK_LOG, 640);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity desk = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        desk.console().setComputerName("desk");
        return new Fleet(mainframe, lab, desk);
    }

    @GameTest(template = ARENA)
    public static void storage_findLockAndUnlockThroughTheShell(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> found = shell(helper, fleet.lab(), "find minecraft:oak_log");
                    helper.assertTrue(says(found, "640"), "find names where the 640 logs are; got " + found);

                    final List<String> locked = shell(helper, fleet.lab(), "lock minecraft:oak_log");
                    helper.assertTrue(says(locked, "LOCK held") && says(locked, "Oak Log"),
                            "lock holds the logs; got " + locked);
                    final List<String> held = shell(helper, fleet.lab(), "locks");
                    helper.assertTrue(says(held, "Oak Log"), "locks lists them; got " + held);

                    final List<String> released = shell(helper, fleet.lab(), "unlock minecraft:oak_log");
                    helper.assertTrue(says(released, "UNLOCK released"), "unlock lets them go; got " + released);
                    final List<String> none = shell(helper, fleet.lab(), "locks");
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
                    helper.assertTrue(fleet.mainframe().installIqlEngine(), "the IQL Engine installs on the Mainframe");
                    fleet.mainframe().installMirror();

                    final List<String> status = shell(helper, fleet.lab(), "iqlengine status");
                    helper.assertTrue(says(status, "IQL Engine: running"), "the engine runs; got " + status);
                    final List<String> stopped = shell(helper, fleet.lab(), "iqlengine stop");
                    helper.assertTrue(says(stopped, "IQL Engine stopped"), "it stops; got " + stopped);
                    final List<String> started = shell(helper, fleet.lab(), "iqlengine start");
                    helper.assertTrue(says(started, "IQL Engine started"), "and starts again; got " + started);

                    final List<String> services = shell(helper, fleet.lab(), "services");
                    helper.assertTrue(says(services, "IQL Engine") && says(services, "running")
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

    private static List<String> shell(final GameTestHelper helper, final PersonalComputerBlockEntity on,
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
