/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.research;

import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * The Research Link Cable: a line of the series' cable block that carries nothing and joins research computers into
 * a group, one group for each connected run. It has no tiers and no length limit; a dye gives a run a colour, and runs
 * of two colours never join, so groups can lie side by side (industrial/docs/design/science.md).
 */
@TextHolder
public final class ResearchLink {

    /** What the cable is for, as its tooltip says it. */
    public static final TextKey JOB = TextKey.of("jsindustrial.cable.research_link.job",
            "Joins research computers into a group, one group for each run; a dyed run is a group of its own.");
    /** The grid its runs group the research computers they join on. */
    public static final GridKind GRID = GridKind.declare(id("research_link"), GridPolicies.GROUP);
    /** Its line, of one generation: it has no tiers. */
    public static final Connection LINE = Connection.of(id("research_link"));
    /* Its thickness in its lane, in pixels: a multi-pair cable, a little thinner than the energy cable. */
    private static final int THICKNESS = 3;
    public static final CableEntry CABLE = IndustrialModule.CONTENT.cable("research_link_cable",
                    CableType.builder(LINE).describe(JOB, null)
                            .grid(GRID).lane(Lane.TOP_RIGHT).thickness(THICKNESS)
                            .jacket(id("block/cable/research_link"))
                            .plug(id("block/cable/plug/research_link")))
            .named("Research Link Cable").tab(IndustrialModule.RESEARCH).register();

    private ResearchLink() {
    }

    /** Declares the grid, the line and the cable, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
