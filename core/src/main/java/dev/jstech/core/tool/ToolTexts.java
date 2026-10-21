/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a building tool tells its player on the action bar, what a tool's tooltip says of its copy, and what the ring
 * of modes and the HUD write of the item in hand.
 */
@TextHolder
public final class ToolTexts {

    /** The item's name and its mode, above the hotbar. */
    public static final TextKey NAMED_MODE = TextKey.of("jscore.tool.named_mode", "%s: %s");
    public static final TextKey RELEASE_TO_CHOOSE = TextKey.of("jscore.tool.release_to_choose",
            "release the key to choose %s");
    /** The small line under the item's name: what it works on, how much it holds of how much. */
    public static final TextKey HUD_ENERGY = TextKey.of("jscore.tool.hud.energy", "energy %s / %s FE");
    public static final TextKey HUD_FUEL = TextKey.of("jscore.tool.hud.fuel", "fuel %s / %s mB");
    public static final TextKey HUD_FLUID = TextKey.of("jscore.tool.hud.fluid", "fluid %s / %s mB");
    public static final TextKey HUD_GAS = TextKey.of("jscore.tool.hud.gas", "gas %s / %s mB");

    public static final TextKey CORNER = TextKey.of("jscore.tool.corner", "First corner marked; use on the second");
    public static final TextKey COPIED = TextKey.of("jscore.tool.copied", "Copied %s blocks");
    public static final TextKey TOO_BIG = TextKey.of("jscore.tool.too_big",
            "A copy holds at most %s blocks; this region has %s");
    public static final TextKey OTHER_WORLD = TextKey.of("jscore.tool.other_world",
            "The first corner is in another world; marked this one instead");
    public static final TextKey CLEARED = TextKey.of("jscore.tool.cleared", "Copy cleared");
    public static final TextKey NOTHING_COPIED = TextKey.of("jscore.tool.nothing_copied",
            "Nothing copied yet: mark two corners in copy mode");
    public static final TextKey OUT_OF = TextKey.of("jscore.tool.out_of", "Out of %s");
    public static final TextKey HOLDS_COPY = TextKey.of("jscore.tool.holds_copy",
            "Holds a copy of %s blocks, %s by %s by %s");

    private ToolTexts() {
    }
}
