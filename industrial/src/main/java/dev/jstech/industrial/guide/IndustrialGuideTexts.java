/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words many drawings of J's Industrial share, each declared once and translated once: where the machines come
 * from, the plan every machine is set up by, and what the balloons on every machine's views point at.
 */
@TextHolder
public final class IndustrialGuideTexts {

    public static final TextKey FROM_THE_TAB = TextKey.of("jsindustrial.manual.from_the_tab",
            "Creative menu, J's Industrial tab. No crafting recipe yet.");
    public static final TextKey SET_UP_NEXT = TextKey.of("jsindustrial.manual.set_up_next",
            "How to set it up follows.");
    public static final TextKey SET_UP_PLAN = TextKey.of("jsindustrial.manual.set_up_plan",
            "Setting it up (seen from above)");
    public static final TextKey GENERATOR = TextKey.of("jsindustrial.manual.generator", "Generator");
    public static final TextKey SECOND_GENERATOR = TextKey.of("jsindustrial.manual.second_generator",
            "second generator (optional)");
    public static final TextKey THE_FRONT = TextKey.of("jsindustrial.manual.the_front",
            "The front: the side you open it from.");
    public static final TextKey ANY_SIDE = TextKey.of("jsindustrial.manual.any_side",
            "Power and items go in on any side.");

    private IndustrialGuideTexts() {
    }
}
