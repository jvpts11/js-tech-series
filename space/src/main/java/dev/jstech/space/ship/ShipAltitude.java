/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.util.Loaded;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * How high a ship flies, in metres, as its HUD shows it: in a world, over the land under it, where that land is loaded,
 * else over the sea; over the line, in its system's space, over the sea of the world whose low orbit it is in, the
 * climb to the line and on from where it passed, so the number runs on across the passage; none out between the worlds.
 */
public final class ShipAltitude {

    /** No altitude: out in a system's space over no world. */
    public static final int NONE = Integer.MIN_VALUE;

    private ShipAltitude() {
    }

    /** How high a ship at {@code x}, {@code y}, {@code z} of {@code level} flies, in metres, or {@link #NONE}. */
    public static int of(final ServerLevel level, final double x, final double y, final double z) {
        if (SystemSpaces.systemOf(level.dimension()).isEmpty()) {
            final int bx = Mth.floor(x);
            final int bz = Mth.floor(z);
            final int under = Loaded.chunk(level, bx >> 4, bz >> 4)
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) : FarMaps.seaOf(level);
            return (int) Math.round(y - under);
        }
        final SolarBody body = SystemLayout.over(x, z);
        if (body == null) {
            return NONE;
        }
        final Optional<ServerLevel> world = SpaceWorlds.get(level, SolarSystemWorlds.worldOf(body))
                .map(found -> level.getServer().getLevel(found.dimension()));
        if (world.isEmpty() || world.get() == null) {
            return NONE;
        }
        return (int) Math.round(SpaceClimbs.ceiling(world.get()) - FarMaps.seaOf(world.get())
                + (y - SystemLayout.ORBIT_Y));
    }
}
