/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a player reads of what an item holds: its mode, whether it is on, its energy, its fluid, its gas and its
 * stacks, and the key that changes its mode.
 */
@TextHolder
public final class ItemTexts {

    public static final TextKey MODE = TextKey.of("jscore.item.mode", "Mode: %s");
    public static final TextKey MODE_KEY = TextKey.of("key.jscore.item_mode", "Change Item Mode");
    public static final TextKey MODE_HINT = TextKey.of("jscore.item.mode.hint",
            "%s changes the mode while held in the main hand, as the mouse wheel does while sneaking");
    public static final TextKey ON = TextKey.of("jscore.item.on", "On");
    public static final TextKey OFF = TextKey.of("jscore.item.off", "Off");
    /** An item turned on or off, said on the action bar: its name, then on or off. */
    public static final TextKey SWITCHED = TextKey.of("jscore.item.switched", "%s: %s");
    public static final TextKey GAS = TextKey.of("jscore.item.gas", "%s: %s of %s mB");
    public static final TextKey GAS_EMPTY = TextKey.of("jscore.item.gas.empty", "No gas, holds %s mB");
    public static final TextKey MODE_NO_KEY = TextKey.of("jscore.item.mode.no_key",
            "Turn the mouse wheel while sneaking to change the mode, or give Change Item Mode a key");
    public static final TextKey ENERGY = TextKey.of("jscore.item.energy", "Energy: %s of %s");
    public static final TextKey FLUID = TextKey.of("jscore.item.fluid", "%s: %s of %s mB");
    public static final TextKey FLUID_EMPTY = TextKey.of("jscore.item.fluid.empty", "No fluid, holds %s mB");
    public static final TextKey ITEMS = TextKey.of("jscore.item.items", "Holds %s of %s stacks");

    private ItemTexts() {
    }
}
