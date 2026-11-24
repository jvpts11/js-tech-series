/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ChainMachineSite;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine step longer than the pattern's timeout: while the machine says it is working, the job waits for it,
 * however few things move in and out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LongStepGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* The implanter stands clear of the standard crafting network, its front west corner here, its front north. */
    private static final BlockPos IMPLANTER_CORNER = new BlockPos(2, 1, 5);
    /* Far shorter than one implant step (ten seconds), so only the machine's own word keeps the job alive. */
    private static final int SHORT_TIMEOUT = 40;

    private LongStepGameTests() {
    }

    /** A job whose machine works one step for longer than the pattern's timeout still finishes, rather than failing. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void engine_aJobDoesNotTimeOutWhileItsMachineWorks(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        ChainMachineSite.formed(helper, ChainMachineSite.machine("ion_implanter"), IMPLANTER_CORNER);
        final BlockPos inlet = IMPLANTER_CORNER;
        final BlockPos outlet = IMPLANTER_CORNER.south();
        final CraftingFloor.Site feeding = new CraftingFloor.Site(inlet.north(), Direction.SOUTH);
        final CraftingFloor.Site receiving = new CraftingFloor.Site(outlet.west(), Direction.EAST);
        CraftingRig.lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4), new BlockPos(4, 2, 4),
                new BlockPos(3, 2, 4), new BlockPos(2, 2, 4), feeding.cable(), feeding.cable().west(),
                receiving.cable().north(), receiving.cable());
        final CraftingInterfacePart part = CraftingRig.addPart(world, feeding,
                new CraftingInterfacePart(HardwareEra.STANDARD));
        CraftingRig.addPart(world, receiving, new ReceivingBusPart());
        final Item dust = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
        final ProcessingPattern implant = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(wafer(WaferWork.Step.IMPLANT)), 1),
                        new ProcessingPattern.ProcessingInput(StorageKey.of(new ItemStack(dust)), 1)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(wafer(WaferWork.Step.DEPOSITION)), 1,
                        ProcessingPattern.FULL_CHANCE)), SHORT_TIMEOUT);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        final AtomicLong started = new AtomicLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.rack().getServerStorage(0).insert(StorageKey.of(wafer(WaferWork.Step.IMPLANT)), 1);
                    net.rack().getServerStorage(0).insert(StorageKey.of(new ItemStack(dust)), 4);
                    helper.assertTrue(part.place(NetworkRecipe.ofProcessing(implant)), "the interface takes it");
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    started.set(helper.getLevel().getGameTime());
                    op.set(net.mainframe().submitNetworkProcessing(implant, 1, "long step"));
                })
                .thenWaitUntil(() -> helper.assertTrue(op.get().isDone(), "the layer is done"))
                .thenExecute(() -> {
                    helper.assertTrue(op.get().produced() == 1, "the wafer came back, the job did not give up: "
                            + op.get().produced());
                    final long took = helper.getLevel().getGameTime() - started.get();
                    helper.assertTrue(took > SHORT_TIMEOUT * 2, "the step really outlasted the timeout; took " + took);
                })
                .thenSucceed();
    }

    /* A Vintage memory wafer on its first layer, going to {@code step}. */
    private static ItemStack wafer(final WaferWork.Step step) {
        final ItemStack wafer = new ItemStack(WaferWork.item(ChainParts.ERAS.getFirst(), step));
        wafer.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(
                BuiltInRegistries.ITEM.getKey(ChainParts.MEMORY_CHIPS.getFirst().get()), 1,
                2, step));
        return wafer;
    }
}
