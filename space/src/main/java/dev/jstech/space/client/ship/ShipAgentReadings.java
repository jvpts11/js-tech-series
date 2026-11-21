/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.agent.AgentReadings;
import dev.jstech.core.agent.AgentReport;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAltitude;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Locale;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;

/**
 * What a ship tells an agent playing the game of its flight ({@link AgentReadings}), as its console's HUD shows it:
 * its name; how fast it goes and climbs; its throttle, its boost and its nozzles; whether its air engines or its
 * thrusters hold it up, its gear is down, it stalls or stands on the land; how high it flies; its autopilot; its
 * pulse jump; and the world it flies in, or the one whose low orbit it flies over in a system's space.
 */
public final class ShipAgentReadings {

    /* Tenths of a second, as the pulse's spool is kept. */
    private static final double TENTHS = 10.0;

    private ShipAgentReadings() {
    }

    /** Declares them, once, from the client's setup, where a ship's console has what its HUD shows. */
    @ApiStatus.Internal
    public static void declare() {
        AgentReadings.structure(JsSpace.MODID, ShipAgentReadings::read);
    }

    /* A ship's flight, from the console aboard {@code structure}; nothing of a structure with none. */
    private static void read(final IStructureBody structure, final AgentReport report) {
        final AirReserveEntity console = ShipConsoles.of(structure.plot());
        if (console == null || console.ship().isEmpty()) {
            return;
        }
        final ShipReadings flight = console.flight();
        final AgentReport ship = report.part("ship");
        ship.words("name", console.ship().get().design().name())
                .number("speed_mps", flight.speed())
                .number("climb_mps", flight.climb())
                .whole("throttle_percent", flight.throttle())
                .whole("boost_percent", flight.boost())
                .flag("boosting", flight.boosting())
                .whole("nozzles_degrees", flight.nozzles())
                .flag("lifting", flight.lifting())
                .flag("thrusting", flight.thrusting())
                .flag("gear_down", flight.gearDown())
                .flag("stalled", flight.stalled())
                .flag("grounded", flight.grounded());
        if (flight.altitude() != ShipAltitude.NONE) {
            ship.whole("altitude_m", flight.altitude());
        }
        final ShipReadings.Pilot pilot = flight.pilot();
        final AgentReport autopilot = ship.part("autopilot");
        autopilot.words("mode", pilot.mode().isEmpty() ? "none" : pilot.mode());
        if (!pilot.mode().isEmpty()) {
            autopilot.number("range_m", pilot.range())
                    .number("closing_mps", pilot.closing())
                    .flag("aligned", pilot.aligned())
                    .whole("apogee_m", pilot.apogee())
                    .whole("seconds", pilot.seconds());
        }
        final ShipReadings.Pulse pulse = flight.pulse();
        final AgentReport jump = ship.part("pulse");
        jump.flag("jumping", pulse.on())
                .whole("spool_percent", pulse.spool())
                .number("spool_left_s", pulse.left() / TENTHS);
        if (pulse.on()) {
            jump.words("nearest", pulse.near())
                    .whole("range_blocks", pulse.range())
                    .whole("seconds", pulse.seconds());
        }
        if (!pulse.dropped().isEmpty()) {
            jump.words("dropped", pulse.dropped());
        }
        world(structure).ifPresent(world -> ship.words("world", world));
    }

    /* The world {@code structure} flies in, or the one whose low orbit it flies over in space; none over none. */
    private static Optional<String> world(final IStructureBody structure) {
        final Optional<SolarBody> here = FlightComputerBlockEntity.here(structure.level());
        if (here.isPresent()) {
            return here.map(ShipAgentReadings::named);
        }
        if (!SystemSpaces.systemOf(structure.level().dimension()).filter(StarSystems.SOLAR_SYSTEM::equals)
                .isPresent()) {
            return Optional.empty();
        }
        final StructurePose at = structure.frame().pose();
        return Optional.ofNullable(SystemLayout.over(at.x(), at.z())).map(ShipAgentReadings::named);
    }

    private static String named(final SolarBody body) {
        return body.name().toLowerCase(Locale.ROOT);
    }
}
