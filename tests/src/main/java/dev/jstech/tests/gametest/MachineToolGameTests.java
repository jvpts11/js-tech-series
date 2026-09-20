/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
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
 * The small tools a person reaches for without thinking: what is running, stopping one, what the memory is
 * spent on, where a command comes from, what day it is.
 *
 * <p>Each family under its own names over one answer, which is the whole point of them: {@code ps} and
 * {@code tasklist} are one list, and {@code kill} and {@code TASKKILL} stop the same program.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineToolGameTests {

    private MachineToolGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    /** A program that stays up, so there is something for these to list and something to stop. */
    private static final String HOLDING = """
            using System.*;
            namespace Programs;
            class Holding : IScript {
                public void OnInit() { }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    private static PersonalComputerBlockEntity machine(final GameTestHelper helper,
                                                       final ResourceLocation system) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        computer.installOs(system);
        return computer;
    }

    private static int start(final GameTestHelper helper, final PersonalComputerBlockEntity computer) {
        final ProgramLauncher.Launch launch = ProgramLauncher.launch(computer, "C:\\progs\\hold.sgs",
                asked -> ICliComputer.FsResult.ok(HOLDING), List.of(), IProgramParent.NONE,
                ProgramPriority.MEDIUM, 4);
        helper.assertTrue(launch.ok(), "the program starts; got " + launch.message());
        return launch.id();
    }

    @GameTest(template = ARENA)
    public static void ps_andKill_listWhatIsRunningAndStopIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final int id = start(helper, computer);

                    final List<String> listed = shell(helper, computer, "ps");
                    helper.assertTrue(says(listed, "hold.sgs") && says(listed, String.valueOf(id)),
                            "ps lists it by the number it answers to; got " + listed);

                    shell(helper, computer, "kill " + id);
                    helper.assertTrue(computer.programs().byId(id) == null,
                            "kill stopped it; the machine still lists " + computer.programs().view().size());

                    final List<String> none = shell(helper, computer, "ps");
                    helper.assertTrue(says(none, "no processes"), "and nothing is running after; got " + none);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void tasklist_andTaskkill_areTheSameTwoToolsInTheOtherFamilysWords(
            final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final int id = start(helper, computer);

                    final List<String> listed = shell(helper, computer, "TASKLIST");
                    helper.assertTrue(says(listed, "hold.sgs"), "tasklist lists it; got " + listed);

                    shell(helper, computer, "TASKKILL /PID " + id);
                    helper.assertTrue(computer.programs().byId(id) == null, "taskkill stopped it");

                    final List<String> ps = shell(helper, computer, "ps");
                    helper.assertTrue(says(ps, "command not found"),
                            "and the other family's name for it is not here; got " + ps);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mem_whereAndDate_answerAboutTheMachineItself(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> mem = shell(helper, computer, "MEM");
                    helper.assertTrue(says(mem, "Total memory") && says(mem, "In use") && says(mem, "Free"),
                            "mem says what the memory is spent on; got " + mem);

                    final List<String> where = shell(helper, computer, "WHERE dir");
                    helper.assertTrue(says(where, "dir") && says(where, "the system"),
                            "where says what put a command here; got " + where);

                    final List<String> date = shell(helper, computer, "DATE");
                    helper.assertTrue(says(date, "Day "), "date reads the world's own clock; got " + date);
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
