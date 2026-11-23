/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.storage.DataContainers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestVehicles;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gas Cylinder names its gas on the network, as a bucket names its fluid: a full one in an Export Bus's filter
 * sends that gas out of the network's storage into the gas store the bus faces, and on a Pattern Studio cell it places
 * a cell of that gas.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GasCylinderNetworkGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos BUS_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos STORE = new BlockPos(3, 2, 3);
    private static final long STORED = 2_000L;
    private static final long CARRIED = 1_000L;

    private GasCylinderNetworkGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void exportBus_sendsTheGasACylinderInItsFilterNames(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        world.setBlock(BUS_CABLE, ComputingModule.ETHERNET_CABLE);
        helper.setBlock(STORE, TestVehicles.PUMP.get());
        final Material chlorine = Elements.of(Element.CHLORINE);
        final StorageKey key = StorageKey.matter(MatterState.GAS, chlorine);
        final ItemStack cylinder = new ItemStack(GasCylinders.CYLINDER.get());
        cylinder.getCapability(MatterCapabilities.GAS_ITEM).insert(new MatterStack(chlorine, CARRIED), false);
        helper.assertTrue(DataContainers.named(cylinder).equals(Optional.of(key)),
                "a cylinder of chlorine names chlorine: " + DataContainers.named(cylinder));
        final PatternWorkbench.DataCell cell = PatternWorkbench.DataCell.fromStack(cylinder);
        helper.assertTrue(cell != null && cell.key().equals(key), "and places a cell of chlorine: " + cell);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.storage(helper.getLevel()).insert(key, STORED);
                    if (world.getBlockEntity(BUS_CABLE) instanceof CableBlockEntity cable) {
                        final var bus = ComputingParts.EXPORT.get().create();
                        cable.addPart(Direction.WEST, bus);
                        bus.setFilter(cylinder);
                    }
                })
                .thenWaitUntil(() -> {
                    final IMatterHandler store = helper.getLevel().getCapability(MatterCapabilities.GAS,
                            helper.absolutePos(STORE), Direction.EAST);
                    final long held = store == null ? -1L : store.stack(0).filter(gas -> gas.material() == chlorine)
                            .map(MatterStack::amount).orElse(0L);
                    helper.assertTrue(held >= CARRIED, "the Export Bus sends the chlorine its filter's cylinder "
                            + "names into the store it faces: " + held);
                })
                .thenSucceed();
    }
}
