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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The processing machines the autocraft tests feed: a kiln with one input that takes it through any face, and a mixer
 * with two inputs that each go in through a face of their own. Both work in a few ticks without power.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestMachines {

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<TestMachineBlock> KILN = TestSounds.CONTENT.block("test_kiln",
            properties -> new TestMachineBlock(properties, TestMachineBlockEntity.Kind.KILN))
            .named("Test kiln").look(IBlockLook.cubeAll("test_kiln")).register();
    public static final BlockEntry<TestMachineBlock> MIXER = TestSounds.CONTENT.block("test_mixer",
            properties -> new TestMachineBlock(properties, TestMachineBlockEntity.Kind.MIXER))
            .named("Test mixer").look(IBlockLook.cubeAll("test_mixer")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestMachineBlockEntity>> MACHINE =
            TestSounds.CONTENT.blockEntity("test_machine", TestMachineBlockEntity::new, KILN, MIXER);

    private TestMachines() {
    }

    /** Declares the machines, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE.get(),
                (machine, side) -> machine.handler(side));
    }
}
