/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * The jobs a data cable does, each a line of its own with a cable for each era it exists in.
 *
 * <p>Access joins the small computers to a router; the backbone joins the routers, the Mainframe and the racks; the
 * long distance joins two networks, between two gateways; HPC joins a supercomputer's nodes to its interface; the
 * crafting line joins the crafting switches to their computer. Splitting the work this way is what keeps a network
 * from needing channels or subnetworks: a player lays the line the job asks for, never counts what a cable holds.
 *
 * <p>Each era of a line is a generation of it, so a port takes its own era's cable and every earlier one. The crafting
 * line has a single cable for every era. {@link DataLines} turns a line into what cables and ports are made of.
 */
public enum DataLine implements IStableId, IStableName {

    ACCESS(0, "access"),
    BACKBONE(1, "backbone"),
    LONG_DISTANCE(2, "long_distance"),
    HPC(3, "hpc"),
    CRAFTING(4, "crafting");

    private final int id;
    private final String serializedName;

    private static final StableIds<DataLine> IDS = StableIds.of(DataLine.class);
    private static final StableNames<DataLine> NAMES = StableNames.of(DataLine.class);

    DataLine(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    /** The line named {@code name}, or null when no line is. */
    public static @Nullable DataLine byName(final @Nullable String name) {
        return NAMES.find(name);
    }

    /** The line numbered {@code id}, or null when no line is. */
    public static @Nullable DataLine byId(final int id) {
        return IDS.find(id);
    }

    /** The data line whose id is {@code lineId}, written {@code jscore:data/<name>}, or null when it is none. */
    public static @Nullable DataLine ofLineId(final @Nullable String lineId) {
        for (final DataLine each : values()) {
            if (each.lineId().equals(lineId)) {
                return each;
            }
        }
        return null;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** The line's id, as cables and ports name it: {@code jscore:data/<name>}. */
    public String lineId() {
        return "jscore:data/" + this.serializedName;
    }

    /** The generation a cable of this line has in {@code era}; the crafting line has one for every era. */
    public int generation(final HardwareEra era) {
        return this == CRAFTING ? 0 : era.id();
    }
}
