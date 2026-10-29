/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The blocks the rocket parts are built of, a kind for each real thing a part has, placed where it goes: the rings',
 * the engines', the tanks', the legs', the lunar lander's and the capsules'. Each shows the one face on all six sides,
 * so a block is known from any side while the part is being built; once the part forms, its model draws each placed
 * block where it stands.
 */
public final class RocketKitBlocks {

    /** Their row of the mod's tab. */
    public static final ContentTab.Section ROCKET_PARTS = SpaceContent.TAB.section();
    /* Every block of the kits, as declared. */
    private static final List<BlockEntry<Block>> ALL = new ArrayList<>();

    // The instrument ring: the flight computer's and the antenna's ring, open at the top.
    public static final BlockEntry<Block> INSTRUMENT_WALL = add(kit("instrument_wall").named("Instrument Wall")
            .register());
    public static final BlockEntry<Block> AVIONICS_PANEL = add(kit("avionics_panel").named("Avionics Panel")
            .register());
    public static final BlockEntry<Block> BATTERY_PANEL = add(kit("battery_panel").named("Battery Panel").register());
    public static final BlockEntry<Block> ACCESS_DOOR = add(kit("access_door").named("Access Door").register());
    public static final BlockEntry<Block> UMBILICAL_PLATE = add(kit("umbilical_plate").named("Umbilical Plate")
            .register());
    public static final BlockEntry<Block> TELEMETRY_ANTENNA = add(kit("telemetry_antenna").named("Telemetry Antenna")
            .register());
    public static final BlockEntry<Block> COOLING_LOUVRES = add(kit("cooling_louvres").named("Cooling Louvres")
            .register());
    public static final BlockEntry<Block> GUIDANCE_PLATFORM = add(kit("guidance_platform").named("Guidance Platform")
            .register());
    // The separation ring, which drops a spent stage, its retro rockets pushing it away.
    public static final BlockEntry<Block> SEPARATION_WALL = add(kit("separation_wall").named("Separation Wall")
            .register());
    public static final BlockEntry<Block> RETRO_ROCKET = add(kit("retro_rocket").named("Retro Rocket").register());
    public static final BlockEntry<Block> ULLAGE_MOTOR = add(kit("ullage_motor").named("Ullage Motor").register());
    // The engine.
    public static final BlockEntry<Block> ENGINE_MOUNT = add(kit("engine_mount").named("Engine Mount").register());
    public static final BlockEntry<Block> COMBUSTION_CHAMBER = add(kit("combustion_chamber")
            .named("Combustion Chamber").register());
    public static final BlockEntry<Block> TURBOPUMP = add(kit("turbopump").named("Turbopump").register());
    public static final BlockEntry<Block> GIMBAL_ACTUATOR = add(kit("gimbal_actuator").named("Gimbal Actuator")
            .register());
    public static final BlockEntry<Block> NOZZLE_WALL = add(kit("nozzle_wall").named("Nozzle Wall").register());
    public static final BlockEntry<Block> NOZZLE_EXTENSION = add(kit("nozzle_extension").named("Nozzle Extension")
            .register());
    // The tank.
    public static final BlockEntry<Block> TANK_WALL = add(kit("tank_wall").named("Tank Wall").register());
    public static final BlockEntry<Block> TANK_DOME = add(kit("tank_dome").named("Tank Dome").register());
    public static final BlockEntry<Block> FILL_PORT = add(kit("fill_port").named("Fill Port").register());
    public static final BlockEntry<Block> VENT_VALVE = add(kit("vent_valve").named("Vent Valve").register());
    public static final BlockEntry<Block> CABLE_RACEWAY = add(kit("cable_raceway").named("Cable Raceway").register());
    public static final BlockEntry<Block> TANK_SKIRT = add(kit("tank_skirt").named("Tank Skirt").register());
    // The landing legs.
    public static final BlockEntry<Block> LEG_MOUNT = add(kit("leg_mount").named("Leg Mount").register());
    public static final BlockEntry<Block> LANDING_LEG = add(kit("landing_leg").named("Landing Leg").register());
    public static final BlockEntry<Block> LEG_ACTUATOR = add(kit("leg_actuator").named("Leg Actuator").register());
    // The lunar lander.
    public static final BlockEntry<Block> DESCENT_FOIL = add(kit("descent_foil").named("Descent Foil").register());
    public static final BlockEntry<Block> DESCENT_ENGINE = add(kit("descent_engine").named("Descent Engine")
            .register());
    public static final BlockEntry<Block> LANDER_LEG = add(kit("lander_leg").named("Lander Leg").register());
    public static final BlockEntry<Block> LANDER_LADDER = add(kit("lander_ladder").named("Lander Ladder").register());
    public static final BlockEntry<Block> CABIN_WALL = add(kit("cabin_wall").named("Cabin Wall").register());
    public static final BlockEntry<Block> CABIN_WINDOW = add(kit("cabin_window").named("Cabin Window").register());
    public static final BlockEntry<Block> PROPELLANT_TANK = add(kit("propellant_tank").named("Propellant Tank")
            .register());
    public static final BlockEntry<Block> RENDEZVOUS_RADAR = add(kit("rendezvous_radar").named("Rendezvous Radar")
            .register());
    public static final BlockEntry<Block> ASCENT_ENGINE = add(kit("ascent_engine").named("Ascent Engine").register());
    // The capsule, and what the lander's cabin shares with it.
    public static final BlockEntry<Block> HEAT_SHIELD = add(kit("heat_shield").named("Heat Shield").register());
    public static final BlockEntry<Block> CAPSULE_HULL = add(kit("capsule_hull").named("Capsule Hull").register());
    public static final BlockEntry<Block> CAPSULE_WINDOW = add(kit("capsule_window").named("Capsule Window")
            .register());
    public static final BlockEntry<Block> CAPSULE_HATCH = add(kit("capsule_hatch").named("Capsule Hatch").register());
    public static final BlockEntry<Block> CREW_SEAT = add(kit("crew_seat").named("Crew Seat").register());
    public static final BlockEntry<Block> FLIGHT_COMPUTER = add(kit("flight_computer").named("Flight Computer")
            .register());
    public static final BlockEntry<Block> THRUSTER_QUAD = add(kit("thruster_quad").named("Thruster Quad").register());
    public static final BlockEntry<Block> PARACHUTE_BAY = add(kit("parachute_bay").named("Parachute Bay").register());
    public static final BlockEntry<Block> DOCKING_PORT = add(kit("docking_port").named("Docking Port").register());

    private RocketKitBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every block of the kits. */
    public static List<BlockEntry<Block>> all() {
        return List.copyOf(ALL);
    }

    /* A block of a kit: metal, as hard as iron, its face {@code block/rocket/<id>} on all six sides. */
    private static BlockBuilder<Block> kit(final String id) {
        return SpaceContent.CONTENT.block(id, Block::new)
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.METAL))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/rocket/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(ROCKET_PARTS);
    }

    private static BlockEntry<Block> add(final BlockEntry<Block> block) {
        ALL.add(block);
        return block;
    }
}
