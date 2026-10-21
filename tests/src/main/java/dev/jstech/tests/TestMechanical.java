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
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineShaft;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.mechanical.ITorque;
import dev.jstech.core.mechanical.MechanicalPartBlock;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.mechanical.MechanicalPartSpec;
import dev.jstech.core.mechanical.ShaftLinkItem;
import dev.jstech.core.mechanical.TorqueCurve;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Parts of mechanical power the tests build with, from the Core's kit: an iron shaft, gears of sixteen and thirty-two
 * teeth, a gearbox, a flywheel, a clutch, a brake, a speed controller, a shear pin, a shaft rated as weakly, two
 * pulleys and a belt, a torque meter; a hand crank of a man's arm (20 N·m, 75 W, up to 60 rpm) and a load that asks 15 N·m; and
 * machines on the machine kit with their shafts out of their backs: an electric motor, a dynamo, and two mills driven
 * by their shafts, one light enough for the crank and one too heavy.
 */
public final class TestMechanical {

    /** The hand crank. */
    public static final TorqueCurve CRANK_CURVE = TorqueCurve.of(20.0, 75.0, 60.0);
    /** The crank's top speed, in revolutions a minute. */
    public static final double CRANK_TOP = 60.0;
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
    /** The flywheel's inertia, in kilogram square metres: enough to coast for seconds on its friction. */
    public static final double FLYWHEEL_INERTIA = 2.0;
    /** The brake's torque at full redstone: more than the crank's. */
    public static final double BRAKE_TORQUE = 30.0;
    /** The shear pin's rating, and the weak shaft's: less than the load asks. */
    public static final double PIN_RATING = 10.0;
    /** The small pulley's radius, in metres; the large one's is twice it. */
    public static final double SMALL_RADIUS = 0.1;
    public static final int BELT_REACH = 8;
    /** The mill: it asks 10 N·m, stops below 20 rpm, and works at its whole speed from 60. */
    public static final double MILL_TORQUE = 10.0;
    public static final double MILL_LEAST = 20.0;
    public static final double MILL_RATED = 60.0;
    /** The heavy mill asks more than the crank can give above the least speed. */
    public static final double HEAVY_MILL_TORQUE = 18.0;
    public static final int MILL_TICKS = 20;

