/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.space.JsSpace;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * The telemetry line: J's Space's whole data line, a line of the series' cable block that carries data only, never the
 * network's id. It joins antennas, receivers, decoders, data recorders and ground stations, and every connected stretch
 * of it is one ground system ({@link GroundSystems}). A device takes its place on the line by offering
 * {@link LineGroups#MEMBER} and {@link #MEMBER} on the faces the line meets.
 */
public final class TelemetryLine {

    /** The grid its stretches group the devices they join on. */
    public static final GridKind GRID = GridKind.declare(id("telemetry"), GridPolicies.GROUP);
    /** Its line, of one generation. */
    public static final Connection LINE = Connection.of(id("telemetry"));
    /** What a device on the line is to its ground system. */
    public static final BlockCapability<IMember, @Nullable Direction> MEMBER = BlockCapability.createSided(
            id("telemetry_member"), IMember.class);

    private TelemetryLine() {
    }

    /** Declares the grid and the line, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    /** A device on the telemetry line: what it does, and how big it is if it is an antenna. */
    public interface IMember {

        /** What it does in its ground system. */
        TelemetryRole role();

        /** How big it is, if it is an antenna. */
        default Optional<AntennaSize> size() {
            return Optional.empty();
        }
    }
}
