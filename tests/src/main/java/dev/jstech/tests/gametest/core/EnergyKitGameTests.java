/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.EnergyConversion;
import dev.jstech.core.energy.EnergyDistributionResult;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.energy.FeBridge;
import dev.jstech.core.energy.GeneratorPort;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's energy kit, as a mod of its own uses it: an energy declared with its unit, its classes and their losses,
 * whose lines of each class are a grid of their own; energy moved between ports of a line's class, losing its class's
 * share each block; a port of another class burnt one class up, ruined by an arc flash two up, and short of voltage a
 * class down, only while the line is live; the conversion to and from FE, which never makes energy; the tooltips naming
 * a port's class; and a generator that follows its load.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EnergyKitGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos SOURCE = new BlockPos(1, 2, 2);
    private static final BlockPos SINK = new BlockPos(5, 2, 2);
    private static final BlockPos ABOVE = new BlockPos(3, 2, 3);
    private static final BlockPos BELOW = new BlockPos(3, 2, 1);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final int WAIT = 10;

    private EnergyKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void energy_hasAGridForEachClassAndFeHasThePowerGrid(final GameTestHelper helper) {
        helper.assertTrue(Energies.FE.grid() == GridKind.POWER && Energies.FE.classless(),
                "FE has no classes, and its one grid is the Core's power grid");
        helper.assertTrue(Energies.byId(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_volts"))
                .orElse(null) == TestEnergies.VOLTS, "a mod's energy is found by its id");
        final GridKind low = TestEnergies.VOLTS.grid(TestEnergies.LOW);
        helper.assertTrue(low.id().equals(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_volts_low"))
                && low != TestEnergies.VOLTS.grid(TestEnergies.HIGH), "each class has a grid of its own");
        helper.assertTrue(Energies.ofGrid(low).orElse(null) == TestEnergies.VOLTS
                && TestEnergies.VOLTS.classOf(low).orElse(null) == TestEnergies.LOW, "a grid knows its class");
        helper.assertTrue(TestEnergies.VOLTS.steps(TestEnergies.LOW, TestEnergies.HIGH) == 2
                && TestEnergies.VOLTS.meeting(TestEnergies.LOW, TestEnergies.HIGH) == WrongClass.ARC_FLASH
                && TestEnergies.VOLTS.meeting(TestEnergies.HIGH, TestEnergies.MEDIUM) == WrongClass.UNDERVOLTAGE,
                "the classes are in order, and the rule reads the steps between them");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void line_movesEnergyLosingItsClassShareEachBlock(final GameTestHelper helper) {
        ends(helper, Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, TestEnergies.LOW_LINE);
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SOURCE)).setStored(100_000L);

        helper.succeedWhen(() -> {
            final EnergyDistributionResult tick = lastTick(helper, TestEnergies.LOW_LINE.get());
            // Three blocks each losing a thousandth: 1004 sent bring the thousand the sink takes, and 4 are lost.
            helper.assertTrue(tick.totalDelivered() == TestEnergyEnds.TAKES, "the sink gets all it takes: "
                    + tick.totalDelivered());
            helper.assertTrue(tick.totalLost() == 4L, "the line loses its class's share each block: "
                    + tick.totalLost());
            helper.assertTrue(TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SINK)).stored() > 0,
                    "the energy arrives");
        });
    }

    @GameTest(template = ARENA)
    public static void wrongClass_burnsOneUpArcsTwoUpAndLeavesOneDownShort(final GameTestHelper helper) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(ABOVE));
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(BELOW));
        helper.setBlock(ABOVE, Blocks.WEATHERED_CUT_COPPER);
        helper.setBlock(BELOW, Blocks.WAXED_CUT_COPPER);
        ends(helper, Blocks.OXIDIZED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, TestEnergies.HIGH_LINE);
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SOURCE)).setStored(100_000L);

        helper.succeedWhen(() -> {
            final EnergyStore medium = store(helper, ABOVE);
            final EnergyStore low = store(helper, SINK);
            final EnergyStore extra = store(helper, BELOW);
            helper.assertTrue(medium.damaged() && !TestEnergyEnds.arced(helper.getLevel(),
                    helper.absolutePos(ABOVE)), "a port one class down burns");
            helper.assertTrue(low.damaged() && TestEnergyEnds.arced(helper.getLevel(), helper.absolutePos(SINK)),
                    "a port two classes down is hit by an arc flash");
            helper.assertTrue(extra.undervoltage(helper.getLevel().getGameTime()) && !extra.damaged(),
                    "a port a class up is short of voltage and unharmed");
            helper.assertTrue(medium.stored() == 0 && low.stored() == 0 && extra.stored() == 0,
                    "and none of them takes the line's energy");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void wrongClass_harmsNothingWhileTheLineIsDead(final GameTestHelper helper) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(ABOVE));
        helper.setBlock(ABOVE, Blocks.WEATHERED_CUT_COPPER);
        ends(helper, Blocks.OXIDIZED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, TestEnergies.HIGH_LINE);

        helper.runAfterDelay(WAIT, () -> {
            helper.assertTrue(!store(helper, ABOVE).damaged() && !store(helper, SINK).damaged(),
                    "a line with nothing on it burns nothing");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void conversion_movesEitherWayAndNeverMakesEnergy(final GameTestHelper helper) {
        final EnergyStore store = new EnergyStore("Test", TestEnergies.VOLTS, TestEnergies.LOW, 1_000L, 1_000L,
                1_000L, () -> { });
        final EnergyStorage fe = new EnergyStorage(1_000, 1_000, 1_000, 500);

        helper.assertTrue(EnergyConversion.convert(fe, store, EnergyConversion.Way.FE_TO_ENERGY, 100L) == 100L
                && store.stored() == 200L && fe.getEnergyStored() == 400, "100 FE become 200 test joules");
        store.setStored(201L);
        final EnergyStorage empty = new EnergyStorage(1_000);
        helper.assertTrue(EnergyConversion.convert(empty, store, EnergyConversion.Way.ENERGY_TO_FE, 1_000L) == 100L
                && empty.getEnergyStored() == 100 && store.stored() == 1L,
                "201 test joules become 100 FE, the odd one left behind");
        final IEnergyStorage view = FeBridge.view(store);
        helper.assertTrue(view.extractEnergy(10, false) == 0 && store.stored() == 1L,
                "half an FE's worth gives no FE");
        helper.assertTrue(view.receiveEnergy(10, false) == 10 && store.stored() == 21L,
                "the FE view takes 10 FE as 20 test joules");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooltips_nameThePortsClassAndItsRating(final GameTestHelper helper) {
        helper.assertTrue(EnergyTooltips.port(TestEnergies.VOLTS, TestEnergies.LOW).orElseThrow().english()
                .equals("Port: Low, 100 V"), "a rated class shows its rating");
        helper.assertTrue(EnergyTooltips.port(TestEnergies.VOLTS, TestEnergies.MEDIUM).orElseThrow().english()
                .equals("Port: Medium"), "a class with no rating shows its name");
        helper.assertTrue(EnergyTooltips.port(Energies.FE, Energies.FE.lowest()).isEmpty(),
                "an energy with no classes names none");
        helper.assertTrue(TestEnergies.VOLTS.amount(500L).english().equals("500 tJ"),
                "an amount is shown in its unit");
        helper.assertTrue(TestEnergies.VOLTS.rate(10L).english().equals("200 tW"),
                "a rate a tick is shown as power, twenty ticks to the second");
        helper.assertTrue(Energies.FE.rate(40L).english().equals("40 FE/t"),
                "and FE, with no unit of power, as so much a tick");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void generator_followsItsLoadAtItsRamp(final GameTestHelper helper) {
        ends(helper, Blocks.COPPER_BULB, Blocks.EXPOSED_CUT_COPPER, TestEnergies.LOW_LINE);
        final GeneratorPort generator = TestEnergyEnds.generatorAt(helper.getLevel(), helper.absolutePos(SOURCE));
        final EnergyStore sink = store(helper, SINK);
        final List<Long> outputs = new ArrayList<>();
        helper.onEachTick(() -> {
            outputs.add(generator.follower().settle());
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(generator.follower().output() == TestEnergyEnds.GENERATOR_MOST,
                    "it reaches its most as the sink keeps drawing: " + outputs);
            for (int i = 1; i < outputs.size(); i++) {
                helper.assertTrue(outputs.get(i) - outputs.get(i - 1) <= TestEnergyEnds.GENERATOR_RAMP,
                        "never faster than its ramp: " + outputs);
            }
            helper.assertTrue(sink.stored() > 0, "and the sink is fed");
        });
    }

    /* A giving end, a taking end and a run of {@code line} between them, every store empty. */
    private static void ends(final GameTestHelper helper, final Block source, final Block sink,
                             final Supplier<CableType> line) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(SOURCE));
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(SINK));
        helper.setBlock(SOURCE, source);
        helper.setBlock(SINK, sink);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, line);
        }
    }

    private static EnergyStore store(final GameTestHelper helper, final BlockPos at) {
        return TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(at));
    }

    /* What the part of the grid the run's first wire is in moved in the last tick. */
    private static EnergyDistributionResult lastTick(final GameTestHelper helper, final CableType type) {
        final OptionalLong number = CoreGrids.places(helper.getLevel())
                .find(TestCables.cable(helper, RUN.get(0)).place(type));
        if (number.isEmpty()) {
            return EnergyDistributionResult.empty();
        }
        return CoreGrids.mover(helper.getLevel(), type.grid())
                .map(mover -> ((LevelEnergy) mover).lastTickOf(number.getAsLong())
                        .orElseGet(EnergyDistributionResult::empty))
                .orElseGet(EnergyDistributionResult::empty);
    }
}
