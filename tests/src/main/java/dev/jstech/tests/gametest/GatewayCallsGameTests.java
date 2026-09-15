/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachineHost;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a program asks of its computer's Gateways, as a real computer answers it: every call and value the system
 * declares as the world's is answered, the Gateway a program chooses is the one its later calls go through, a computer
 * without that Gateway stops the program, and a computer in no world cannot be reached. What crosses to a ComputerCraft
 * computer and back is the Σ# Gateway tests' to run.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GatewayCallsGameTests {

    private GatewayCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos GATEWAY = new BlockPos(3, 2, 2);

    /** Where a call reads, and makes, the asking program's choice of Gateway. */
    private static final class Choosing implements IWorldCall {

        private String gateway;

        private Choosing(final String gateway) {
            this.gateway = gateway;
        }

        @Override
        public void moved(final long bytes) {
        }

        @Override
        public String gateway() {
            return this.gateway;
        }

        @Override
        public void chooseGateway(final String name) {
            this.gateway = name;
        }
    }

    /** Makes one of the calls, or reads one of the values, the way a program's line does. */
    private static Object ask(final MachineHost host, final IWorldCall call, final MemberId id,
                              final Object... arguments) {
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(call, null, arguments, 1);
    }

    private static MemberId member(final String name, final String... parameters) {
        return new MemberId("Gateway", name, List.of(parameters));
    }

    /** Makes the call and checks it stops the program saying exactly that. */
    private static void halts(final GameTestHelper helper, final String said, final Runnable asking) {
        try {
            asking.run();
            helper.fail("the call stops the program: " + said);
        } catch (final Halt halt) {
            helper.assertTrue(said.equals(halt.getMessage()), "it says " + said + "; got " + halt.getMessage());
        }
    }

    /** A computer with a Gateway linked to it, its socket facing the computer. */
    private static PersonalComputerBlockEntity fleet(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(COMPUTER);
        helper.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                .setValue(NetworkGatewayBlock.FACING, Direction.EAST));
        return computer;
    }

    private static NetworkGatewayBlockEntity gateway(final GameTestHelper helper) {
        if (helper.getBlockEntity(GATEWAY) instanceof NetworkGatewayBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no Gateway at " + GATEWAY);
    }

    @GameTest(template = ARENA)
    public static void bind_answersEveryWorldCallAndValueOfTheGateway(final GameTestHelper helper) {
        final MachineHost host =
                new MachineHost(TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER));

        for (final IMemberSpec member : SystemApi.type("Gateway").members()) {
            if (member.kind() == MemberKind.WORLD) {
                helper.assertTrue(host.bind(member.id()) != null,
                        member.id().describe() + " is answered by the machine");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gateway_saysSoOnAComputerWithoutOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineHost host = new MachineHost(pc);
                    final Choosing call = new Choosing("");

                    helper.assertTrue(Boolean.FALSE.equals(ask(host, call, member("Online"))),
                            "there is no Gateway to be online");
                    helper.assertTrue(ask(host, call, member("Names")) instanceof Values.ListValue names
                            && names.items().isEmpty(), "nor any to name");
                    halts(helper, "this computer has no Gateway", () -> ask(host, call, member("Current")));
                    halts(helper, "this computer has no Gateway called east",
                            () -> ask(host, call, member("Select", "string"), "east"));
                    helper.assertTrue(call.gateway().isEmpty(), "and a Gateway that is not there is never chosen");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void select_choosesTheComputersGatewayByNameWhateverItsCase(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = fleet(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    final MachineHost host = new MachineHost(pc);
                    final String name = gateway(helper).name();
                    final String typed = name.toUpperCase(Locale.ROOT);
                    final Choosing call = new Choosing("");

                    helper.assertTrue(Boolean.TRUE.equals(ask(host, call, member("Select", "string"), typed)),
                            "the computer has that Gateway");
                    helper.assertTrue(typed.equals(call.gateway()), "and the program chose it; got " + call.gateway());
                    helper.assertTrue(name.equals(ask(host, call, member("Current"))),
                            "its later calls go through that Gateway");
                    helper.assertTrue(Boolean.TRUE.equals(ask(host, call, member("Online"))), "which is online");
                    helper.assertTrue(ask(host, call, member("Computers")) instanceof Values.ListValue,
                            "and says which computers are on its wire");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void calls_goThroughTheGatewayTheProgramChose(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = fleet(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    final MachineHost host = new MachineHost(pc);
                    final Choosing call = new Choosing("elsewhere");

                    helper.assertTrue(Boolean.FALSE.equals(ask(host, call, member("Online"))),
                            "the Gateway it chose is not there");
                    halts(helper, "this computer has no Gateway called elsewhere",
                            () -> ask(host, call, member("Peripherals")));
                    helper.assertTrue(ask(host, call, member("Names")) instanceof Values.ListValue names
                            && names.items().size() == 1, "though the computer still names the one it has");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void gateway_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        final PersonalComputerBlockEntity loose =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        halts(helper, "this machine cannot reach Gateway",
                () -> ask(new MachineHost(loose), new Choosing(""), member("Online")));
        helper.succeed();
    }
}
