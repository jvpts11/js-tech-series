/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.EnergyDistributionResult;
import dev.jstech.core.energy.EnergyUnit;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
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
 * what a generator makes along the wires of a lane to the machines they plug into, losing what its cables lose; and the
 * series' basic cable, the Insulated Copper Cable, carrying J's Industrial's FE while J's Energy is off, up to its
 * ceiling and losing a little.
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
        generator(helper);
        final CompressorBlockEntity machine = compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedEnergy.INSULATED_COPPER_CABLE);
            TestCables.lay(helper, at, ComputingModule.ETHERNET_CABLE);
        }

        helper.succeedWhen(() -> {
            final CableBlockEntity last = TestCables.cable(helper, RUN.get(RUN.size() - 1));
            final GridKind lowVoltage = JsEnergy.ENERGY.grid(JsEnergy.LOW);
            helper.assertTrue(last.wires().size() == 2, "energy and data share the blocks");
            helper.assertTrue(last.wire(SharedEnergy.INSULATED_COPPER_CABLE.get()).slot() == Lane.BOTTOM,
                    "energy runs in the bottom lane");
            final int east = 1 << Direction.EAST.get3DDataValue();
            helper.assertTrue((last.plugs(Lane.BOTTOM) & east) != 0, "the energy wire plugs into the machine");
            helper.assertTrue((last.plugs(Lane.TOP_LEFT) & east) == 0, "the data wire does not");
            helper.assertTrue(CoreGrids.contains(helper.getLevel(), lowVoltage,
                    last.place(SharedEnergy.INSULATED_COPPER_CABLE.get())), "the energy wire is in the energy grid");
            helper.assertTrue(!CoreGrids.contains(helper.getLevel(), lowVoltage,
                    last.place(ComputingModule.ETHERNET_CABLE.get())), "the data wire is not");
            helper.assertTrue(machine.getEnergy().getEnergyStored() > 0, "energy arrives at the machine");
        });
    }

    @GameTest(template = ARENA)
    public static void lossyCables_loseTheirShareOnTheWay(final GameTestHelper helper) {
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

    @GameTest(template = ARENA)
    public static void aModsOwnGrid_sharesEnergyByTheCoresPolicy(final GameTestHelper helper) {
        generator(helper);
        compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.OWN_GRID_ENERGY);
        }

        helper.succeedWhen(() -> {
            final CableBlockEntity first = TestCables.cable(helper, RUN.get(0));
            final OptionalLong found = CoreGrids.places(helper.getLevel())
                    .find(first.place(TestCableTypes.OWN_GRID_ENERGY.get()));
            helper.assertTrue(found.isPresent(), "the first wire has its place in a grid");
            final long number = found.getAsLong();
            helper.assertTrue(CoreGrids.of(helper.getLevel(), TestCableTypes.OWN_POWER).contains(number)
                    && !CoreGrids.of(helper.getLevel(), GridKind.POWER).contains(number),
                    "the wires are in the mod's own grid, not the Core's");
            final EnergyDistributionResult tick = CoreGrids.mover(helper.getLevel(), TestCableTypes.OWN_POWER)
                    .map(mover -> ((LevelEnergy) mover).lastTickOf(number)
                            .orElseGet(EnergyDistributionResult::empty))
                    .orElseGet(EnergyDistributionResult::empty);
            helper.assertTrue(tick.totalDelivered() == 600L, "and carry the energy the machine takes: "
                    + tick.totalDelivered());
        });
    }

    @GameTest(template = ARENA)
    public static void grids_areARegistryEachWithItsPolicy(final GameTestHelper helper) {
        helper.assertTrue(GridKind.all().containsAll(List.of(GridKind.POWER, GridKind.FLUID, GridKind.DATA,
                TestCableTypes.OWN_POWER)), "the Core's grids and a mod's");
        helper.assertTrue(GridKind.byId(TestCableTypes.OWN_POWER.id()).orElseThrow() == TestCableTypes.OWN_POWER
                && GridKind.POWER.policy() == GridPolicies.ENERGY && GridKind.FLUID.policy() == GridPolicies.FLUID
                && GridKind.DATA.carriesNetwork() && !GridKind.PERIPHERAL.carriesNetwork(),
                "each found by its id, with its policy");
        helper.assertTrue(CoreGrids.mover(helper.getLevel(), GridKind.PERIPHERAL).isEmpty()
                && CoreGrids.mover(helper.getLevel(), GridKind.DATA).isEmpty(),
                "a grid that only joins moves nothing");
        helper.succeed();
    }

    // The series' basic cable, which J's Industrial's machines run on

    @GameTest(template = ARENA)
    public static void basicCable_feedsMachinesUpToItsCeilingLosingALittle(final GameTestHelper helper) {
        generator(helper);
        final CompressorBlockEntity first = compressor(helper, MACHINE);
        final CompressorBlockEntity second = compressor(helper, BESIDE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedEnergy.INSULATED_COPPER_CABLE);
        }

        helper.succeedWhen(() -> {
            final EnergyDistributionResult tick = lastTick(helper, SharedEnergy.INSULATED_COPPER_CABLE.get());
            // The generator gives a thousand a tick and the two machines want six hundred each: the cable carries less.
            helper.assertTrue(tick.totalDelivered() > 0L, "the cable carries energy: " + tick);
            helper.assertTrue(tick.totalDelivered() + tick.totalLost() <= SharedEnergy.CABLE_CEILING,
                    "no more than its ceiling a tick: " + tick);
            helper.assertTrue(tick.totalLost() > 0L, "and loses a little on the way: " + tick);
            helper.assertTrue(first.getEnergy().getEnergyStored() > 0
                    && first.getEnergy().getEnergyStored() == second.getEnergy().getEnergyStored(),
                    "the two machines share it evenly: " + first.getEnergy().getEnergyStored() + " and "
                            + second.getEnergy().getEnergyStored());
        });
    }

    @GameTest(template = ARENA)
    public static void basicCable_stopsFeedingWhenARunIsCut(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper);
        final CompressorBlockEntity machine = compressor(helper, MACHINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedEnergy.INSULATED_COPPER_CABLE);
        }

        final int[] before = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.getEnergy().getEnergyStored() > 0, "energy flows"))
                .thenExecute(() -> TestCables.cable(helper, RUN.get(1)).take(SharedEnergy.INSULATED_COPPER_CABLE.get()))
                .thenIdle(2)
                .thenExecute(() -> before[0] = generator.getEnergy().getEnergyStored())
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(generator.getEnergy().getEnergyStored() == before[0],
                        "nothing leaves a cut run"))
                .thenSucceed();
    }

    /* What the part of the grid the run's first wire is in moved in the last tick, in its own kind of grid. */
    private static EnergyDistributionResult lastTick(final GameTestHelper helper, final CableType type) {
        final CableBlockEntity first = TestCables.cable(helper, RUN.get(0));
        final OptionalLong number = CoreGrids.places(helper.getLevel()).find(first.place(type));
        if (number.isEmpty()) {
            return EnergyDistributionResult.empty();
        }
        return CoreGrids.mover(helper.getLevel(), type.grid())
                .flatMap(mover -> ((LevelEnergy) mover).lastTickOf(number.getAsLong()))
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
