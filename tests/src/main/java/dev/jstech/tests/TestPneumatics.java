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
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.transit.CapsuleStationBlockEntity;
import dev.jstech.industrial.line.PneumaticStation;
import dev.jstech.industrial.machine.IndustrialProcessingMachine;
import dev.jstech.industrial.machine.MachineRating;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * J's Industrial's compressed air and pneumatic post, on blocks of the test mod's own: a press with an air port,
 * which presses cobblestone into stone in a second, a tick of air at a time, and a station of the pneumatic post.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestPneumatics {

    public static final MachineRating PRESS_RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 1_000L);
    /** How long the press takes, in ticks, and the air it spends a tick, in millibuckets. */
    public static final int PRESSES_IN = 20;
    public static final long PRESS_AIR = 1L;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> AIR_PRESS = TestSounds.CONTENT.block("test_air_press",
            Device.of(() -> TestPneumatics.AIR_PRESS_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test air press").look(IBlockLook.cubeAll("test_air_press")).register();
    public static final BlockEntry<DeviceBlock> CAPSULE_STATION = TestSounds.CONTENT.block("test_capsule_station",
            Device.of(() -> TestPneumatics.CAPSULE_STATION_ENTITY.get())
                    .ticks(CapsuleStationBlockEntity::serverTick)::block)
            .named("Test capsule station").look(IBlockLook.cubeAll("test_capsule_station")).register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirPress>> AIR_PRESS_ENTITY =
            TestSounds.CONTENT.blockEntity("test_air_press", AirPress::new, AIR_PRESS);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticStation>>
            CAPSULE_STATION_ENTITY = TestSounds.CONTENT.blockEntity("test_capsule_station", (pos, state) ->
            new PneumaticStation(TestPneumatics.CAPSULE_STATION_ENTITY.get(), pos, state), CAPSULE_STATION);

    private TestPneumatics() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        PneumaticStation.registerWithAir(event, CAPSULE_STATION_ENTITY.get());
    }

    /** The test press with an air port. */
    public static final class AirPress extends IndustrialProcessingMachine {

        public AirPress(final BlockPos pos, final BlockState state) {
            super(AIR_PRESS_ENTITY.get(), pos, state, PRESS_RATING, ProcessingMachineBlockEntity.Layout.items(1, 1),
                    PRESS_AIR);
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return input.items().getFirst().is(Items.COBBLESTONE)
                    ? Optional.of(Processing.single(0, new ItemStack(Items.STONE), PRESSES_IN)) : Optional.empty();
        }
    }
}
