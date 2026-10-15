/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The pages of the series' shared tools in the Core's chapter of the manual: the Basic Wrench. */
public final class ToolsGuide {

    /* The Core's chapter's section on cables, where the wrench's page goes. */
    private static final ModGuide.SectionRef CABLES = new ModGuide.SectionRef("cables");

    private ToolsGuide() {
    }

    /** Declares the pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("basic_wrench", CABLES).titled("Basic Wrench")
                .icon(SharedTools.WRENCH::get).covers(SharedTools.WRENCH::get)
                .whatItIs("An open-ended spanner of iron, the series' shared wrench.")
                .whatItIsFor("Setting how each end of a pipe or a line of items meets the block it plugs into.")
                .table("The modes of an end")
                .property("Normal", "As the block says: a machine's output gives, its input takes, a tank both")
                .property("Push", "The line only fills the block, and never draws from it")
                .property("Pull", "The line only draws from the block, and never fills it")
                .property("Off", "The end plugs into nothing, and joins no cable beside it")
                .howToGetIt("Craft it of four iron ingots: two at the top corners, two down the middle.")
                .howToUseIt("Use it on the end of a wire, near the block it meets, to go round the modes.",
                        "Sneak to go round them the other way.",
                        "The line says which end it set and how, above the hotbar.")
                .whatCanGoWrong("Nothing changes.",
                        "Point at the wire itself, near the end you want; a part on the face closes it.")
                .register();
    }
}
