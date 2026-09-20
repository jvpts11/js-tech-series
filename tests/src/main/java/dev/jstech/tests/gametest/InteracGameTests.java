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

    /** The glance the program opens with, and the DOS family's way of asking what it does. */
    @GameTest(template = ARENA)
    public static void status_andHelp_answerOnAMachineOfEitherFamily(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> glance = shell(helper, fleet.lab(), "interac");
                    helper.assertTrue(says(glance, "Network") && says(glance, "Mainframe")
                                    && says(glance, "Stored"),
                            "the glance says whose network it is and what it holds; got " + glance);

                    final List<String> dos = shell(helper, fleet.lab(), "INTERAC /?");
                    helper.assertTrue(says(dos, "Works the data network"),
                            "and the DOS switch asks what it does, in any case; got " + dos);

                    final List<String> sorted = shell(helper, fleet.lab(), "INTERAC LIST LOG /S:COUNT");
                    helper.assertTrue(says(sorted, "Oak Log"), "a DOS switch sorts the listing; got " + sorted);
                })
                .thenSucceed();
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
