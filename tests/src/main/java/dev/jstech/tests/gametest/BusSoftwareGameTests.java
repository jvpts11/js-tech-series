/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.IqlBusSetter;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.core.multipart.PartType;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A bus set from software: an IQL statement, a job that keeps a bus to some hours of the day, and a program's calls,
 * each setting what the bus's era can be set to, refused where it cannot, and marked with what set it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BusSoftwareGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos OTHER_CABLE = new BlockPos(3, 2, 2);
    private static final int QUERY_ROWS = 16;
    private static final String SETTER = """
            using System.*;
            using System.IO.*;
            using System.Network.*;
            namespace Programs;
            class Stocker {
                static void Main() {
                    Bus b = bus("Ore in");
                    b.Keep(8).Priority(3).Between(6, 18);
                    Console.PrintLine("set " + b.Name);
                }
            }
            """;

    private BusSoftwareGameTests() {
    }

    /** An IQL statement sets the bus it names, marked as typed. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iql_setsABusAndMarksItAsTyped(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final AbstractBusPart bus = mount(helper, CABLE, ComputingParts.IMPORT.get(), "Ore in");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(run(helper, mainframe, "SET BUS 'Ore in' KEEP 16 MAX 64").ok(),
                            "the statement runs");
                    helper.assertTrue(bus.keep() == 16 && bus.max() == 64, "keep 16, max 64; got " + bus.keep()
                            + "/" + bus.max());
                    helper.assertValueEqual(bus.setBy(BusSettings.KEEP), IqlBusSetter.TYPED, "marked as typed");
                    helper.assertTrue(run(helper, mainframe, "SET BUS 'Ore in' WHEN STOCK iron_ore < 512").ok(),
                            "a condition");
                    helper.assertTrue(bus.conditions().contains(BusCondition.stock("minecraft:iron_ore", 512)),
                            "the bus waits on the stock; it has " + bus.conditions());
                    helper.assertFalse(run(helper, mainframe, "SET BUS 'Nobody' ON").ok(), "no such bus");
                })
                .thenSucceed();
    }

    /** What an era cannot be set to is refused from software as in the window. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iql_isRefusedWhatTheEraCannot(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final AbstractBusPart bus = mount(helper, CABLE, ComputingParts.LEGACY_IMPORT.get(), "Old");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertFalse(run(helper, mainframe, "SET BUS 'Old' PRIORITY 5").ok(),
                            "a Legacy bus has no priority");
                    helper.assertTrue(bus.priority() == 0, "and has none");
                })
                .thenSucceed();
    }

    /** A job that switches the bus on between two hours is the bus's hours, marked with the job's name. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iql_jobOfHoursSetsTheBusHours(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final AbstractBusPart bus = mount(helper, CABLE, ComputingParts.IMPORT.get(), "Ore in");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    final IqlEngine.Outcome outcome = run(helper, mainframe,
                            "CREATE JOB night_shift AS SET BUS 'Ore in' ON WHEN TIME BETWEEN 18:00 AND 06:00");
                    helper.assertTrue(outcome.ok(), "the job is taken: " + outcome.message());
                    helper.assertTrue(bus.conditions().contains(BusCondition.hours(18, 6)),
                            "the bus moves between 18:00 and 06:00; it has " + bus.conditions());
                    helper.assertValueEqual(bus.setBy(BusSettings.CONDITIONS), "night_shift",
                            "marked with the job's name");
                })
                .thenSucceed();
    }

    /** A program's calls set the bus, chained, marked with the program. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void sigma_setsABusThroughItsCalls(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final AbstractBusPart bus = mount(helper, CABLE, ComputingParts.IMPORT.get(), "Ore in");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    final SigmaCompiler.Result built =
                            SigmaCompiler.compile(List.of(new SourceFile("Stocker.sgs", SETTER)));
                    helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
                    final MachinePrograms programs = mainframe.programs();
                    final MachinePrograms.Started started =
                            programs.start("stocker.asm", built.assembly(), 1, mainframe);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    programs.tick(4096);
                    final List<String> said = programs.byId(started.id()).process().console();
                    helper.assertTrue(said.equals(List.of("set Ore in")), "it ran to its end; it said " + said);
                    helper.assertTrue(bus.keep() == 8 && bus.priority() == 3, "keep 8, priority 3; got "
                            + bus.keep() + "/" + bus.priority());
                    helper.assertTrue(bus.conditions().contains(BusCondition.hours(6, 18)), "and its hours");
                    helper.assertFalse(bus.setBy(BusSettings.KEEP).isEmpty(), "marked with the program");
                })
                .thenSucceed();
    }

    /* A running Mainframe with an HBW cable beside it, and another one past it. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, OTHER_CABLE, ComputingModule.HBW_CABLE);
        return mainframe;
    }

    private static <T extends AbstractBusPart> T mount(final GameTestHelper helper, final BlockPos at,
                                                       final PartType<T> type, final String name) {
        final T bus = type.create();
        TestCables.cable(helper, at).addPart(Direction.SOUTH, bus);
        bus.setName(name);
        return bus;
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(new ServerCliComputer(mainframe,
                helper.getLevel())), statement, QUERY_ROWS);
    }
}
