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
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.vehicle.Fuel;
import dev.jstech.core.vehicle.FuelPump;
import dev.jstech.core.vehicle.Fuels;
import dev.jstech.core.vehicle.VehicleDef;
import dev.jstech.core.vehicle.VehicleKeyItem;
import dev.jstech.core.vehicle.VehicleLightRole;
import dev.jstech.core.vehicle.VehiclePartDef;
import dev.jstech.core.vehicle.VehiclePartKind;
import dev.jstech.core.vehicle.VehicleParts;
import dev.jstech.core.vehicle.VehicleRoles;
import dev.jstech.core.vehicle.Vehicles;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The vehicles of the test mod, declared the way a mod declares its own, to prove the Core's vehicles: a van that
 * burns the test fuel, with two seats, a boot, a cargo tank of water, two paints and a key, which tows a trailer; an
 * electric drone held up by four rotors; a boat that burns the test gas; their parts, their fuels, and a fuel pump of
 * every kind of fuel. They are drawn as the test crate: the tests look at what they do, not at them. The numbers are
 * the tests', small enough that a few seconds show what a fuel, a battery or a blow does.
 */
@TextHolder
public final class TestVehicles {

    public static final TextKey VAN_NAME = TextKey.of("vehicle.jstests.test_van", "Test Van");
    public static final TextKey TRAILER_NAME = TextKey.of("vehicle.jstests.test_trailer", "Test Trailer");
    public static final TextKey DRONE_NAME = TextKey.of("vehicle.jstests.test_drone", "Test Drone");
    public static final TextKey BOAT_NAME = TextKey.of("vehicle.jstests.test_boat", "Test Boat");
    public static final TextKey ENGINE_NAME = TextKey.of("vehicle_part.jstests.test_engine", "Test Engine");
    public static final TextKey MOTOR_NAME = TextKey.of("vehicle_part.jstests.test_motor", "Test Electric Motor");
    public static final TextKey WHEEL_NAME = TextKey.of("vehicle_part.jstests.test_wheel", "Test Wheel");
    public static final TextKey TANK_NAME = TextKey.of("vehicle_part.jstests.test_tank", "Test Fuel Tank");
    public static final TextKey BATTERY_NAME = TextKey.of("vehicle_part.jstests.test_battery", "Test Battery");
    public static final TextKey GEARBOX_NAME = TextKey.of("vehicle_part.jstests.test_gearbox", "Test Gearbox");
    public static final TextKey HEADLIGHT_NAME = TextKey.of("vehicle_part.jstests.test_headlight", "Test Headlight");
    public static final TextKey HORN_NAME = TextKey.of("vehicle_part.jstests.test_horn", "Test Horn");
    public static final TextKey ROTOR_NAME = TextKey.of("vehicle_part.jstests.test_rotor", "Test Rotor");
    public static final TextKey PROPELLER_NAME = TextKey.of("vehicle_part.jstests.test_propeller",
            "Test Propeller");
    public static final TextKey FUEL_NAME = TextKey.of("fuel.jstests.test_fuel", "Test Fuel");
    public static final TextKey GAS_FUEL_NAME = TextKey.of("fuel.jstests.test_gas_fuel", "Test Gas Fuel");
    public static final TextKey WHITE_NAME = TextKey.of("paint.jstests.white", "White");
    public static final TextKey RED_NAME = TextKey.of("paint.jstests.red", "Red");
    public static final TextKey HIGH_ROOF_NAME = TextKey.of("variant.jstests.high_roof", "High Roof");
    public static final TextKey PLAIN_NAME = TextKey.of("variant.jstests.plain", "Plain");

    /** The liquids the test fuel is: the test mod's cool oil, by its own tag. */
    public static final TagKey<Fluid> FUEL_LIQUIDS = TagKey.create(Registries.FLUID, id("test_fuel"));
    /** What a millibucket of the test fuel gives, in joules: little, so a test sees a tank go down. */
    public static final double FUEL_JOULES = 100_000;
    /** What a millibucket of the test gas gives, in joules, as pressed. */
    public static final double GAS_JOULES = 50_000;
    /** The van's engine, in watts. */
    public static final double VAN_POWER = 60_000;
    /** What the van's fuel tank holds, in millibuckets. */
    public static final int VAN_TANK = 1_000;
    /** What the drone's battery holds, in joules. */
    public static final int DRONE_BATTERY = 2_000_000;
    /** The van's boot, in slots. */
    public static final int VAN_BOOT = 9;
    /** The van's cargo tank of water, in millibuckets. */
    public static final int VAN_CARGO = 4_000;
    /** What the pump holds: a tank of liquid, a volume of gas and a store of charge. */
    public static final FuelPump.Size PUMP_SIZE = new FuelPump.Size(16_000, 16_000, 10_000_000);
    /** What the pump gives a vehicle at most each half second. */
    public static final FuelPump.Flow PUMP_FLOW = new FuelPump.Flow(250, 5_000, 200_000);

