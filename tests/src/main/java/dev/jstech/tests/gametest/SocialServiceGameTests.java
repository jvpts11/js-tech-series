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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.SetupGate;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The two services whose cost follows how much they are used, on the machines that really run them.
 *
 * <p>Both belong to a server mounted in a rack, not to the network and not to the Mainframe: somebody
 * mounts the machine, installs the software and switches it on, and pulling the server takes the
 * conversations and the source history with it. The rules and the arithmetic are covered by plain JUnit
 * over {@code MessengerLog} and {@code KnotRepository}; what only a running world can answer is here.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SocialServiceGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;

    private static final ResourceLocation MESSENGER = Programs.MESSENGER_SERVICE;
    private static final ResourceLocation KNOTHUB = Programs.KNOT_HUB;

    /** A Mainframe, a cable and a rack with one server in row 0, all on one network. */
    private record Base(MainframeBlockEntity mainframe, ServerRackBlockEntity rack) {

        /** Where the network finds that program running, or null when no machine on it runs one. */
        private ServerServices.Host serving(final GameTestHelper helper, final ResourceLocation program) {
            return ServerServices.find(helper.getLevel(), mainframe.networkUuid(), program);
        }
    }

    private static Base base(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(3, 2, 2), Direction.EAST);
        // A machine with nothing on its drive takes no software at all, so the server gets a system first.
        rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
        return new Base(mainframe, rack);
    }

    /* The messenger */

    @GameTest(template = ARENA)
    public static void messenger_isNowhereOnTheNetworkUntilAServerRunsIt(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(base.serving(helper, MESSENGER) == null,
                            "a network with nothing installed offers no messenger");
                    base.rack().consoleOf(0).install(MESSENGER.toString());
                    helper.assertTrue(base.rack().hasService(0, MESSENGER),
                            "the server should be running it now");
                    helper.assertTrue(base.serving(helper, MESSENGER) != null,
                            "and the network should find it on that server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_isNotServedByAMachineSwitchedOff(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    base.rack().consoleOf(0).install(MESSENGER.toString());
                    helper.assertTrue(base.rack().unitRunning(0), "the seeded server comes up running");
                    base.rack().toggleBayPower(0);
                    helper.assertFalse(base.rack().unitRunning(0),
                            "a bay whose switch is off runs nothing");
                    helper.assertTrue(base.rack().serviceSlot(MESSENGER) < 0,
                            "and the rack offers no service off a machine that is not running");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_weighsWhatItKeepsOnTheServerHoldingIt(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    base.rack().consoleOf(0).install(MESSENGER.toString());
                    final MessengerLog log = base.rack().messengerAt(0);
                    helper.assertTrue(log != null, "a mounted server holds a messenger log");
                    final long before = log.historyBytes();
                    log.say("lobby", "ada", "the smelter stalled",
                            helper.getLevel().getGameTime(), false);
                    helper.assertTrue(log.historyBytes() > before,
                            "keeping a message should cost disk space");
                    log.connect("ada", MessengerLog.LOBBY, helper.getLevel().getGameTime());
                    log.connect("grace", MessengerLog.LOBBY, helper.getLevel().getGameTime());
                    helper.assertTrue(log.ramMb() > MessengerLog.BASE_RAM_MB,
                            "two people connected should cost more than the floor, got " + log.ramMb());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_ridesOnTheServerItemBetweenRacks(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(MESSENGER.toString());
                    rack.messengerAt(0).say("smelter", "ada", "bay three again",
                            helper.getLevel().getGameTime(), false);
                    rack.flushServices(0);
                    /*
                     * Pulled out and put back: taking the machine out drops everything the rack was holding
                     * in memory for that row, so what comes back can only have come off the item.
                     */
                    final ItemStack server = rack.getServers().extractItem(0, 1, false);
                    helper.assertFalse(server.isEmpty(), "the machine should have come out");
                    helper.assertTrue(rack.messengerAt(0) == null, "and that row should hold none");
                    rack.getServers().setStackInSlot(0, server);
                    final MessengerLog back = rack.messengerAt(0);
                    helper.assertTrue(back != null && back.size() == 1,
                            "the conversation should have travelled with the machine, found "
                                    + (back == null ? "no machine" : back.size()));
                    helper.assertTrue(back.room("smelter", 10).size() == 1,
                            "and come back in the room it was said in");
                    helper.assertTrue(rack.hasService(0, MESSENGER),
                            "the service should have travelled with it too");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_takesItsConversationsWithItWhenRemoved(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(MESSENGER.toString());
                    rack.messengerAt(0).say("lobby", "ada", "hello",
                            helper.getLevel().getGameTime(), false);
                    rack.consoleOf(0).uninstall(MESSENGER.toString());
                    rack.serviceUninstalled(0, MESSENGER);
                    helper.assertTrue(rack.messengerAt(0).size() == 0,
                            "a service nobody can reach keeps nothing");
                    helper.assertTrue(rack.messengerAt(0).historyBytes() == 0L,
                            "and it weighs nothing either");
                })
                .thenSucceed();
    }

    /* The repository */

    @GameTest(template = ARENA)
    public static void knot_isNowhereOnTheNetworkUntilAServerRunsIt(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(base.serving(helper, KNOTHUB) == null,
                            "a network with nothing installed keeps no source");
                    base.rack().consoleOf(0).install(KNOTHUB.toString());
                    helper.assertTrue(base.serving(helper, KNOTHUB) != null,
                            "the network should find it on that server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_weighsWhatItKeeps(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    base.rack().consoleOf(0).install(KNOTHUB.toString());
                    final KnotRepository repository = base.rack().knotAt(0);
                    helper.assertTrue(repository != null, "a mounted server holds a repository");
                    final long before = repository.bytes();
                    helper.assertTrue(repository.commit("plant.sgs", "ada", "first pass",
                            "int floor = 8000;", helper.getLevel().getGameTime()) != null,
                            "it should have kept that");
                    helper.assertTrue(repository.bytes() > before,
                            "keeping a revision should cost disk space");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_ridesOnTheServerItemAndKeepsItsNumbering(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(KNOTHUB.toString());
                    rack.knotAt(0).commit("plant.sgs", "ada", "first pass",
                            "int floor = 8000;", helper.getLevel().getGameTime());
                    rack.knotAt(0).commit("plant.sgs", "grace", "raise it",
                            "int floor = 10000;", helper.getLevel().getGameTime());
                    rack.flushServices(0);
                    final ItemStack server = rack.getServers().extractItem(0, 1, false);
                    rack.getServers().setStackInSlot(0, server);
                    final KnotRepository back = rack.knotAt(0);
                    helper.assertTrue(back != null && back.revisionsOf("plant.sgs").size() == 2,
                            "both revisions should have travelled with the machine, found "
                                    + (back == null ? "no machine" : back.revisionsOf("plant.sgs").size()));
                    helper.assertTrue("int floor = 10000;".equals(back.head("plant.sgs")),
                            "the newest text should have come back");
                    final KnotRepository.Revision next = back.commit("plant.sgs", "grace",
                            "three", "int floor = 12000;", helper.getLevel().getGameTime());
                    helper.assertTrue(next != null && next.number() == 3,
                            "numbering must carry on from what was read back, got "
                                    + (next == null ? "nothing" : next.number()));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_takesItsHistoryWithItWhenRemoved(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(KNOTHUB.toString());
                    rack.knotAt(0).commit("a.sgs", "ada", "one", "1",
                            helper.getLevel().getGameTime());
                    rack.consoleOf(0).uninstall(KNOTHUB.toString());
                    rack.serviceUninstalled(0, KNOTHUB);
                    helper.assertTrue(rack.knotAt(0).files().isEmpty(),
                            "a service nobody can reach keeps nothing");
                    helper.assertTrue(rack.knotAt(0).bytes() == 0L,
                            "and it weighs nothing either");
                })
                .thenSucceed();
    }

    /* Where they may be installed at all */

    @GameTest(template = ARENA)
    public static void socialServices_refuseToInstallOnAMainframe(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    for (final ResourceLocation program : List.of(MESSENGER, KNOTHUB)) {
                        final String path = program.getPath();
                        final ProgramSpec spec = OsRegistry.getProgram(program);
                        helper.assertTrue(spec != null, path + " should be a registered program");
                        helper.assertTrue(spec.hostScope() == HostScope.SERVER,
                                path + " belongs on a server in a rack");
                        helper.assertTrue(SetupGate.refusal(base.mainframe(), spec, false, true).isPresent(),
                                path + " must refuse a Mainframe");
                        helper.assertTrue(
                                SetupGate.refusal(base.rack().unitHost(0), spec, false, true).isEmpty(),
                                path + " must accept a server, said: " + SetupGate
                                        .refusal(base.rack().unitHost(0), spec, false, true)
                                        .map(said -> said.english()).orElse(""));
                    }
                })
                .thenSucceed();
    }
}
