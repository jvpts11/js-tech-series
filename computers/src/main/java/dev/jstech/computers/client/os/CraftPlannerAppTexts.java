/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The Craft Planner window's own title, apart from what {@link CraftPlannerTexts} covers. */
@TextHolder
final class CraftPlannerAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.craft_planner_app.title", "Craft Planner");

    private CraftPlannerAppTexts() {
    }
}
