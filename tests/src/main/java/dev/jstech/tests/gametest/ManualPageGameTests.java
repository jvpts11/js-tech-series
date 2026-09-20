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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One body of text about a command, read by every way a player can ask.
 *
 * <p>What is held to account here is that they really are one: {@code man} on a Unix system and {@code /?} on
 * a DOS one answer with the same words, and neither of them will teach a command the machine in front of the
 * player cannot run.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ManualPageGameTests {

    private ManualPageGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    /**
     * A machine on a real network, because what a manual has a page for is what the machine can run, and half
     * of what these computers can run is only there when there is a network under them.
     */
    private static PersonalComputerBlockEntity machine(final GameTestHelper helper,
                                                       final ResourceLocation system) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        computer.installOs(system);
        return computer;
    }

    @GameTest(template = ARENA)
    public static void man_printsThePageTheCommandItselfWrites(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> page = shell(helper, computer, "man grep");
                    helper.assertTrue(says(page, "NAME") && says(page, "SYNOPSIS") && says(page, "DESCRIPTION")
                                    && says(page, "OPTIONS") && says(page, "EXAMPLES") && says(page, "SEE ALSO"),
                            "a real page has all its parts; got " + page);
                    helper.assertTrue(says(page, "-v") && says(page, "the lines that do NOT hold it"),
                            "the options are the command's own words; got " + page);
                })
                .thenSucceed();
    }

    /** The same words, asked for the way the other family asks. */
    @GameTest(template = ARENA)
    public static void aDosSwitch_answersWithTheSamePage(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> page = shell(helper, computer, "INTERAC /?");
                    helper.assertTrue(says(page, "Description") && says(page, "without writing a line of IQL"),
                            "the DOS switch prints the command's own page; got " + page);
                    helper.assertTrue(says(page, "Examples") && says(page, "interac put hand"),
                            "examples and all; got " + page);

                    final List<String> helped = shell(helper, computer, "help interac");
                    helper.assertTrue(says(helped, "without writing a line of IQL"),
                            "and so does help, which is the same question in other words; got " + helped);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void apropos_findsACommandByWhatItIsFor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> found = shell(helper, computer, "apropos network");
                    helper.assertTrue(says(found, "interac"),
                            "a word finds the commands whose summary holds it; got " + found);

                    final List<String> nothing = shell(helper, computer, "apropos zzzzz");
                    helper.assertTrue(says(nothing, "nothing appropriate"),
                            "and a word nothing answers to says so; got " + nothing);

                    final List<String> one = shell(helper, computer, "whatis grep");
                    helper.assertTrue(says(one, "grep (1)"), "whatis answers in one line; got " + one);
                })
                .thenSucceed();
    }

    /**
     * The manual never teaches what the machine cannot run, which is the same filter the prompt uses to decide
     * whether a word is a command at all.
     */
    @GameTest(template = ARENA)
    public static void man_hasNoPageForACommandThisMachineDoesNotHave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> dos = shell(helper, computer, "man cls");
                    helper.assertTrue(says(dos, "No manual entry"),
                            "cls belongs to the other family, so this machine has no page for it; got " + dos);
                })
                .thenSucceed();
    }

    /** listcmd is nowhere at all until a server turns it on, which is how it ships. */
    @GameTest(template = ARENA)
    public static void listcmd_isNotThereUnlessAServerTurnsItOn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> typed = shell(helper, computer, "listcmd");
                    helper.assertTrue(says(typed, "command not found"),
                            "off, typing it is an unknown word; got " + typed);
                    final List<String> listed = shell(helper, computer, "apropos everything");
                    helper.assertTrue(!says(listed, "listcmd"), "and it is in no list either; got " + listed);
                })
                .thenSucceed();
    }

    private static List<String> shell(final GameTestHelper helper, final PersonalComputerBlockEntity on,
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