    public static final ResourceLocation FUEL = Fuels.declare(id("test_fuel"),
            Fuel.liquid(FUEL_NAME.key(), FUEL_LIQUIDS, FUEL_JOULES));
    public static final ResourceLocation GAS_FUEL = Fuels.declare(id("test_gas_fuel"),
            Fuel.gas(GAS_FUEL_NAME.key(), List.of(id("testogen")), GAS_JOULES));

    public static final ResourceLocation ENGINE = VehicleParts.declare(id("test_engine"), VehiclePartDef.engine(
            ENGINE_NAME.key(), 150, new VehiclePartDef.Engine(VAN_POWER, 800, 4_000, 0.3, false, List.of(FUEL),
                    Optional.empty())).mendedWith(id("minecraft", "iron_ingot")));
    public static final ResourceLocation GAS_ENGINE = VehicleParts.declare(id("test_gas_engine"),
            VehiclePartDef.engine(ENGINE_NAME.key(), 80, new VehiclePartDef.Engine(20_000, 800, 4_000, 0.3, false,
                    List.of(GAS_FUEL), Optional.empty())));
    public static final ResourceLocation MOTOR = VehicleParts.declare(id("test_motor"), VehiclePartDef.engine(
            MOTOR_NAME.key(), 4, new VehiclePartDef.Engine(4_000, 0, 9_000, 0.9, true, List.of(),
                    Optional.empty())));
    public static final ResourceLocation WHEEL = VehicleParts.declare(id("test_wheel"), VehiclePartDef.wheel(
            WHEEL_NAME.key(), 20, new VehiclePartDef.Wheel(0.35, 1.0, 40_000, 4_000, 0.15))
            .mendedWith(id("minecraft", "leather")));
    public static final ResourceLocation TANK = VehicleParts.declare(id("test_tank"),
            VehiclePartDef.tank(TANK_NAME.key(), 10, VAN_TANK));
    public static final ResourceLocation BATTERY = VehicleParts.declare(id("test_battery"),
            VehiclePartDef.battery(BATTERY_NAME.key(), 6, DRONE_BATTERY));
    public static final ResourceLocation GEARBOX = VehicleParts.declare(id("test_gearbox"), VehiclePartDef.gearbox(
            GEARBOX_NAME.key(), 40, new VehiclePartDef.Gearbox(List.of(3.5, 2.1, 1.4, 1.0, 0.8), 3.2, 3.9)));
    public static final ResourceLocation HEADLIGHT = VehicleParts.declare(id("test_headlight"),
            VehiclePartDef.light(HEADLIGHT_NAME.key(), 1, new VehiclePartDef.Light(VehicleLightRole.HEAD, 14,
                    0xFFF4D6, 12)));
    public static final ResourceLocation HORN = VehicleParts.declare(id("test_horn"), VehiclePartDef.horn(
            HORN_NAME.key(), 1, new VehiclePartDef.Horn(Optional.empty())));
    public static final ResourceLocation ROTOR = VehicleParts.declare(id("test_rotor"), VehiclePartDef.rotor(
            ROTOR_NAME.key(), 1, new VehiclePartDef.Thrust(150)));
    public static final ResourceLocation PROPELLER = VehicleParts.declare(id("test_propeller"),
            VehiclePartDef.propeller(PROPELLER_NAME.key(), 15, new VehiclePartDef.Thrust(2_000)));

