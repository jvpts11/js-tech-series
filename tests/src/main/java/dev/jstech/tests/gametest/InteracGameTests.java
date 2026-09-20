/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.interac.InteracScreen;
import dev.jstech.computers.program.cli.interac.InteracState;
import dev.jstech.computers.program.cli.interac.InteracView;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network at the prompt, on a real network: what it holds, where, what a thing is, and the holds put on
 * it, each asked for the way a player types it.
 *
 * <p>What is held to account here is the half that needs the world: that a name a person writes finds the
 * thing the network is really holding, that a name fitting several is answered with the several, and that the
 * words that change something reach the same services the graphical program reaches.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class InteracGameTests {

    private InteracGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final int WIDTH = 80;
    private static final int HEIGHT = 19;

    /** The Mainframe with a rack holding logs of two kinds, and a personal computer to type at. */
    private record Fleet(PersonalComputerBlockEntity lab, ServerRackBlockEntity rack) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(2, 2, 1));
        rack.getServerStorage(0).insert(Items.OAK_LOG, 640);
        rack.getServerStorage(0).insert(Items.SPRUCE_LOG, 128);
        // Well inside what one seeded server holds, so the count read back is the count put in.
        rack.getServerStorage(0).insert(Items.COBBLESTONE, 2048);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        lab.console().setComputerName("lab");
        return new Fleet(lab, rack);
    }

    @GameTest(template = ARENA)
    public static void list_showsWhatTheNetworkHoldsAndNarrowsToWhatWasAskedFor(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> all = shell(helper, fleet.lab(), "interac list");
                    helper.assertTrue(says(all, "Cobblestone") && says(all, "2,048"),
                            "a listing names what the network holds, with its count; got " + all);

                    final List<String> logs = shell(helper, fleet.lab(), "interac list log");
                    helper.assertTrue(says(logs, "Oak Log") && says(logs, "Spruce Log")
                                    && !says(logs, "Cobblestone"),
                            "and a word narrows it to what matches; got " + logs);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void where_namesTheServerHoldingIt(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> where = shell(helper, fleet.lab(), "interac where oak log");
                    helper.assertTrue(says(where, "Oak Log") && says(where, "640"),
                            "where says how much there is in all; got " + where);
                    helper.assertTrue(where.size() > 1, "and names the server holding it; got " + where);
                })
                .thenSucceed();
    }

    /**
     * A name that fits several things is not guessed at. The player is shown what it fits, so the next line
     * they type names the one they meant, which is the whole reason the answer is a question.
     */
    @GameTest(template = ARENA)
    public static void aNameThatFitsSeveralThings_isAnsweredWithTheSeveral(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> asked = shell(helper, fleet.lab(), "interac where log");
                    helper.assertTrue(says(asked, "fits") && says(asked, "Oak Log") && says(asked, "Spruce Log"),
                            "it says what the name fits rather than picking one; got " + asked);

                    final List<String> exact = shell(helper, fleet.lab(), "interac where minecraft:oak_log");
                    helper.assertTrue(says(exact, "640") && !says(exact, "fits"),
                            "and a name written in full is that thing and no question; got " + exact);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void info_saysWhatAThingIsAndHowMuchThereIs(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> info = shell(helper, fleet.lab(), "interac info cobblestone");
                    helper.assertTrue(says(info, "Cobblestone") && says(info, "minecraft:cobblestone")
                                    && says(info, "2,048"),
                            "info names it, both ways, with how much there is; got " + info);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void lock_holdsWhatWasNamedAndUnlockLetsItGo(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    shell(helper, fleet.lab(), "interac lock oak log");
                    final List<String> held = shell(helper, fleet.lab(), "interac locks");
                    helper.assertTrue(says(held, "Oak Log"), "what was locked is listed as held; got " + held);

                    shell(helper, fleet.lab(), "interac unlock oak log");
                    final List<String> none = shell(helper, fleet.lab(), "interac locks");
                    helper.assertTrue(says(none, "no items are locked"), "and unlock lets it go; got " + none);
                })
                .thenSucceed();
    }

    /**
     * Taking something into your own hands, and leaving it in the machine instead.
     *
     * <p>A shell built with nobody typing at it is what a session opened on another machine is, so it is also
     * how this holds the rule to account: there is nowhere to put anything, and it says so rather than
     * reaching across the world for a player.
     */
    @GameTest(template = ARENA)
    public static void get_needsSomebodyAtTheMachineUnlessItStaysThere(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> nobody = shell(helper, fleet.lab(), "interac get 42 cobblestone");
                    helper.assertTrue(says(nobody, "nobody is at this machine"),
                            "with nobody typing there is nowhere to put it; got " + nobody);

                    final List<String> local = shell(helper, fleet.lab(), "interac get 42 cobblestone --to local");
                    helper.assertTrue(says(local, "SELECT queued") && says(local, "local storage"),
                            "and asking for it to stay in the machine works from anywhere; got " + local);

                    final List<String> dos = shell(helper, fleet.lab(), "INTERAC GET 42 COBBLESTONE /LOCAL");
                    helper.assertTrue(says(dos, "SELECT queued"),
                            "the DOS switch says the same thing; got " + dos);
                })
                .thenSucceed();
    }

    /** Handing something over needs somebody to take it from, the same way. */
    @GameTest(template = ARENA)
    public static void put_hand_needsSomebodyHoldingSomething(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> nobody = shell(helper, fleet.lab(), "interac put hand");
                    helper.assertTrue(says(nobody, "nobody is at this machine"),
                            "there is nobody to take anything from; got " + nobody);

                    final List<String> named = shell(helper, fleet.lab(), "interac put 8 oak log");
                    helper.assertTrue(says(named, "holds no") || says(named, "INSERT queued"),
                            "and naming something puts it in from the machine's own storage; got " + named);
                })
                .thenSucceed();
    }

    /** Starring belongs to the computer, so what the prompt stars the window shows, and the other way about. */
    @GameTest(template = ARENA)
    public static void fav_starsAThingOnTheComputerItself(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> none = shell(helper, fleet.lab(), "interac fav");
                    helper.assertTrue(says(none, "nothing is starred"), "nothing is starred yet; got " + none);

                    shell(helper, fleet.lab(), "interac fav cobblestone");
                    helper.assertTrue(fleet.lab().console().settings().favourites()
                                    .contains("item|minecraft:cobblestone"),
                            "the computer itself is what remembers it; it has "
                                    + fleet.lab().console().settings().favourites());

                    final List<String> listed = shell(helper, fleet.lab(), "interac fav");
                    helper.assertTrue(says(listed, "minecraft:cobblestone"), "and it lists it; got " + listed);

                    shell(helper, fleet.lab(), "interac fav cobblestone");
                    helper.assertTrue(fleet.lab().console().settings().favourites().isEmpty(),
                            "saying it again takes the star off");
                })
                .thenSucceed();
    }

    /** The glance the program opens with, and the DOS family's way of asking what it does. */
    @GameTest(template = ARENA)
    public static void status_andHelp_answerOnAMachineOfEitherFamily(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> glance = shell(helper, fleet.lab(), "interac status");
                    helper.assertTrue(says(glance, "Network") && says(glance, "Mainframe")
                                    && says(glance, "Stored"),
                            "the glance says whose network it is and what it holds; got " + glance);

                    final List<String> dos = shell(helper, fleet.lab(), "INTERAC /?");
                    helper.assertTrue(says(dos, "work the data network"),
                            "and the DOS switch asks what it does, in any case; got " + dos);

                    final List<String> sorted = shell(helper, fleet.lab(), "INTERAC LIST LOG /S:COUNT");
                    helper.assertTrue(says(sorted, "Oak Log"), "a DOS switch sorts the listing; got " + sorted);
                })
                .thenSucceed();
    }

    /** The word on its own gives the terminal to the full-screen view, which the machine draws. */
    @GameTest(template = ARENA)
    public static void view_takesTheTerminalAndIsDrawnByTheMachine(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer computer = new ServerCliComputer(fleet.lab(), helper.getLevel());
                    final CliShell.HandOver over =
                            CliCommands.shellFor(computer, WIDTH).run("interac", computer).handOver();
                    helper.assertTrue(over != null, "the word on its own gives the terminal away");
                    helper.assertTrue("interac".equals(over.editor()) && InteracState.names(over.path()),
                            "and names the view and where it opens; got " + over);

                    final List<String> screen = InteracView.screen(computer, opening());
                    helper.assertTrue(screen.size() == HEIGHT,
                            "a screen is a glass's worth of rows; got " + screen.size());
                    helper.assertTrue(says(screen, "[Network]") && says(screen, "Oak Log")
                                    && says(screen, "Cobblestone"),
                            "the rows come off the network itself; got " + screen);
                    helper.assertTrue(says(screen, "Mainframe up"),
                            "and the bar says how the network is; got " + screen.get(0));

                    final List<String> logs = InteracView.screen(computer, opening().searchingFor("log"));
                    helper.assertTrue(says(logs, "Oak Log") && !says(logs, "Cobblestone"),
                            "a search narrows the list; got " + logs);
                    helper.assertTrue(InteracScreen.rowsSaid(logs) == 2,
                            "and the screen says how many rows there are; got "
                                    + InteracScreen.rowsSaid(logs));

                    final List<String> servers =
                            InteracView.screen(computer, opening().onTab(InteracState.TAB_SERVERS));
                    helper.assertTrue(says(servers, "[Servers]") && says(servers, "% full"),
                            "another heading is another list off the same machine; got " + servers);
                })
                .thenSucceed();
    }

    /** A key that asks for something is carried out by the machine, once, when the view is drawn. */
    @GameTest(template = ARENA)
    public static void view_carriesOutWhatWasAskedOfItExactlyOnce(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer computer = new ServerCliComputer(fleet.lab(), helper.getLevel());
                    final InteracState asked = opening().searchingFor("cobblestone")
                            .asking("lock" + InteracView.NOW, 64L);

                    final List<String> done = InteracView.screen(computer, asked);
                    helper.assertTrue(computer.locks().size() == 1,
                            "the hold asked for was really put on; got " + computer.locks());
                    helper.assertTrue(computer.locks().get(0).quantity() == 64L,
                            "for as many as were asked for; got " + computer.locks());
                    helper.assertTrue(says(done, "64"), "and the screen says so; got " + done);

                    InteracView.screen(computer, asked.done());
                    helper.assertTrue(computer.locks().get(0).quantity() == 64L,
                            "asking for the same view again does not do it a second time; got "
                                    + computer.locks());

                    final List<String> held =
                            InteracView.screen(computer, opening().onTab(InteracState.TAB_LOCKED));
                    helper.assertTrue(says(held, "[Locked]") && says(held, "Cobblestone"),
                            "and the heading for holds shows it; got " + held);
                })
                .thenSucceed();
    }

    /** The view as it opens, on a glass the size of the one these tests read. */
    private static InteracState opening() {
        return InteracState.OPENING.on(WIDTH, HEIGHT);
    }

    private static List<String> shell(final GameTestHelper helper, final IComputerTerminalHost on,
                                      final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        final List<String> out = new ArrayList<>();
        for (final CliLine line : CliCommands.shellFor(computer, WIDTH).run(command, computer).lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }
}
