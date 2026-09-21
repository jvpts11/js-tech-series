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
 * What a program reads of its network's Mainframe, as a real computer answers it: a computer with no cable has no
 * Mainframe and stops a program that asks for more, and a cabled one reads the work of the Mainframe running its
 * network.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MainframeCallsGameTests {

    private MainframeCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Ticks for a cable to carry the network to the machines on it. */
    private static final int PROPAGATE = 10;

    /** Where a call says what it moved; nothing read of the Mainframe is priced by its size. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };

    /** Makes a call on the Mainframe, or reads one of its values, the way a program's line does. */
    private static Object ask(final MachineServices host, final String name, final String... arguments) {
        final List<String> parameters = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            parameters.add("string");
        }
        final MemberId id = new MemberId("Mainframe", name, parameters);
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNCOUNTED, null, arguments.clone(), 1);
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
    public static void mainframe_isNoneForAComputerWithNoCable(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineServices host = pc.services();

                    helper.assertTrue(Boolean.FALSE.equals(ask(host, "Online")), "it has no Mainframe to read");
                    try {
                        ask(host, "PeakToday");
                        helper.fail("reading a Mainframe stops a program on a machine with no network");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_NETWORK,
                                "it says there is no network; got " + halt.getMessage());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void mainframe_readsTheWorkOfTheNetworkItIsCabledTo(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = cabled(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + PROPAGATE, () -> {
                    helper.assertTrue(pc.networkUuid() != null, "the computer joined the Mainframe's network");
                    final MachineServices host = pc.services();

                    helper.assertTrue(Boolean.TRUE.equals(ask(host, "Online")), "it finds the Mainframe running");
                    helper.assertTrue(ask(host, "PeakToday") instanceof Integer, "it reads the day's peak");
                    final Object select = ask(host, "Stats", "Select");
                    helper.assertTrue(select instanceof Values.Obj stat && "select".equals(stat.get("Type"))
                                    && stat.get("Count") instanceof Integer,
                            "a kind of Operation reads as a record named in lower case, with a count");
                    helper.assertTrue(ask(host, "Work") instanceof Values.ListValue, "it lists the work done");
                })
                .thenSucceed();
    }
}
