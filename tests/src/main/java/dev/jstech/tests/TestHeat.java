/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Blocks of the test mod that hold heat, to prove the Core's heat: a litre of copper, a litre of iron, the same litre
 * of copper with ten square metres of fins as a radiator, and a heater that puts fifty kilowatts into a litre of iron.
 */
public final class TestHeat {

    /** A litre: small enough that a test sees its temperature move in a few seconds. */
    public static final double LITRE = 0.001;
    /** The radiator's fins, in square metres. */
    public static final double FINS = 10.0;
    /** What the heater puts in, in watts: enough to warm a litre of copper beside it within seconds. */
    public static final double HEATER_WATTS = 50_000.0;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> COPPER = TestSounds.CONTENT.block("test_heat_copper",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test heat copper").look(IBlockLook.cubeAll("test_heat_copper")).register();
    public static final BlockEntry<DeviceBlock> IRON = TestSounds.CONTENT.block("test_heat_iron",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test heat iron").look(IBlockLook.cubeAll("test_heat_iron")).register();
    public static final BlockEntry<DeviceBlock> RADIATOR = TestSounds.CONTENT.block("test_radiator",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test radiator").look(IBlockLook.cubeAll("test_radiator")).register();
    public static final BlockEntry<DeviceBlock> HEATER = TestSounds.CONTENT.block("test_heater",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get()).ticks(HeatBlockEntity::tick)::block)
            .named("Test heater").look(IBlockLook.cubeAll("test_heater")).register();
    /* One block entity for all four, which reads from its block what it is. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_heat", HeatBlockEntity::new, COPPER, IRON, RADIATOR, HEATER);

    private TestHeat() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** A block that holds heat, of the material and with the fins its block says, the heater heating itself. */
    public static final class HeatBlockEntity extends SyncedBlockEntity implements IHeatHolder {

        private final HeatBody heat;

        public HeatBlockEntity(final BlockPos pos, final BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
            final boolean copper = state.is(COPPER.get()) || state.is(RADIATOR.get());
            this.heat = HeatBody.of(Thermal.of(Elements.of(copper ? Element.COPPER : Element.IRON), false), LITRE)
                    .exposed(state.is(RADIATOR.get()) ? FINS : 0.0);
            HeatPorts.keep(this, this.heat);
        }

        /** The heater puts its watts in, each tick. */
        public static void tick(final Level level, final BlockPos pos, final BlockState state,
                                final HeatBlockEntity heater) {
            if (heater.heat.known()) {
                heater.heat.exchange(HEATER_WATTS / 20.0);
            }
        }

        @Override
        public HeatBody heatBody() {
            return this.heat;
        }
    }
}