    /*
     * Their block states and models are written by hand: the test mod writes no models of its own. The shaft, the
     * gears and the pulleys are drawn turning, by the Core's models; the shaft whirs as it turns, loudest at the
     * crank's top speed.
     */
    public static final BlockEntry<MechanicalPartBlock> SHAFT = TestSounds.CONTENT.block("test_shaft",
            properties -> MechanicalPartBlock.turning(properties, MechanicalPartSpec.shaft(0.005, 0.1),
                    () -> TestMechanical.PART.get()).turningSound(TestSounds.WHIR, CRANK_TOP))
            .named("Test shaft").look(IBlockLook.cubeAll("test_shaft")).register();
    public static final BlockEntry<MechanicalPartBlock> SMALL_GEAR = turning("test_small_gear", "Test small gear",
            MechanicalPartSpec.gear(SMALL_TEETH, 0.05, 0.2));
    public static final BlockEntry<MechanicalPartBlock> LARGE_GEAR = turning("test_large_gear", "Test large gear",
            MechanicalPartSpec.gear(LARGE_TEETH, 0.2, 0.3));
    public static final BlockEntry<MechanicalPartBlock> GEARBOX = part("test_gearbox", "Test gearbox",
            MechanicalPartSpec.gearbox(0.05, 0.3));
    public static final BlockEntry<MechanicalPartBlock> FLYWHEEL = part("test_flywheel", "Test flywheel",
            MechanicalPartSpec.flywheel(FLYWHEEL_INERTIA, 0.1));
    public static final BlockEntry<MechanicalPartBlock> CLUTCH = part("test_clutch", "Test clutch",
            MechanicalPartSpec.clutch(0.01, 0.1));
    public static final BlockEntry<MechanicalPartBlock> BRAKE = part("test_brake", "Test brake",
            MechanicalPartSpec.brake(BRAKE_TORQUE, 0.01, 0.1));
    public static final BlockEntry<MechanicalPartBlock> CONTROLLER = part("test_speed_controller",
            "Test speed controller", MechanicalPartSpec.controller(0.01, 0.1));
    public static final BlockEntry<MechanicalPartBlock> SHEAR_PIN = part("test_shear_pin", "Test shear pin",
            MechanicalPartSpec.shearPin(PIN_RATING, 0.005, 0.1));
    public static final BlockEntry<MechanicalPartBlock> WEAK_SHAFT = part("test_weak_shaft", "Test weak shaft",
            MechanicalPartSpec.shaft(0.005, 0.1).rated(PIN_RATING));
    public static final BlockEntry<MechanicalPartBlock> SMALL_PULLEY = turning("test_small_pulley",
            "Test small pulley", MechanicalPartSpec.pulley(SMALL_RADIUS, 0.02, 0.1));
    public static final BlockEntry<MechanicalPartBlock> LARGE_PULLEY = turning("test_large_pulley",
            "Test large pulley", MechanicalPartSpec.pulley(2 * SMALL_RADIUS, 0.08, 0.1));
    public static final BlockEntry<MechanicalPartBlock> TORQUE_METER = part("test_torque_meter",
            "Test torque meter", MechanicalPartSpec.torqueMeter(0.005, 0.0));
    /** A propeller on a shaft, pushing north with its full thrust at the crank's top speed (its physics' data). */
    public static final BlockEntry<MechanicalPartBlock> PROPELLER = part("test_propeller", "Test propeller",
            MechanicalPartSpec.shaft(0.01, 0.1));
    /* Its item model is written by hand. */
    public static final ItemEntry<ShaftLinkItem> BELT = TestSounds.CONTENT.item("test_belt",
            properties -> new ShaftLinkItem(properties, false, BELT_REACH)).named("Test belt")
            .look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
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
                    TestMechanical.PART.get(), pos, state), SHAFT, SMALL_GEAR, LARGE_GEAR, GEARBOX, FLYWHEEL, CLUTCH,
                    BRAKE, CONTROLLER, SHEAR_PIN, WEAK_SHAFT, SMALL_PULLEY, LARGE_PULLEY, TORQUE_METER, PROPELLER);
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
    public static final BlockEntry<DeviceBlock> MILL = TestSounds.CONTENT.block("test_mill",
            Device.of(() -> TestMechanical.MILL_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test mill").look(IBlockLook.cubeAll("test_mill")).register();
    public static final BlockEntry<DeviceBlock> HEAVY_MILL = TestSounds.CONTENT.block("test_heavy_mill",
            Device.of(() -> TestMechanical.MILL_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test heavy mill").look(IBlockLook.cubeAll("test_heavy_mill")).register();
    /* One block entity for both mills, which reads from its block how much it asks. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillBlockEntity>> MILL_ENTITY =
            TestSounds.CONTENT.blockEntity("test_mill", MillBlockEntity::new, MILL, HEAVY_MILL);

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

    /* A part drawn turning by the Core's models. */
    private static BlockEntry<MechanicalPartBlock> turning(final String id, final String name,
                                                           final MechanicalPartSpec spec) {
        return TestSounds.CONTENT.block(id, properties -> MechanicalPartBlock.turning(properties, spec,
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

    /** A mill driven by its shaft: cobblestone into gravel, as fast as its shaft turns. */
    public static final class MillBlockEntity extends ProcessingMachineBlockEntity {

        public MillBlockEntity(final BlockPos pos, final BlockState state) {
            super(MILL_ENTITY.get(), pos, state, ProcessingMachineBlockEntity.Layout.items(1, 1),
                    MachineEnergy.fe(0, 0, 0), 0L);
            turnsWith(MachineShaft.driven(state.is(HEAVY_MILL.get()) ? HEAVY_MILL_TORQUE : MILL_TORQUE, MILL_LEAST,
                    MILL_RATED, RelativeFace.BACK));
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return input.items().getFirst().is(Items.COBBLESTONE) ? Optional.of(Processing.single(0,
                    new ItemStack(Items.GRAVEL), MILL_TICKS)) : Optional.empty();
        }
    }
}
