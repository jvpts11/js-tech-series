/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/**
 * What a power supply's wear is called wherever it shows: its tooltip, the assembly screens, the POST of each
 * firmware, the Device Manager and the network's event log. A redundant supply's units are named by letter, A first.
 */
@TextHolder
@ApiStatus.Experimental
public final class PsuTexts {

    public static final TextKey WEAR = TextKey.of("jsc.psu.wear", "Wear: %s%%");
    public static final TextKey DEGRADED = TextKey.of("jsc.psu.degraded", "Degraded: %s%% worn");
    public static final TextKey FAILED = TextKey.of("jsc.psu.failed", "Failed");
    public static final TextKey UNITS_FAILED = TextKey.of("jsc.psu.units_failed", "%s of %s units failed");
    public static final TextKey UNIT_FAILED = TextKey.of("jsc.psu.unit_failed", "Unit %s: failed");
    public static final TextKey UNIT_CARRIES = TextKey.of("jsc.psu.unit_carries",
            "Unit %s: %s%% worn, carries the load");
    public static final TextKey UNIT_WORN = TextKey.of("jsc.psu.unit_worn", "Unit %s: %s%% worn");
    public static final TextKey PACE_LIGHT = TextKey.of("jsc.psu.pace_light", "Load %s%%: wears 8x slower");
    public static final TextKey PACE_MEDIUM = TextKey.of("jsc.psu.pace_medium", "Load %s%%: wears 3x slower");
    public static final TextKey PACE_FULL = TextKey.of("jsc.psu.pace_full", "Load %s%%: wears at its full pace");
    public static final TextKey REPLACE_SOON = TextKey.of("jsc.psu.replace_soon", "Replace it before it fails");
    public static final TextKey PSU_FAILURE = TextKey.of("jsc.psu.psu_failure", "PSU failure");
    public static final TextKey SUPPLY_FAILED = TextKey.of("jsc.psu.supply_failed", "%s failed");
    public static final TextKey REPLACE = TextKey.of("jsc.psu.replace", "Replace the power supply");
    /** The short state the Device Manager writes beside the supply's name. */
    public static final TextKey NOTE_DEGRADED = TextKey.of("jsc.psu.note_degraded", "degraded, %s%% worn");
    public static final TextKey NOTE_UNITS = TextKey.of("jsc.psu.note_units", "%s of %s units failed");
    /** The network's event log. */
    public static final TextKey LOG_DEGRADED = TextKey.of("jsc.event_log.psu_degraded",
            "Power supply degraded: %s, %s%% worn");
    public static final TextKey LOG_UNIT_FAILED = TextKey.of("jsc.event_log.psu_unit_failed",
            "Redundant power supply: %s of %s units of %s failed");
    public static final TextKey LOG_FAILED = TextKey.of("jsc.event_log.psu_failed",
            "Power supply failed: %s; the machine went dark");

    private PsuTexts() {
    }

    /** {@code wear} as a whole percent, what every line shows. */
    public static int percent(final float wear) {
        return Math.round(Math.max(0f, Math.min(1f, wear)) * 100f);
    }

    /** The letter unit {@code unit} is named by, A for the first. */
    public static String letter(final int unit) {
        return String.valueOf((char) ('A' + Math.max(0, Math.min(25, unit))));
    }

    /** The state lines of a supply of {@code spec} in {@code condition}: its wear, each unit's for a redundant one. */
    public static List<Text> state(final PsuSpec spec, final PsuCondition condition) {
        final List<Text> lines = new ArrayList<>();
        if (condition.failed(spec)) {
            lines.add(FAILED.text());
            return lines;
        }
        final int failed = condition.failedUnits(spec);
        if (spec.redundant() && failed > 0) {
            lines.add(UNITS_FAILED.with(failed, spec.units()));
            for (int unit = 0; unit < spec.units(); unit++) {
                final float wear = condition.wearOf(unit);
                lines.add(wear >= 1f ? UNIT_FAILED.with(letter(unit))
                        : UNIT_CARRIES.with(letter(unit), percent(wear)));
            }
            return lines;
        }
        final float worst = condition.worst(spec);
        lines.add(worst >= PsuWear.DEGRADED ? DEGRADED.with(percent(worst)) : WEAR.with(percent(worst)));
        if (spec.redundant()) {
            for (int unit = 0; unit < spec.units(); unit++) {
                lines.add(UNIT_WORN.with(letter(unit), percent(condition.wearOf(unit))));
            }
        }
        if (worst >= PsuWear.DEGRADED) {
            lines.add(REPLACE_SOON.text());
        }
        return lines;
    }

    /** How fast it wears at {@code load}, its parts' watts over its rating, for each working unit. */
    public static Text pace(final double load) {
        final int shown = (int) Math.round(load * 100.0);
        return switch (PsuWear.lifeTimes(load)) {
            case 1 -> PACE_FULL.with(shown);
            case 3 -> PACE_MEDIUM.with(shown);
            default -> PACE_LIGHT.with(shown);
        };
    }

    /** The short note the Device Manager writes beside the supply, or nothing for one in good order. */
    public static Text note(final PsuSpec spec, final PsuCondition condition) {
        if (condition.failed(spec)) {
            return FAILED.text();
        }
        if (spec.redundant() && condition.failedUnits(spec) > 0) {
            return NOTE_UNITS.with(condition.failedUnits(spec), spec.units());
        }
        return condition.degraded(spec) ? NOTE_DEGRADED.with(percent(condition.worst(spec))) : Text.EMPTY;
    }
}
