/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the HUD editor says, and the names of the Core's own HUD elements. Another mod's element is named by the
 * translation key {@code hud.<namespace>.<path>} of its id.
 */
@TextHolder
public final class HudTexts {

    public static final TextKey EDIT = TextKey.of("gui.jscore.hud_editor", "Edit the HUD");
    public static final TextKey HOW = TextKey.of("gui.jscore.hud_editor.how",
            "Drag a box to move it; its eye hides it.");
    public static final TextKey SNAPS = TextKey.of("gui.jscore.hud_editor.snaps",
            "Boxes snap to the grid and to each other.");
    public static final TextKey RESET = TextKey.of("gui.jscore.hud_editor.reset", "Reset all");
    /** A box's label: the element's mod, then the element. */
    public static final TextKey OWNED = TextKey.of("gui.jscore.hud_editor.owned", "%s: %s");
    /** What the setting says it holds, in the settings screen. */
    public static final TextKey AS_DECLARED = TextKey.of("gui.jscore.hud_editor.as_declared",
            "Where each mod puts them");
    public static final TextKey PLACED = TextKey.of("gui.jscore.hud_editor.placed", "%s placed by you");
    /** The Core's own elements. */
    public static final TextKey NOTIFICATIONS = TextKey.of("hud.jscore.notifications", "Notifications");
    public static final TextKey GEIGER_COUNTER = TextKey.of("hud.jscore.geiger_counter", "Geiger counter");
    public static final TextKey POCKET_DOSIMETER = TextKey.of("hud.jscore.pocket_dosimeter", "Pocket dosimeter");
    public static final TextKey HAZARD_MAP = TextKey.of("hud.jscore.hazard_map", "Hazard map");
    public static final TextKey BODY_TEMPERATURE = TextKey.of("hud.jscore.body_temperature", "Body temperature");

    private HudTexts() {
    }
}
