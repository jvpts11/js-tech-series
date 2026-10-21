/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core's tooltips say of themselves. */
@TextHolder
public final class TooltipTexts {

    /** The hint at the foot of a tooltip whose sections wait for a key. */
    public static final TextKey MORE = TextKey.of("jscore.tooltip.more", "Hold Shift for more");
    public static final TextKey MORE_NUMBERS = TextKey.of("jscore.tooltip.more_numbers",
            "Hold Ctrl for the numbers");
    public static final TextKey MORE_BOTH = TextKey.of("jscore.tooltip.more_both",
            "Hold Shift for more, Ctrl for the numbers");
    /** The last slot of what an item holds: how many kinds more. */
    public static final TextKey MORE_KINDS = TextKey.of("jscore.tooltip.more_kinds", "+%s");
    /** How worn an item is, beside its bar. */
    public static final TextKey DURABILITY = TextKey.of("jscore.tooltip.durability", "Durability: %s of %s");
    /** A kind an item holds, in the whole list: its count and its name. */
    public static final TextKey HELD = TextKey.of("jscore.tooltip.held", "%s %s");

    private TooltipTexts() {
    }
}
