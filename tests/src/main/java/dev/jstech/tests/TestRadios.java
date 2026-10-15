/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.signal.IRadio;
import dev.jstech.core.signal.RadioBlock;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SensorBlock;
import dev.jstech.core.signal.SensorBlockEntity;
import dev.jstech.core.signal.Signals;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Radios and a sensor of the test mod, built on the Core's kit for redstone and signals: a transmitter that sends 32
 * blocks, a receiver, and a relay that sends again 24 blocks, all of one block entity type; a light sensor that reads
 * the sky's light and its own height, speaking either in redstone; and a block that counts its neighbours' updates.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestRadios {

    /** How far the test transmitter and the test relay send in open air. */
    public static final double TRANSMITTER_POWER = 32;
    public static final double RELAY_POWER = 24;

    public static final TextKey SKY_LIGHT = TextKey.of("jstests.sensor.sky_light", "Sky light");
    public static final TextKey HEIGHT = TextKey.of("jstests.sensor.height", "Height");

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<RadioBlock> TRANSMITTER = TestSounds.CONTENT.block("test_transmitter",
            properties -> new RadioBlock(properties, IRadio.Role.TRANSMITTER, TRANSMITTER_POWER,
                    () -> TestRadios.RADIO.get())).named("Test transmitter")
            .look(IBlockLook.cubeAll("test_transmitter")).register();
    public static final BlockEntry<RadioBlock> RECEIVER = TestSounds.CONTENT.block("test_receiver",
            properties -> new RadioBlock(properties, IRadio.Role.RECEIVER, 0, () -> TestRadios.RADIO.get()))
            .named("Test receiver").look(IBlockLook.cubeAll("test_receiver")).register();
    public static final BlockEntry<RadioBlock> RELAY = TestSounds.CONTENT.block("test_relay",
            properties -> new RadioBlock(properties, IRadio.Role.RELAY, RELAY_POWER, () -> TestRadios.RADIO.get()))
            .named("Test relay").look(IBlockLook.cubeAll("test_relay")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO =
            TestSounds.CONTENT.blockEntity("test_radio", (pos, state) -> new RadioBlockEntity(
                    TestRadios.RADIO.get(), pos, state), TRANSMITTER, RECEIVER, RELAY);
    public static final BlockEntry<LightSensorBlock> SENSOR = TestSounds.CONTENT.block("test_light_sensor",
            LightSensorBlock::new).named("Test light sensor").look(IBlockLook.cubeAll("test_light_sensor"))
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightSensorBlockEntity>> SENSOR_ENTITY =
            TestSounds.CONTENT.blockEntity("test_light_sensor", LightSensorBlockEntity::new, SENSOR);
    /** A block that counts how often its neighbours tell it they changed, for the tests of update storms. */
    public static final BlockEntry<UpdateCounterBlock> COUNTER = TestSounds.CONTENT.block("test_update_counter",
            UpdateCounterBlock::new).named("Test update counter").look(IBlockLook.cubeAll("test_update_counter"))
            .register();

    private static final Map<BlockPos, Integer> COUNTED = new ConcurrentHashMap<>();

    private TestRadios() {
    }

    /** Declares the radios and the sensor, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        Signals.offer(event, SENSOR_ENTITY.get());
    }

    /** How many times the update counter at {@code pos}, a place in the world, was told a neighbour changed. */
    public static int updatesAt(final BlockPos pos) {
        return COUNTED.getOrDefault(pos, 0);
    }

    /** The update counter: every change a neighbour tells it of is counted, by its place. */
    public static final class UpdateCounterBlock extends Block {

        public UpdateCounterBlock(final Properties properties) {
            super(properties);
        }

        @Override
        protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                       final Block neighbour, final BlockPos from, final boolean moved) {
            super.neighborChanged(state, level, pos, neighbour, from, moved);
            if (!level.isClientSide()) {
                COUNTED.merge(pos.immutable(), 1, Integer::sum);
            }
        }
    }

    /** The light sensor's block. */
    public static final class LightSensorBlock extends SensorBlock {

        public LightSensorBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new LightSensorBlockEntity(pos, state);
        }
    }

    /** The light sensor: the sky's light on it, from dark to full day, and its height, from the floor to the sky. */
    public static final class LightSensorBlockEntity extends SensorBlockEntity {

        public LightSensorBlockEntity(final BlockPos pos, final BlockState state) {
            super(SENSOR_ENTITY.get(), pos, state);
        }

        @Override
        public List<Reading> readings() {
            if (level == null) {
                return List.of();
            }
            final int light = level.getBrightness(LightLayer.SKY, worldPosition.above());
            return List.of(new Reading(SKY_LIGHT, light, 0, Reading.FULL, null),
                    new Reading(HEIGHT, worldPosition.getY(), level.getMinBuildHeight(), level.getMaxBuildHeight(),
                            null));
        }
    }
}
