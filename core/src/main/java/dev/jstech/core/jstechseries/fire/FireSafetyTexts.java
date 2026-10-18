/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the series' fire safety things say: an extinguisher's tooltip, the detectors' readings. */
@TextHolder
public final class FireSafetyTexts {

    public static final TextKey PUTS_OUT = TextKey.of("jscore.extinguisher.puts_out", "Puts out: %s");
    public static final TextKey NEVER_ON = TextKey.of("jscore.extinguisher.never_on", "Never on: %s");
    public static final TextKey EMPTY = TextKey.of("jscore.extinguisher.empty", "Empty");
    public static final TextKey REFILLS = TextKey.of("jscore.extinguisher.refills",
            "Use it on water to fill it again");
    public static final TextKey SMOKE = TextKey.of("jscore.detector.smoke", "Smoke");
    public static final TextKey ALARM = TextKey.of("jscore.detector.alarm", "Alarm");
    public static final TextKey PERCENT = TextKey.of("jscore.detector.percent", "%");
    public static final TextKey LIST = TextKey.of("jscore.extinguisher.list", "%s, %s");

    private FireSafetyTexts() {
    }
}
