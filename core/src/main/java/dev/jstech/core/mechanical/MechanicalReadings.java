/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * Rotation as sensors read it and speak it in redstone: a tachometer's speed, from a standstill to the top of its
 * span, and a torque meter's torque, either way; and the lines that tell a player how fast a part turns, how hard,
 * and how much power its network carries.
 *
 * <pre>{@code
 * public List<Reading> readings() {
 *     return List.of(MechanicalReadings.tachometer(shaft.rpm(), 3_000));
 * }
 * }</pre>
 */
@TextHolder
public final class MechanicalReadings {

    public static final TextKey SPEED = TextKey.of("jscore.mechanical.reading.speed", "Speed");
    public static final TextKey TORQUE = TextKey.of("jscore.mechanical.reading.torque", "Torque");
    public static final TextKey RPM = TextKey.of("jscore.mechanical.reading.rpm", "rpm");
    public static final TextKey NEWTON_METRES = TextKey.of("jscore.mechanical.reading.newton_metres", "N·m");
    public static final TextKey SPEED_LINE = TextKey.of("jscore.mechanical.speed_line", "Speed: %s rpm");
    public static final TextKey TORQUE_LINE = TextKey.of("jscore.mechanical.torque_line", "Torque: %s N·m");
    public static final TextKey POWER_LINE = TextKey.of("jscore.mechanical.power_line", "Network: %s W");
    public static final TextKey JAMMED_LINE = TextKey.of("jscore.mechanical.jammed_line",
            "Jammed: its gears would turn two ways");

    private MechanicalReadings() {
    }

    /** A tachometer's reading of {@code rpm}, either way: no signal at a standstill, the full 15 at {@code most}. */
    public static Reading tachometer(final double rpm, final double most) {
        return new Reading(SPEED, Math.abs(rpm), 0.0, Math.max(1.0, most), RPM);
    }

    /** A torque meter's reading of {@code newtonMetres}, either way: the full 15 at {@code most}. */
    public static Reading torqueMeter(final double newtonMetres, final double most) {
        return new Reading(TORQUE, Math.abs(newtonMetres), 0.0, Math.max(1.0, most), NEWTON_METRES);
    }

    /** The line that tells how fast a part turns. */
    public static Text speedLine(final double rpm) {
        return SPEED_LINE.with(Text.literal(UnitFormatter.forCurrentLocale().decimal(rpm)));
    }

    /** The line that tells how hard a part turns. */
    public static Text torqueLine(final double newtonMetres) {
        return TORQUE_LINE.with(Text.literal(UnitFormatter.forCurrentLocale().decimal(newtonMetres)));
    }

    /** The line that tells how much power a network carries. */
    public static Text powerLine(final double watts) {
        return POWER_LINE.with(Text.literal(UnitFormatter.forCurrentLocale().decimal(watts)));
    }
}
