/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.SetupGate;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.wan.FederationEdge;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanFederation;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static dev.jstech.tests.testkit.computers.TestShell.text;

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

    /* What the prompt of {@code cli} printed for {@code line}. */
    private static String said(final ServerCliComputer cli, final String line) {
        return text(CliCommands.shellFor(cli, 80).run(line, cli));
    }

    /*
     * Federates the hub's network with {@code far} this tick, as their WAN gateways would say it: {@code hub}'s port
     * lets {@code far} do what {@code grant} says, and {@code far}'s lets the hub's network see it.
     */
    private static void federate(final GameTestHelper helper, final MainframeBlockEntity hub,
                                 final MainframeBlockEntity far, final PortSettings grant) {
        final long now = helper.getLevel().getGameTime();
        final DataLink line = new DataLink(DataLine.ACCESS, HardwareEra.STANDARD);
        final WanFederation federation = WanFederation.of(helper.getLevel());
        federation.publish(new FederationEdge(hub.networkUuid(), far.networkUuid(), 1L, 1, 2L, 1, line, 64L, grant),
                now);
        federation.publish(new FederationEdge(far.networkUuid(), hub.networkUuid(), 2L, 1, 1L, 1, line, 64L,
                PortSettings.NEW), now);
    }

    private static Base base(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
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

    /* The kept conversations, at the service's own server */

    @GameTest(template = ARENA)
    public static void msgsvc_deletesAKeptConversationOffTheServerItem(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(MESSENGER.toString());
                    final long now = helper.getLevel().getGameTime();
                    rack.messengerAt(0).say(MessengerLog.LOBBY, "Ada", "the smelter stalled", now, false);
                    rack.messengerAt(0).say(MessengerLog.privateRoom("Ada", "Grace"), "Grace", "on it", now, false);
                    final ServerCliComputer cli = new ServerCliComputer(rack, helper.getLevel());
                    final String listed = text(CliCommands.shellFor(cli, 80).run("msgsvc list", cli));
                    helper.assertTrue(listed.contains("1  Everybody") && listed.contains("2  Ada, Grace"),
                            "msgsvc lists both conversations; got " + listed);
                    final String deleted = text(CliCommands.shellFor(cli, 80).run("msgsvc delete 2", cli));
                    helper.assertTrue(deleted.startsWith("Deleted \"Ada, Grace\": 1 line,"),
                            "and deletes the second; got " + deleted);
                    // What comes back off the item is what the server kept: the deletion was written down at once.
                    final ItemStack server = rack.getServers().extractItem(0, 1, false);
                    rack.getServers().setStackInSlot(0, server);
                    final MessengerLog back = rack.messengerAt(0);
                    helper.assertTrue(back != null && back.conversations().size() == 1
                                    && back.room(MessengerLog.privateRoom("Ada", "Grace"), 10).isEmpty(),
                            "the deleted conversation is gone from the server item too");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void msgsvc_isNoCommandOnAServerWithoutTheService(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(base.rack(), helper.getLevel());
                    final String said = text(CliCommands.shellFor(cli, 80).run("msgsvc list", cli));
                    helper.assertTrue(said.contains("not found"),
                            "a server with no Messenger Service has no msgsvc; got " + said);
                })
                .thenSucceed();
    }

    /** Knot at the prompt: a file pushed twice, its log newest first, an older revision pulled back, the status. */
    @GameTest(template = ARENA)
    public static void knot_atThePromptPushesPullsAndReadsTheLog(final GameTestHelper helper) {
        final Base base = base(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = base.rack();
                    rack.consoleOf(0).install(KNOTHUB.toString());
                    rack.consoleOf(0).install(Programs.KNOT.toString());
                    final ServerCliComputer cli = new ServerCliComputer(rack, helper.getLevel());
                    cli.writeFile("oplog.sgs", "int kept = 1;");
                    final String first = said(cli, "knot push oplog.sgs -m first");
                    helper.assertTrue(first.startsWith("revision ") && first.contains(" of oplog.sgs, by "),
                            "a push says the revision it made and who signed it; got " + first);
                    cli.writeFile("oplog.sgs", "int kept = 2;");
                    said(cli, "knot push oplog.sgs -m \"second pass\"");
                    final String log = said(cli, "knot log oplog.sgs");
                    helper.assertTrue(log.contains("second pass") && log.indexOf("second pass") < log.indexOf("first"),
                            "the log lists the newest first; got " + log);
                    final int older = rack.knotAt(0).revisionsOf("oplog.sgs").getFirst().number();
                    cli.deleteFile("oplog.sgs");
                    final String pulled = said(cli, "knot pull oplog.sgs " + older);
                    helper.assertTrue(pulled.contains("oplog.sgs at revision " + older + " written to oplog.sgs"),
                            "a pull says what it wrote where; got " + pulled);
                    helper.assertValueEqual(cli.readFile("oplog.sgs").message().english(), "int kept = 1;",
                            "the revision asked for is on the disk again");
                    final String status = said(cli, "knot status");
                    helper.assertTrue(status.contains("(this network)") && status.contains("oplog.sgs   2 revisions"),
                            "the status names the hub and what it keeps; got " + status);
                })
                .thenSucceed();
    }

    /**
     * A KnotHub across a federation: a machine of the other network reads it where the hub's network lets it see, and
     * pushes to it only where it lets it send.
     */
    @GameTest(template = ARENA)
    public static void knot_acrossAFederationReadsWhereSeenAndPushesWhereSent(final GameTestHelper helper) {
        final Base base = base(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity far = world.placeRunningMainframe(new BlockPos(1, 2, 6));
        world.setBlock(new BlockPos(2, 2, 6), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 6), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 6), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 6));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    base.rack().consoleOf(0).install(KNOTHUB.toString());
                    base.rack().knotAt(0).commit("oplog.sgs", "ada", "first", "int kept = 1;",
                            helper.getLevel().getGameTime());
                    computer.console().install(Programs.KNOT.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    federate(helper, base.mainframe(), far, new PortSettings(true, false, false, false, false, true));
                    final String status = said(cli, "knot status");
                    helper.assertTrue(status.contains("oplog.sgs   1 revision"),
                            "the other network sees what the hub keeps; got " + status);
                    cli.writeFile("oplog.sgs", "int kept = 2;");
                    final String refused = said(cli, "knot push oplog.sgs -m mine");
                    helper.assertTrue(refused.contains("does not let this one send to it"),
                            "and may not push without the right to send; got " + refused);
                    federate(helper, base.mainframe(), far, new PortSettings(true, false, true, false, false, true));
                    final String pushed = said(cli, "knot push oplog.sgs -m mine");
                    helper.assertTrue(pushed.contains(" of oplog.sgs, by "),
                            "with it, the push is made; got " + pushed);
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
