/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineShaft;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.mechanical.ITorque;
import dev.jstech.core.mechanical.MechanicalPartBlock;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.mechanical.MechanicalPartSpec;
import dev.jstech.core.mechanical.TorqueCurve;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Parts of mechanical power the tests build with, from the Core's kit: an iron shaft, gears of sixteen and thirty-two
 * teeth, a gearbox, a hand crank of a man's arm (20 N·m, 75 W, up to 60 rpm), a load that asks 15 N·m, and two
 * machines on the machine kit: an electric motor and a dynamo, their shafts out of their backs.
 */
public final class TestMechanical {

    /** The hand crank. */
    public static final TorqueCurve CRANK_CURVE = TorqueCurve.of(20.0, 75.0, 60.0);
    /** What the load asks, in newton-metres. */
    public static final double LOAD = 15.0;
    /** The motor: 50 N·m, 5 kW, up to 1,500 rpm, at nine tenths. */
    public static final TorqueCurve MOTOR_CURVE = TorqueCurve.of(50.0, 5_000.0, 1_500.0);
    /** The dynamo: up to 100 N·m and 10 kW, at its rated 3,000 rpm, at nine tenths. */
    public static final TorqueCurve DYNAMO_CURVE = TorqueCurve.of(100.0, 10_000.0, 3_000.0);
    public static final double EFFICIENCY = 0.9;
    public static final int ENERGY = 1_000_000;
    public static final int SMALL_TEETH = 16;
    public static final int LARGE_TEETH = 32;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<MechanicalPartBlock> SHAFT = part("test_shaft", "Test shaft",
            MechanicalPartSpec.shaft(0.005, 0.1));
    public static final BlockEntry<MechanicalPartBlock> SMALL_GEAR = part("test_small_gear", "Test small gear",
            MechanicalPartSpec.gear(SMALL_TEETH, 0.05, 0.2));
    public static final BlockEntry<MechanicalPartBlock> LARGE_GEAR = part("test_large_gear", "Test large gear",
            MechanicalPartSpec.gear(LARGE_TEETH, 0.2, 0.3));
    public static final BlockEntry<MechanicalPartBlock> GEARBOX = part("test_gearbox", "Test gearbox",
            MechanicalPartSpec.gearbox(0.05, 0.3));
    public static final BlockEntry<MechanicalPartBlock> CRANK = TestSounds.CONTENT.block("test_crank",
            properties -> new MechanicalPartBlock(properties, MechanicalPartSpec.shaft(0.01, 0.0),
                    () -> TestMechanical.CRANK_ENTITY.get())).named("Test crank")
            .look(IBlockLook.cubeAll("test_crank")).register();
    public static final BlockEntry<MechanicalPartBlock> LOAD_BLOCK = TestSounds.CONTENT.block("test_motion_load",
            properties -> new MechanicalPartBlock(properties, MechanicalPartSpec.shaft(0.01, 0.0),
                    () -> TestMechanical.LOAD_ENTITY.get())).named("Test motion load")
            .look(IBlockLook.cubeAll("test_motion_load")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPartBlockEntity>> PART =
            TestSounds.CONTENT.blockEntity("test_mechanical_part", (pos, state) -> new MechanicalPartBlockEntity(
                    TestMechanical.PART.get(), pos, state), SHAFT, SMALL_GEAR, LARGE_GEAR, GEARBOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrankBlockEntity>> CRANK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_crank", CrankBlockEntity::new, CRANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoadBlockEntity>> LOAD_ENTITY =
            TestSounds.CONTENT.blockEntity("test_motion_load", LoadBlockEntity::new, LOAD_BLOCK);
    public static final BlockEntry<DeviceBlock> MOTOR = TestSounds.CONTENT.block("test_motor",
            Device.of(() -> TestMechanical.SHAFT_MACHINE.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test motor").look(IBlockLook.cubeAll("test_motor")).register();
    public static final BlockEntry<DeviceBlock> DYNAMO = TestSounds.CONTENT.block("test_dynamo",
            Device.of(() -> TestMechanical.SHAFT_MACHINE.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test dynamo").look(IBlockLook.cubeAll("test_dynamo")).register();
    /* One block entity for both machines, which reads from its block which it is. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftMachineBlockEntity>> SHAFT_MACHINE =
            TestSounds.CONTENT.blockEntity("test_shaft_machine", ShaftMachineBlockEntity::new, MOTOR, DYNAMO);

    private TestMechanical() {
    }

    /** Declares the parts, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static BlockEntry<MechanicalPartBlock> part(final String id, final String name,
                                                        final MechanicalPartSpec spec) {
        return TestSounds.CONTENT.block(id, properties -> new MechanicalPartBlock(properties, spec,
                () -> TestMechanical.PART.get())).named(name).look(IBlockLook.cubeAll(id)).register();
    }

    /** The hand crank, always turned. */
    public static final class CrankBlockEntity extends MechanicalPartBlockEntity {

        public CrankBlockEntity(final BlockPos pos, final BlockState state) {
            super(CRANK_ENTITY.get(), pos, state);
        }

        @Override
        public ITorque torque() {
            return CRANK_CURVE::torque;
        }
    }

    /** The load, always asking. */
    public static final class LoadBlockEntity extends MechanicalPartBlockEntity {

        public LoadBlockEntity(final BlockPos pos, final BlockState state) {
            super(LOAD_ENTITY.get(), pos, state);
        }

        @Override
        public ITorque torque() {
            return ITorque.load(LOAD);
        }
    }

    /** The motor or the dynamo, as its block says. */
    public static final class ShaftMachineBlockEntity extends MachineBlockEntity {

        public ShaftMachineBlockEntity(final BlockPos pos, final BlockState state) {
            super(SHAFT_MACHINE.get(), pos, state, 0, MachineEnergy.fe(ENERGY, ENERGY, ENERGY));
            turnsWith(state.is(MOTOR.get()) ? MachineShaft.motor(MOTOR_CURVE, EFFICIENCY, RelativeFace.BACK)
                    : MachineShaft.dynamo(DYNAMO_CURVE, EFFICIENCY, RelativeFace.BACK));
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return Objects.requireNonNull(shaft()).run();
        }
    }
}
