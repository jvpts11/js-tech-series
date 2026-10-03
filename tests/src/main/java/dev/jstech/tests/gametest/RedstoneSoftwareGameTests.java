/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.IqlBusSetter;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A Redstone Interface set from software: an IQL statement and a program's calls each find an interface of the machine
 * that runs them by its name, make it read or emit, and mark it with what set it; a name no interface of the machine
 * answers to, or a strength redstone does not have, is refused.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneSoftwareGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    /** An interface against the Mainframe's east face, its lens facing east. */
    private static final BlockPos GATE = new BlockPos(2, 2, 2);
    /** Another against its north face, its lens facing north, a block of redstone in front of it. */
    private static final BlockPos LEVER = new BlockPos(1, 2, 1);
    private static final int QUERY_ROWS = 16;
    /* Long enough for the interfaces to link and read, and a little more. */
    private static final int SETTLE_TICKS = 10;
    private static final String GATEKEEPER = """
            using System.*;
            using System.IO.*;
            using System.Machine.*;
            namespace Programs;
            class Gatekeeper {
                static void Main() {
                    Redstone lever = redstone("Lever");
                    Redstone gate = Redstone.Named("gate");
                    Console.PrintLine("lever " + Convert.ToString(lever.Level()));
                    gate.Out(lever.Level());
                    Console.PrintLine("gate " + Convert.ToString(gate.Level()));
                    if (redstone("Nobody") == null) {
                        Console.PrintLine("no other");
                    }
                    gate.Out(16);
                    Console.PrintLine("never");
                }
            }
            """;

    private RedstoneSoftwareGameTests() {
    }

    /** An IQL statement sets the interface it names, marked as typed; a strength or a name it cannot is refused. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iql_setsAnInterfaceOfTheMachineThatRunsIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        placeSensor(helper, GATE, Direction.EAST);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    final RedstoneInterfaceBlockEntity gate = sensor(helper, GATE);
                    helper.assertTrue(gate.live(), "the interface links to the Mainframe");
                    helper.assertTrue(gate.rename("Gate"), "and is named");
                    final IqlEngine.Outcome out = run(helper, mainframe, "SET REDSTONE 'Gate' OUT 12");
                    helper.assertTrue(out.ok(), "OUT runs: " + out.message());
                    helper.assertTrue(gate.emits() && gate.strength() == 12, "it emits 12");
                    helper.assertValueEqual(gate.setBy(), IqlBusSetter.TYPED, "marked as typed");
                    helper.assertTrue(run(helper, mainframe, "set redstone 'gate' in").ok(),
                            "IN runs, the name in any letters");
                    helper.assertTrue(!gate.emits(), "it reads");
                    helper.assertFalse(run(helper, mainframe, "SET REDSTONE 'Gate' OUT 16").ok(),
                            "16 is no strength");
                    helper.assertFalse(run(helper, mainframe, "SET REDSTONE 'Nobody' IN").ok(),
                            "no interface of the machine is called so");
                })
                .thenSucceed();
    }

    /** A program reads one interface and sets another from it, by their names, and stops at a strength out of range. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void sigma_readsOneInterfaceAndSetsAnother(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        placeSensor(helper, GATE, Direction.EAST);
        placeSensor(helper, LEVER, Direction.NORTH);
        helper.setBlock(LEVER.north(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    sensor(helper, GATE).rename("Gate");
                    sensor(helper, LEVER).rename("Lever");
                })
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    final SigmaCompiler.Result built =
                            SigmaCompiler.compile(List.of(new SourceFile("Gatekeeper.sgs", GATEKEEPER)));
                    helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
                    final MachinePrograms programs = mainframe.programs();
                    final MachinePrograms.Started started =
                            programs.start("gatekeeper.asm", built.assembly(), 1, mainframe);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    programs.tick(4096);
                    final List<String> said = programs.byId(started.id()).process().console();
                    helper.assertTrue(said.size() >= 3 && said.subList(0, 3)
                                    .equals(List.of("lever 15", "gate 15", "no other")),
                            "it read the lever and set the gate; it said " + said);
                    helper.assertTrue(!said.contains("never"), "16 stopped it; it said " + said);
                    final RedstoneInterfaceBlockEntity gate = sensor(helper, GATE);
                    helper.assertTrue(gate.emits() && gate.strength() == 15, "the gate emits 15");
                    helper.assertFalse(gate.setBy().isEmpty() || gate.setBy().equals(IqlBusSetter.TYPED),
                            "marked with the program; got " + gate.setBy());
                })
                .thenSucceed();
    }

    private static void placeSensor(final GameTestHelper helper, final BlockPos at, final Direction lens) {
        helper.setBlock(at, ComputingModule.STANDARD_REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(RedstoneInterfaceBlock.FACING, lens));
    }

    private static RedstoneInterfaceBlockEntity sensor(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof RedstoneInterfaceBlockEntity sensor)) {
            throw new IllegalStateException("no Redstone Interface at " + at);
        }
        return sensor;
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(new ServerCliComputer(mainframe,
                helper.getLevel())), statement, QUERY_ROWS);
    }
}
