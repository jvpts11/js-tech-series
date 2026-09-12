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
import dev.jstech.computers.cannon.machine.HostGateway;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A Cannon program reaching the ComputerCraft side through a Gateway of its own machine: it finds the
 * Gateway the machine has, names it, and hears what a computer over there says.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GatewayCannonGameTests {

    private GatewayCannonGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos GATEWAY = new BlockPos(3, 2, 2);

    /** A program that asks about its Gateway and then listens for what the other side says. */
    private static final String BRIDGE = """
            using System.*;
            using System.IO.*;
            using System.Network.*;
            namespace Plant;
            class Bridge : IScript {
                public void OnInit() {
                    Console.PrintLine("online " + Gateway.Online);
                    Console.PrintLine("names " + Gateway.Names().Count);
                    Console.PrintLine("current " + Gateway.Current);
                    Gateway.OnMessage(Heard);
                }
                void Heard(GatewayMessage said) {
                    Console.PrintLine("heard " + said.From + " " + said.Text);
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

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

    /** A ComputerCraft computer as far as the Gateway is concerned: a number and somewhere to put events. */
    private static final class FakeComputer implements dan200.computercraft.api.peripheral.IComputerAccess {

        private final int id;
        private final List<Object[]> asked = new java.util.ArrayList<>();

        private FakeComputer(final int id) {
            this.id = id;
        }

        /** What the Gateway last asked this computer to do, or null when it has been asked nothing. */
        private Object[] lastQuestion() {
            return this.asked.isEmpty() ? null : this.asked.getLast();
        }

        @Override
        public String mount(final String where, final dan200.computercraft.api.filesystem.Mount mount,
                            final String drive) {
            return null;
        }

        @Override
        public String mountWritable(final String where,
                                    final dan200.computercraft.api.filesystem.WritableMount mount,
                                    final String drive) {
            return null;
        }

        @Override
        public void unmount(final String where) {
        }

        @Override
        public int getID() {
            return this.id;
        }

        @Override
        public void queueEvent(final String event, final Object... arguments) {
            if ("jsc_ask".equals(event)) {
                this.asked.add(arguments);
            }
        }

        @Override
        public String getAttachmentName() {
            return "right";
        }

        @Override
        public java.util.Map<String, dan200.computercraft.api.peripheral.IPeripheral> getAvailablePeripherals() {
            return java.util.Map.of();
        }

        @Override
        public dan200.computercraft.api.peripheral.IPeripheral getAvailablePeripheral(final String name) {
            return null;
        }

        @Override
        public dan200.computercraft.api.peripheral.WorkMonitor getMainThreadMonitor() {
            throw new UnsupportedOperationException("no main thread monitor in this test");
        }
    }

    /** The Gateway's ComputerCraft side, with a computer attached to its front. */
    private static dev.jstech.computers.integration.computercraft.GatewayPeripheral attach(
            final GameTestHelper helper, final FakeComputer computer) {
        final dan200.computercraft.api.peripheral.IPeripheral found = helper.getLevel().getCapability(
                dan200.computercraft.api.peripheral.PeripheralCapability.get(),
                helper.absolutePos(GATEWAY), Direction.EAST);
        if (!(found instanceof dev.jstech.computers.integration.computercraft.GatewayPeripheral peripheral)) {
            throw new IllegalStateException("the Gateway's front is not a jsc_gateway peripheral: " + found);
        }
        peripheral.attach(computer);
        return peripheral;
    }

    /** A program that asks a computer over there to do something, and waits for the answer. */
    private static final String ASKER = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            namespace Plant;
            class Asker : IScript {
                public void OnInit() {
                    Console.PrintLine("agent " + Gateway.HasAgent(9));
                    Console.PrintLine("read " + Gateway.Read(9, "/startup"));
                    Console.PrintLine("over");
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    /*
     * The whole way across and back: the agent says it is there, a program here asks that computer for
     * one of its files, the program stops where it stands until the answer comes, and carries on with it.
     */
    @GameTest(template = ARENA)
    public static void asking_acrossWaitsForTheAnswerAndCarriesOnWithIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = fleet(helper);
        final FakeComputer cc = new FakeComputer(9);
        final int[] program = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    attach(helper, cc).hello(cc);
                    helper.assertTrue(gateway(helper).hasAgent(9), "the agent said it is there");
                    final MachinePrograms.Started started =
                            computer.cannon().start("asker.can", ASKER, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    program[0] = started.id();
                    // The terminal holds it, so what it printed is still there once it has finished.
                    computer.cannon().hold(program[0]);
                    computer.cannon().tick(8192);
                    final List<String> said = computer.cannon().byId(program[0]).process().console();
                    helper.assertTrue(said.size() == 1 && "agent true".equals(said.getFirst()),
                            "it knows the agent is there and then waits; console " + said);
                    final Object[] question = cc.lastQuestion();
                    helper.assertTrue(question != null && question.length >= 3 && "read".equals(question[1])
                            && "/startup".equals(question[2]), "the question went over; got "
                            + (question == null ? "nothing" : java.util.Arrays.toString(question)));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final Object[] question = cc.lastQuestion();
                    try {
                        attach(helper, cc).answer(cc, new dan200.computercraft.api.lua.ObjectArguments(
                                question[0], "shell.run(\"jsc\")"));
                    } catch (final dan200.computercraft.api.lua.LuaException refused) {
                        helper.fail("the answer was refused: " + refused.getMessage());
                    }
                    computer.cannon().tick(8192);
                    final List<String> said = computer.cannon().byId(program[0]).process().console();
                    helper.assertTrue(said.size() == 3 && said.get(1).equals("read shell.run(\"jsc\")"),
                            "the program went on with the answer; console " + said);
                })
                .thenSucceed();
    }

    /*
     * The agent reaches their computers the way their computers are given anything: as a data pack the
     * server serves. This is the proof that it is there and is what it should be, made from our source
     * by our own translator, without a ComputerCraft computer having to be turned on to find out.
     */
    @GameTest(template = ARENA)
    public static void agent_isServedToComputerCraftAsAPackOfOneFile(final GameTestHelper helper) {
        final net.minecraft.resources.ResourceLocation at =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "computercraft", dev.jstech.computers.integration.computercraft.JscRom.AGENT_PATH);
        final java.util.Optional<net.minecraft.server.packs.resources.Resource> found =
                helper.getLevel().getServer().getResourceManager().getResource(at);
        helper.assertTrue(found.isPresent(), "the server serves the agent at " + at);
        try (java.io.InputStream held = found.orElseThrow().open()) {
            final String lua = new String(held.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            helper.assertTrue(lua.startsWith("local P"), "it is a translated program; starts " + lua.substring(0,
                    Math.min(20, lua.length())));
            helper.assertTrue(lua.contains("_serve("), "and it is the agent, which serves");
        } catch (final java.io.IOException unreadable) {
            helper.fail("the agent could not be read: " + unreadable.getMessage());
        }
        helper.succeed();
    }

    /** The machine's Gateway is the one a program on it finds, by name, and it is the current one. */
    @GameTest(template = ARENA)
    public static void gateway_isTheOneTheMachineHasLinkedToIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = fleet(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    final List<NetworkGatewayBlockEntity> mine = HostGateway.gatewaysOf(computer);
                    helper.assertTrue(mine.size() == 1, "the computer has its Gateway; got " + mine.size());
                    final MachinePrograms.Started started =
                            computer.cannon().start("bridge.can", BRIDGE, 1, computer);
                    helper.assertTrue(started.ok(), "the program starts: " + started.message());
                    computer.cannon().tick(8192);
                    final List<String> said = computer.cannon().byId(started.id()).process().console();
                    helper.assertTrue(said.size() >= 3, "it asked its three questions; got " + said);
                    helper.assertTrue("online true".equals(said.get(0)), "the Gateway is there; got " + said.get(0));
                    helper.assertTrue("names 1".equals(said.get(1)), "one Gateway; got " + said.get(1));
                    helper.assertTrue(said.get(2).equals("current " + gateway(helper).name()),
                            "and it is the one the machine has; got " + said.get(2));
                })
                .thenSucceed();
    }

    /** A ComputerCraft computer calling the Gateway's own send is heard by the program on this side. */
    @GameTest(template = ARENA)
    public static void send_fromAComputerCraftComputerReachesTheProgram(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = fleet(helper);
        final int[] program = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    final MachinePrograms.Started started =
                            computer.cannon().start("bridge.can", BRIDGE, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    program[0] = started.id();
                    computer.cannon().tick(8192);
                    final FakeComputer cc = new FakeComputer(9);
                    try {
                        attach(helper, cc).send(cc, "from the other side");
                    } catch (final dan200.computercraft.api.lua.LuaException refused) {
                        helper.fail("the Gateway refused the message: " + refused.getMessage());
                    }
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = computer.cannon().byId(program[0]).process().console();
                    helper.assertTrue(said.getLast().equals("heard 9 from the other side"),
                            "the program heard what the computer said; console " + said);
                })
                .thenSucceed();
    }

    /** What a ComputerCraft computer says through the Gateway reaches the program listening for it. */
    @GameTest(template = ARENA)
    public static void message_fromTheOtherSideReachesTheProgramListening(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = fleet(helper);
        final int[] program = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    final MachinePrograms.Started started =
                            computer.cannon().start("bridge.can", BRIDGE, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    program[0] = started.id();
                    computer.cannon().tick(8192);
                    gateway(helper).said(7, "hello", helper.getLevel().getGameTime());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = computer.cannon().byId(program[0]).process().console();
                    helper.assertTrue(said.getLast().equals("heard 7 hello"),
                            "the program heard it; console " + said);
                })
                .thenSucceed();
    }
}
