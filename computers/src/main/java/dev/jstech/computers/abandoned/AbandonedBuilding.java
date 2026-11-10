/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import static dev.jstech.core.jstechseries.tier.HardwareEra.ADVANCED;
import static dev.jstech.core.jstechseries.tier.HardwareEra.LEGACY;
import static dev.jstech.core.jstechseries.tier.HardwareEra.STANDARD;
import static dev.jstech.core.jstechseries.tier.HardwareEra.TRANSITION;
import static dev.jstech.core.jstechseries.tier.HardwareEra.VINTAGE;
import static dev.jstech.core.jstechseries.tier.HardwareEra.WITH_PARTS;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.List;

/**
 * The twenty kinds of building that lie abandoned across the Overworld (computers/docs/design/survival.md, "Abandoned
 * buildings"): each with the eras it may be of, the average distance in chunks between two of the same kind
 * (estimates), and the size of its ground, in blocks across and deep.
 */
public enum AbandonedBuilding implements IStableName {

    ABANDONED_HOUSE("abandoned_house", 32, 19, 18, List.of(VINTAGE, LEGACY)),
    EWASTE_DUMP("ewaste_dump", 32, 23, 21, WITH_PARTS),
    DELIVERY_VAN("delivery_van", 32, 20, 14, WITH_PARTS),
    COMPUTER_SHOP("computer_shop", 48, 17, 15, WITH_PARTS),
    LAN_HOUSE("lan_house", 48, 22, 20, List.of(LEGACY, TRANSITION)),
    SCHOOL("school", 48, 25, 20, List.of(VINTAGE, LEGACY, TRANSITION)),
    LIBRARY("library", 48, 23, 22, WITH_PARTS),
    RADIO_TV("radio_tv", 48, 27, 20, List.of(VINTAGE, LEGACY)),
    TELEPHONE_EXCHANGE("telephone_exchange", 48, 25, 22, WITH_PARTS),
    UNIVERSITY_LAB("university_lab", 72, 25, 23, WITH_PARTS),
    BANK("bank", 72, 25, 22, WITH_PARTS),
    MIDSOFT_OFFICE("midsoft_office", 72, 23, 18, WITH_PARTS),
    SOFTWARE_HOUSE("software_house", 72, 21, 16, WITH_PARTS),
    STARTUP("startup", 72, 25, 20, List.of(TRANSITION, STANDARD, ADVANCED)),
    GAME_STUDIO("game_studio", 72, 24, 20, List.of(LEGACY, TRANSITION, STANDARD, ADVANCED)),
    HACKER_DEN("hacker_den", 72, 17, 16, List.of(LEGACY, TRANSITION, STANDARD, ADVANCED)),
    ELECTRONICS_FACTORY("electronics_factory", 72, 30, 24, WITH_PARTS),
    MINING_FARM("mining_farm", 72, 28, 22, List.of(STANDARD, ADVANCED)),
    DATACENTER("datacenter", 112, 38, 30, List.of(LEGACY, TRANSITION, STANDARD, ADVANCED)),
    SUPERCOMPUTING_CENTRE("supercomputing_centre", 160, 36, 29, List.of(STANDARD, ADVANCED));

    private final String serializedName;
    private final int spacing;
    private final int width;
    private final int depth;
    private final List<Era> eras;

    /* The hacker's den is dug into the ground: its ground lies this many blocks above its foot. */
    private static final int DEN_DEPTH = 4;

    AbandonedBuilding(final String serializedName, final int spacing, final int width, final int depth,
                      final List<Era> eras) {
        this.serializedName = serializedName;
        this.spacing = spacing;
        this.width = width;
        this.depth = depth;
        this.eras = List.copyOf(eras);
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** The average distance between two of this kind, in chunks. */
    public int spacing() {
        return spacing;
    }

    /** The eras it may be of, the Vintage first. */
    public List<Era> eras() {
        return eras;
    }

    /** How far its ground reaches from its middle, in blocks: what the land under it is read across. */
    public int reach() {
        return Math.max(width, depth) / 2;
    }

    /** How many blocks its template's ground lies above its foot: none but for the hacker's den's basement. */
    public int sunk() {
        return this == HACKER_DEN ? DEN_DEPTH : 0;
    }
}
