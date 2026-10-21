/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What players read of the vehicles: the seats' roles, the kinds of parts, the keys' names, and what they are told. */
@TextHolder
public final class VehicleTexts {

    public static final TextKey DRIVER = TextKey.of("jscore.vehicle.role.driver", "Driver");
    public static final TextKey PASSENGER = TextKey.of("jscore.vehicle.role.passenger", "Passenger");

    public static final TextKey ENGINE = TextKey.of("jscore.vehicle.part_kind.engine", "Engine");
    public static final TextKey WHEEL = TextKey.of("jscore.vehicle.part_kind.wheel", "Wheel");
    public static final TextKey TANK = TextKey.of("jscore.vehicle.part_kind.tank", "Fuel tank");
    public static final TextKey BATTERY = TextKey.of("jscore.vehicle.part_kind.battery", "Battery");
    public static final TextKey GEARBOX = TextKey.of("jscore.vehicle.part_kind.gearbox", "Gearbox");
    public static final TextKey LIGHT = TextKey.of("jscore.vehicle.part_kind.light", "Light");
    public static final TextKey HORN = TextKey.of("jscore.vehicle.part_kind.horn", "Horn");
    public static final TextKey ROTOR = TextKey.of("jscore.vehicle.part_kind.rotor", "Rotor");
    public static final TextKey PROPELLER = TextKey.of("jscore.vehicle.part_kind.propeller", "Propeller");

    public static final TextKey NOT_YOURS = TextKey.of("jscore.vehicle.not_yours",
            "You may not drive this vehicle: it belongs to %s");
    public static final TextKey LOCKED = TextKey.of("jscore.vehicle.locked", "It is locked");
    public static final TextKey LOCKED_NOW = TextKey.of("jscore.vehicle.locked_now", "Locked");
    public static final TextKey UNLOCKED_NOW = TextKey.of("jscore.vehicle.unlocked_now", "Unlocked");
    public static final TextKey WRONG_KEY = TextKey.of("jscore.vehicle.wrong_key", "This key is not this vehicle's");
    public static final TextKey NO_SEAT = TextKey.of("jscore.vehicle.no_seat", "Every seat is taken");
    public static final TextKey NO_PLACE = TextKey.of("jscore.vehicle.no_place",
            "This vehicle has no empty place for this part");
    public static final TextKey FITTED = TextKey.of("jscore.vehicle.fitted", "Fitted: %s");
    public static final TextKey TAKEN_OUT = TextKey.of("jscore.vehicle.taken_out", "Taken out: %s");
    public static final TextKey MENDED = TextKey.of("jscore.vehicle.mended", "%s mended, now at %s");
    public static final TextKey NOTHING_TO_MEND = TextKey.of("jscore.vehicle.nothing_to_mend",
            "None of its parts this mends is worn");
    public static final TextKey PAINTED = TextKey.of("jscore.vehicle.painted", "Painted: %s");
    public static final TextKey NO_PAINT = TextKey.of("jscore.vehicle.no_paint",
            "This vehicle has no paint of that colour");
    public static final TextKey HITCHED = TextKey.of("jscore.vehicle.hitched", "Hitched");
    public static final TextKey UNHITCHED = TextKey.of("jscore.vehicle.unhitched", "Unhitched");
    public static final TextKey NOTHING_TO_HITCH = TextKey.of("jscore.vehicle.nothing_to_hitch",
            "No vehicle's tow point is near enough to hitch to");

    public static final TextKey KEY_OF = TextKey.of("jscore.vehicle.key_of", "Opens and starts: %s");
    public static final TextKey KEY_BLANK = TextKey.of("jscore.vehicle.key_blank", "Cut for no vehicle yet");
    public static final TextKey OWNER = TextKey.of("jscore.vehicle.owner", "Owner: %s");
    public static final TextKey CARRIES_STATE = TextKey.of("jscore.vehicle.carries_state",
            "Its parts, fuel and load go with it");

    public static final TextKey LIGHTS_KEY = TextKey.of("jscore.key.vehicle_lights", "Vehicle: lights");
    public static final TextKey HORN_KEY = TextKey.of("jscore.key.vehicle_horn", "Vehicle: horn");
    public static final TextKey SEAT_KEY = TextKey.of("jscore.key.vehicle_seat", "Vehicle: next seat");
    public static final TextKey IGNITION_KEY = TextKey.of("jscore.key.vehicle_ignition", "Vehicle: ignition");
    public static final TextKey SHIFT_UP_KEY = TextKey.of("jscore.key.vehicle_shift_up", "Vehicle: shift up");
    public static final TextKey SHIFT_DOWN_KEY = TextKey.of("jscore.key.vehicle_shift_down", "Vehicle: shift down");
    public static final TextKey GEARBOX_KEY = TextKey.of("jscore.key.vehicle_gearbox",
            "Vehicle: automatic or manual gearbox");
    public static final TextKey INDICATE_LEFT_KEY = TextKey.of("jscore.key.vehicle_indicate_left",
            "Vehicle: indicate left");
    public static final TextKey INDICATE_RIGHT_KEY = TextKey.of("jscore.key.vehicle_indicate_right",
            "Vehicle: indicate right");

    public static final TextKey RUN_OVER = TextKey.of("death.attack.jscore.run_over", "%1$s was run over");
    public static final TextKey RUN_OVER_BY = TextKey.of("death.attack.jscore.run_over.player",
            "%1$s was run over by %2$s");

    private VehicleTexts() {
    }
}
