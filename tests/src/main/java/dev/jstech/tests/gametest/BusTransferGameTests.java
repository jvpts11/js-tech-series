/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for the Import and Export Buses moving items between chests and the network, including the flush
 * conservation and the type filter.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BusTransferGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private BusTransferGameTests() {
    }

    @GameTest(template = ARENA)
    public static void importBus_movesChestItemsIntoNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        // Import Bus part on the east face of the cable end, facing a barrel of cobblestone.
        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            cable.addPart(Direction.EAST, new ImportBusPart(HardwareEra.STANDARD));
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);
        if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container container) {
            container.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        }

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 50, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    final long held = NetworkStorage.of(helper.getLevel(), net).count(Items.COBBLESTONE);
                    helper.assertTrue(held > 0, "import bus must move items into the network; got " + held);
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(!log.isEmpty() && log.get(0).type() == OperationRecord.TYPE_INSERT,
                            "the import should be logged as an INSERT");
                    helper.assertTrue(!log.get(0).moves().isEmpty()
                                    && log.get(0).moves().get(0).from().equals("Import Bus"),
                            "provenance should name the Import Bus");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void importBus_flushConservation(final GameTestHelper helper) {
        /*
         * Break the cable while items are in the "flushed" state (extracted from source,
         * INSERT dispatched but not yet settled) and verify nothing is silently discarded.
         * Items must be conserved: either in network storage (if INSERT completed first)
         * or dropped as entities at the cable position (if INSERT was still in flight).
         */
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack at " + rack);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            cable.addPart(Direction.EAST, new ImportBusPart(HardwareEra.STANDARD));
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, Blocks.BARREL);
        if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container container) {
            container.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        }

        /*
         * After SETTLE the network is live and extraction starts. FLUSH_TICKS (20) later the bus
         * dispatches an INSERT; the HDD's disk-seek latency (10 ticks) means the INSERT is still
         * in flight at tick SETTLE+22. Breaking the cable at that point exercises the dropContents
         * path for in-flight flushes, and items must not vanish.
         */
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 22, () -> helper.destroyBlock(cableEnd))
                .thenExecuteAfter(SETTLE, () -> {
                    final long inNetwork = mainframe.networkUuid() != null
                            ? NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(Items.COBBLESTONE)
                            : 0L;
                    final long inWorld = helper.getLevel().getEntitiesOfClass(
                                    net.minecraft.world.entity.item.ItemEntity.class,
                                    new net.minecraft.world.phys.AABB(helper.absolutePos(cableEnd)).inflate(6.0))
                            .stream()
                            .filter(e -> e.getItem().is(Items.COBBLESTONE))
                            .mapToLong(e -> e.getItem().getCount())
                            .sum();
                    helper.assertTrue(inNetwork + inWorld > 0L,
                            "cobblestone must not be silently discarded: inNetwork=" + inNetwork
                                    + " inWorld=" + inWorld);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void exportBus_movesNetworkItemsIntoChest(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        // Export Bus part on the east face of the cable end, facing a barrel.
        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            cable.addPart(Direction.EAST, new ExportBusPart(HardwareEra.STANDARD));
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable
                            && cable.getPart(Direction.EAST) instanceof ExportBusPart bus) {
                        bus.setFilter(new ItemStack(Items.COBBLESTONE));
                    }
                })
                .thenExecuteAfter(50, () -> {
                    long inBarrel = 0L;
                    if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            if (container.getItem(i).is(Items.COBBLESTONE)) {
                                inBarrel += container.getItem(i).getCount();
                            }
                        }
                    }
                    helper.assertTrue(inBarrel > 0, "export bus must move items into the chest; got " + inBarrel);
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(!log.isEmpty() && log.get(0).type() == OperationRecord.TYPE_DELETE,
                            "the export should be logged as a DELETE");
                    helper.assertTrue(!log.get(0).moves().isEmpty()
                                    && log.get(0).moves().get(0).to().equals("Export Bus"),
                            "provenance should go to the Export Bus");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void importBus_filterImportsOnlyThatType(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            final ImportBusPart bus = new ImportBusPart(HardwareEra.STANDARD);
            cable.addPart(Direction.EAST, bus);
            bus.setFilter(new ItemStack(Items.COBBLESTONE)); // import only cobblestone, leave the dirt
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container c) {
                        c.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                        c.setItem(1, new ItemStack(Items.DIRT, 64));
                    }
                })
                .thenExecuteAfter(80, () -> {
                    final long cobble = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    final long dirt = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.DIRT);
                    helper.assertTrue(cobble > 0L, "the filtered import must pull cobblestone; got " + cobble);
                    helper.assertTrue(dirt == 0L, "the filter must leave dirt in the barrel; net dirt=" + dirt);
                })
                .thenSucceed();
    }
}
