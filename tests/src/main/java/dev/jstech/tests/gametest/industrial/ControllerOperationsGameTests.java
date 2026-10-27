/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.transit.CapsuleTransit;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.integration.computers.ControllerOperations;
import dev.jstech.industrial.integration.computers.FieldbusCards;
import dev.jstech.industrial.integration.computers.IndustrialControlCable;
import dev.jstech.industrial.integration.computers.IndustrialControllerBlockEntity;
import dev.jstech.industrial.integration.computers.IndustrialControllers;
import dev.jstech.industrial.line.CompressedAir;
import dev.jstech.industrial.line.PneumaticPost;
import dev.jstech.industrial.line.PneumaticStation;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidMachines;
import dev.jstech.tests.TestPneumatics;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Industrial Controller Computer on the data network: it writes a row of the {@code machines} table for each
 * machine its cards read and one of the {@code controllers} table for itself, saying how many its cables reach past
 * its cards; and it carries out what the network asks through {@code RUN}: a machine's value and what it does with
 * redstone set, a valve opened, a capsule sent, waiting for the station's air when it has none.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ControllerOperationsGameTests {

    private static final String ARENA = "empty";
    /* The control cable south of the controller, at the computer's place in the era's network; the machines beside. */
    private static final List<BlockPos> RUN_OF_CABLE = List.of(new BlockPos(9, 2, 3), new BlockPos(9, 2, 4),
            new BlockPos(9, 2, 5));
    private static final List<BlockPos> BESIDE = List.of(new BlockPos(10, 2, 3), new BlockPos(10, 2, 4),
            new BlockPos(10, 2, 5));
    private static final int ROWS = 50;
    private static final int TARGET = 3;
    private static final int NO_STATION = 9;

    private ControllerOperationsGameTests() {
    }

    /**
     * A Vintage controller with its RS-485 card, reading two machines: the machines table has a row for each, by its
     * name, kind and card; the controllers table says one more is left out; a machine is switched off and set to work
     * without a signal; and a request for a machine it does not read is refused.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void controller_rowsItsMachinesAndSetsThem(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, EraKit.VINTAGE);
        final IndustrialControllerBlockEntity controller = controller(world, EraKit.VINTAGE,
                IndustrialControllers.VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER.get(), FieldbusCards.RS485.get());
        for (int i = 0; i < RUN_OF_CABLE.size(); i++) {
            TestCables.lay(helper, RUN_OF_CABLE.get(i), IndustrialControlCable.CABLE);
            helper.setBlock(BESIDE.get(i), IndustrialModule.COMPRESSOR.get());
        }
        helper.startSequence()
                .thenWaitUntil(() -> joined(helper, network.mainframe()))
                .thenWaitUntil(() -> helper.assertTrue(controller.machines().size() == 2,
                        "its card reads two machines: " + controller.machines()))
                .thenExecute(() -> {
                    final IqlTable machines = run(helper, network.mainframe(), "QUERY machines").table();
                    helper.assertTrue(machines.rows().size() == 2, "a row for each machine it reads: "
                            + machines.rows());
                    final Set<String> names = column(machines, "name");
                    helper.assertTrue(names.equals(Set.of("Compressor #1", "Compressor #2")),
                            "each named by its block and its number: " + names);
                    helper.assertTrue(column(machines, "kind").equals(Set.of("Compressor")), "of its kind");
                    helper.assertTrue(column(machines, "card").equals(Set.of("RS-485, node 1", "RS-485, node 2")),
                            "and the node of the card that reads it: " + column(machines, "card"));
                    final IqlTable controllers = run(helper, network.mainframe(), "QUERY controllers").table();
                    helper.assertTrue(controllers.rows().size() == 1, "a row for the controller");
                    helper.assertTrue(controllers.cell(0, "reads").english().equals("2")
                                    && controllers.cell(0, "left_out").english().equals("1"),
                            "it reads two and leaves one out: reads " + controllers.cell(0, "reads").english()
                                    + ", left out " + controllers.cell(0, "left_out").english());
                    final IqlEngine.Outcome off = run(helper, network.mainframe(), "RUN jsindustrial:machine_set "
                            + "WITH machine = 'Compressor #1', parameter = 'on', value = 0");
                    helper.assertTrue(off.ok(), "the network takes the request: " + off.message());
                    final IqlEngine.Outcome low = run(helper, network.mainframe(), "RUN jsindustrial:machine_set "
                            + "WITH machine = 'Compressor #2', redstone = 'low'");
                    helper.assertTrue(low.ok(), "and the one for its redstone: " + low.message());
                    final IqlEngine.Outcome unread = run(helper, network.mainframe(), "RUN jsindustrial:machine_set "
                            + "WITH machine = 'Compressor #3', parameter = 'on', value = 0");
                    helper.assertTrue(!unread.ok(), "a machine it does not read is refused: " + unread.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(values(helper, BESIDE.get(0)).read("on").orElse(1) == 0,
                        "the first is switched off"))
                .thenWaitUntil(() -> helper.assertTrue(values(helper, BESIDE.get(1)).redstoneMode()
                        .filter(mode -> mode == RedstoneMode.LOW).isPresent(), "the second works without a signal"))
                .thenSucceed();
    }

    /**
     * A Standard controller with its EtherNet/IP card, reading a hand valve and a capsule station: the valve opens,
     * and the station, without air, holds the capsule asked of it, the request waiting, until its air comes.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void controller_opensAValveAndSendsACapsuleOnceThereIsAir(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, EraKit.STANDARD);
        final IndustrialControllerBlockEntity controller = controller(world, EraKit.STANDARD,
                IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER.get(), FieldbusCards.ETHERNET_IP.get());
        TestCables.lay(helper, RUN_OF_CABLE.get(0), IndustrialControlCable.CABLE);
        TestCables.lay(helper, RUN_OF_CABLE.get(1), IndustrialControlCable.CABLE);
        helper.setBlock(BESIDE.get(0), TestFluidMachines.HAND_VALVE.get());
        final PneumaticStation sender = station(helper, BESIDE.get(1), 1);
        sender.setOtherwise(NO_STATION);
        station(helper, BESIDE.get(1).east(2), TARGET);
        TestCables.lay(helper, BESIDE.get(1).east(), TestCableTypes.CAPSULE_TUBE);
        sender.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        helper.startSequence()
                .thenWaitUntil(() -> joined(helper, network.mainframe()))
                .thenWaitUntil(() -> helper.assertTrue(controller.machines().size() == 2,
                        "it reads the valve and the station: " + controller.machines()))
                .thenExecute(() -> {
                    final IqlTable machines = run(helper, network.mainframe(), "QUERY machines").table();
                    final Set<String> names = column(machines, "name");
                    helper.assertTrue(names.size() == 2, "both in the machines table: " + names);
                    final String valve = names.stream().filter(name -> name.contains("valve")).findFirst()
                            .orElse("");
                    final String station = names.stream().filter(name -> name.contains("station")).findFirst()
                            .orElse("");
                    final IqlEngine.Outcome open = run(helper, network.mainframe(),
                            "RUN jsindustrial:valve_set WITH valve = '" + valve + "', open = true");
                    helper.assertTrue(open.ok(), "the network takes the valve's request: " + open.message());
                    final IqlEngine.Outcome send = run(helper, network.mainframe(),
                            "RUN jsindustrial:station_send WITH station = '" + station + "', to = " + TARGET);
                    helper.assertTrue(send.ok(), "and the station's: " + send.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(values(helper, BESIDE.get(0)).read("open").orElse(0) == 1,
                        "the valve opens"))
                .thenIdle(PneumaticPost.RULES.every() * 2)
                .thenExecute(() -> {
                    helper.assertTrue(sender.outgoing().getStackInSlot(0).getCount() == 8,
                            "without air the capsule waits in the station");
                    sender.airPort().tank().insert(new MatterStack(CompressedAir.air(), PneumaticPost.AIR_A_SEND),
                            false);
                })
                .thenWaitUntil(() -> helper.assertTrue(sender.outgoing().getStackInSlot(0).isEmpty()
                                && CapsuleTransit.inFlight(helper.getLevel()).stream().anyMatch(capsule ->
                                capsule.to().equals(helper.absolutePos(BESIDE.get(1).east(2)))),
                        "with its air, the station sends it to the address asked"))
                .thenSucceed();
    }

    /* The era's controller at the computer's place, built of the era's parts and the card, its system on, on. */
    private static IndustrialControllerBlockEntity controller(final TestWorldBuilder world, final EraKit kit,
                                                              final Block block, final Item card) {
        world.setBlock(EraNetwork.COMPUTER, block);
        world.faceRearTowardCable(EraNetwork.COMPUTER);
        final IndustrialControllerBlockEntity controller = world.blockEntity(EraNetwork.COMPUTER,
                IndustrialControllerBlockEntity.class);
        final ItemStackHandler hardware = controller.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuSpec.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1, new ItemStack(card));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        controller.installOs(kit.desktopOs());
        controller.togglePower();
        return controller;
    }

    private static PneumaticStation station(final GameTestHelper helper, final BlockPos at, final int address) {
        helper.setBlock(at, TestPneumatics.CAPSULE_STATION.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        final PneumaticStation station = helper.getBlockEntity(at);
        station.setAddress(address);
        return station;
    }

    /* Whether the controller is a member of the Mainframe's network that carries out the requests to set machines. */
    private static void joined(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        final int members = mainframe.networkUuid() == null ? 0 : NetworkSystem.get(helper.getLevel())
                .membersCarryingOut(mainframe.networkUuid(), ControllerOperations.MACHINE_SET.id()).size();
        helper.assertTrue(members == 1, "the controller joins the network: " + members);
    }

    private static Set<String> column(final IqlTable table, final String column) {
        return IntStream.range(0, table.rows().size()).mapToObj(row -> table.cell(row, column).english())
                .collect(Collectors.toSet());
    }

    private static IMachineParameters values(final GameTestHelper helper, final BlockPos at) {
        final IMachineParameters values = helper.getLevel().getCapability(IMachineParameters.CAPABILITY,
                helper.absolutePos(at), null);
        if (values == null) {
            helper.fail("no values at " + at);
        }
        return values;
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(mainframe, helper.getLevel())), statement, ROWS);
    }
}
