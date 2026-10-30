/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.space.universe.SphereMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The ground systems of a world: each connected stretch of the telemetry line, with the devices it joins, each where
 * it stands on the world's sphere, read as they stand now.
 */
public final class GroundSystems {

    private GroundSystems() {
    }

    /** The ground system the device at {@code pos} of {@code level} is on; none off the line. */
    public static Optional<GroundSystem> of(final ServerLevel level, final BlockPos pos) {
        return LineGroups.of(level, TelemetryLine.GRID).flatMap(groups -> groups.groupOf(pos))
                .map(group -> systemOf(level, group));
    }

    /** Every ground system of {@code level}. */
    public static List<GroundSystem> all(final ServerLevel level) {
        return LineGroups.of(level, TelemetryLine.GRID).map(groups -> groups.groups().stream()
                .map(group -> systemOf(level, group)).toList()).orElse(List.of());
    }

    /** Whether the ground system of the device at {@code pos} of {@code level} hears {@code target} now. */
    public static boolean hears(final ServerLevel level, final BlockPos pos, final Contact.Target target) {
        return of(level, pos).filter(system -> Contact.heard(system, target, level.getGameTime())).isPresent();
    }

    /* The ground system the stretch {@code group} joins: each of its devices, where it stands. */
    private static GroundSystem systemOf(final ServerLevel level, final LineGroup group) {
        final double width = Math.max(1, level.getWorldBorder().getSize());
        final List<GroundSystem.Device> devices = new ArrayList<>();
        for (final BlockPos pos : group.members()) {
            final TelemetryLine.IMember member = level.getCapability(TelemetryLine.MEMBER, pos, null);
            if (member != null) {
                devices.add(new GroundSystem.Device(member.role(), member.size(),
                        SphereMap.longitude(pos.getX() + 0.5, width), SphereMap.latitude(pos.getZ() + 0.5, width)));
            }
        }
        return new GroundSystem(devices);
    }
}
