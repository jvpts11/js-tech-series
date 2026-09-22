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
import dev.jstech.computers.machine.IqlService;
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program speaking the network's own language, as a real computer answers it: a computer with no cable stops a
 * program that speaks it, a cabled one runs statements on its Mainframe with one engine kept for all of them, and a
 * computer in no world cannot be reached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IqlCallsGameTests {

    private IqlCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Ticks for a cable to carry the network to the machines on it. */
    private static final int PROPAGATE = 10;
    private static final String TEXT = "string";
    private static final String NO_MAINFRAME = "this computer is not on a network with a Mainframe";

    /** Where a call says what it moved and how many rows it brought back; the test does not count them here. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };

    private static MemberId statement(final String name, final String... parameters) {
        return new MemberId("Iql", name, List.of(parameters));
    }

    /** Makes one of the calls the way a program's line does, straight to what the machine bound for it. */
    private static Object ask(final MachineServices host, final MemberId id, final Object... arguments) {
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNCOUNTED, null, arguments, 1);
    }

    /** A running Mainframe, a rack and a router, with a computer cabled to them. */
    private static PersonalComputerBlockEntity cabled(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        return world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
    }

    @GameTest(template = ARENA)
    public static void iql_stopsAProgramOnAComputerWithNoCable(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    try {
                        ask(pc.services(), statement("Run", TEXT), "QUERY items");
                        helper.fail("a statement stops a program on a machine with no Mainframe to run it");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_NETWORK
                                        && NO_MAINFRAME.equals(halt.getMessage()),
                                "it says there is no Mainframe; got " + halt.getMessage());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void iql_runsStatementsOnTheMainframeOfACabledComputer(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = cabled(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + PROPAGATE, () -> {
                    helper.assertTrue(pc.networkUuid() != null, "the computer joined the Mainframe's network");
                    final MachineServices host = pc.services();

                    helper.assertTrue(ask(host, statement("Query", TEXT), "QUERY items") instanceof Values.ListValue,
                            "a query comes back as the rows it read");
                    final Object bad = ask(host, statement("Run", TEXT), "garbage tokens");
                    helper.assertTrue(bad instanceof Values.Obj result && Boolean.FALSE.equals(result.get("Ok")),
                            "a statement the engine refuses is answered as refused");
                    try {
                        ask(host, statement("Query", TEXT), "garbage tokens");
                        helper.fail("a refused query stops the program");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.REFUSED, "as refused; got " + halt.reason());
                    }
                    try {
                        ask(host, statement("RunFile", TEXT), "missing.iql");
                        helper.fail("a file of statements that is not there stops the program");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_OBJECT, "as missing; got " + halt.reason());
                    }
                    final IqlService iql = pc.services().iql();
                    helper.assertTrue(iql != null && iql.engine() != null && iql.engine() == iql.engine(),
                            "one engine serves every statement while the network's Mainframe stays the same");
                })
                .thenSucceed();
    }

    /**
     * A file of statements run at the prompt is read the way the studio writes it: one statement a line, with
     * comments and blank lines left out. It used to be read as one statement, so any file of two failed.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void iql_runsEveryStatementOfAFileRunAtThePrompt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = cabled(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (helper.getBlockEntity(new BlockPos(2, 2, 1)) instanceof ServerRackBlockEntity rack) {
                        rack.getServerStorage(0).insert(Items.COBBLESTONE, 64);
                    }
                })
                .thenExecuteAfter(PROPAGATE, () -> {
                    DiskFilesystem.write(pc.systemDisk(), "hold.iql", FileType.IQL,
                            "-- keep some stone back\nLOCK 10 cobblestone\n\nUNLOCK cobblestone\n",
                            Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    final IqlService iql = pc.services().iql();
                    helper.assertTrue(iql != null, "the computer reaches its Mainframe's engine");
                    final ICliComputer.FsResult ran = iql.runFile("hold.iql");
                    helper.assertTrue(ran.ok(), "both statements run, the comment and the blank line left out; got "
                            + ran.message());

                    DiskFilesystem.write(pc.systemDisk(), "broken.iql", FileType.IQL,
                            "LOCK 10 cobblestone\nnot a statement\n", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    final ICliComputer.FsResult refused = iql.runFile("broken.iql");
                    helper.assertTrue(!refused.ok() && refused.message().contains("not a statement"),
                            "a line that is not a statement is named; got " + refused.message());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void iql_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity loose =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        try {
            ask(loose.services(), statement("Run", TEXT), "QUERY items");
            helper.fail("a computer in no world cannot run a statement");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach Iql".equals(halt.getMessage()),
                    "it says IQL cannot be reached; got " + halt.getMessage());
        }
        helper.succeed();
    }
}
