/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Atmosphere Detector's screen. */
@TextHolder
public final class AtmosphereTexts {

    public static final TextKey PLATE = TextKey.of("jscore.atmosphere.plate", "ATMOSPHERE DETECTOR");
    public static final TextKey THE_AIR = TextKey.of("jscore.atmosphere.the_air", "THE AIR");
    public static final TextKey THE_ROOM = TextKey.of("jscore.atmosphere.the_room", "THE ROOM");
    public static final TextKey KILOPASCALS = TextKey.of("jscore.atmosphere.kilopascals", "%s kPa");
    public static final TextKey UNIT = TextKey.of("jscore.atmosphere.unit", "kPa");
    public static final TextKey FALLING = TextKey.of("jscore.atmosphere.falling", "FALLING %s kPa/s");
    public static final TextKey RISING = TextKey.of("jscore.atmosphere.rising", "RISING %s kPa/s");
    public static final TextKey STEADY = TextKey.of("jscore.atmosphere.steady", "STEADY");
    public static final TextKey BREATHABLE = TextKey.of("jscore.atmosphere.breathable", "BREATHABLE");
    public static final TextKey HYPOXIA = TextKey.of("jscore.atmosphere.hypoxia", "HYPOXIA");
    public static final TextKey LEAKING = TextKey.of("jscore.atmosphere.leaking", "LEAKING");
    public static final TextKey VACUUM = TextKey.of("jscore.atmosphere.vacuum", "VACUUM");
    public static final TextKey SHOW = TextKey.of("jscore.atmosphere.show", "SHOW");
    public static final TextKey IN_THE_WORLD = TextKey.of("jscore.atmosphere.in_the_world", "IN THE WORLD");
    public static final TextKey HYPOXIA_LINE = TextKey.of("jscore.atmosphere.hypoxia_line",
            "O2 under 16 kPa: HYPOXIA");
    public static final TextKey FINE_LINE = TextKey.of("jscore.atmosphere.fine_line", "O2 over 16 kPa: FINE");
    public static final TextKey VACUUM_LINE = TextKey.of("jscore.atmosphere.vacuum_line", "No air: VACUUM");
    public static final TextKey STALE_LINE = TextKey.of("jscore.atmosphere.stale_line", "CO2 over 1%: HEADACHE");
    public static final TextKey TOXIC_LINE = TextKey.of("jscore.atmosphere.toxic_line", "O2 over 50 kPa: TOXIC");
    public static final TextKey ROOM_LINE = TextKey.of("jscore.atmosphere.room_line", "%s m3, %s leaks");
    public static final TextKey SEALED_LINE = TextKey.of("jscore.atmosphere.sealed_line", "%s m3, sealed");
    public static final TextKey OPEN_AIR = TextKey.of("jscore.atmosphere.open_air", "OPEN AIR");
    public static final TextKey SHOW_TIP = TextKey.of("jscore.atmosphere.show_tip", "Show the room in the world");
    public static final TextKey HIDE_TIP = TextKey.of("jscore.atmosphere.hide_tip", "Stop showing the room");

    private AtmosphereTexts() {
    }
}