    public static final ItemEntry<VehicleKeyItem> KEY = TestSounds.CONTENT.item("test_vehicle_key",
            properties -> new VehicleKeyItem(properties.stacksTo(1))).named("Test Vehicle Key")
            .look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    public static final ResourceLocation VAN = Vehicles.declare(id("test_van"), VehicleDef.builder(VAN_NAME.key())
            .model(TestModels.CRATE.id())
            .body(1.8F, 1.6F, 1_500, 0.9)
            .seat(new Vec3(0.4, 0.5, 0.4), VehicleRoles.DRIVER)
            .seat(new Vec3(-0.4, 0.5, 0.4), VehicleRoles.PASSENGER)
            .place("engine", VehiclePartKind.ENGINE, new Vec3(0, 0.5, 0.9), ENGINE)
            .place("gearbox", VehiclePartKind.GEARBOX, new Vec3(0, 0.3, 0.5), GEARBOX)
            .place("tank", VehiclePartKind.TANK, new Vec3(0, 0.3, -0.6), TANK)
            .place("headlights", VehiclePartKind.LIGHT, new Vec3(0, 0.6, 1.0), HEADLIGHT)
            .place("horn", VehiclePartKind.HORN, new Vec3(0, 0.6, 1.0), HORN)
            .wheel("front_left", new Vec3(0.7, 0, 0.9), WHEEL, true, false)
            .wheel("front_right", new Vec3(-0.7, 0, 0.9), WHEEL, true, false)
            .wheel("rear_left", new Vec3(0.7, 0, -0.9), WHEEL, false, true)
            .wheel("rear_right", new Vec3(-0.7, 0, -0.9), WHEEL, false, true)
            .boot(VAN_BOOT)
            .cargo(VAN_CARGO, FluidTags.WATER)
            .paint("white", WHITE_NAME.key(), id("minecraft", "textures/block/white_concrete.png"), DyeColor.WHITE)
            .paint("red", RED_NAME.key(), id("minecraft", "textures/block/red_concrete.png"), DyeColor.RED)
            .variant("plain", PLAIN_NAME.key(), null)
            .variant("high_roof", HIGH_ROOF_NAME.key(), null)
            .tow(new Vec3(0, 0.4, -1.1))
            .key(KEY.getId())
            .door("door_left")
            .build());
    public static final ResourceLocation TRAILER = Vehicles.declare(id("test_trailer"),
            VehicleDef.builder(TRAILER_NAME.key())
                    .model(TestModels.CRATE.id())
                    .body(1.4F, 1.0F, 300, 0.6)
                    .wheel("left", new Vec3(0.6, 0, 0), WHEEL, false, false)
                    .wheel("right", new Vec3(-0.6, 0, 0), WHEEL, false, false)
                    .boot(27)
                    .hitch(new Vec3(0, 0.4, 1.2))
                    .build());
    public static final ResourceLocation DRONE = Vehicles.declare(id("test_drone"),
            VehicleDef.builder(DRONE_NAME.key())
                    .model(TestModels.CRATE.id())
                    .movement(VehicleDef.Movement.AIR)
                    .body(1.0F, 0.5F, 10, 0.3)
                    .seat(new Vec3(0, 0.3, 0), VehicleRoles.DRIVER)
                    .place("motor", VehiclePartKind.ENGINE, new Vec3(0, 0.2, 0), MOTOR)
                    .place("battery", VehiclePartKind.BATTERY, new Vec3(0, 0.1, 0), BATTERY)
                    .place("rotor_front_left", VehiclePartKind.ROTOR, new Vec3(0.4, 0.4, 0.4), ROTOR)
                    .place("rotor_front_right", VehiclePartKind.ROTOR, new Vec3(-0.4, 0.4, 0.4), ROTOR)
                    .place("rotor_rear_left", VehiclePartKind.ROTOR, new Vec3(0.4, 0.4, -0.4), ROTOR)
                    .place("rotor_rear_right", VehiclePartKind.ROTOR, new Vec3(-0.4, 0.4, -0.4), ROTOR)
                    .build());
    public static final ResourceLocation BOAT = Vehicles.declare(id("test_boat"),
            VehicleDef.builder(BOAT_NAME.key())
                    .model(TestModels.CRATE.id())
                    .movement(VehicleDef.Movement.WATER)
                    .body(1.6F, 0.8F, 300, 1.2)
                    .seat(new Vec3(0, 0.3, -0.4), VehicleRoles.DRIVER)
                    .place("engine", VehiclePartKind.ENGINE, new Vec3(0, 0.3, -0.8), GAS_ENGINE)
                    .place("tank", VehiclePartKind.TANK, new Vec3(0, 0.2, 0), TANK)
                    .place("propeller", VehiclePartKind.PROPELLER, new Vec3(0, -0.2, -1.0), PROPELLER)
                    .build());

    /* Its block state and model are written by hand, as the test mod's are. */
    public static final BlockEntry<DeviceBlock> PUMP = TestSounds.CONTENT.block("test_fuel_pump",
            Device.of(() -> TestVehicles.PUMP_ENTITY.get()).ticks(PumpBlockEntity::tick)::block)
            .named("Test fuel pump").look(IBlockLook.cubeAll("test_fuel_pump")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP_ENTITY =
            TestSounds.CONTENT.blockEntity("test_fuel_pump", PumpBlockEntity::new, PUMP);

    private TestVehicles() {
    }

    /** Declares the vehicles, their parts and fuels and the pump, before the test mod's content is registered. */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event ->
                FuelPump.offer(event, PUMP_ENTITY.get(), PumpBlockEntity::pump));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }

    private static ResourceLocation id(final String namespace, final String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** The test pump: the Core's fuel pump as it comes. */
    public static final class PumpBlockEntity extends SyncedBlockEntity {

        private final FuelPump pump = new FuelPump(PUMP_SIZE, PUMP_FLOW, this::setChanged);

        public PumpBlockEntity(final BlockPos pos, final BlockState state) {
            super(PUMP_ENTITY.get(), pos, state);
            fields().part("Pump", this.pump).save();
        }

        /** Fills the vehicles in reach, each server tick. */
        public static void tick(final Level level, final BlockPos pos, final BlockState state,
                                final PumpBlockEntity pump) {
            if (level instanceof ServerLevel server) {
                pump.pump.tick(server, pos);
            }
        }

        /** Its pump. */
        public FuelPump pump() {
            return this.pump;
        }
    }
}
