/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.jstechseries.tier.IndustrialTier;

/**
 * What goes in a place of a Computational Research Centre's gates: its processors, of three tiers, each making research
 * and drawing power that all becomes heat; and what takes the heat away, each by its own way and up to its own rating
 * (industrial/docs/design/science.md). Each has a number of its own, which the formed model is told the parts by.
 */
public enum CrcPart implements IStableId {
    /** Transistor boards, as the IBM 7090's SMS cards. */
    RESEARCH_PROCESSOR(1, "research_processor", IndustrialTier.T3, 1.0, 5_000.0, Cooling.NONE, "p3"),
    /** A fibreglass board of DIP chips. */
    INTEGRATED_RESEARCH_PROCESSOR(2, "integrated_research_processor", IndustrialTier.T4, 4.0, 20_000.0, Cooling.NONE,
            "p4"),
    /** One chip under its heat spreader. */
    NANOSCALE_RESEARCH_PROCESSOR(3, "nanoscale_research_processor", IndustrialTier.T5, 16.0, 80_000.0, Cooling.NONE,
            "p5"),
    /** An extruded aluminium sink, shedding its heat to the air by itself. */
    HEAT_SINK(4, "heat_sink", IndustrialTier.T3, 0.0, 5_000.0, Cooling.AIR, "sink"),
    /** A copper cold plate, its heat carried away by the water of the coolant loop. */
    LIQUID_COOLER(5, "liquid_cooler", IndustrialTier.T3, 0.0, 15_000.0, Cooling.WATER, "cooler"),
    /** An evaporator whose heat pipes carry its heat out through the Heat Port. */
    HEAT_PIPE_COOLER(6, "heat_pipe_cooler", IndustrialTier.T4, 0.0, 25_000.0, Cooling.HEAT_PIPE, "pipe");

    private final int id;
    private final String path;
    private final IndustrialTier tier;
    private final double research;
    private final double watts;
    private final Cooling cooling;
    private final String bone;

    CrcPart(final int id, final String path, final IndustrialTier tier, final double research, final double watts,
            final Cooling cooling, final String bone) {
        this.id = id;
        this.path = path;
        this.tier = tier;
        this.research = research;
        this.watts = watts;
        this.cooling = cooling;
        this.bone = bone;
    }

    /** Its number, never 0, which a place holding nothing is told by. */
    @Override
    public int id() {
        return this.id;
    }

    /** Its item's path. */
    public String path() {
        return this.path;
    }

    /** The tier it comes at. */
    public IndustrialTier tier() {
        return this.tier;
    }

    /** The research it makes, in points a second; none for what cools. */
    public double research() {
        return this.research;
    }

    /**
     * For a processor, the power it draws, in watts, all of which becomes heat; for what cools, the most it takes
     * away, in watts.
     */
    public double watts() {
        return this.watts;
    }

    /** How it takes heat away; none for a processor. */
    public Cooling cooling() {
        return this.cooling;
    }

    /** Whether it is a processor, making research. */
    public boolean processor() {
        return this.cooling == Cooling.NONE;
    }

    /** The name its bones in the formed model end with, {@code place_<n>_<bone>}. */
    public String bone() {
        return this.bone;
    }

    /** How a part takes heat away, and through what. */
    public enum Cooling {
        /** It makes heat rather than taking it. */
        NONE,
        /** To the air round it, by itself. */
        AIR,
        /** Into the water flowing in at the Coolant Inlet and out at the Coolant Outlet. */
        WATER,
        /** Out through the Heat Port, by heat pipe. */
        HEAT_PIPE
    }
}
