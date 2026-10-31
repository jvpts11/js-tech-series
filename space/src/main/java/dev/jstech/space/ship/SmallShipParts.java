/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipPlace;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The small starship's catalogue of 25 parts in ten kinds, the base the player builds a ship of: every fuselage section
 * ends in the class's standard joint, so any cockpit fits any module and any module any engine. Three cockpits; the
 * airlock, docking and corridor hull modules (the docking module's dorsal collar is an airlock too); three holds of the
 * medium size, one of them opening its ramp at the tail; two fusion reactors; a twin-bell and a four-bell tail engine
 * and a magnetic nozzle in a nacelle; side intakes and a nacelle intake, both turning their nozzles down for a vertical
 * take-off; three wings, the stub wings carrying the nacelles; two fins; two thrusters; wheels and legs. Then its five
 * small functional modules, a third of the small hull's room each: the workshop, the laboratory, the quarters,
 * computing and small cargo. The lengths along the spine are those of the parts on the block grid.
 */
@TextHolder
public final class SmallShipParts {

    public static final TextKey ORBITER_COCKPIT = TextKey.of("jsspace.ship_part.orbiter_cockpit",
            "Orbiter Cockpit");
    public static final TextKey CAB_COCKPIT = TextKey.of("jsspace.ship_part.cab_cockpit", "Cab Cockpit");
    public static final TextKey GREENHOUSE_COCKPIT = TextKey.of("jsspace.ship_part.greenhouse_cockpit",
            "Greenhouse Cockpit");
    public static final TextKey AIRLOCK = TextKey.of("jsspace.ship_part.airlock", "Airlock");
    public static final TextKey DOCKING = TextKey.of("jsspace.ship_part.docking", "Docking Module");
    public static final TextKey CORRIDOR = TextKey.of("jsspace.ship_part.corridor", "Corridor");
    public static final TextKey BAY = TextKey.of("jsspace.ship_part.bay", "Bay Doors Hold");
    public static final TextKey RAMP = TextKey.of("jsspace.ship_part.ramp", "Ramp Hold");
    public static final TextKey SIDE_DOOR = TextKey.of("jsspace.ship_part.side_door", "Side Door Hold");
    public static final TextKey REACTOR_FLUSH = TextKey.of("jsspace.ship_part.reactor_flush",
            "Reactor, Flush Radiators");
    public static final TextKey REACTOR_FINS = TextKey.of("jsspace.ship_part.reactor_fins",
            "Reactor, Radiator Fins");
    public static final TextKey TWIN_BELLS = TextKey.of("jsspace.ship_part.twin_bells", "Twin Bells");
    public static final TextKey CLUSTER = TextKey.of("jsspace.ship_part.cluster", "Cluster");
    public static final TextKey MAGNETIC_NOZZLE = TextKey.of("jsspace.ship_part.magnetic_nozzle",
            "Magnetic Nozzle");
    public static final TextKey SIDE_INTAKES = TextKey.of("jsspace.ship_part.side_intakes", "Side Intakes");
    public static final TextKey NACELLE_INTAKE = TextKey.of("jsspace.ship_part.nacelle_intake", "Nacelle Intake");
    public static final TextKey BLENDED_DELTA = TextKey.of("jsspace.ship_part.blended_delta", "Blended Delta");
    public static final TextKey STUB_WINGS = TextKey.of("jsspace.ship_part.stub_wings", "Stub Wings");
    public static final TextKey SWEPT_WINGS = TextKey.of("jsspace.ship_part.swept_wings", "Swept Wings");
    public static final TextKey TWIN_CANTED_FINS = TextKey.of("jsspace.ship_part.twin_canted_fins",
            "Twin Canted Fins");
    public static final TextKey SINGLE_FIN = TextKey.of("jsspace.ship_part.single_fin", "Single Fin");
    public static final TextKey BLISTERS = TextKey.of("jsspace.ship_part.blisters", "Thruster Blisters");
    public static final TextKey QUADS = TextKey.of("jsspace.ship_part.quads", "Thruster Quads");
    public static final TextKey WHEELS = TextKey.of("jsspace.ship_part.wheels", "Wheels");
    public static final TextKey LEGS = TextKey.of("jsspace.ship_part.legs", "Legs");
    public static final TextKey WORKSHOP = TextKey.of("jsspace.ship_part.workshop", "Workshop");
    public static final TextKey LAB = TextKey.of("jsspace.ship_part.lab", "Laboratory");
    public static final TextKey QUARTERS = TextKey.of("jsspace.ship_part.quarters", "Quarters");
    public static final TextKey COMPUTING = TextKey.of("jsspace.ship_part.computing", "Computing");
    public static final TextKey SMALL_CARGO = TextKey.of("jsspace.ship_part.small_cargo", "Small Cargo");
    /* The room a small module takes in the hull: three fit a small ship. */
    private static final int SMALL = 1;
    /* A small module's length along the spine. */
    private static final int MODULE_LENGTH = 2;
    /* The room a medium module takes in the hull: all of a small ship's. */
    private static final int MEDIUM = 3;
    private static final double KILO = 1_000;
    private static final double MEGA = 1_000_000;
    /* The parts' ids in the catalogue's order, as they are declared. */
    private static final List<ResourceLocation> ORDER = new ArrayList<>();

    private SmallShipParts() {
    }

    /**
     * Declares the parts, once, from the mod's constructor: each of the maker of its family in the catalogue, the
     * orbiter line Meridian Orbital's, the working line Ironside Yards', the enamel line Kosmozavod's.
     */
    public static void declare() {
        final ResourceLocation meridian = SmallShipMakers.MERIDIAN_ORBITAL;
        final ResourceLocation ironside = SmallShipMakers.IRONSIDE_YARDS;
        final ResourceLocation kosmozavod = SmallShipMakers.KOSMOZAVOD_ID;
        // Each part's figures: tonnes, kilonewtons pushed and lifted, megawatts made (or taken), seats and beds.
        spine("small_orbiter_cockpit", ORBITER_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false, meridian,
                figures(9.0, 0, 0, -2.0, 1, 0));
        spine("small_cab_cockpit", CAB_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false, ironside,
                figures(8.0, 0, 0, -1.6, 1, 0));
        spine("small_greenhouse_cockpit", GREENHOUSE_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false,
                kosmozavod, figures(10.0, 0, 0, -2.4, 1, 0));
        spine("small_airlock", AIRLOCK, ShipPartKind.HULL, ShipPlace.SPINE, 3, 0, true, meridian,
                figures(4.0, 0, 0, -0.4, 0, 0));
        spine("small_docking", DOCKING, ShipPartKind.HULL, ShipPlace.SPINE, 3, 0, true, kosmozavod,
                figures(4.5, 0, 0, -0.5, 0, 0));
        spine("small_corridor", CORRIDOR, ShipPartKind.HULL, ShipPlace.SPINE, 2, 0, false, ironside,
                figures(1.6, 0, 0, -0.1, 0, 0));
        spine("small_bay", BAY, ShipPartKind.HOLD, ShipPlace.SPINE, 5, MEDIUM, false, meridian,
                figures(6.0, 0, 0, -0.3, 0, 0));
        spine("small_ramp", RAMP, ShipPartKind.HOLD, ShipPlace.TAIL, 5, MEDIUM, false, ironside,
                figures(6.5, 0, 0, -0.4, 0, 0));
        spine("small_side_door", SIDE_DOOR, ShipPartKind.HOLD, ShipPlace.SPINE, 5, MEDIUM, false, kosmozavod,
                figures(6.0, 0, 0, -0.3, 0, 0));
        spine("small_reactor_flush", REACTOR_FLUSH, ShipPartKind.REACTOR, ShipPlace.SPINE, 2, 0, false, meridian,
                figures(9.6, 0, 0, 24.0, 0, 0));
        spine("small_reactor_fins", REACTOR_FINS, ShipPartKind.REACTOR, ShipPlace.SPINE, 2, 0, false, ironside,
                figures(10.4, 0, 0, 30.0, 0, 0));
        hung("small_twin_bells", TWIN_BELLS, ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, false, meridian,
                figures(5.0, 420, 0, -1.0, 0, 0));
        hung("small_cluster", CLUSTER, ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, false, kosmozavod,
                figures(6.0, 520, 0, -1.2, 0, 0));
        hung("small_magnetic_nozzle", MAGNETIC_NOZZLE, ShipPartKind.MAIN_ENGINE, ShipPlace.ON_WINGS, false,
                ironside, figures(3.5, 360, 0, -9.0, 0, 0));
        hung("small_side_intakes", SIDE_INTAKES, ShipPartKind.AIR_ENGINE, ShipPlace.SIDES, false, meridian,
                figures(3.0, 0, 420, -12.8, 0, 0));
        hung("small_nacelle_intake", NACELLE_INTAKE, ShipPartKind.AIR_ENGINE, ShipPlace.ON_WINGS, false, ironside,
                figures(3.6, 0, 480, -14.0, 0, 0));
        // The wings: tonnes, and kilonewtons held up at the wing's speed in the air at sea level.
        hung("small_blended_delta", BLENDED_DELTA, ShipPartKind.WINGS, ShipPlace.WING_ROOT, false, meridian,
                wings(3.5, 400));
        hung("small_stub_wings", STUB_WINGS, ShipPartKind.WINGS, ShipPlace.WING_ROOT, true, ironside,
                wings(2.0, 260));
        hung("small_swept_wings", SWEPT_WINGS, ShipPartKind.WINGS, ShipPlace.WING_ROOT, false, kosmozavod,
                wings(3.0, 360));
        hung("small_twin_canted_fins", TWIN_CANTED_FINS, ShipPartKind.FINS, ShipPlace.SPINE_END, false, meridian,
                figures(0.8, 0, 0, -0.2, 0, 0));
        hung("small_single_fin", SINGLE_FIN, ShipPartKind.FINS, ShipPlace.SPINE_END, false, kosmozavod,
                figures(0.5, 0, 0, -0.1, 0, 0));
        hung("small_blisters", BLISTERS, ShipPartKind.THRUSTERS, ShipPlace.AROUND, false, meridian,
                figures(0.6, 0, 0, -1.4, 0, 0));
        hung("small_quads", QUADS, ShipPartKind.THRUSTERS, ShipPlace.AROUND, false, ironside,
                figures(0.5, 0, 0, -1.2, 0, 0));
        // The wheels roll, on the land and a runway; the legs' feet stand where they come down.
        ShipParts.declare(id("small_wheels"), new ShipPart(WHEELS.key(), ShipPartKind.GEAR, ShipClass.SMALL,
                ShipPlace.UNDER, 0, 0, false, false, Optional.of(meridian), figures(1.5, 0, 0, 0, 0, 0), true));
        ORDER.add(id("small_wheels"));
        hung("small_legs", LEGS, ShipPartKind.GEAR, ShipPlace.UNDER, false, ironside, figures(1.2, 0, 0, 0, 0, 0));
        // The small functional modules, each a section of the spine taking a third of the small hull's room.
        module("small_workshop", WORKSHOP, ironside, figures(1.4, 0, 0, -0.6, 0, 0));
        module("small_lab", LAB, meridian, figures(1.4, 0, 0, -0.8, 0, 0));
        module("small_quarters", QUARTERS, kosmozavod, figures(1.2, 0, 0, -0.2, 0, 1));
        module("small_computing", COMPUTING, kosmozavod, figures(1.5, 0, 0, -1.2, 0, 0));
        module("small_small_cargo", SMALL_CARGO, ironside, figures(1.0, 0, 0, 0, 0, 0));
    }

    /** The id of the catalogue's part {@code path}. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    /** The catalogue's parts in its own order, as a builder shows them. */
    public static List<ResourceLocation> order() {
        return List.copyOf(ORDER);
    }

    private static void module(final String path, final TextKey name, final ResourceLocation maker,
                               final ShipPart.Figures figures) {
        spine(path, name, ShipPartKind.MODULE, ShipPlace.SPINE, MODULE_LENGTH, SMALL, false, maker, figures);
    }

    private static void spine(final String path, final TextKey name, final ShipPartKind kind, final ShipPlace place,
                              final int length, final int room, final boolean airlock, final ResourceLocation maker,
                              final ShipPart.Figures figures) {
        ShipParts.declare(id(path), new ShipPart(name.key(), kind, ShipClass.SMALL, place, length, room, airlock,
                false, Optional.of(maker), figures));
        ORDER.add(id(path));
    }

    private static void hung(final String path, final TextKey name, final ShipPartKind kind, final ShipPlace place,
                             final boolean carries, final ResourceLocation maker, final ShipPart.Figures figures) {
        ShipParts.declare(id(path), new ShipPart(name.key(), kind, ShipClass.SMALL, place, 0, 0, false, carries,
                Optional.of(maker), figures));
        ORDER.add(id(path));
    }

    /* A part's figures in the catalogue's units: tonnes, kilonewtons and megawatts. */
    private static ShipPart.Figures figures(final double tonnes, final double thrustKilonewtons,
                                            final double liftKilonewtons, final double megawatts, final int seats,
                                            final int beds) {
        return new ShipPart.Figures(tonnes * KILO, thrustKilonewtons * KILO, liftKilonewtons * KILO,
                megawatts * MEGA, seats, beds);
    }

    /* A pair of wings' figures in the catalogue's units: tonnes, and kilonewtons held up at the wing's speed. */
    private static ShipPart.Figures wings(final double tonnes, final double heldKilonewtons) {
        return new ShipPart.Figures(tonnes * KILO, 0, 0, 0, 0, 0, heldKilonewtons * KILO);
    }
}
