/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.energy.ConverterLimit;
import dev.jstech.core.jstechseries.energy.EnergyConverterBlock;
import dev.jstech.core.jstechseries.energy.EnergyConverterBlockEntity;
import dev.jstech.core.jstechseries.energy.EnergyConverterMenu;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The FE converters: a cabinet facing whoever placed it, FE on their left and J's Energy on their right, moving energy
 * either way up to its limit a tick, never above its class's ceiling, never drawing more than it moves; a configurable
 * one set to any class, showing it on its side; stopped by its switch or by a line of the wrong class; set from its
 * screen's controls; made of the game's own materials.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EnergyConverterGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos BATTERY = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos CONVERTER = new BlockPos(3, 2, 2);
    private static final BlockPos MACHINE = new BlockPos(4, 2, 2);
    private static final long LOW_LIMIT_FE = 5_000L;

    private EnergyConverterGameTests() {
    }

    @GameTest(template = ARENA)
    public static void sides_feOnTheLeftOfWhoeverFacesItAndJeOnTheRight(final GameTestHelper helper) {
        converter(helper, EnergyConverters.LOW.get());
        final BlockPos at = helper.absolutePos(CONVERTER);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.EAST)
                != null, "facing north, its FE side is the east, the left of whoever faces it");
        helper.assertTrue(helper.getLevel().getCapability(EnergyPorts.PORT, at, Direction.WEST) != null,
                "its J's Energy side the west");
        for (final Direction other : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.UP}) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, other) == null
                    && helper.getLevel().getCapability(EnergyPorts.PORT, at, other) == null,
                    "and nothing on its " + other.getSerializedName());
        }
        final IEnergyPort port = helper.getLevel().getCapability(EnergyPorts.PORT, at, Direction.WEST);
        helper.assertTrue(port != null && port.portClass() == JsEnergy.LOW, "a low voltage port");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void feToJe_chargesABatteryNeverTakingMoreThanItsLimitATick(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.LOW.get());
        final BatteryBankBlockEntity battery = battery(helper);
        TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final IEnergyStorage fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(CONVERTER), Direction.EAST);
        helper.assertTrue(fe != null && fe.canReceive() && !fe.canExtract(), "toward J's Energy, its FE side takes");
        helper.onEachTick(() -> {
            final int taken = fe.receiveEnergy(Integer.MAX_VALUE, false);
            helper.assertTrue(taken <= LOW_LIMIT_FE, "it takes a tick's worth of its limit at most, not " + taken);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(battery.store().stored() > 0, "the battery charges from it");
            helper.assertTrue(converter.movedFe() > 0 && converter.movedFe() <= LOW_LIMIT_FE,
                    "moving up to its limit a tick");
        });
    }

    @GameTest(template = ARENA)
    public static void jeToFe_feedsAnFeMachineFromABattery(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.LOW.get());
        converter.setToJe(false);
        final BatteryBankBlockEntity battery = battery(helper);
        battery.store().setStored(SharedEnergy.BATTERY_CAPACITY);
        TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        // A machine of the game's energy alone: the test mod's FE taking end.
        helper.setBlock(MACHINE, Blocks.EXPOSED_COPPER);
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(MACHINE));
        helper.succeedWhen(() -> {
            helper.assertTrue(TestEnergyEnds.feStoreAt(helper.getLevel(), helper.absolutePos(MACHINE))
                    .getEnergyStored() > 0, "it pushes FE into the machine");
            helper.assertTrue(battery.store().stored() < SharedEnergy.BATTERY_CAPACITY, "drawn from the battery");
        });
    }

    @GameTest(template = ARENA)
    public static void switchedOff_takesAndMovesNothing(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.LOW.get());
        converter.setOn(false);
        final IEnergyStorage fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(CONVERTER), Direction.EAST);
        helper.assertTrue(fe != null && fe.receiveEnergy(1_000, false) == 0, "switched off, it takes nothing");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(converter.movedFe() == 0L, "and moves nothing");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void fixed_keepsItsLimitUnderItsCeiling(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.LOW.get());
        helper.assertTrue(converter.limit().equals(new ConverterLimit(100, ConverterLimit.KILO)),
                "a fixed converter starts at its ceiling, 100 kW");
        converter.turnDrum(0, true);
        converter.turnPrefix();
        helper.assertTrue(converter.limit().watts() <= ConverterLimit.ceiling(VoltageClass.LOW),
                "turned up, it stays at its ceiling");
        converter.turnDrum(1, false);
        helper.assertTrue(converter.limit().equals(new ConverterLimit(0, ConverterLimit.KILO)),
                "turned down, it goes down");
        converter.turnDrum(2, true);
        helper.assertTrue(converter.limitJoules() == 500L, "10 kW is 500 joules a tick");
        converter.setVoltage(VoltageClass.ULTRA_HIGH);
        helper.assertTrue(converter.voltageClass() == VoltageClass.LOW, "a fixed converter keeps its class");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void configurable_takesAnyClassAndShowsItOnItsSide(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.CONFIGURABLE.get());
        converter.setVoltage(VoltageClass.HIGH);
        converter.turnPrefix();
        helper.assertTrue(converter.limit().watts() == 100_000_000L, "at high voltage 100 MW is under the ceiling");
        final IEnergyPort port = helper.getLevel().getCapability(EnergyPorts.PORT, helper.absolutePos(CONVERTER),
                Direction.WEST);
        helper.assertTrue(port != null && port.portClass() == JsEnergy.HIGH, "its port is of the class it is set to");
        converter.setVoltage(VoltageClass.LOW);
        helper.assertTrue(converter.limit().watts() == ConverterLimit.ceiling(VoltageClass.LOW),
                "set lower, its limit comes down to the lower ceiling");
        converter.setVoltage(VoltageClass.EXTRA_HIGH);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(CONVERTER)
                .getValue(EnergyConverterBlock.VOLTAGE) == VoltageClass.EXTRA_HIGH.rank(),
                "its block shows the class it is set to"));
    }

    @GameTest(template = ARENA)
    public static void wrongClassLine_stopsItAndLightsItsFault(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.CONFIGURABLE.get());
        converter.setVoltage(VoltageClass.MEDIUM);
        converter.setToJe(false);
        final BatteryBankBlockEntity battery = battery(helper);
        battery.store().setStored(SharedEnergy.BATTERY_CAPACITY);
        TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        helper.succeedWhen(() -> {
            helper.assertTrue(converter.view().fault(), "a low voltage line leaves a medium voltage port short");
            helper.assertTrue(converter.movedFe() == 0L, "and it moves nothing");
        });
    }

    @GameTest(template = ARENA)
    public static void menu_setsItAsItsFaceDoes(final GameTestHelper helper) {
        final EnergyConverterBlockEntity converter = converter(helper, EnergyConverters.CONFIGURABLE.get());
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.fromString(
                "6c0a7f52-3c2e-4d51-9a4f-5f1d8e2b7a01"), "converter_menu"));
        final EnergyConverterMenu menu = new EnergyConverterMenu(1, player.getInventory(), converter);
        menu.clickMenuButton(player, EnergyConverterMenu.FIRST_KEY + VoltageClass.MEDIUM.rank() - 1);
        helper.assertTrue(converter.voltageClass() == VoltageClass.MEDIUM, "a class key sets the class");
        menu.clickMenuButton(player, EnergyConverterMenu.TO_FE);
        helper.assertTrue(!converter.toJe(), "the knob turned to JE to FE");
        menu.clickMenuButton(player, EnergyConverterMenu.TO_JE);
        helper.assertTrue(converter.toJe(), "and back");
        menu.clickMenuButton(player, EnergyConverterMenu.FIRST_DRUM_DOWN + 1);
        helper.assertTrue(converter.limit().digits() == 0, "a drum turned down");
        menu.clickMenuButton(player, EnergyConverterMenu.FIRST_DRUM_UP);
        helper.assertTrue(converter.limit().digits() == 1000, "and another up");
        menu.clickMenuButton(player, EnergyConverterMenu.PREFIX);
        helper.assertTrue(converter.limit().watts() <= ConverterLimit.ceiling(VoltageClass.MEDIUM),
                "the prefix never takes it above the ceiling");
        menu.clickMenuButton(player, EnergyConverterMenu.SWITCH);
        helper.assertTrue(!converter.on(), "the switch turns it off");
        helper.assertTrue(converter.view().voltage() == VoltageClass.MEDIUM.rank() && !converter.view().on(),
                "and its screen sees all of it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recipes_ofTheGamesMaterialsStandForEveryConverter(final GameTestHelper helper) {
        for (final String id : new String[] {"fe_converter_lv", "fe_converter_mv", "fe_converter_hv",
                "fe_converter_ehv", "fe_converter_uhv", "configurable_energy_converter"}) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                    "jscore", id)).isPresent(), "the " + id + " is made of the game's own materials");
        }
        helper.succeed();
    }

    private static EnergyConverterBlockEntity converter(final GameTestHelper helper, final Block block) {
        helper.setBlock(CONVERTER, block);
        return helper.getBlockEntity(CONVERTER);
    }

    private static BatteryBankBlockEntity battery(final GameTestHelper helper) {
        helper.setBlock(BATTERY, SharedEnergy.BATTERY.get());
        return helper.getBlockEntity(BATTERY);
    }
}
