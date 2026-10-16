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
import dev.jstech.core.light.Flicker;
import dev.jstech.core.light.LampBlock;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightKinds;
import dev.jstech.core.light.LightMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Lamps of the test mod, to prove the Core's lighting kit: a red alarm lamp that pulses while redstone powers it, a
 * lamp that shines while it gets the watts it draws, a floodlight whose cone reaches far past the game's fifteen
 * blocks, a narrow beam, and a green lamp that can be broken. Their kinds are declared here and written to the test
 * mod's data by its data run; their block states and models are written by hand.
 */
public final class TestLights {

    /** The alarm's red. */
    public static final int ALARM_RED = 0xFF2010;
    /** The fragile lamp's green. */
    public static final int FRAGILE_GREEN = 0x30FF50;
    /** What the powered lamp draws, in watts. */
    public static final double POWERED_WATTS = 40.0;
    /** How far the floodlight and the beam reach, in blocks. */
    public static final int FLOOD_REACH = 48;
    /** The floodlight's cone, in degrees. */
    public static final double FLOOD_ANGLE = 30.0;

    /** A red alarm lamp, pulsing while powered by redstone. */
    public static final ResourceLocation ALARM = LightKinds.declare(id("alarm"), new LightKind(ALARM_RED, 15, 15,
            LightKind.Shape.POINT, 0, Flicker.PULSE, LightKind.Control.REDSTONE, 0, false));
    /** A white lamp that shines while it gets its watts. */
    public static final ResourceLocation POWERED = LightKinds.declare(id("powered"), new LightKind(LightMath.WHITE, 14,
            14, LightKind.Shape.POINT, 0, Flicker.STEADY, LightKind.Control.POWER, POWERED_WATTS, false));
    /** A warm floodlight, always on, its cone reaching far. */
    public static final ResourceLocation FLOOD = LightKinds.declare(id("floodlight"), new LightKind(0xFFF0D0, 15,
            FLOOD_REACH, LightKind.Shape.SPOT, FLOOD_ANGLE, Flicker.STEADY, LightKind.Control.ALWAYS, 0, false));
    /** A narrow cyan beam, always on. */
    public static final ResourceLocation BEAM = LightKinds.declare(id("beam"), new LightKind(0x40E0FF, 15,
            FLOOD_REACH, LightKind.Shape.BEAM, 0, Flicker.STEADY, LightKind.Control.ALWAYS, 0, false));
    /** A green lamp, always on, that can be broken. */
    public static final ResourceLocation FRAGILE = LightKinds.declare(id("fragile"), new LightKind(FRAGILE_GREEN,
            12, 12, LightKind.Shape.POINT, 0, Flicker.STEADY, LightKind.Control.ALWAYS, 0, true));

    public static final BlockEntry<LampBlock> ALARM_LAMP = TestSounds.CONTENT.block("test_alarm_lamp",
            properties -> new LampBlock(properties, ALARM, () -> TestLights.LAMP.get()))
            .named("Test alarm lamp").look(IBlockLook.cubeAll("test_alarm_lamp")).register();
    public static final BlockEntry<LampBlock> POWERED_LAMP = TestSounds.CONTENT.block("test_powered_lamp",
            properties -> new LampBlock(properties, POWERED, () -> TestLights.LAMP.get()))
            .named("Test powered lamp").look(IBlockLook.cubeAll("test_powered_lamp")).register();
    public static final BlockEntry<LampBlock> FLOODLIGHT = TestSounds.CONTENT.block("test_floodlight",
            properties -> new LampBlock(properties, FLOOD, () -> TestLights.LAMP.get()))
            .named("Test floodlight").look(IBlockLook.cubeAll("test_floodlight")).register();
    public static final BlockEntry<LampBlock> BEAM_LAMP = TestSounds.CONTENT.block("test_beam_lamp",
            properties -> new LampBlock(properties, BEAM, () -> TestLights.LAMP.get()))
            .named("Test beam lamp").look(IBlockLook.cubeAll("test_beam_lamp")).register();
    public static final BlockEntry<LampBlock> FRAGILE_LAMP = TestSounds.CONTENT.block("test_fragile_lamp",
            properties -> new LampBlock(properties, FRAGILE, () -> TestLights.LAMP.get()))
            .named("Test fragile lamp").look(IBlockLook.cubeAll("test_fragile_lamp")).register();
    /* One block entity for every lamp, which reads its kind from its block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LampBlockEntity>> LAMP =
            TestSounds.CONTENT.blockEntity("test_lamp", (pos, state) -> new LampBlockEntity(TestLights.LAMP.get(),
                    pos, state), ALARM_LAMP, POWERED_LAMP, FLOODLIGHT, BEAM_LAMP, FRAGILE_LAMP);

    private TestLights() {
    }

    /** Declares the lamps and their kinds, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
