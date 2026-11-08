/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.power;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a UPS is called on where it stands and what it holds, wherever it is read: its block, a rack's screen and the
 * battery a system shows.
 *
 * <p>Pure: no game types.
 */
@TextHolder
public final class UpsTexts {

    /** Where the power comes from. */
    public static final TextKey ON_LINE = TextKey.of("jsc.ups.on_line", "On line");
    public static final TextKey ON_BATTERY = TextKey.of("jsc.ups.on_battery", "On battery");
    public static final TextKey BYPASS = TextKey.of("jsc.ups.bypass", "Bypass");
    /** Why a UPS in bypass protects nothing. */
    public static final TextKey BYPASS_WHY = TextKey.of("jsc.ups.bypass_why",
            "The load is more than it carries: it runs straight from the line, unprotected");
    /** How full the battery is, and how long it lasts at the load. */
    public static final TextKey BATTERY = TextKey.of("jsc.ups.battery", "Battery: %s%%");
    public static final TextKey BATTERY_LASTS = TextKey.of("jsc.ups.battery_lasts", "Battery: %s%%, about %s min");
    /** What a UPS holds and the most it carries. */
    public static final TextKey HOLDS = TextKey.of("jsc.ups.holds", "Holds %s, carries %s");
    /** The battery under a system's notice: how full, how long it lasts, and when the system shuts itself down. */
    public static final TextKey PERCENT = TextKey.of("jsc.ups.percent", "%s%%");
    public static final TextKey MINUTES_LEFT = TextKey.of("jsc.ups.minutes_left", "about %s min left");
    public static final TextKey PERCENT_MINUTES_LEFT = TextKey.of("jsc.ups.percent_minutes_left",
            "%s%%, about %s min left");
    public static final TextKey SHUTS_DOWN = TextKey.of("jsc.ups.shuts_down", "Shuts down at 10%%");
    /** The battery in a system's notification area, under the cursor: where the power comes from, and how full. */
    public static final TextKey TIP = TextKey.of("jsc.ups.tip", "%s: %s%%");

    private UpsTexts() {
    }

    /** Where the power comes from, as {@code mode} says it; nothing without a UPS. */
    public static Text mode(final UpsReading.Mode mode) {
        return switch (mode) {
            case NONE -> Text.EMPTY;
            case ON_LINE -> ON_LINE.text();
            case ON_BATTERY -> ON_BATTERY.text();
            case BYPASS -> BYPASS.text();
        };
    }

    /** How full the battery of {@code reading} is, and how long it lasts while it carries a load. */
    public static Text battery(final UpsReading reading) {
        return reading.minutesLeft() > 0L ? BATTERY_LASTS.with(reading.percent(), reading.minutesLeft())
                : BATTERY.with(reading.percent());
    }
}
