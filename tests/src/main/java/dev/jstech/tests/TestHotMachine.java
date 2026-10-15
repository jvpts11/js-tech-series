/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.heat.IHeatPort;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineHeat;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SensorBlock;
import dev.jstech.core.signal.SensorBlockEntity;
import dev.jstech.core.signal.Signals;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A machine of the test mod's own that makes heat, to prove the machines' heat: a litre of iron for a casing that its
 * work warms by forty kilowatts, standing up to 400 K; and a thermocouple, a sensor that reads the heat of the block
 * under it.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestHotMachine {

    /** What its work gives off, in watts: a litre of iron warms by half a kelvin a tick. */
    public static final double WATTS = 40_000.0;
    /** The hottest it stands, in kelvin. */
    public static final double LIMIT = 400.0;
    private static final int ENERGY = 1_000;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> BLOCK = TestSounds.CONTENT.block("test_hot_machine",
            Device.of(() -> TestHotMachine.BLOCK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test hot machine").look(IBlockLook.cubeAll("test_hot_machine")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotMachineBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_hot_machine", HotMachineBlockEntity::new, BLOCK);
    public static final BlockEntry<ThermocoupleBlock> THERMOCOUPLE = TestSounds.CONTENT.block("test_thermocouple",
            ThermocoupleBlock::new).named("Test thermocouple").look(IBlockLook.cubeAll("test_thermocouple"))
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermocoupleBlockEntity>> SENSOR =
            TestSounds.CONTENT.blockEntity("test_thermocouple", ThermocoupleBlockEntity::new, THERMOCOUPLE);

    private TestHotMachine() {
    }

    /** Declares the machine and the sensor, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        Signals.offer(event, SENSOR.get());
    }

    /** The machine: always at work while it may, and so always warming. */
    public static final class HotMachineBlockEntity extends MachineBlockEntity {

        public HotMachineBlockEntity(final BlockPos pos, final BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state, 0, MachineEnergy.fe(ENERGY, ENERGY, 0));
            holdsHeat(MachineHeat.of(Thermal.of(Elements.of(Element.IRON), false), TestHeat.LITRE, WATTS, LIMIT));
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }

    /** The thermocouple's block. */
    public static final class ThermocoupleBlock extends SensorBlock {

        public ThermocoupleBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new ThermocoupleBlockEntity(pos, state);
        }
    }

    /** The thermocouple: the temperature of the block under it, when that block holds heat. */
    public static final class ThermocoupleBlockEntity extends SensorBlockEntity {

        public ThermocoupleBlockEntity(final BlockPos pos, final BlockState state) {
            super(SENSOR.get(), pos, state);
        }

        @Override
        public List<Reading> readings() {
            if (level == null) {
                return List.of();
            }
            final IHeatPort below = HeatPorts.at(level, worldPosition.below(), Direction.UP);
            return below == null || Double.isNaN(below.temperature()) ? List.of()
                    : List.of(HeatReadings.thermocouple(below.temperature()));
        }
    }
}
