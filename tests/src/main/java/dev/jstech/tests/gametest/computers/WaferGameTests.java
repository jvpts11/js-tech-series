/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.holds;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.made;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.place;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.WaferMachineBlockEntity;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A chip made layer by layer (computers/docs/design/survival.md, "How a chip is made"): a Vintage memory chip's wafer
 * through each machine in turn. The Lithography Station starts it from a silicon wafer and its mask, which stays in
 * its slot; the Plasma Etcher etches the layer with its acid; the Ion Implanter dopes it, one dust doping eight layers;
 * the Deposition Chamber lays the wiring and sends it back for its next layer, or finishes it on its last; the
 * Packaging Line cuts the finished wafer into its eight chips, each in its ceramic package. A machine makes nothing
 * from more than it takes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WaferGameTests {

    private static final String ARENA = "empty";
    /* A Vintage chip's layers, and the chips a memory wafer is cut into. */
    private static final int VINTAGE_LAYERS = 2;
    private static final int MEMORY_CHIPS = 8;
    private static final int BUCKET = 1_000;

    private WaferGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void lithography_startsAMemoryChipFromAWaferAndItsMask(final GameTestHelper helper) {
        final ChainMachine station = machine("lithography_vintage");
        final BlockPos terminal = formed(helper, station);
        helper.runAfterDelay(20, () -> {
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, new ItemStack(MaterialUses.item(Elements.of(Element.SILICON), Forms.WAFER).get()),
                    false);
            in.insertItem(1, new ItemStack(ChainParts.MEMORY_MASKS.getFirst().get()), false);
            pour(helper, station, SeriesChemicals.PHOTORESIST);
        });
        helper.succeedWhen(() -> {
            final ChainMachineBlockEntity machine = machineAt(helper, terminal);
            final WaferWork work = made(machine, ChainChips.waferInProcess(ChainParts.ERAS.getFirst()).get())
                    .get(ComputingComponents.WAFER_WORK.get());
            helper.assertTrue(work != null && work.layer() == 1 && work.layers() == VINTAGE_LAYERS
                    && work.next() == WaferWork.Step.ETCH
                    && work.chip().equals(id(ChainParts.MEMORY_CHIPS.getFirst().get())),
                    "a wafer in process on its first layer of a memory chip, for the etcher");
            helper.assertTrue(machine.getInventory().getStackInSlot(1).is(ChainParts.MEMORY_MASKS.getFirst().get()),
                    "the mask stays in its slot, never used up");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void etcher_etchesTheLayerWithItsAcid(final GameTestHelper helper) {
        final ChainMachine etcher = machine("plasma_etcher");
        final BlockPos terminal = formed(helper, etcher);
        helper.runAfterDelay(20, () -> {
            machineAt(helper, terminal).getInventory().insertItem(0, wafer(1, WaferWork.Step.ETCH), false);
            pour(helper, etcher, SeriesChemicals.HYDROFLUORIC_ACID);
        });
        helper.succeedWhen(() -> helper.assertTrue(next(helper, terminal) == WaferWork.Step.IMPLANT,
                "the etched wafer goes on to the implanter"));
    }

    /* What a machine was given is its own to spend: nothing draws its acid back out, whichever way it asks. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void etcher_givesNoneOfTheAcidItWasGivenBackOut(final GameTestHelper helper) {
        final ChainMachine etcher = machine("plasma_etcher");
        final BlockPos terminal = formed(helper, etcher);
        helper.runAfterDelay(20, () -> {
            pour(helper, etcher, SeriesChemicals.HYDROFLUORIC_ACID);
            final IFluidHandler whole = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(terminal), null);
            helper.assertTrue(whole != null, "the machine offers its tanks");
            final FluidStack acid = new FluidStack(MaterialLiquids.fluid(SeriesChemicals.HYDROFLUORIC_ACID).get(),
                    BUCKET);
            helper.assertTrue(whole.drain(BUCKET, IFluidHandler.FluidAction.EXECUTE).isEmpty()
                            && whole.drain(acid, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "no pipe or bucket draws the acid back out of the machine");
            int held = 0;
            for (int tank = 0; tank < whole.getTanks(); tank++) {
                held += whole.getFluidInTank(tank).getAmount();
            }
            helper.assertTrue(held == BUCKET, "the bucket of acid stays in the machine; it holds " + held);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void implanter_dopesEightLayersWithOneDust(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("ion_implanter"));
        final Item phosphorus = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
        helper.runAfterDelay(20, () -> {
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, wafer(1, WaferWork.Step.IMPLANT), false);
            in.insertItem(1, new ItemStack(phosphorus), false);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(next(helper, terminal) == WaferWork.Step.DEPOSITION,
                    "the doped wafer goes on to the deposition chamber");
            helper.assertTrue(((WaferMachineBlockEntity) machineAt(helper, terminal)).layersLeft() == 7
                    && !holds(machineAt(helper, terminal), phosphorus, 1), "the dust is spent, good for seven more");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void implanter_dopesTheNextLayerWhileTheDustLeftWaits(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("ion_implanter"));
        final Item phosphorus = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    final IItemHandler in = machineAt(helper, terminal).getInventory();
                    in.insertItem(0, wafer(1, WaferWork.Step.IMPLANT), false);
                    in.insertItem(1, new ItemStack(phosphorus, 2), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        ((WaferMachineBlockEntity) machineAt(helper, terminal)).layersLeft() == 7,
                        "the first layer takes a dust, good for seven more"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity implanter = machineAt(helper, terminal);
                    final IItemHandler in = implanter.getInventory();
                    for (int slot = 0; slot < in.getSlots(); slot++) {
                        if (implanter.layout().isOutput(slot)) {
                            in.extractItem(slot, 64, false);
                        }
                    }
                    in.insertItem(0, wafer(2, WaferWork.Step.IMPLANT), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        ((WaferMachineBlockEntity) machineAt(helper, terminal)).layersLeft() == 6,
                        "the next layer is doped with the dust still standing in its slot: "
                                + machineAt(helper, terminal).status() + " "
                                + contents(machineAt(helper, terminal).getInventory())))
                .thenExecute(() -> helper.assertTrue(
                        machineAt(helper, terminal).getInventory().getStackInSlot(1).getCount() == 1,
                        "and that dust waits there, untouched"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void deposition_laysTheWiringAndSendsTheWaferBack(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("deposition_chamber"));
        helper.runAfterDelay(20, () -> {
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, wafer(1, WaferWork.Step.DEPOSITION), false);
            in.insertItem(1, new ItemStack(MaterialUses.item(Elements.of(Element.ALUMINIUM), Forms.NUGGET).get()),
                    false);
        });
        helper.succeedWhen(() -> {
            final WaferWork work = made(machineAt(helper, terminal),
                    ChainChips.waferInProcess(ChainParts.ERAS.getFirst()).get()).get(ComputingComponents.WAFER_WORK
                    .get());
            helper.assertTrue(work != null && work.layer() == 2 && work.next() == WaferWork.Step.LITHOGRAPHY,
                    "the wafer goes back to the station for its second layer");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void deposition_finishesTheWaferOnItsLastLayer(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("deposition_chamber"));
        helper.runAfterDelay(20, () -> {
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, wafer(VINTAGE_LAYERS, WaferWork.Step.DEPOSITION), false);
            in.insertItem(1, new ItemStack(MaterialUses.item(Elements.of(Element.ALUMINIUM), Forms.NUGGET).get()),
                    false);
        });
        helper.succeedWhen(() -> {
            final WaferWork work = made(machineAt(helper, terminal), ChainParts.FINISHED_WAFERS.getFirst().get())
                    .get(ComputingComponents.WAFER_WORK.get());
            helper.assertTrue(work != null && work.next() == WaferWork.Step.PACKAGING,
                    "the last layer done, the wafer is finished, for the packaging line");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void packaging_cutsAFinishedWaferIntoItsChips(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("packaging_line"));
        helper.runAfterDelay(20, () -> {
            final ItemStack finished = new ItemStack(ChainParts.FINISHED_WAFERS.getFirst().get());
            finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(id(ChainParts.MEMORY_CHIPS.getFirst()
                    .get()), VINTAGE_LAYERS, VINTAGE_LAYERS, WaferWork.Step.PACKAGING));
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, finished, false);
            in.insertItem(1, new ItemStack(ChainParts.CERAMIC_PACKAGE.get(), MEMORY_CHIPS), false);
            in.insertItem(2, new ItemStack(ChainParts.ALUMINIUM_BONDING_WIRE.get(), 2 * MEMORY_CHIPS), false);
        });
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal),
                ChainParts.MEMORY_CHIPS.getFirst().get(), MEMORY_CHIPS), "eight memory chips, each in ceramic"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lithography_makesNothingFromMoreThanItTakes(final GameTestHelper helper) {
        final ChainMachine station = machine("lithography_vintage");
        final BlockPos terminal = formed(helper, station);
        helper.runAfterDelay(20, () -> {
            final IItemHandler in = machineAt(helper, terminal).getInventory();
            in.insertItem(0, new ItemStack(MaterialUses.item(Elements.of(Element.SILICON), Forms.WAFER).get(), 2),
                    false);
            in.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
            pour(helper, station, SeriesChemicals.PHOTORESIST);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(!holds(machineAt(helper, terminal),
                    ChainChips.waferInProcess(ChainParts.ERAS.getFirst()).get(), 1),
                    "no wafer is started with no mask, a stray block in the mask's slot");
            helper.succeed();
        });
    }

    /* What each slot of {@code inventory} holds, for a failure to say. */
    private static String contents(final IItemHandler inventory) {
        final StringBuilder out = new StringBuilder();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            out.append(slot).append('=').append(inventory.getStackInSlot(slot)).append(' ');
        }
        return out.toString();
    }

    /* A Vintage memory chip's wafer in process on {@code layer}, for {@code step}. */
    private static ItemStack wafer(final int layer, final WaferWork.Step step) {
        final ItemStack wafer = new ItemStack(ChainChips.waferInProcess(ChainParts.ERAS.getFirst()).get());
        wafer.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(id(ChainParts.MEMORY_CHIPS.getFirst().get()),
                layer, VINTAGE_LAYERS, step));
        return wafer;
    }

    /* Where the wafer the machine made stands, or none before it made one. */
    private static WaferWork.Step next(final GameTestHelper helper, final BlockPos terminal) {
        final WaferWork work = made(machineAt(helper, terminal), ChainChips.waferInProcess(ChainParts.ERAS.getFirst())
                .get()).get(ComputingComponents.WAFER_WORK.get());
        return work == null ? null : work.next();
    }

    /* A bucket of {@code liquid} poured in through the machine's liquid inlet. */
    private static void pour(final GameTestHelper helper, final ChainMachine machine, final Material liquid) {
        final IFluidHandler inlet = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(place(machine, "liquid_inlet")), Direction.UP);
        helper.assertTrue(inlet != null, "the liquid inlet opens onto the machine");
        inlet.fill(new FluidStack(MaterialLiquids.fluid(liquid).get(), BUCKET), IFluidHandler.FluidAction.EXECUTE);
    }

    private static ResourceLocation id(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
