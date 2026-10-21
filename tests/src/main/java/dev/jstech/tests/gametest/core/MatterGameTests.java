/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.chemical.IChemicalPort;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.integration.mekanism.MekanismIntegration;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.ExoticMatter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.ContainmentTank;
import dev.jstech.core.matter.MatterBridges;
import dev.jstech.core.matter.MatterGrids;
import dev.jstech.core.matter.MatterRelease;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.TestFluids;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.TestMatterEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The states of matter that are neither solid nor liquid: a tank holds its volume times its pressure, of one material
 * at a time; a line of each state moves only its own, from an output into an input or a tank, at what it carries in a
 * tick, and loses nothing; a gas kept as a fluid by another mod, or as a chemical, comes in through its bridge as the
 * Core's material; a multiblock's ports pass each state; plasma dissipates from a containment that loses its energy,
 * and exotic matter stays sealed in; a gas let out is lost, exotic matter is never let out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MatterGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final ResourceLocation MEKANISM_HYDROGEN = ResourceLocation.fromNamespaceAndPath("mekanism",
            "hydrogen");
    private static final ResourceLocation MEKANISM_SLURRY = ResourceLocation.fromNamespaceAndPath("mekanism",
            "dirty_iron");
    private static final ResourceLocation MEKANISM_TANK = ResourceLocation.fromNamespaceAndPath("mekanism",
            "basic_chemical_tank");

    private MatterGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tank_holdsItsVolumeTimesItsPressureOfOneMaterial(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        final MatterTank tank = new MatterTank(MatterState.GAS, 1_000L, 200L, () -> { });

        helper.assertTrue(tank.capacity() == 200_000L, "a thousand millibuckets at 200 times the air's pressure");
        helper.assertTrue(tank.insert(new MatterStack(hydrogen, 250_000L), false) == 200_000L,
                "it takes what it holds and no more");
        helper.assertTrue(tank.insert(new MatterStack(Elements.of(Element.OXYGEN), 10L), true) == 0L,
                "a tank holding hydrogen takes no oxygen");
        helper.assertTrue(new MatterTank(MatterState.GAS, 10L, 1L, () -> { })
                        .insert(new MatterStack(Elements.of(Element.IRON), 10L), true) == 0L,
                "nor a gas tank iron, which is no gas");
        helper.assertTrue(tank.extract(hydrogen, 50_000L, false) == 50_000L, "it gives back what is asked");

        final CompoundTag saved = new CompoundTag();
        tank.save(saved);
        final MatterTank loaded = new MatterTank(MatterState.GAS, 1_000L, 200L, () -> { });
        loaded.load(saved);
        helper.assertTrue(loaded.held().equals(Optional.of(new MatterStack(hydrogen, 150_000L))),
                "what it holds is saved with it: " + loaded.held());
        helper.assertTrue(loaded.release().map(MatterStack::amount).orElse(0L) == 150_000L
                && loaded.held().isEmpty(), "and a broken tank lets it all out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gasLine_movesWhatItCarriesFromAnOutputIntoAnInput(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        ends(helper, Blocks.CHISELED_COPPER, Blocks.WAXED_CHISELED_COPPER, MatterState.GAS,
                TestCableTypes.GAS_LINE);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(hydrogen, 10_000L), false);

        helper.succeedWhen(() -> {
            final OptionalLong moved = MatterGrids.movedLastTickAt(helper.getLevel(), GridKind.GAS,
                    TestCables.cable(helper, RUN.get(0)).place(TestCableTypes.GAS_LINE.get()));
            helper.assertTrue(moved.isPresent() && moved.getAsLong() == TestCableTypes.GAS_RATE,
                    "the line moves five hundred millibuckets a tick: " + moved);
            final long in = amount(helper, INPUT, MatterState.GAS, hydrogen);
            helper.assertTrue(in > 0, "hydrogen arrives");
            helper.assertTrue(amount(helper, OUTPUT, MatterState.GAS, hydrogen) + in == 10_000L,
                    "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void gasLine_fillsATank(final GameTestHelper helper) {
        final Material oxygen = Elements.of(Element.OXYGEN);
        ends(helper, Blocks.CHISELED_COPPER, Blocks.COPPER_GRATE, MatterState.GAS, TestCableTypes.GAS_LINE);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(oxygen, 2_000L), false);

        helper.succeedWhen(() -> helper.assertTrue(amount(helper, INPUT, MatterState.GAS, oxygen) == 2_000L,
                "the tank fills with all the oxygen"));
    }

    @GameTest(template = ARENA)
    public static void slurryLine_carriesSlurryWhichNoGasLineMeets(final GameTestHelper helper) {
        ends(helper, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, MatterState.SLURRY, TestCableTypes.SLURRY_LINE);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.SLURRY)
                .insert(new MatterStack(TestMaterials.TEST_MUD, 1_000L), false);
        helper.assertFalse(GridKind.GAS.policy().plugsInto(helper.getLevel(), helper.absolutePos(INPUT),
                Direction.WEST), "a gas line does not plug into a store of slurry");
        helper.assertTrue(GridKind.SLURRY.policy().plugsInto(helper.getLevel(), helper.absolutePos(INPUT),
                Direction.WEST), "a slurry line does");

        helper.succeedWhen(() -> {
            final long in = amount(helper, INPUT, MatterState.SLURRY, TestMaterials.TEST_MUD);
            helper.assertTrue(in > 0, "the slurry arrives");
            helper.assertTrue(amount(helper, OUTPUT, MatterState.SLURRY, TestMaterials.TEST_MUD) + in == 1_000L,
                    "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void gasLine_bringsInAGasAnotherModKeepsAsAFluid(final GameTestHelper helper) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        final FluidTank output = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT));
        output.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.assertFalse(GridKind.GAS.policy().plugsInto(helper.getLevel(), helper.absolutePos(OUTPUT),
                Direction.EAST), "a gas line does not plug into a tank of water");
        output.setFluid(TestFluids.GAS.stack(5_000));
        helper.assertTrue(GridKind.GAS.policy().plugsInto(helper.getLevel(), helper.absolutePos(OUTPUT),
                Direction.EAST), "it plugs into a tank of a gas");
        helper.assertTrue(MatterBridges.materialOfFluid(BuiltInRegistries.FLUID.getKey(TestFluids.GAS.source()))
                .orElse(null) == TestMaterials.TESTOGEN, "the test gas is testogen");
        helper.setBlock(INPUT, Blocks.WAXED_CHISELED_COPPER);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.GAS_LINE);
        }

        helper.succeedWhen(() -> {
            final long in = amount(helper, INPUT, MatterState.GAS, TestMaterials.TESTOGEN);
            helper.assertTrue(in > 0, "the gas arrives as testogen in the Core's store");
            helper.assertTrue(output.getFluidAmount() + in == 5_000L, "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void gasLine_fillsAChemicalModsTankWithTheSameGas(final GameTestHelper helper) {
        helper.assertTrue(MekanismIntegration.isLoaded() && ChemicalBridges.anyRegistered(),
                "Mekanism and its bridge are on the dev runtime");
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        helper.assertTrue(MatterBridges.materialOfChemical(MEKANISM_HYDROGEN).orElse(null) == hydrogen,
                "Mekanism's hydrogen is the Core's hydrogen");
        helper.assertTrue(ChemicalBridges.stateOf(MEKANISM_HYDROGEN).orElse(null) == MatterState.GAS,
                "and a gas");
        helper.assertTrue(ChemicalBridges.stateOf(MEKANISM_SLURRY).orElse(null) == MatterState.SLURRY,
                "while a dirty ore of Mekanism's is a slurry");
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        helper.setBlock(OUTPUT, Blocks.CHISELED_COPPER);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(hydrogen, 4_000L), false);
        ScenarioBuilder.forGameTest(helper).placeFromItem(INPUT, BuiltInRegistries.BLOCK.get(MEKANISM_TANK));
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.GAS_LINE);
        }

        helper.succeedWhen(() -> {
            final Optional<IChemicalPort> tank = ChemicalBridges.portFor(helper.getLevel(),
                    helper.absolutePos(INPUT), Direction.UP);
            helper.assertTrue(tank.isPresent(), "the tank is a chemical store");
            final long in = tank.get().count(MEKANISM_HYDROGEN);
            helper.assertTrue(in > 0, "Mekanism's hydrogen arrives in it");
            helper.assertTrue(amount(helper, OUTPUT, MatterState.GAS, hydrogen) + in == 4_000L, "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void containment_letsPlasmaDissipateWithoutEnergy(final GameTestHelper helper) {
        final long[] energy = {100L};
        final ContainmentTank containment = new ContainmentTank(MatterState.PLASMA, 100L, 1_000L, 10L, () -> { });
        final MatterStack plasma = new MatterStack(Elements.of(Element.HYDROGEN), 500L);

        helper.assertTrue(containment.insert(plasma, true) == 0L, "a containment never powered takes nothing");
        containment.tick(upkeep(energy));
        helper.assertTrue(containment.powered() && energy[0] == 100L, "an empty one spends nothing to be ready");
        helper.assertTrue(containment.insert(plasma, false) == 500L, "a powered one takes plasma");
        containment.tick(upkeep(energy));
        helper.assertTrue(energy[0] == 90L && containment.held().isPresent(), "it spends its upkeep holding it");
        energy[0] = 5L;
        containment.tick(upkeep(energy));
        helper.assertTrue(!containment.powered() && containment.held().isEmpty(),
                "without enough energy the plasma dissipates");
        helper.assertTrue(containment.insert(plasma, true) == 0L, "and it takes no more");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void containment_keepsExoticMatterSealedWithoutEnergy(final GameTestHelper helper) {
        final long[] energy = {100L};
        final ContainmentTank containment = new ContainmentTank(MatterState.EXOTIC, 100L, 1L, 10L, () -> { });
        containment.tick(upkeep(energy));
        helper.assertTrue(containment.insert(new MatterStack(ExoticMatter.NEUTRONIUM, 50L), false) == 50L,
                "a powered containment takes neutronium");
        energy[0] = 0L;
        containment.tick(upkeep(energy));
        helper.assertTrue(!containment.powered() && containment.held().map(MatterStack::amount).orElse(0L) == 50L,
                "without energy it keeps all it holds");
        helper.assertTrue(containment.extract(ExoticMatter.NEUTRONIUM, 50L, true) == 0L
                && containment.insert(new MatterStack(ExoticMatter.NEUTRONIUM, 1L), true) == 0L,
                "giving nothing out and taking nothing in");
        helper.assertTrue(containment.release().isEmpty() && containment.held().isPresent(),
                "even broken, it lets nothing out");
        energy[0] = 100L;
        containment.tick(upkeep(energy));
        helper.assertTrue(containment.extract(ExoticMatter.NEUTRONIUM, 20L, false) == 20L,
                "with its energy back it gives again");
        boolean refused = false;
        try {
            new ContainmentTank(MatterState.GAS, 1L, 1L, 1L, () -> { });
        } catch (final IllegalArgumentException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "a containment holds plasma or exotic matter, not gas");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void release_losesAGasAndNeverLetsOutExoticMatter(final GameTestHelper helper) {
        MatterRelease.release(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS,
                new MatterStack(Elements.of(Element.HYDROGEN), 1_000L));
        boolean refused = false;
        try {
            MatterRelease.release(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.EXOTIC,
                    new MatterStack(ExoticMatter.NEUTRONIUM, 1L));
        } catch (final IllegalArgumentException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "exotic matter is never let out");
        helper.succeed();
    }

    /* An output and an input of {@code state} with empty tanks, and a run of {@code line} between them. */
    private static void ends(final GameTestHelper helper, final Block output, final Block input,
                             final MatterState state, final Supplier<CableType> line) {
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        helper.setBlock(OUTPUT, output);
        helper.setBlock(INPUT, input);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), state);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, line);
        }
    }

    private static long amount(final GameTestHelper helper, final BlockPos at, final MatterState state,
                               final Material material) {
        return TestMatterEnds.amountAt(helper.getLevel(), helper.absolutePos(at), state, material);
    }

    /* Pays a containment's upkeep from {@code energy}, as its block's energy store would. */
    private static ContainmentTank.IUpkeep upkeep(final long[] energy) {
        return (amount, simulate) -> {
            final long paid = Math.min(amount, energy[0]);
            if (!simulate) {
                energy[0] -= paid;
            }
            return paid;
        };
    }
}
