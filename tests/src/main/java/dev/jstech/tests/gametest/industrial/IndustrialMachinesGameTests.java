/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineTransfers;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.machine.Emission;
import dev.jstech.industrial.machine.IndustrialGenerator;
import dev.jstech.industrial.machine.IndustrialMachines;
import dev.jstech.industrial.machine.IndustrialProcessingMachine;
import dev.jstech.industrial.machine.MachineEmissions;
import dev.jstech.industrial.machine.MachineRating;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestUpgrades;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What every machine of J's Industrial shares: its tier, its port of J's Energy and its power; upgrade slots by its
 * tier, whose gains add up while their savings multiply; a damaged machine at half speed drawing a quarter more, its
 * upgrades idle; what it gives off as it works, its radiation held in by a shielding; and its generators, which burn
 * only what is drawn from them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialMachinesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FIRST = new BlockPos(1, 2, 1);
    private static final BlockPos SECOND = new BlockPos(3, 2, 1);
    private static final BlockPos THIRD = new BlockPos(5, 2, 1);
    private static final BlockPos FOURTH = new BlockPos(1, 2, 4);
    /** Beside the first, touching it. */
    private static final BlockPos AGAINST_FIRST = new BlockPos(2, 2, 1);
    /** The Compressor's time and draw for a plate, as its recipes say. */
    private static final int PLATE_TICKS = 120;
    private static final long COMPRESSOR_DRAW = 150L;
    private static final double SOOT = 1_000.0;
    private static final double RAYS = 5.0;
    private static final int TICKS_A_SECOND = 20;

    private IndustrialMachinesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void rating_putsTheFourMachinesAtTierOneOnLowVoltage(final GameTestHelper helper) {
        rated(helper, FIRST, IndustrialModule.MACERATOR, 4_000L);
        rated(helper, SECOND, IndustrialModule.COMPRESSOR, 3_000L);
        rated(helper, THIRD, IndustrialModule.ELECTRIC_FURNACE, 3_000L);
        rated(helper, FOURTH, IndustrialModule.COAL_GENERATOR, 5_000L);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void upgradeSlots_growWithTheTier(final GameTestHelper helper) {
        helper.assertTrue(IndustrialMachines.upgradeSlots(IndustrialTier.T1) == 2, "two on a T1 machine");
        helper.assertTrue(IndustrialMachines.upgradeSlots(IndustrialTier.T2) == 3
                && IndustrialMachines.upgradeSlots(IndustrialTier.T3) == 3, "three on T2 and T3");
        helper.assertTrue(IndustrialMachines.upgradeSlots(IndustrialTier.T4) == 4
                && IndustrialMachines.upgradeSlots(IndustrialTier.T9) == 4, "four from T4 on");
        try {
            IndustrialMachines.upgradeSlots(IndustrialTier.T0);
            helper.fail("the T0 is worked by hand, with no machine");
        } catch (final IllegalArgumentException expected) {
            // As it should.
        }
        helper.assertTrue(compressor(helper, FIRST).layout().upgrades() == 2, "the Compressor, of T1, has two");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void upgrades_addWhatEachGainsAndMultiplyWhatEachSaves(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper, FIRST);
        final int first = machine.layout().firstUpgrade();
        machine.getInventory().setStackInSlot(first, new ItemStack(TestUpgrades.MOTOR.get(), 2));
        machine.getInventory().setStackInSlot(first + 1, new ItemStack(TestUpgrades.THRIFT.get(), 2));
        final UpgradeEffect effect = machine.upgradeEffect();
        helper.assertTrue(effect.speed() == 2.0, "two motors of half again add up to twice as fast, not 2.25: "
                + effect.speed());
        helper.assertTrue(effect.energy() == 1.5, "and to half again the energy, not 1.5625: " + effect.energy());
        helper.assertTrue(effect.use() == 0.25, "two savings of half leave a quarter: " + effect.use());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void damaged_runsAtHalfSpeedDrawsAQuarterMoreAndIdlesItsUpgrades(final GameTestHelper helper) {
        final CompressorBlockEntity plain = pressing(helper, FIRST);
        final CompressorBlockEntity motors = pressing(helper, SECOND);
        final CompressorBlockEntity damaged = pressing(helper, THIRD);
        for (final CompressorBlockEntity machine : List.of(motors, damaged)) {
            machine.getInventory().setStackInSlot(machine.layout().firstUpgrade(),
                    new ItemStack(TestUpgrades.MOTOR.get(), 2));
        }
        damaged.damage(10);
        helper.onEachTick(() -> {
            for (final CompressorBlockEntity machine : List.of(plain, motors, damaged)) {
                machine.power().generate(machine.power().capacity());
            }
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(plain.getMaxProgress() == PLATE_TICKS && plain.drawn() == COMPRESSOR_DRAW,
                    "whole and plain, a plate takes its time at its draw: " + plain.getMaxProgress() + " ticks, "
                            + plain.drawn() + " J a tick");
            helper.assertTrue(motors.getMaxProgress() == PLATE_TICKS / 2 && motors.drawn() == 225L,
                    "two motors make it twice as fast for half again the draw: " + motors.getMaxProgress()
                            + " ticks, " + motors.drawn() + " J a tick");
            helper.assertTrue(damaged.getMaxProgress() == PLATE_TICKS * 2 && damaged.drawn() == 188L,
                    "damaged, its motors idle, it takes twice the time for a quarter more: "
                            + damaged.getMaxProgress() + " ticks, " + damaged.drawn() + " J a tick");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void emissions_pollutePerSecondOfWorkAndRadiateUnlessHeldIn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(FIRST);
        final ChunkPos chunk = new ChunkPos(at);
        final MachineEmissions emissions = new MachineEmissions(List.of(
                Emission.pollutes(SeriesPollution.SOOT, SOOT), Emission.radiates(IonizingRadiation.RAYS, RAYS)));
        final double before = Pollution.at(level, chunk, SeriesPollution.SOOT);
        for (int tick = 1; tick < TICKS_A_SECOND; tick++) {
            emissions.settle(level, at, true, false);
        }
        helper.assertTrue(Math.abs(Pollution.at(level, chunk, SeriesPollution.SOOT) - before) < 1.0,
                "nothing before a whole second of work");
        emissions.settle(level, at, true, false);
        helper.assertTrue(Math.abs(Pollution.at(level, chunk, SeriesPollution.SOOT) - before - SOOT) < 1.0,
                "a second of work puts its soot on its chunk");
        helper.assertTrue(Math.abs(Radiation.strength(level, at, IonizingRadiation.RAYS) - RAYS) < 1e-6,
                "working unshielded, it radiates");
        emissions.settle(level, at, true, true);
        helper.assertTrue(Radiation.strength(level, at, IonizingRadiation.RAYS) == 0.0, "a shielding holds it in");
        emissions.settle(level, at, true, false);
        emissions.settle(level, at, false, false);
        helper.assertTrue(Radiation.strength(level, at, IonizingRadiation.RAYS) == 0.0,
                "and it stops as the machine rests");
        // Takes back what it gave off, so no other test meets it.
        Pollution.takeUp(level, at, SeriesPollution.SOOT, SOOT);
        emissions.stop(level, at);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shielding_inAnUpgradeSlotHoldsTheRadiationIn(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper, FIRST);
        helper.assertFalse(machine.shielded(), "bare, nothing holds its radiation in");
        machine.getInventory().setStackInSlot(machine.layout().firstUpgrade(),
                new ItemStack(TestUpgrades.SHIELDING.get()));
        helper.assertTrue(machine.shielded(), "a shielding in an upgrade slot does");
        helper.assertTrue(machine.upgradeEffect().equals(UpgradeEffect.NONE), "and changes nothing of its work");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void coalGenerator_burnsOnlyWhatIsDrawn(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper, FIRST);
        final long piece = CoalGeneratorBlockEntity.joulesOf(new ItemStack(Items.COAL));
        final long round = CoalGeneratorBlockEntity.RATING.joulesPerTick() * MachineTransfers.EVERY;
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(generator.power().stored() == round,
                    "alone, it fills its port, a round of its pushing: " + generator.power().stored());
            helper.assertTrue(generator.fuelLeft() == piece - round,
                    "and burns no more than that: " + (piece - generator.fuelLeft()) + " J burnt");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void coalGenerator_feedsAMachineAgainstItBurningWhatItDraws(final GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = generator(helper, FIRST);
        final CompressorBlockEntity machine = pressing(helper, AGAINST_FIRST);
        final long piece = CoalGeneratorBlockEntity.joulesOf(new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.getInventory().getStackInSlot(1).is(IndustrialModule.IRON_PLATE.get()),
                    "the machine against it presses a plate");
            final long burnt = piece - generator.fuelLeft();
            helper.assertTrue(burnt < helper.getTick() * CoalGeneratorBlockEntity.RATING.joulesPerTick(),
                    "the generator burning what it draws, less than at its full power: " + burnt + " J in "
                            + helper.getTick() + " ticks");
        });
    }

    /* Places {@code block} at {@code at} and checks it is of T1, on low voltage, of {@code watts}. */
    private static void rated(final GameTestHelper helper, final BlockPos at, final BlockEntry<?> block,
                              final long watts) {
        helper.setBlock(at, block.get());
        final BlockEntity found = helper.getBlockEntity(at);
        final MachineRating rating = switch (found) {
            case IndustrialProcessingMachine machine -> machine.rating();
            case IndustrialGenerator generator -> generator.rating();
            default -> throw new IllegalStateException("no machine of J's Industrial at " + at);
        };
        final MachineBlockEntity machine = (MachineBlockEntity) found;
        helper.assertTrue(rating.tier() == IndustrialTier.T1 && rating.portClass() == JsEnergy.LOW
                && rating.watts() == watts && rating.emissions().isEmpty(),
                block.getId() + " is a T1 machine of " + watts + " W at low voltage, giving off nothing: " + rating);
        helper.assertTrue(machine.power().energy() == JsEnergy.ENERGY
                && machine.power().portClass().equals(Optional.of(JsEnergy.LOW)),
                "its port is of J's Energy, low voltage");
    }

    private static CompressorBlockEntity compressor(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, IndustrialModule.COMPRESSOR.get());
        return helper.getBlockEntity(at);
    }

    /* A Compressor with iron ingots to press. */
    private static CompressorBlockEntity pressing(final GameTestHelper helper, final BlockPos at) {
        final CompressorBlockEntity machine = compressor(helper, at);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 16));
        return machine;
    }

    /* A Coal Generator with one coal. */
    private static CoalGeneratorBlockEntity generator(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, IndustrialModule.COAL_GENERATOR.get());
        final CoalGeneratorBlockEntity generator = helper.getBlockEntity(at);
        generator.getInventory().setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        return generator;
    }
}
