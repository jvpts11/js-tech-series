/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks the starship parts are built of, a kind for each real thing a part has, grouped by the section that places
 * them: the structure every section shares, the cockpit's, the airlock's and the docking port's, the reactor's, the
 * main engines', the air-breathing engines', the wings' and fins', the thrusters' and the landing gear's, and the
 * functional modules'. They are the same for every maker: the maker and the paint belong to the formed model. Each
 * shows the one face on all six sides, so a block is known from any side while the part is being built.
 */
public final class ShipKitBlocks {

    /** Their row of the mod's tab. */
    public static final ContentTab.Section SHIP_PARTS = SpaceContent.TAB.section();
    /* Every block of the kit, as declared. */
    private static final List<BlockEntry<Block>> ALL = new ArrayList<>();

    // The structure every section shares.
    public static final BlockEntry<Block> HULL_FRAME = add(kit("hull_frame").named("Hull Frame").register());
    public static final BlockEntry<Block> HULL_PLATING = add(kit("hull_plating").named("Hull Plating").register());
    public static final BlockEntry<Block> ACCESS_PANEL = add(kit("access_panel").named("Access Panel").register());
    public static final BlockEntry<Block> LIVERY_PLATING = add(kit("livery_plating").named("Livery Plating")
            .register());
    public static final BlockEntry<Block> THERMAL_PLATING = add(kit("thermal_plating").named("Thermal Plating")
            .register());
    public static final BlockEntry<Block> CARBON_EDGE = add(kit("carbon_edge").named("Carbon Edge").register());
    public static final BlockEntry<Block> DECK_PLATE = add(kit("deck_plate").named("Deck Plate").register());
    public static final BlockEntry<Block> DECK_GRATING = add(kit("deck_grating").named("Deck Grating").register());
    public static final BlockEntry<Block> BULKHEAD = add(kit("bulkhead").named("Bulkhead").register());
    public static final BlockEntry<Block> PRESSURE_DOOR = add(kit("pressure_door").named("Pressure Door").register());
    public static final BlockEntry<Block> CONDUIT = add(kit("conduit").named("Conduit").register());
    public static final BlockEntry<Block> CABLE_TRAY = add(kit("cable_tray").named("Cable Tray").register());
    public static final BlockEntry<Block> VIEWPORT = add(kit("viewport").named("Viewport").register());
    public static final BlockEntry<Block> LIGHT_PANEL = add(kit("light_panel").named("Light Panel").register());
    // The cockpit.
    public static final BlockEntry<Block> CANOPY_GLASS = add(kit("canopy_glass").named("Canopy Glass").register());
    public static final BlockEntry<Block> CANOPY_FRAME = add(kit("canopy_frame").named("Canopy Frame").register());
    public static final BlockEntry<Block> FLIGHT_CONSOLE = add(kit("flight_console").named("Flight Console")
            .register());
    public static final BlockEntry<Block> OVERHEAD_PANEL = add(kit("overhead_panel").named("Overhead Panel")
            .register());
    public static final BlockEntry<Block> PILOT_SEAT = add(kit("pilot_seat").named("Pilot Seat").register());
    public static final BlockEntry<Block> NOSE_SENSOR = add(kit("nose_sensor").named("Nose Sensor").register());
    // The airlock and the docking port.
    public static final BlockEntry<Block> AIRLOCK_DOOR = add(kit("airlock_door").named("Airlock Door").register());
    public static final BlockEntry<Block> AIRLOCK_CYCLER = add(kit("airlock_cycler").named("Airlock Cycler")
            .register());
    public static final BlockEntry<Block> SUIT_LOCKER = add(fixture("suit_locker", SuitLockerBlock::new)
            .named("Suit Locker").register());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuitLockerEntity>> SUIT_LOCKER_ENTITY =
            SpaceContent.CONTENT.blockEntity("suit_locker", SuitLockerEntity::new, SUIT_LOCKER);
    public static final BlockEntry<Block> OXYGEN_PANEL = add(fixture("oxygen_panel", OxygenPanelBlock::new)
            .named("Oxygen Panel").register());
    public static final BlockEntry<Block> DOCKING_COLLAR = add(kit("docking_collar").named("Docking Collar")
            .register());
    public static final BlockEntry<Block> DOCKING_GUIDE = add(kit("docking_guide").named("Docking Guide").register());
    // The fusion reactor.
    public static final BlockEntry<Block> FUSION_CORE = add(kit("fusion_core").named("Fusion Core").register());
    public static final BlockEntry<Block> MAGNETIC_COIL = add(kit("magnetic_coil").named("Magnetic Coil").register());
    public static final BlockEntry<Block> REACTOR_SHIELD = add(kit("reactor_shield").named("Reactor Shield")
            .register());
    public static final BlockEntry<Block> COOLANT_PUMP = add(kit("coolant_pump").named("Coolant Pump").register());
    public static final BlockEntry<Block> RADIATOR_PANEL = add(kit("radiator_panel").named("Radiator Panel")
            .register());
    // The main engines; the rocket kit has an Engine Mount of its own.
    public static final BlockEntry<Block> ENGINE_MOUNT = add(kit("ship_engine_mount").named("Ship Engine Mount")
            .register());
    public static final BlockEntry<Block> THRUST_CHAMBER = add(kit("thrust_chamber").named("Thrust Chamber")
            .register());
    public static final BlockEntry<Block> BELL_SEGMENT = add(kit("bell_segment").named("Bell Segment").register());
    public static final BlockEntry<Block> PROPELLANT_FEED = add(kit("propellant_feed").named("Propellant Feed")
            .register());
    public static final BlockEntry<Block> FUEL_TANK = add(kit("fuel_tank").named("Fuel Tank").register());
    // The air-breathing engines.
    public static final BlockEntry<Block> INTAKE_LIP = add(kit("intake_lip").named("Intake Lip").register());
    public static final BlockEntry<Block> INTAKE_FAN = add(kit("intake_fan").named("Intake Fan").register());
    public static final BlockEntry<Block> COMPRESSOR_STAGE = add(kit("compressor_stage").named("Compressor Stage")
            .register());
    public static final BlockEntry<Block> SWIVEL_DUCT = add(kit("swivel_duct").named("Swivel Duct").register());
    public static final BlockEntry<Block> NOZZLE_VANE = add(kit("nozzle_vane").named("Nozzle Vane").register());
    // The wings and fins.
    public static final BlockEntry<Block> WING_SPAR = add(kit("wing_spar").named("Wing Spar").register());
    public static final BlockEntry<Block> WING_SKIN = add(kit("wing_skin").named("Wing Skin").register());
    public static final BlockEntry<Block> FLAP_ACTUATOR = add(kit("flap_actuator").named("Flap Actuator").register());
    public static final BlockEntry<Block> WING_TIP_LIGHT = add(kit("wing_tip_light").named("Wing Tip Light")
            .register());
    public static final BlockEntry<Block> CONTROL_SURFACE = add(kit("control_surface").named("Control Surface")
            .register());
    // The thrusters and the landing gear.
    public static final BlockEntry<Block> THRUSTER_BLOCK = add(kit("thruster_block").named("Thruster Block")
            .register());
    public static final BlockEntry<Block> VALVE_PACK = add(kit("valve_pack").named("Valve Pack").register());
    public static final BlockEntry<Block> LANDING_STRUT = add(kit("landing_strut").named("Landing Strut").register());
    public static final BlockEntry<Block> GEAR_BAY_DOOR = add(kit("gear_bay_door").named("Gear Bay Door").register());
    public static final BlockEntry<Block> LANDING_FOOT = add(kit("landing_foot").named("Landing Foot").register());
    public static final BlockEntry<Block> WHEEL = add(kit("wheel").named("Wheel").register());
    // The functional modules and the holds.
    public static final BlockEntry<Block> LAB_PANEL = add(kit("lab_panel").named("Lab Panel").register());
    public static final BlockEntry<Block> SAMPLE_RACK = add(kit("sample_rack").named("Sample Rack").register());
    public static final BlockEntry<Block> SERVER_PANEL = add(kit("server_panel").named("Server Panel").register());
    public static final BlockEntry<Block> ANTENNA_FEED = add(kit("antenna_feed").named("Antenna Feed").register());
    public static final BlockEntry<Block> BUNK_FRAME = add(fixture("bunk_frame", BunkBlock::new).named("Bunk Frame")
            .register());
    public static final BlockEntry<Block> CARGO_RACK = add(kit("cargo_rack").named("Cargo Rack").register());
    public static final BlockEntry<Block> TIE_DOWN_DECK = add(kit("tie_down_deck").named("Tie-Down Deck").register());
    public static final BlockEntry<Block> RAMP_HINGE = add(kit("ramp_hinge").named("Ramp Hinge").register());
    public static final BlockEntry<Block> RAMP_PLATE = add(kit("ramp_plate").named("Ramp Plate").register());

