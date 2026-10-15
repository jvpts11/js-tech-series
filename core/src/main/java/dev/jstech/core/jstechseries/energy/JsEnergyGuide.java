/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The pages of J's Energy's things in the Core's chapter of the manual: the clamp meter. */
public final class JsEnergyGuide {

    /* The Core's chapter's section on energy, where these pages go. */
    private static final ModGuide.SectionRef ENERGY = new ModGuide.SectionRef("energy");

    private JsEnergyGuide() {
    }

    /** Declares the pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("clamp_meter", ENERGY).titled("Clamp Meter").icon(ClampMeter.ITEM::get)
                .covers(ClampMeter.ITEM::get)
                .whatItIs("A handheld instrument that reads energy. Clamped on a wire of a line, it reads what"
                        + " crosses the wire and which way; clamped on a machine or a store, what goes in and out of"
                        + " each of its faces.")
                .whatItIsFor("Finding where the energy goes, how much a line carries against its ceiling, and which"
                        + " machine draws it.")
                .table("Its keys and its dial")
                .property("HOLD", "Keeps the reading still")
                .property("MAX MIN", "Shows the most and the least that crossed in a tick since it was clamped")
                .property("SHOW", "Shows the reading beside the crosshair while it is in hand")
                .property("LIGHT", "Lights the display")
                .property("Dial", "Off, J's Energy's own units, or FE")
                .howToGetIt("Take it from the creative tab J's Tech.")
                .howToUseIt("Use it on a cable to clamp it on the energy wire you aim at.",
                        "Use it on a machine or a store to read it face by face.",
                        "Hold it: its reading shows beside the crosshair.",
                        "Use it in the air to hold it up and read the details on its display.")
                .whatCanGoWrong("It reads nothing.",
                        "Its dial is off, or what it was clamped on is gone. Turn the dial, or clamp it again.",
                        "A line reads no direction.",
                        "Nothing crossed the wire yet, or it was clamped a moment ago; wait a second.")
                .register();
    }
}
