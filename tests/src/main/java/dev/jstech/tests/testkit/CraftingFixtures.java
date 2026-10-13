/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.TestMachineBlockEntity;
import dev.jstech.tests.TestMachines;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The network, machines and patterns the autocrafting GameTests share: an assembled Mainframe with a server
 * rack and a Crafting Computer, a test kiln fed through a Crafting Interface, a cluster of Supercomputer
 * nodes, and the small crafting patterns the bench and the processing tests submit.
 */
public final class CraftingFixtures {

    private CraftingFixtures() {
    }

    /** The assembled test network, with handles on the parts the assertions need. */
    public record Network(MainframeBlockEntity mainframe, ServerRackBlockEntity rack, CraftingComputerBlockEntity cc) {

        public void seed(final Item item, final int count) {
            rack.getServerStorage(0).insert(item, count);
        }

        public NetworkStorage storage(final GameTestHelper helper) {
            return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
        }
    }

    public static Network buildCraftingNetwork(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        return new Network(net.mainframe(), net.rack(), net.cc());
    }

    /* A test kiln fed by an interface against it, beside the network's Crafting Computer. */
    public static CraftingRig kilnRig(final GameTestHelper helper, final Network net) {
        return CraftingRig.direct(TestWorldBuilder.forGameTest(helper), net.cc, TestMachines.KILN.get());
    }

    public static TestMachineBlockEntity kiln(final CraftingRig rig) {
        final TestMachineBlockEntity kiln = rig.machine();
        if (kiln == null) {
            throw new IllegalStateException("no test kiln at " + rig.machinePos());
        }
        return kiln;
    }

    /* Cobblestone and stone together, in the network and in the kiln. */
    public static long both(final GameTestHelper helper, final Network net, final CraftingRig rig) {
        final NetworkStorage storage = net.storage(helper);
        return rig.total(storage, Items.COBBLESTONE) + rig.total(storage, Items.STONE);
    }

    public static ProcessingPattern cobblePattern(final int timeout) {
        return CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, timeout);
    }

    public static ProcessingPattern smelt() {
        return CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200);
    }

    public static StorageKey storageKey(final Item item) {
        return StorageKey.of(item);
    }

    /* A Crafting Computer with nothing in it but a Crafting Card, enough for its ROM. */
    public static CraftingComputerBlockEntity placeComputerWithCard(final GameTestHelper helper,
                                                                     final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer)) {
            throw new IllegalStateException("no crafting computer at " + pos);
        }
        computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        return computer;
    }

    public static CraftingComputerBlockEntity placeSecondCraftingComputer(final GameTestHelper helper,
                                                                           final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.CRAFTING_COMPUTER.get());
        TestWorldBuilder.forGameTest(helper).faceRearTowardCable(pos);
        if (!(helper.getBlockEntity(pos) instanceof CraftingComputerBlockEntity ccBe)) {
            throw new IllegalStateException("no second crafting computer");
        }
        final var hw = ccBe.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        ccBe.togglePower();
        return ccBe;
    }

    /**
     * A cluster the way a player wires one: the HBW Interface, a run of high-compute cable east of it,
     * and one Supercomputer Rack hanging off each cable block, each rack seating one node. Racks are
     * leaves on the fabric, so they sit beside the cable run rather than in it.
     */
    public static void placeCluster(final GameTestHelper helper, final BlockPos hub, final int nodes) {
        helper.setBlock(hub, ComputingModule.HBW_INTERFACE.get());
        for (int i = 1; i <= nodes; i++) {
            final BlockPos cable = hub.east(i);
            TestCables.lay(helper, cable, ComputingModule.HPC_CABLE);
            // Above the cable, not beside it: the fixtures' computers and cables occupy the row in front.
            final BlockPos rackPos = cable.above();
            helper.setBlock(rackPos, ComputingModule.SUPERCOMPUTER_RACK.get());
            if (helper.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack) {
                rack.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
            }
        }
    }

    public static CraftingPattern planksPattern(final int count) {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.OAK_LOG));
        return new CraftingPattern(grid, new ItemStack(Items.OAK_PLANKS, count));
    }

    public static CraftingPattern sticksPattern() {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.OAK_PLANKS));
        grid.set(3, new ItemStack(Items.OAK_PLANKS));
        return new CraftingPattern(grid, new ItemStack(Items.STICK, 4));
    }

    public static CraftingPattern stoneButtonPattern() {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.STONE));
        return new CraftingPattern(grid, new ItemStack(Items.STONE_BUTTON));
    }

    public static CraftingPattern nuggetsToIngot() {
        final List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.add(new ItemStack(Items.IRON_NUGGET));
        }
        return new CraftingPattern(grid, new ItemStack(Items.IRON_INGOT));
    }

    public static List<ItemStack> grid(final Item first) {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(first));
        return grid;
    }

    public static List<ItemStack> emptyGrid() {
        final List<ItemStack> grid = new ArrayList<>(CraftingPattern.GRID_SIZE);
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
