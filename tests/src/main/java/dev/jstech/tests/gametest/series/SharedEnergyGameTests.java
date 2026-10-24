/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import dev.jstech.core.energy.EnergyBlockItem;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.jstechseries.energy.BatteryBankBlock;
import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.JsEnergySettings;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' basic energy content: the Insulated Copper Cable, a low-voltage line of J's Energy that loses a tenth over
 * a hundred blocks, and the Lead-Acid Battery Bank, 50 MJ at low voltage, its charge on its gauge; while J's Energy is
 * off both speak FE, so the battery feeds any mod's machine along the basic cable; their recipes of the game's own
 * materials give way to J's Industrial's.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SharedEnergyGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos BATTERY = new BlockPos(1, 2, 2);
    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));

    private SharedEnergyGameTests() {
    }

    @GameTest(template = ARENA)
    public static void basicCable_isALowLineOfJsEnergyLosingATenthOverAHundredBlocks(final GameTestHelper helper) {
        helper.assertTrue(SharedEnergy.INSULATED_COPPER_CABLE.get().grid() == JsEnergy.ENERGY.grid(JsEnergy.LOW),
                "the basic cable is a low-voltage line of J's Energy");
        helper.assertTrue(SharedEnergy.INSULATED_COPPER_CABLE.get().throughput() == 500L,
                "carrying 10 kW, five hundred joules a tick");
        helper.assertTrue(SharedEnergy.INSULATED_COPPER_CABLE.get().loss() * 100 == 100_000,
                "and losing a tenth over a hundred blocks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void battery_feedsAMachineAlongTheBasicCable(final GameTestHelper helper) {
        helper.assertTrue(JsEnergy.speaksFe(), "J's Energy is off, as it comes");
        final BatteryBankBlockEntity battery = battery(helper);
        battery.store().setStored(SharedEnergy.BATTERY_CAPACITY);
        helper.setBlock(MACHINE, IndustrialModule.COMPRESSOR.get());
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedEnergy.INSULATED_COPPER_CABLE);
        }
        helper.succeedWhen(() -> {
            final CompressorBlockEntity machine = helper.getBlockEntity(MACHINE);
            helper.assertTrue(machine.power().stored() > 0, "the machine's energy comes from the battery");
            helper.assertTrue(battery.store().stored() < SharedEnergy.BATTERY_CAPACITY, "which gives it");
        });
    }

    @GameTest(template = ARENA)
    public static void battery_showsItsChargeOnItsGauge(final GameTestHelper helper) {
        final BatteryBankBlockEntity battery = battery(helper);
        battery.store().setStored(SharedEnergy.BATTERY_CAPACITY / 2);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(BATTERY)
                .getValue(BatteryBankBlock.CHARGE) == 2, "half full, its gauge shows two of four steps"));
    }

    @GameTest(template = ARENA)
    public static void battery_speaksFeOnlyWhileTheSeriesDoes(final GameTestHelper helper) {
        battery(helper);
        final BlockPos at = helper.absolutePos(BATTERY);
        try {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.UP)
                    != null, "with J's Energy off it speaks FE");
            JsEnergySettings.setEnabled(true);
            helper.getLevel().invalidateCapabilities(at);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.UP)
                    == null, "with it on, joules alone");
            JsEnergySettings.setAcceptFe(true);
            helper.getLevel().invalidateCapabilities(at);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.UP)
                    != null, "unless the server lets J's Energy blocks take FE directly");
        } finally {
            JsEnergySettings.setEnabled(false);
            JsEnergySettings.setAcceptFe(false);
            helper.getLevel().invalidateCapabilities(at);
        }
        helper.assertTrue(SharedEnergy.BATTERY.get().asItem() instanceof EnergyBlockItem item
                && item.energy() == JsEnergy.ENERGY && item.portClass() == JsEnergy.LOW,
                "its item names its port, low voltage");
        helper.assertTrue(Energies.ofGrid(SharedEnergy.INSULATED_COPPER_CABLE.get().grid()).orElse(null)
                == JsEnergy.ENERGY, "the cable's grid is J's Energy's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recipes_ofTheGamesMaterialsGiveWayToIndustrials(final GameTestHelper helper) {
        final ResourceLocation cable = ResourceLocation.fromNamespaceAndPath("jscore", "insulated_copper_cable");
        final ResourceLocation battery = ResourceLocation.fromNamespaceAndPath("jscore", "lead_acid_battery_bank");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(cable).isEmpty()
                && helper.getLevel().getRecipeManager().byKey(battery).isEmpty(),
                "with J's Industrial installed, its own recipes stand instead");
        helper.succeed();
    }

    private static BatteryBankBlockEntity battery(final GameTestHelper helper) {
        helper.setBlock(BATTERY, SharedEnergy.BATTERY.get());
        return helper.getBlockEntity(BATTERY);
    }
}
