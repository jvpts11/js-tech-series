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
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program asking its network for work, as a real computer answers it: every call the system declares is answered,
 * a computer with no cable stops a program that asks, a cabled one answers asking and looking alike, a refusal
 * included, and a computer in no world cannot be reached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationsCallsGameTests {

    private OperationsCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Ticks for a cable to carry the network to the machines on it. */
    private static final int PROPAGATE = 10;
    private static final String TEXT = "string";

    /** Where a call says what it moved and who asked; the asking program here has no name. */
    private static final IWorldCall UNNAMED = bytes -> {
    };

    private static MemberId operation(final String name, final String... parameters) {
        return new MemberId("Operations", name, List.of(parameters));
    }

    /** Makes one of the calls the way a program's line does, straight to what the machine bound for it. */
    private static Object ask(final MachineServices host, final MemberId id, final Object... arguments) {
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNNAMED, null, arguments, 1);
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
    public static void bind_answersEveryCallTheSystemDeclaresOnTheOperations(final GameTestHelper helper) {
        final MachineServices host = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2)).services();

        for (final IMemberSpec member : SystemApi.type("Operations").members()) {
            helper.assertTrue(host.bind(member.id()) != null, member.id().describe() + " is answered by the machine");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operations_stopAProgramOnAComputerWithNoCable(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    try {
                        ask(pc.services(), operation("List"));
                        helper.fail("asking a network for work stops a program on a machine with none");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_NETWORK
                                        && "this computer is not on a network".equals(halt.getMessage()),
                                "it says there is no network; got " + halt.getMessage());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void operations_answerAskingAndLookingOnACabledComputer(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = cabled(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + PROPAGATE, () -> {
                    helper.assertTrue(pc.networkUuid() != null, "the computer joined the Mainframe's network");
                    final MachineServices host = pc.services();

                    helper.assertTrue(ask(host, operation("List")) instanceof Values.ListValue,
                            "it lists what is in flight");
                    helper.assertTrue(ask(host, operation("Get", TEXT), "no-such-operation") == null,
                            "an Operation that is not in flight reads as nothing");
                    final Object asked = ask(host, operation("Pull", TEXT, "long"), "minecraft:bedrock", 1L);
                    helper.assertTrue(asked instanceof Values.Obj result && "AskResult".equals(result.type())
                                    && result.get("Ok") instanceof Boolean && result.get("Message") instanceof String,
                            "an ask is answered with whether the network took it and why, never by stopping");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void operations_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity loose =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        try {
            ask(loose.services(), operation("List"));
            helper.fail("a computer in no world cannot ask a network for work");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach Operations".equals(halt.getMessage()),
                    "it says the Operations cannot be reached; got " + halt.getMessage());
        }
        helper.succeed();
    }
}
