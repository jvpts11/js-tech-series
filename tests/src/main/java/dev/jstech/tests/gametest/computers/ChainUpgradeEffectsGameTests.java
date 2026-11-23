/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.CORNER;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.holds;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.made;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.place;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Each effect of the shared upgrades, proven on the chain's machines at work: motors make a Crusher faster and draw
 * more through its power entry; a Parallel Line works two sets at once; Precision Tooling makes more and two Byproduct
 * Separators make a byproduct of one chance in two come every time; a Solvent Recovery Unit leaves water in the
 * Electrolytic Cell and a Gas Recovery Unit hydrogen and chlorine in the Chemical Reactor; a Muffler silences a
 * Crusher at work.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainUpgradeEffectsGameTests {

    private static final String ARENA = "empty";
    private static final String ENERGY = TestComputerEnergy.BATCH;
    /* A second Crusher beside the first, clear of it. */
    private static final BlockPos BESIDE = CORNER.offset(5, 0, 0);
    private static final double CLOSE = 0.01;
    /* How many ferrites the Precision Tooling crushes: two inductors each, none more without it. */
    private static final int FERRITES = 20;
    /* How many zinc ores two separators crush: their sulfur, one chance in two, comes every time. */
    private static final int ZINC = 10;
    private static final int WATER = 1_000;
    private static final long GAS = 500L;
    /* Ticks of work past a crusher's store of four seconds at four times its draw: some forty, and a margin. */
    private static final int STEADY = 80;

    private ChainUpgradeEffectsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 1_200)
    public static void motors_makeACrusherFaster(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        final BlockPos plain = formed(helper, crusher);
        final BlockPos fast = formed(helper, crusher, BESIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, plain).formed()
                        && machineAt(helper, fast).formed(), "both crushers form"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, fast), new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 4));
                    machineAt(helper, plain).getInventory().insertItem(0, new ItemStack(ChainParts.FERRITE.get()),
                            false);
                    machineAt(helper, fast).getInventory().insertItem(0, new ItemStack(ChainParts.FERRITE.get()),
                            false);
                })
                .thenWaitUntil(() -> helper.assertTrue(holds(machineAt(helper, fast), ChainParts.INDUCTOR.get(), 2),
                        "the crusher of four motors crushes its ferrite"))
                .thenExecute(() -> helper.assertTrue(!holds(machineAt(helper, plain), ChainParts.INDUCTOR.get(), 1),
                        "while the plain one is still at it"))
                .thenSucceed();
    }

    /*
     * Four motors draw four times the energy a tick, and the power entry lets that much in for as long as the crusher
     * works, past its few seconds' store: the draw a tick is read once the store has had time to run dry.
     */
    @GameTest(template = ARENA, timeoutTicks = 1_200, batch = ENERGY)
    public static void motors_drawMoreThroughThePowerEntry(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        final BlockPos plain = formed(helper, crusher);
        final BlockPos fast = formed(helper, crusher, BESIDE);
        final BlockPos plainEntry = helper.absolutePos(place(crusher, "power_entry"));
        final BlockPos fastEntry = helper.absolutePos(place(crusher, "power_entry").subtract(CORNER).offset(BESIDE));
        // How many ticks the four motors' crusher has worked drawing, and what each drew its last such tick.
        final long[] worked = new long[1];
        final long[] last = new long[2];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, plain).formed()
                        && machineAt(helper, fast).formed(), "both crushers form"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, fast), new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 4));
                    machineAt(helper, plain).getInventory().insertItem(0,
                            new ItemStack(ChainParts.FERRITE.get(), FERRITES), false);
                    machineAt(helper, fast).getInventory().insertItem(0,
                            new ItemStack(ChainParts.FERRITE.get(), FERRITES), false);
                })
                .thenWaitUntil(() -> {
                    // A cable at each power entry gives it a tick's worth every tick.
                    TestComputerEnergy.feed(machineAt(helper, plain).energyClassPort(PortKind.ENERGY_INPUT,
                            plainEntry, Direction.SOUTH));
                    TestComputerEnergy.feed(machineAt(helper, fast).energyClassPort(PortKind.ENERGY_INPUT,
                            fastEntry, Direction.SOUTH));
                    final long plainDrew = machineAt(helper, plain).drawn();
                    final long fastDrew = machineAt(helper, fast).drawn();
                    if (plainDrew > 0 && fastDrew > 0) {
                        worked[0]++;
                        last[0] = plainDrew;
                        last[1] = fastDrew;
                    }
                    helper.assertTrue(worked[0] >= STEADY, "both work past their store: " + worked[0] + " ticks");
                })
                .thenExecute(() -> helper.assertTrue(Math.abs(last[1] / (double) last[0] - 4.0) < CLOSE,
                        "four motors draw four times as much a tick, the store long dry: " + last[1] + " to "
                                + last[0]))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void parallelLine_crushesTwoSetsAtOnce(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher forms"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, terminal), new ItemStack(SharedUpgrades.PARALLEL_LINE.get()));
                    machineAt(helper, terminal).getInventory().insertItem(0,
                            new ItemStack(ChainParts.FERRITE.get(), 2), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 1),
                        "it crushes"))
                .thenExecute(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 4),
                        "both ferrites at once, four inductors in one go: " + made(machineAt(helper, terminal),
                                ChainParts.INDUCTOR.get())))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 12_000)
    public static void precisionTooling_makesMoreOfWhatItCrushes(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher forms"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, terminal), new ItemStack(SharedUpgrades.PRECISION_TOOLING.get(), 2));
                    machineAt(helper, terminal).getInventory().insertItem(0,
                            new ItemStack(ChainParts.FERRITE.get(), FERRITES), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).getInventory().getStackInSlot(0)
                        .isEmpty() && !machineAt(helper, terminal).running(), "it crushes every ferrite"))
                .thenExecute(() -> {
                    final int made = made(machineAt(helper, terminal), ChainParts.INDUCTOR.get()).getCount();
                    // Two each would make 40; a fifth more makes 48 on the whole, never fewer than 40.
                    helper.assertTrue(made > FERRITES * 2 && made <= FERRITES * 3, "more than two inductors a "
                            + "ferrite on the whole: " + made);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 6_000)
    public static void separators_makeAByproductComeEveryTime(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher forms"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, terminal), new ItemStack(SharedUpgrades.BYPRODUCT_SEPARATOR.get(), 2));
                    machineAt(helper, terminal).getInventory().insertItem(0, new ItemStack(MaterialUses.oreBlock(
                            Elements.of(Element.ZINC), HostRocks.STONE).get(), ZINC), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).getInventory().getStackInSlot(0)
                        .isEmpty() && !machineAt(helper, terminal).running(), "it crushes every zinc ore"))
                .thenExecute(() -> {
                    final ItemStack sulfur = made(machineAt(helper, terminal),
                            MaterialUses.item(Elements.of(Element.SULFUR), Forms.DUST).get());
                    helper.assertTrue(sulfur.getCount() == ZINC, "two separators double sulfur's one chance in two: "
                            + "it comes every time: " + sulfur.getCount() + " of " + ZINC);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1_200)
    public static void solventRecovery_leavesWaterInTheCell(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("electrolytic_cell"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the cell forms"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity cell = machineAt(helper, terminal);
                    upgrade(cell, new ItemStack(SharedUpgrades.SOLVENT_RECOVERY_UNIT.get()));
                    cell.inputTank(0).fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);
                    cell.getInventory().insertItem(0, new ItemStack(MaterialUses.item(SeriesCompounds.SALT,
                            Forms.DUST).get()), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(holds(machineAt(helper, terminal), MaterialUses.item(
                        SeriesCompounds.CAUSTIC_SODA, Forms.DUST).get(), 1), "the cell splits its salt water"))
                .thenExecute(() -> helper.assertTrue(machineAt(helper, terminal).inputTank(0).getFluidAmount()
                        == WATER - 850, "it spent 850 of its 1,000 mB of water: "
                        + machineAt(helper, terminal).inputTank(0).getFluidAmount() + " left"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1_200)
    public static void gasRecovery_leavesGasInTheReactor(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("chemical_reactor"));
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        final Material chlorine = Elements.of(Element.CHLORINE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the reactor forms"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity reactor = machineAt(helper, terminal);
                    upgrade(reactor, new ItemStack(SharedUpgrades.GAS_RECOVERY_UNIT.get()));
                    final List<MatterTank> gases = reactor.matter().inputs(MatterState.GAS);
                    gases.get(0).insert(new MatterStack(hydrogen, GAS), false);
                    gases.get(1).insert(new MatterStack(chlorine, GAS), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(held(machineAt(helper, terminal).matter()
                        .outputs(MatterState.GAS), SeriesChemicals.HYDROGEN_CHLORIDE) > 0,
                        "it makes hydrogen chloride"))
                .thenExecute(() -> {
                    final List<MatterTank> gases = machineAt(helper, terminal).matter().inputs(MatterState.GAS);
                    helper.assertTrue(held(gases, hydrogen) == GAS - 425 && held(gases, chlorine) == GAS - 425,
                            "it spent 425 of each 500 mB of gas: " + held(gases, hydrogen) + " hydrogen and "
                                    + held(gases, chlorine) + " chlorine left");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void muffler_silencesACrusherAtWork(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher forms"))
                .thenExecute(() -> {
                    upgrade(machineAt(helper, terminal), new ItemStack(SharedUpgrades.MUFFLER.get()));
                    machineAt(helper, terminal).getInventory().insertItem(0,
                            new ItemStack(ChainParts.FERRITE.get(), 4), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).running(), "it works"))
                .thenExecute(() -> helper.assertTrue(machineAt(helper, terminal).loops().isEmpty(),
                        "and runs no loop at all: " + machineAt(helper, terminal).loops()))
                .thenSucceed();
    }

    /* Puts {@code upgrade} into the machine's first upgrade slot, as a hopper would. */
    private static void upgrade(final ChainMachineBlockEntity machine, final ItemStack upgrade) {
        machine.getInventory().insertItem(machine.layout().firstUpgrade(), upgrade, false);
    }

    private static long held(final List<MatterTank> tanks, final Material material) {
        return tanks.stream().mapToLong(tank -> tank.held().filter(stack -> stack.material() == material)
                .map(MatterStack::amount).orElse(0L)).sum();
    }
}