    private ShipKitBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every block of the kit. */
    public static List<BlockEntry<Block>> all() {
        return List.copyOf(ALL);
    }

    /* A block of the kit: metal, as hard as iron, its face {@code block/ship/<id>} on all six sides. */
    private static BlockBuilder<Block> kit(final String id) {
        return metal(SpaceContent.CONTENT.block(id, Block::new))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/ship/" + id)));
    }

    /* A block of the kit that works aboard, made by {@code factory}: drawn by its part's model once that is formed. */
    private static BlockBuilder<Block> fixture(final String id,
                                               final Function<BlockBehaviour.Properties, Block> factory) {
        final IBlockModel cube = new IBlockModel.CubeAll(id, "block/ship/" + id);
        final IBlockModel formed = new IBlockModel.ParticleOnly(id + "_formed", "jsspace:block/ship/" + id);
        return metal(SpaceContent.CONTENT.block(id, factory))
                .look(IBlockLook.byState(state -> state.getValue(ShipFixtureBlock.FORMED) ? formed : cube));
    }

    /* Metal, as hard as iron, dropping itself, in the tab's row. */
    private static BlockBuilder<Block> metal(final BlockBuilder<Block> block) {
        return block.properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.METAL))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(SHIP_PARTS);
    }

    private static BlockEntry<Block> add(final BlockEntry<Block> block) {
        ALL.add(block);
        return block;
    }
}
