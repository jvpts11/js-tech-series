/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What J's Industrial's grid equipment says of itself to whoever looks at it. */
@TextHolder
public final class IndustrialEnergyTexts {

    /** The two classes it joins. */
    public static final TextKey JOINS = TextKey.of("jsindustrial.joiner.joins", "Joins %s and %s");
    /** The most it carries. */
    public static final TextKey CEILING = TextKey.of("jsindustrial.joiner.ceiling", "Carries up to %s");
    /** What crossed it last tick, and which way: a transformer steps up or down, a station rectifies or inverts. */
    public static final TextKey STEPPING_UP = TextKey.of("jsindustrial.joiner.stepping_up", "Stepping up %s");
    public static final TextKey STEPPING_DOWN = TextKey.of("jsindustrial.joiner.stepping_down", "Stepping down %s");
    public static final TextKey RECTIFYING = TextKey.of("jsindustrial.joiner.rectifying", "Rectifying %s");
    public static final TextKey INVERTING = TextKey.of("jsindustrial.joiner.inverting", "Inverting %s");
    public static final TextKey IDLE = TextKey.of("jsindustrial.joiner.idle", "Carrying nothing");

    private IndustrialEnergyTexts() {
    }
}
