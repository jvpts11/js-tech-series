/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.jstechseries.energy.ConverterLimit;
import dev.jstech.core.jstechseries.energy.GridEquipment;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.text.Text;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.machine.JoinerRating;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestIndustrialEnergy;
import dev.jstech.tests.testkit.TestCables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's energy contract: its grid equipment joining two neighbouring classes by the equipment J's Core names
 * for them, a transformer carrying energy from the side it comes from either way, up to the ceiling of its higher
 * class; and its batteries, ports of J's Energy at low voltage that a machine's battery slot charges and draws on.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialEnergyGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GENERATOR = new BlockPos(1, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2));
    /** At the run's end, its back to it. */
    private static final BlockPos TRANSFORMER = new BlockPos(4, 2, 2);
    private static final BlockPos MACHINE = new BlockPos(2, 2, 4);

    private IndustrialEnergyGameTests() {
    }

    @GameTest(template = ARENA)
    public static void joinerRating_joinsNeighboursByTheEquipmentTheCoreNames(final GameTestHelper helper) {
        final JoinerRating distribution = JoinerRating.transformer(IndustrialTier.T1, VoltageClass.LOW,
                VoltageClass.MEDIUM);
        helper.assertTrue(distribution.watts() == ConverterLimit.ceiling(VoltageClass.MEDIUM)
                && distribution.lowerClass() == JsEnergy.LOW && distribution.higherClass() == JsEnergy.MEDIUM,
                "a transformer between low and medium carries up to medium's ceiling");
        final JoinerRating station = JoinerRating.hvdcStation(IndustrialTier.T5);
        helper.assertTrue(station.equipment() == GridEquipment.HVDC_STATION
                && station.lower() == VoltageClass.EXTRA_HIGH && station.higher() == VoltageClass.ULTRA_HIGH,
                "an HVDC station joins the extra-high class to the ultra-high");
        refused(helper, () -> JoinerRating.transformer(IndustrialTier.T1, VoltageClass.LOW, VoltageClass.HIGH),
                "classes that are not neighbours join through nothing");
        refused(helper, () -> JoinerRating.transformer(IndustrialTier.T5, VoltageClass.EXTRA_HIGH,
                VoltageClass.ULTRA_HIGH), "direct current is joined by a station, not a transformer");
        refused(helper, () -> new JoinerRating(GridEquipment.HVDC_STATION, IndustrialTier.T1, VoltageClass.LOW,
                VoltageClass.MEDIUM), "and alternating current by a transformer, not a station");
        refused(helper, () -> JoinerRating.transformer(IndustrialTier.T1, VoltageClass.MEDIUM, VoltageClass.LOW),
                "the lower class comes first");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void transformer_carriesEnergyEitherWayUpToItsCeiling(final GameTestHelper helper) {
        final TestIndustrialEnergy.TransformerBlockEntity transformer = transformer(helper);
        final EnergyStore low = transformer.lowPort();
        final EnergyStore high = transformer.highPort();
        final long ceiling = TestIndustrialEnergy.TRANSFORMER_RATING.joulesPerTick();
        helper.assertTrue(transformer.port(Direction.EAST) == high && transformer.port(Direction.WEST) == low
                && transformer.port(Direction.NORTH) == null, "its higher class on its front, its lower behind");
        helper.assertTrue(low.portClass() == JsEnergy.LOW && high.portClass() == JsEnergy.MEDIUM,
                "of the classes it joins");
        low.setStored(low.capacity());
        high.setStored(0L);
        transformer.tick();
        helper.assertTrue(transformer.moved() == ceiling && low.stored() == ceiling && high.stored() == ceiling,
                "energy on its low side steps up, no more than its ceiling a tick: " + transformer.moved());
        low.setStored(0L);
        high.setStored(high.capacity());
        transformer.tick();
        helper.assertTrue(transformer.moved() == -ceiling, "and on its high side steps down: " + transformer.moved());
        helper.assertTrue(described(transformer).stream().anyMatch(line -> line.english().startsWith(
                "Stepping down")), "which whoever looks at it is told");
        low.setStored(1_000L);
        high.setStored(0L);
        transformer.tick();
        helper.assertTrue(low.stored() == 500L && high.stored() == 500L,
                "below its ceiling it evens its sides, making and losing nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void transformer_takesALowLineOnItsBackToItsMediumSide(final GameTestHelper helper) {
        helper.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get());
        final CoalGeneratorBlockEntity generator = helper.getBlockEntity(GENERATOR);
        generator.getInventory().setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedEnergy.INSULATED_COPPER_CABLE);
        }
        final TestIndustrialEnergy.TransformerBlockEntity transformer = transformer(helper);
        helper.succeedWhen(() -> helper.assertTrue(transformer.highPort().stored() > 0,
                "what the generator makes crosses to its medium side; it holds " + transformer.highPort().stored()));
    }

    @GameTest(template = ARENA)
    public static void battery_isAPortOfJsEnergyAtLowVoltage(final GameTestHelper helper) {
        final ItemStack battery = new ItemStack(TestIndustrialEnergy.BATTERY.get());
        final IEnergyPort port = battery.getCapability(EnergyPorts.ITEM);
        helper.assertTrue(port != null && port.energy() == JsEnergy.ENERGY && port.portClass() == JsEnergy.LOW
                && port.capacity() == 100_000L, "a battery holds J's Energy at low voltage, as its rating says");
        final long perTick = TestIndustrialEnergy.BATTERY_RATING.joulesPerTick();
        helper.assertTrue(port.receive(1_000_000L, false) == perTick, "it charges at its power: "
                + port.stored());
        final ItemStack copy = battery.copy();
        helper.assertTrue(Objects.requireNonNull(copy.getCapability(EnergyPorts.ITEM)).stored() == perTick,
                "and its charge goes with it");
        helper.assertTrue(port.extract(1_000_000L, false) == perTick && port.stored() == 0L,
                "it discharges at its power too");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void battery_chargesInAMachinesSlotAndCarriesItThroughAnOutage(final GameTestHelper helper) {
        helper.setBlock(MACHINE, IndustrialModule.COMPRESSOR.get());
        final CompressorBlockEntity machine = helper.getBlockEntity(MACHINE);
        final ItemStack battery = new ItemStack(TestIndustrialEnergy.BATTERY.get());
        Objects.requireNonNull(machine.power().protection()).slots().setStackInSlot(EnergyProtection.BATTERY,
                battery);
        final boolean[] grid = {true};
        helper.onEachTick(() -> {
            if (grid[0]) {
                machine.power().generate(machine.power().capacity());
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(charge(machine) >= 1_000L,
                        "the battery charges while the grid keeps the machine full: " + charge(machine)))
                .thenExecute(() -> {
                    grid[0] = false;
                    Objects.requireNonNull(machine.power().portStore()).setStored(0L);
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.power().stored() > 0,
                        "with the grid down, it powers the machine"))
                .thenSucceed();
    }

    private static TestIndustrialEnergy.TransformerBlockEntity transformer(final GameTestHelper helper) {
        helper.setBlock(TRANSFORMER, TestIndustrialEnergy.TRANSFORMER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(TRANSFORMER);
    }

    /* What the battery in the machine's battery slot holds. */
    private static long charge(final CompressorBlockEntity machine) {
        final IEnergyPort port = Objects.requireNonNull(machine.power().protection()).slots()
                .getStackInSlot(EnergyProtection.BATTERY).getCapability(EnergyPorts.ITEM);
        return port == null ? 0L : port.stored();
    }

    private static List<Text> described(final TestIndustrialEnergy.TransformerBlockEntity transformer) {
        final List<Text> lines = new ArrayList<>();
        transformer.describe(lines, false);
        return lines;
    }

    private static void refused(final GameTestHelper helper, final Runnable making, final String message) {
        try {
            making.run();
            helper.fail(message);
        } catch (final IllegalArgumentException expected) {
            // As it should.
        }
    }
}
