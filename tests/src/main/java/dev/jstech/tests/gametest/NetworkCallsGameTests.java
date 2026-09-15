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
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a program reads of the data network, as a real computer answers it: a computer with no cable says it is on
 * none and stops a program that asks for more, a cabled one reads its Mainframe's network, and a computer in no world
 * cannot be reached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkCallsGameTests {

    private NetworkCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Ticks for a cable to carry the network to the machines on it. */
    private static final int PROPAGATE = 10;
    private static final String IRON = "minecraft:iron_ingot";

    /** Where a call says what it moved; nothing read of the network is priced by its size. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };

    /** Makes a call on the network, or reads one of its values, the way a program's line does. */
    private static Object ask(final MachineServices host, final String name, final String... arguments) {
        final List<String> parameters = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            parameters.add("string");
        }
        final MemberId id = new MemberId("Network", name, parameters);
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNCOUNTED, null, arguments.clone(), 1);
    }

    @GameTest(template = ARENA)
    public static void network_isNoneForAComputerWithNoCable(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineServices host = pc.services();

                    helper.assertTrue(Boolean.FALSE.equals(ask(host, "Online")), "it says it is on no network");
                    helper.assertTrue(ask(host, "Current") == null, "and names none");
                    try {
                        ask(host, "Total", IRON);
                        helper.fail("asking what a network holds stops a program on a machine with none");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_NETWORK
                                        && "this computer is not on a network".equals(halt.getMessage()),
                                "it says there is no network; got " + halt.getMessage());
                    }
                    helper.assertTrue(pc.networkStock(IRON) == 0L, "a watch on it counts nothing");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void network_isTheMainframesForAComputerCabledToIt(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + PROPAGATE, () -> {
                    helper.assertTrue(pc.networkUuid() != null, "the computer joined the Mainframe's network");
                    final MachineServices host = pc.services();

                    helper.assertTrue(Boolean.TRUE.equals(ask(host, "Online")), "it says it is on a network");
                    final Object current = ask(host, "Current");
                    helper.assertTrue(current instanceof String id && !id.isEmpty(), "and names it; got " + current);
                    helper.assertTrue(ask(host, "Capacity") instanceof Long, "it reads what the network can hold");
                    helper.assertTrue(ask(host, "Total", IRON) instanceof Long, "and how much of an item it holds");
                    helper.assertTrue(ask(host, "Types") instanceof Values.ListValue,
                            "it lists what the network holds");
                    helper.assertTrue(ask(host, "Servers") instanceof Values.ListValue, "and the servers holding it");
                    helper.assertTrue(pc.networkStock(IRON) >= 0L, "a watch on it can be counted");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void network_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity loose =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        try {
            ask(loose.services(), "Online");
            helper.fail("a computer in no world cannot read a network");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach Network".equals(halt.getMessage()),
                    "it says the network cannot be reached; got " + halt.getMessage());
        }
        helper.assertTrue(loose.networkStock(IRON) == 0L, "and a watch on it counts nothing");
        helper.succeed();
    }
}
