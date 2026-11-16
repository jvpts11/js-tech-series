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

/** What a ship's HUD reads, projected on its canopy for its pilot: short, as a HUD's labels are. */
@TextHolder
public final class ShipHudTexts {

    public static final TextKey NORTH = TextKey.of("jsspace.ship_hud.north", "N");
    public static final TextKey EAST = TextKey.of("jsspace.ship_hud.east", "E");
    public static final TextKey SOUTH = TextKey.of("jsspace.ship_hud.south", "S");
    public static final TextKey WEST = TextKey.of("jsspace.ship_hud.west", "W");
    public static final TextKey SPEED = TextKey.of("jsspace.ship_hud.speed", "SPD M/S");
    public static final TextKey BOOST = TextKey.of("jsspace.ship_hud.boost", "BOOST");
    public static final TextKey TURN_SLOW = TextKey.of("jsspace.ship_hud.turn_slow", "TURN SLOW");
    public static final TextKey ALTITUDE = TextKey.of("jsspace.ship_hud.altitude", "ALT");
    public static final TextKey CLIMB = TextKey.of("jsspace.ship_hud.climb", "VS %s");
    public static final TextKey POWER = TextKey.of("jsspace.ship_hud.power", "PWR %s/%s MW");
    public static final TextKey FUEL = TextKey.of("jsspace.ship_hud.fuel", "D2 %s%%");
    public static final TextKey NOZZLES = TextKey.of("jsspace.ship_hud.nozzles", "NOZZLES %s");
    public static final TextKey GEAR_UP = TextKey.of("jsspace.ship_hud.gear_up", "GEAR UP");
    public static final TextKey GEAR_DOWN = TextKey.of("jsspace.ship_hud.gear_down", "GEAR DOWN");
    public static final TextKey AUTOPILOT = TextKey.of("jsspace.ship_hud.autopilot", "AP");
    public static final TextKey LAND = TextKey.of("jsspace.ship_hud.land", "LAND");
    public static final TextKey ORBIT = TextKey.of("jsspace.ship_hud.orbit", "ORBIT");
    public static final TextKey DOCK = TextKey.of("jsspace.ship_hud.dock", "DOCK");
    public static final TextKey PAD = TextKey.of("jsspace.ship_hud.pad", "PAD %s M");
    public static final TextKey RANGE = TextKey.of("jsspace.ship_hud.range", "RANGE %s");
    public static final TextKey CLOSE = TextKey.of("jsspace.ship_hud.close", "CLOSE %s M/S");
    public static final TextKey ALIGNED = TextKey.of("jsspace.ship_hud.aligned", "ALIGNED");
    public static final TextKey APOGEE = TextKey.of("jsspace.ship_hud.apogee", "APO %s  %s");
    public static final TextKey PULSE = TextKey.of("jsspace.ship_hud.pulse", "PULSE");
    public static final TextKey PULSE_SPOOL = TextKey.of("jsspace.ship_hud.pulse_spool", "PULSE %s S");
    public static final TextKey PULSE_HOLD = TextKey.of("jsspace.ship_hud.pulse_hold", "HOLD %s");
    public static final TextKey PULSE_EXIT = TextKey.of("jsspace.ship_hud.pulse_exit", "%s TO EXIT");
    public static final TextKey PULSE_FUEL = TextKey.of("jsspace.ship_hud.pulse_fuel", "D2 %s%%  X%s");
    public static final TextKey PULSE_WORLD = TextKey.of("jsspace.ship_hud.pulse_world", "%s %s  %s");
    public static final TextKey PULSE_OFF = TextKey.of("jsspace.ship_hud.pulse_off", "PULSE OFF");
    public static final TextKey PULSE_NEAR = TextKey.of("jsspace.ship_hud.pulse_near", "NEAR %s");
    public static final TextKey PULSE_SUN = TextKey.of("jsspace.ship_hud.pulse_sun", "NEAR THE SUN");
    public static final TextKey PULSE_SHIP = TextKey.of("jsspace.ship_hud.pulse_ship", "SHIP NEAR");
    public static final TextKey PULSE_DOCKED = TextKey.of("jsspace.ship_hud.pulse_docked", "DOCKED: UNDOCK FIRST");
    public static final TextKey PULSE_SPENT = TextKey.of("jsspace.ship_hud.pulse_spent", "D2 SPENT");
    public static final TextKey OVERWORLD = TextKey.of("jsspace.ship_hud.overworld", "OVERWORLD");
    public static final TextKey MOON = TextKey.of("jsspace.ship_hud.moon", "MOON");
    public static final TextKey MARS = TextKey.of("jsspace.ship_hud.mars", "MARS");

    private ShipHudTexts() {
    }
}
