/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.EnergyDistributionResult;
import dev.jstech.core.energy.EnergyUnit;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Energy in the Core: FE as the unit every other is counted against, energy kept in an item, and the energy grid moving
 * what a generator makes along the wires of a lane to the machines they plug into, losing what its cables lose; and
 * Industrial's energy cable, which loses nothing and limits nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EnergyGridGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GENERATOR = new BlockPos(1, 2, 4);
    private static final BlockPos MACHINE = new BlockPos(5, 2, 4);
    private static final BlockPos BESIDE = new BlockPos(4, 2, 5);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 4), new BlockPos(3, 2, 4),
            new BlockPos(4, 2, 4));
    private static final int FULL = 16_000;

    private EnergyGridGameTests() {
    }

    // Units and items

    @GameTest(template = ARENA)
    public static void fe_isTheUnitEveryOtherIsCountedAgainst(final GameTestHelper helper) {
        final EnergyUnit fe = CoreEnergy.FE.get();

        helper.assertTrue(CoreEnergy.UNITS.get(ResourceLocation.fromNamespaceAndPath("jscore", "fe")) == fe,
                "FE is registered as jscore:fe");
        helper.assertTrue(fe.toFe(1_234L) == 1_234L && fe.fromFe(1_234L) == 1_234L, "FE is worth one FE");
        helper.assertTrue(Objects.equals(fe.id(), ResourceLocation.fromNamespaceAndPath("jscore", "fe")),
                "FE knows its own id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itemEnergy_isKeptInTheItemAndTravelsWithIt(final GameTestHelper helper) {
        final ItemStack battery = new ItemStack(Items.STICK);
        final ItemEnergyStorage storage = new ItemEnergyStorage(battery, 1_000L, 300, 200);

        helper.assertTrue(storage.receiveEnergy(500, true) == 300, "it takes no more than it takes at a time");
        helper.assertTrue(storage.getEnergyStored() == 0, "a simulation changes nothing");
        helper.assertTrue(storage.receiveEnergy(500, false) == 300, "it takes 300");
        helper.assertTrue(battery.getOrDefault(CoreEnergy.ENERGY.get(), 0L) == 300L, "the item holds what it took");
        helper.assertTrue(storage.extractEnergy(1_000, false) == 200, "it gives no more than it gives at a time");
        final ItemStack copy = battery.copy();
        helper.assertTrue(new ItemEnergyStorage(copy, 1_000L, 300, 200).getEnergyStored() == 100,
                "a copy of the item holds what the item held");
        storage.setStored(5_000L);
        helper.assertTrue(storage.stored() == 1_000L, "it never holds more than it can");
        helper.succeed();
    }

    // The grid

    @GameTest(template = ARENA)
    public static void energyLane_runsBesideDataAndCarriesEnergyAlone(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper);
        final CompressorBlockEntity machine = compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, IndustrialModule.ENERGY_CABLE);
            TestCables.lay(helper, at, ComputingModule.ETHERNET_CABLE);
        }

        helper.succeedWhen(() -> {
            final CableBlockEntity last = TestCables.cable(helper, RUN.get(RUN.size() - 1));
            helper.assertTrue(last.wires().size() == 2, "energy and data share the blocks");
            helper.assertTrue(last.wire(IndustrialModule.ENERGY_CABLE.get()).slot() == Lane.BOTTOM_LEFT,
                    "energy runs in the bottom left lane");
            final int east = 1 << Direction.EAST.get3DDataValue();
            helper.assertTrue((last.plugs(Lane.BOTTOM_LEFT) & east) != 0, "the energy wire plugs into the machine");
            helper.assertTrue((last.plugs(Lane.TOP_LEFT) & east) == 0, "the data wire does not");
            helper.assertTrue(CoreGrids.contains(helper.getLevel(), GridKind.POWER,
                    last.place(IndustrialModule.ENERGY_CABLE.get())), "the energy wire is in the energy grid");
            helper.assertTrue(!CoreGrids.contains(helper.getLevel(), GridKind.POWER,
                    last.place(ComputingModule.ETHERNET_CABLE.get())), "the data wire is not");
            helper.assertTrue(machine.getEnergy().getEnergyStored() > 0, "energy arrives at the machine");
            helper.assertTrue(FULL - generator.getEnergy().getEnergyStored() == machine.getEnergy().getEnergyStored(),
                    "every FE the generator gave arrived");
        });
    }

    @GameTest(template = ARENA)
    public static void lossyCables_loseTheirThousandthsOnTheWay(final GameTestHelper helper) {
        generator(helper);
        compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.LOSSY_ENERGY);
        }

        helper.succeedWhen(() -> {
            final EnergyDistributionResult tick = lastTick(helper, TestCableTypes.LOSSY_ENERGY.get());
            // Three cables each losing a tenth: the 600 the machine takes means 858 sent, of which 258 are lost.
            helper.assertTrue(tick.totalDelivered() == 600L, "the machine gets all it takes in a tick: "
                    + tick.totalDelivered());
            helper.assertTrue(tick.totalLost() == 258L, "the cables lose three tenths, rounded up: "
                    + tick.totalLost());
        });
    }

    // Industrial's energy cable

    @GameTest(template = ARENA)
    public static void energyCable_feedsMachinesWithoutLossOrLimit(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper);
        final CompressorBlockEntity first = compressor(helper, MACHINE);
        final CompressorBlockEntity second = compressor(helper, BESIDE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, IndustrialModule.ENERGY_CABLE);
        }

        helper.succeedWhen(() -> {
            final EnergyDistributionResult tick = lastTick(helper, IndustrialModule.ENERGY_CABLE.get());
            // The generator gives a thousand a tick, and the two machines want six hundred each.
            helper.assertTrue(tick.totalDelivered() == 1_000L, "the cable carries all the generator gives: "
                    + tick.totalDelivered());
            helper.assertTrue(tick.totalLost() == 0L, "the cable loses nothing");
            final int got = first.getEnergy().getEnergyStored() + second.getEnergy().getEnergyStored();
            helper.assertTrue(FULL - generator.getEnergy().getEnergyStored() == got,
                    "every FE the generator gave arrived");
            helper.assertTrue(first.getEnergy().getEnergyStored() == second.getEnergy().getEnergyStored(),
                    "the two machines share it evenly");
        });
    }

    @GameTest(template = ARENA)
    public static void energyCable_stopsFeedingWhenARunIsCut(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper);
        final CompressorBlockEntity machine = compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, IndustrialModule.ENERGY_CABLE);
        }

        final int[] before = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.getEnergy().getEnergyStored() > 0, "energy flows"))
                .thenExecute(() -> TestCables.cable(helper, RUN.get(1)).take(IndustrialModule.ENERGY_CABLE.get()))
                .thenIdle(2)
                .thenExecute(() -> before[0] = generator.getEnergy().getEnergyStored())
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(generator.getEnergy().getEnergyStored() == before[0],
                        "nothing leaves a cut run"))
                .thenSucceed();
    }

    /* What the part of the grid the run's first wire is in moved in the last tick. */
    private static EnergyDistributionResult lastTick(final GameTestHelper helper, final CableType type) {
        final CableBlockEntity first = TestCables.cable(helper, RUN.get(0));
        return CoreEnergy.lastTickAt(helper.getLevel(), first.place(type))
                .orElseGet(EnergyDistributionResult::empty);
    }

    private static CoalGeneratorBlockEntity generator(final GameTestHelper helper) {
        helper.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get());
        final CoalGeneratorBlockEntity generator = machine(helper, GENERATOR, CoalGeneratorBlockEntity.class);
        generator.getEnergy().setEnergyStored(FULL);
        return generator;
    }

    private static CompressorBlockEntity compressor(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, IndustrialModule.COMPRESSOR.get());
        return machine(helper, at, CompressorBlockEntity.class);
    }

    private static <E extends BlockEntity> E machine(final GameTestHelper helper, final BlockPos at,
                                                     final Class<E> type) {
        final BlockEntity found = helper.getBlockEntity(at);
        if (!type.isInstance(found)) {
            helper.fail("no " + type.getSimpleName() + " at " + at);
        }
        return type.cast(found);
    }
}
