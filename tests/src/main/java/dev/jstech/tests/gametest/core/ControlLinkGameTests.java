/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.MemberRuns;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.PowerDrawn;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidMachines;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestNetworkMembers;
import dev.jstech.tests.TestOvenBattery;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the industrial control cable and what a controller reads: a controller linking the machines its
 * cables reach, nearest first, as many as its ports, no further than eight cables, one controller to a machine, and
 * letting go of a cut cable; what every machine now offers (its state, its redstone setting, what it draws and on
 * what port, its fuse and its battery), the valves' and the tanks' values; a member writing a row of a table for each
 * machine it reads; and the network giving a request to the member that takes it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ControlLinkGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 5;
    private static final int ROWS = 64;
    private static final BlockPos CONTROLLER = new BlockPos(1, 2, 1);

    private ControlLinkGameTests() {
    }

    /** It links the machines its cable reaches, nearest first, as many as its ports; the first linked keep theirs. */
    @GameTest(template = ARENA)
    public static void keep_linksTheNearestMachinesAsManyAsItsPorts(final GameTestHelper helper) {
        helper.setBlock(CONTROLLER, TestNetworkMembers.CONTROLLER.get());
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 3, 1), TestKitMachine.BLOCK.get());
        }
        for (int x = 2; x <= 5; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 1), TestCableTypes.CONTROL);
        }
        final TestNetworkMembers.ControllerBlockEntity controller = controller(helper, CONTROLLER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    controller.controlPorts(2);
                    controller.keepNow();
                    helper.assertTrue(controller.machines().equals(List.of(at(helper, 3, 3, 1), at(helper, 4, 3, 1))),
                            "the two nearest: " + controller.machines());
                    helper.assertTrue(ControlLinks.controllerOf(helper.getLevel(), helper.absolutePos(
                            new BlockPos(3, 3, 1))).equals(Optional.of(helper.absolutePos(CONTROLLER))),
                            "a machine says which controller it is linked to");
                    controller.controlPorts(3);
                    controller.keepNow();
                    helper.assertTrue(controller.machines().size() == 3, "a third port, a third machine");
                    controller.controlPorts(1);
                    controller.keepNow();
                    helper.assertTrue(controller.machines().equals(List.of(at(helper, 3, 3, 1))),
                            "one port left, the first linked keeps it: " + controller.machines());
                })
                .thenSucceed();
    }

    /** A machine more than eight cables away is out of reach. */
    @GameTest(template = ARENA)
    public static void keep_reachesNoFurtherThanEightCables(final GameTestHelper helper) {
        helper.setBlock(CONTROLLER, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(new BlockPos(9, 3, 1), TestKitMachine.BLOCK.get());
        helper.setBlock(new BlockPos(10, 3, 1), TestKitMachine.BLOCK.get());
        for (int x = 2; x <= 10; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 1), TestCableTypes.CONTROL);
        }
        final TestNetworkMembers.ControllerBlockEntity controller = controller(helper, CONTROLLER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    controller.controlPorts(4);
                    controller.keepNow();
                    helper.assertTrue(controller.machines().equals(List.of(at(helper, 9, 3, 1))),
                            "the machine at eight cables, not the one at nine: " + controller.machines());
                })
                .thenSucceed();
    }

    /** A machine has one controller: the first to reach it, until that one is gone. */
    @GameTest(template = ARENA)
    public static void keep_givesAMachineOneController(final GameTestHelper helper) {
        final BlockPos other = new BlockPos(7, 2, 1);
        helper.setBlock(CONTROLLER, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(other, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(new BlockPos(4, 3, 1), TestKitMachine.BLOCK.get());
        for (int x = 2; x <= 6; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 1), TestCableTypes.CONTROL);
        }
        final TestNetworkMembers.ControllerBlockEntity first = controller(helper, CONTROLLER);
        final TestNetworkMembers.ControllerBlockEntity second = controller(helper, other);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    first.keepNow();
                    second.keepNow();
                    helper.assertTrue(first.machines().size() == 1 && second.machines().isEmpty(),
                            "the first keeps it: " + first.machines() + ", " + second.machines());
                    helper.setBlock(CONTROLLER, Blocks.AIR);
                    second.keepNow();
                    helper.assertTrue(second.machines().size() == 1, "the first gone, the second takes it");
                })
                .thenSucceed();
    }

    /** A cut cable unlinks what it reached. */
    @GameTest(template = ARENA)
    public static void keep_letsGoOfWhatACutCableReached(final GameTestHelper helper) {
        helper.setBlock(CONTROLLER, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(new BlockPos(4, 3, 1), TestKitMachine.BLOCK.get());
        for (int x = 2; x <= 4; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 1), TestCableTypes.CONTROL);
        }
        final TestNetworkMembers.ControllerBlockEntity controller = controller(helper, CONTROLLER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    controller.keepNow();
                    helper.assertTrue(controller.machines().size() == 1, "linked");
                    helper.setBlock(new BlockPos(3, 2, 1), Blocks.AIR);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    controller.keepNow();
                    helper.assertTrue(controller.machines().isEmpty(), "the cable cut, let go");
                    helper.assertTrue(ControlLinks.controllerOf(helper.getLevel(), helper.absolutePos(
                            new BlockPos(4, 3, 1))).isEmpty(), "and the machine has no controller");
                })
                .thenSucceed();
    }

    /**
     * Every machine offers its state, its redstone setting, which can be set, and what it draws; one on a port of the
     * series' energy, its port's class, its fuse and its battery too.
     */
    @GameTest(template = ARENA)
    public static void parameters_offerTheStateRedstoneDrawFuseAndBattery(final GameTestHelper helper) {
        final BlockPos kit = new BlockPos(1, 2, 1);
        final BlockPos oven = new BlockPos(3, 2, 1);
        helper.setBlock(kit, TestKitMachine.BLOCK.get());
        helper.setBlock(oven, TestOvenBattery.TERMINAL.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IMachineParameters machine = values(helper, kit);
                    helper.assertTrue(machine.statusNow().isPresent(), "its state");
                    helper.assertTrue(machine.redstoneMode().equals(Optional.of(RedstoneMode.IGNORE)),
                            "it ignores a signal at first: " + machine.redstoneMode());
                    helper.assertTrue(machine.setRedstoneMode(RedstoneMode.HIGH)
                            && machine.redstoneMode().equals(Optional.of(RedstoneMode.HIGH)), "set to run with one");
                    helper.assertTrue(machine.powerDrawn().isPresent(), "what it draws");
                    final IMachineParameters terminal = values(helper, oven);
                    final PowerDrawn draws = terminal.powerDrawn().orElseThrow();
                    helper.assertTrue(draws.portClass().equals(Optional.of(JsEnergy.LOW)),
                            "its port's class: " + draws.portClass());
                    helper.assertTrue(terminal.read("fuse").orElse(-1) == 1 && terminal.read("battery").orElse(-1)
                            == 0, "a whole fuse and no battery");
                    helper.assertTrue(terminal.statusNow().equals(Optional.of(MachineStatus.NOT_FORMED)),
                            "a terminal standing alone is not formed: " + terminal.statusNow());
                })
                .thenSucceed();
    }

    /** A valve opened by hand is opened from outside, one that follows a signal only read; a relief valve is set. */
    @GameTest(template = ARENA)
    public static void parameters_ofValvesAndTanks(final GameTestHelper helper) {
        final BlockPos hand = new BlockPos(1, 2, 1);
        final BlockPos signal = new BlockPos(3, 2, 1);
        final BlockPos relief = new BlockPos(5, 2, 1);
        final BlockPos tank = new BlockPos(7, 2, 1);
        helper.setBlock(hand, TestFluidMachines.HAND_VALVE.get());
        helper.setBlock(signal, TestFluidMachines.SIGNAL_VALVE.get());
        helper.setBlock(relief, TestFluidMachines.RELIEF_VALVE.get());
        helper.setBlock(tank, IndustrialModule.TANK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(values(helper, hand).read("open").orElse(-1) == 0, "shut at first");
                    helper.assertTrue(values(helper, hand).write("open", 1), "opened from outside");
                    helper.assertTrue(!values(helper, signal).write("open", 1), "a signal valve is only read");
                    helper.assertTrue(values(helper, relief).write("set_pressure", 12)
                            && values(helper, relief).read("set_pressure").orElse(-1) == 12, "set to open at 12 atm");
                    final IFluidHandler fluid = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                            helper.absolutePos(tank), Direction.UP);
                    helper.assertTrue(fluid != null && fluid.fill(new FluidStack(Fluids.WATER, 3_000),
                            IFluidHandler.FluidAction.EXECUTE) == 3_000, "the tank takes water");
                    helper.assertTrue(values(helper, tank).read("amount").orElse(-1) == 3_000,
                            "and says it holds it: " + values(helper, tank).read("amount"));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(values(helper, hand).read("open").orElse(-1) == 1,
                        "and the hand valve opens on its next tick"))
                .thenSucceed();
    }

    /**
     * A member writes a row of its table for each machine it reads, where each stands; and the network gives a request
     * to the member that takes it, or says why the member named will not.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rows_oneAMachineAndRequestsToTheMemberThatTakesThem(final GameTestHelper helper) {
        final BlockPos firstController = new BlockPos(2, 2, 1);
        final BlockPos secondController = new BlockPos(2, 2, 3);
        final BlockPos firstMachine = new BlockPos(3, 2, 0);
        final BlockPos secondMachine = new BlockPos(3, 2, 4);
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper)
                .placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestWorldBuilder.forGameTest(helper).placeSeededRack(new BlockPos(3, 2, 2), Direction.EAST);
        helper.setBlock(firstController, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(secondController, TestNetworkMembers.CONTROLLER.get());
        helper.setBlock(firstMachine, TestKitMachine.BLOCK.get());
        helper.setBlock(secondMachine, TestKitMachine.BLOCK.get());
        TestCables.lay(helper, new BlockPos(2, 2, 0), TestCableTypes.CONTROL);
        TestCables.lay(helper, new BlockPos(2, 2, 4), TestCableTypes.CONTROL);
        final String secondAt = helper.absolutePos(secondMachine).toShortString();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final int joined = mainframe.networkUuid() == null ? 0 : NetworkSystem.get(helper.getLevel())
                            .membersCarryingOut(mainframe.networkUuid(), TestNetworkMembers.SWITCH_OFF.id()).size();
                    helper.assertTrue(joined == 2, "both controllers join the network: " + joined);
                })
                .thenExecute(() -> {
                    controller(helper, firstController).keepNow();
                    controller(helper, secondController).keepNow();
                    final IqlTable table = run(helper, mainframe, "QUERY jstests:test_controlled").table();
                    helper.assertTrue(table.rows().size() == 2, "a row for each machine: " + table.rows());
                    final List<String> places = List.of(table.cell(0, "at").english(), table.cell(1, "at").english());
                    helper.assertTrue(places.contains(helper.absolutePos(firstMachine).toShortString())
                            && places.contains(secondAt), "each where its machine stands: " + places);
                    final IqlEngine.Outcome off = run(helper, mainframe, "RUN jstests:switch_off WITH machine = '"
                            + secondAt + "'");
                    helper.assertTrue(off.ok(), "given to the controller that reads it: " + off.message());
                    final String firstName = NetworkSystem.get(helper.getLevel()).memberAt(helper.absolutePos(
                            firstController).asLong()).map(member -> MemberRuns.nameOf(helper.getLevel(), member))
                            .orElse("");
                    final IqlEngine.Outcome refused = run(helper, mainframe, "RUN jstests:switch_off ON '" + firstName
                            + "' WITH machine = '" + secondAt + "'");
                    helper.assertTrue(!refused.ok() && refused.message().equals(
                            "the controller reads no machine at " + secondAt), "the other says why: "
                            + refused.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(values(helper, secondMachine).read("on").orElse(1) == 0,
                        "the second machine switched off"))
                .thenExecute(() -> helper.assertTrue(values(helper, firstMachine).read("on").orElse(0) == 1,
                        "and the first left on"))
                .thenSucceed();
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(mainframe, helper.getLevel())), statement, ROWS);
    }

    private static long at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z)).asLong();
    }

    private static IMachineParameters values(final GameTestHelper helper, final BlockPos at) {
        final IMachineParameters values = helper.getLevel().getCapability(IMachineParameters.CAPABILITY,
                helper.absolutePos(at), null);
        if (values == null) {
            throw new GameTestAssertException("no values offered at " + at.toShortString());
        }
        return values;
    }

    private static TestNetworkMembers.ControllerBlockEntity controller(final GameTestHelper helper,
                                                                       final BlockPos at) {
        if (helper.getBlockEntity(at) instanceof TestNetworkMembers.ControllerBlockEntity controller) {
            return controller;
        }
        throw new GameTestAssertException("no controller at " + at.toShortString());
    }
}
