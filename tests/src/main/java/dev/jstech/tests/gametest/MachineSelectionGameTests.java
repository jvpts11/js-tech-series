/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachineBlockEntity;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A job goes only to an interface that can run its pattern. Two interfaces hold the same recipe: one feeds its kiln
 * through a router whose filter takes only raw iron, the other sits against its kiln. A raw copper job has no way into
 * the first, so it runs on the second instead of stalling on the first. This is the group of machines a player builds
 * to spread load, where a router's filter decides what each one is for.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineSelectionGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos SECOND_KILN = new BlockPos(3, 2, 5);

    private MachineSelectionGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void selectsTheInterfaceWhoseRoutersCarryTheInput(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig routed = CraftingRig.routed(world, net.cc(), TestMachines.KILN.get(), false);
        CraftingRig.lay(world, new BlockPos(4, 2, 3), new BlockPos(3, 2, 3), new BlockPos(3, 2, 4),
                new BlockPos(2, 2, 4), new BlockPos(2, 2, 5));
        world.setBlock(SECOND_KILN, TestMachines.KILN.get());
        final CraftingInterfacePart direct = CraftingRig.addPart(world,
                new CraftingFloor.Site(new BlockPos(3, 2, 4), Direction.SOUTH),
                new CraftingInterfacePart(HardwareEra.STANDARD));
        CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(2, 2, 5), Direction.EAST),
                new ReceivingBusPart());
        final ProcessingPattern copper = CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    routed.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
                    net.seed(Items.RAW_COPPER, 16);
                    helper.assertTrue(routed.hold(copper), "the routed interface holds the copper recipe");
                    helper.assertTrue(direct.place(NetworkRecipe.ofProcessing(copper)),
                            "and so does the one against the second kiln");
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    op.set(net.mainframe().submitNetworkProcessing(copper, 4, "select"));
                    helper.assertTrue(op.get() != null, "the processing operation is accepted");
                })
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(direct.id().equals(op.get().interfaceId()),
                            "the job runs on the interface that can feed raw copper");
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_COMPLETED, "and completes there");
                    helper.assertTrue(net.storage(helper.getLevel()).count(StorageKey.of(Items.COPPER_INGOT)) == 4,
                            "four ingots in the network");
                    final TestMachineBlockEntity filtered = routed.machine();
                    helper.assertTrue(filtered != null && filtered.made() == 0 && filtered.input(0).isEmpty(),
                            "the kiln behind the iron router was never fed");
                })
                .thenSucceed();
    }
}
