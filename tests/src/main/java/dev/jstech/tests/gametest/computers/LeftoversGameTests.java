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
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.crafting.CraftingDispatch;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ChainMachineSite;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine left as the job found it: a job that gives up takes back what it fed and no recipe used, a job takes back
 * the tool it lent its machine, and an interface against one of the series' machines runs one recipe at a time unless
 * set otherwise.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LeftoversGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* The implanter stands clear of the standard crafting network, its front west corner here, its front north. */
    private static final BlockPos IMPLANTER_CORNER = new BlockPos(2, 1, 5);
    private static final int SHORT_TIMEOUT = 40;
    private static final int DUST = 4;
    /* A Vintage chip's layers; a bucket of photoresist; a pattern's timeout longer than a station's step. */
    private static final int VINTAGE_LAYERS = 2;
    private static final int BUCKET = 1_000;
    private static final int STATION_TIMEOUT = 600;

    private LeftoversGameTests() {
    }

    /**
     * An implant asked for with dust on the network and no wafer: the job feeds the dust, the implanter never starts,
     * the job gives up, and the dust is back on the network, none of it left in the implanter for the next job.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void job_aJobThatGivesUpTakesBackWhatItFed(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingInterfacePart part = implanter(helper, world);
        final Item dust = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
        final ProcessingPattern implant = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(wafer(WaferWork.Step.IMPLANT)), 1),
                        new ProcessingPattern.ProcessingInput(StorageKey.of(new ItemStack(dust)), 1)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(wafer(WaferWork.Step.DEPOSITION)), 1,
                        ProcessingPattern.FULL_CHANCE)), SHORT_TIMEOUT);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.rack().getServerStorage(0).insert(StorageKey.of(new ItemStack(dust)), DUST);
                    helper.assertTrue(part.place(NetworkRecipe.ofProcessing(implant)), "the interface takes it");
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(implant, 1, "no wafer")))
                .thenWaitUntil(() -> helper.assertTrue(op.get().isDone(), "the job gives up"))
                .thenExecute(() -> {
                    helper.assertTrue(op.get().toRecord().status() == OperationRecord.STATUS_FAILED,
                            "it failed; status " + op.get().toRecord().status());
                    helper.assertTrue(heldIn(helper, dust) == 0, "nothing of the dust is left in the implanter; "
                            + heldIn(helper, dust));
                    final long stored = NetworkStorage.of(helper.getLevel(), net.mainframe().networkUuid())
                            .count(StorageKey.of(new ItemStack(dust)));
                    helper.assertTrue(stored == DUST, "all the dust is back on the network; " + stored);
                })
                .thenSucceed();
    }

    /** An interface against one of the series' machines, set neither way, runs one recipe at a time. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void exclusive_anInterfaceAgainstASeriesMachineRunsOneRecipeAtATime(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingInterfacePart part = implanter(helper, world);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> net.cc().forgetFloor())
                .thenExecuteAfter(SETTLE, () -> {
                    final CraftingFloor floor = CraftingFloor.around(helper.getLevel(), net.cc().getBlockPos());
                    final CraftingFloor.Site site = new CraftingFloor.Site(
                            helper.absolutePos(IMPLANTER_CORNER.north()), Direction.SOUTH);
                    final CraftingFloor.Reach reach = floor.reach(site);
                    helper.assertTrue(reach.mode() == CraftingFloor.Mode.DIRECT, "it feeds the implanter straight");
                    helper.assertFalse(part.exclusiveSet(), "nobody set it");
                    helper.assertTrue(CraftingDispatch.exclusive(helper.getLevel(), part, reach),
                            "it runs one recipe at a time");
                })
                .thenSucceed();
    }

    /**
     * One Lithography Station exposes two chips, each job lending it its own mask: the memory chip's job brings the
     * memory mask and takes it back when it ends, so the controller chip's job, bringing its own, finds the station
     * holding no other mask; with the memory mask left in it, the station held two and exposed nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void tools_oneStationServesTwoChipsEachJobLendingItsMask(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final ChainMachine station = ChainMachineSite.machine("lithography_vintage");
        final BlockPos terminal = ChainMachineSite.formed(helper, station, IMPLANTER_CORNER);
        final CraftingFloor.Site feeding = new CraftingFloor.Site(IMPLANTER_CORNER.offset(2, 0, -1), Direction.SOUTH);
        final CraftingFloor.Site receiving = new CraftingFloor.Site(IMPLANTER_CORNER.offset(3, 0, 1), Direction.WEST);
        CraftingRig.lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4), new BlockPos(4, 2, 4), feeding.cable(),
                new BlockPos(5, 2, 5), new BlockPos(5, 2, 6), receiving.cable());
        final CraftingInterfacePart part = CraftingRig.addPart(world, feeding,
                new CraftingInterfacePart(HardwareEra.STANDARD));
        CraftingRig.addPart(world, receiving, new ReceivingBusPart());
        final Item silicon = MaterialUses.item(Elements.of(Element.SILICON), Forms.WAFER).get();
        final Item memoryMask = ChainParts.MEMORY_MASKS.getFirst().get();
        final Item controllerMask = ChainParts.CONTROLLER_MASKS.getFirst().get();
        final ProcessingPattern memory = exposing(silicon, memoryMask, ChainParts.MEMORY_CHIPS.getFirst().get());
        final ProcessingPattern controller = exposing(silicon, controllerMask,
                ChainParts.CONTROLLER_CHIPS.getFirst().get());
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.rack().getServerStorage(0).insert(StorageKey.of(new ItemStack(silicon)), 2);
                    net.rack().getServerStorage(0).insert(StorageKey.of(new ItemStack(memoryMask)), 1);
                    net.rack().getServerStorage(0).insert(StorageKey.of(new ItemStack(controllerMask)), 1);
                    helper.assertTrue(part.place(NetworkRecipe.ofProcessing(memory))
                            && part.place(NetworkRecipe.ofProcessing(controller)), "the interface takes both");
                    final IFluidHandler resist = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                            helper.absolutePos(IMPLANTER_CORNER.offset(1, 1, 1)), Direction.UP);
                    helper.assertTrue(resist != null, "the station's liquid inlet takes its photoresist");
                    resist.fill(new FluidStack(MaterialLiquids.fluid(SeriesChemicals.PHOTORESIST).get(), BUCKET),
                            IFluidHandler.FluidAction.EXECUTE);
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(memory, 1, "memory")))
                .thenWaitUntil(() -> helper.assertTrue(op.get().isDone(), "the memory chip's layer is exposed"))
                .thenExecute(() -> {
                    helper.assertTrue(op.get().produced() == 1, "the memory chip's job made its wafer: "
                            + op.get().produced());
                    final long back = NetworkStorage.of(helper.getLevel(), net.mainframe().networkUuid())
                            .count(StorageKey.of(new ItemStack(memoryMask)));
                    helper.assertTrue(masksIn(helper, terminal) == 0 && back == 1, "it took its mask back to the "
                            + "network: the station holds " + masksIn(helper, terminal) + ", the network " + back);
                    op.set(net.mainframe().submitNetworkProcessing(controller, 1, "controller"));
                })
                .thenWaitUntil(() -> helper.assertTrue(op.get().isDone(), "the controller chip's layer is exposed"))
                .thenExecute(() -> {
                    helper.assertTrue(op.get().produced() == 1, "the same station made the controller chip's "
                            + "wafer: " + op.get().produced());
                    final NetworkStorage stored = NetworkStorage.of(helper.getLevel(), net.mainframe().networkUuid());
                    final long memoryBack = stored.count(StorageKey.of(new ItemStack(memoryMask)));
                    final long controllerBack = stored.count(StorageKey.of(new ItemStack(controllerMask)));
                    helper.assertTrue(memoryBack == 1 && controllerBack == 1 && masksIn(helper, terminal) == 0,
                            "both masks are back on the network: memory " + memoryBack + ", controller "
                                    + controllerBack + ", in the station " + masksIn(helper, terminal));
                })
                .thenSucceed();
    }

    /* A pattern exposing a silicon wafer's first layer of {@code chip} through {@code mask}, lent to each job. */
    private static ProcessingPattern exposing(final Item silicon, final Item mask, final Item chip) {
        final ItemStack exposed = new ItemStack(WaferWork.item(ChainParts.ERAS.getFirst(), WaferWork.Step.ETCH));
        exposed.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(BuiltInRegistries.ITEM.getKey(chip), 1,
                VINTAGE_LAYERS, WaferWork.Step.ETCH));
        return new ProcessingPattern(List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(new ItemStack(
                silicon)), 1), new ProcessingPattern.ProcessingInput(StorageKey.of(new ItemStack(mask)), 1, false,
                ProcessingPattern.ProcessingInput.MOST_LASTS)), List.of(new ProcessingPattern.ProcessingOutput(
                StorageKey.of(exposed), 1, ProcessingPattern.FULL_CHANCE)), STATION_TIMEOUT);
    }

    /* How many masks the station at {@code terminal} holds in its input slots. */
    private static int masksIn(final GameTestHelper helper, final BlockPos terminal) {
        final ProcessingMachineBlockEntity machine = ChainMachineSite.machineAt(helper, terminal);
        int held = 0;
        for (int slot = 0; slot < machine.layout().firstOutput(); slot++) {
            final ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (machine.usesAsTool(stack)) {
                held += stack.getCount();
            }
        }
        return held;
    }

    /* The ion implanter formed, an interface on its inlet, a Receiving Bus on its outlet, both on the network. */
    private static CraftingInterfacePart implanter(final GameTestHelper helper, final TestWorldBuilder world) {
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
        return part;
    }

    /* How much of {@code item} the implanter's input slots hold. */
    private static long heldIn(final GameTestHelper helper, final Item item) {
        final var machine = CraftingDispatch.machineAt(helper.getLevel(), helper.absolutePos(IMPLANTER_CORNER));
        if (machine.isEmpty() || !(machine.get() instanceof ProcessingMachineBlockEntity processing)) {
            return -1L;
        }
        long held = 0L;
        for (int slot = 0; slot < processing.layout().firstOutput(); slot++) {
            final ItemStack stack = processing.getInventory().getStackInSlot(slot);
            if (stack.is(item)) {
                held += stack.getCount();
            }
        }
        return held;
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
