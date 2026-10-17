/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.hazard.BlockDensity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The radiation that comes down from the sky, in millisieverts an hour: a little at the bottom of the overworld's air,
 * more as the air thins with height, the most where there is no air at all, as on a station in orbit; a mod sets a
 * dimension's own, its radiation belts or a violent star. Rock and buildings above stop it, each solid block a share.
 */
public final class CosmicRadiation {

    /** Under a full sea-level sky, as at the bottom of the overworld's air. */
    public static final double SEA_LEVEL = 0.0003;
    /** Where there is no air above at all. */
    public static final double NO_AIR = 0.02;
    /* The overworld's air pressure at sea level, in kilopascals. */
    private static final double SEA_PRESSURE = 101.3;
    /* How much a solid block above lets through, and how many blocks above are looked at. */
    private static final double PER_BLOCK_ABOVE = 0.3;
    private static final int LOOKED_ABOVE = 8;
    /* How dense a block must be to count as a roof. */
    private static final double SOLID = 500;
    /* What a mod set for a dimension, under an open sky with no air. */
    private static final Map<ResourceKey<Level>, Double> DECLARED = new ConcurrentHashMap<>();

    private CosmicRadiation() {
    }

    /** Sets {@code dimension}'s radiation from the sky where there is no air, as its belts or its star make it. */
    public static void declare(final ResourceKey<Level> dimension, final double noAir) {
        DECLARED.put(dimension, noAir);
    }

    /** How much comes down from the sky at {@code at} of {@code level}. */
    public static double at(final ServerLevel level, final Vec3 at) {
        final double pressure = DimensionRulesData.of(level).at(at.y).pressure();
        final double thin = 1 - Math.min(1, Math.max(0, pressure) / SEA_PRESSURE);
        final double open = SEA_LEVEL + (DECLARED.getOrDefault(level.dimension(), NO_AIR) - SEA_LEVEL) * thin;
        return open * roof(level, BlockPos.containing(at));
    }

    /* The share the blocks above {@code pos} let through: each solid one a share, an open sky all of it. */
    private static double roof(final ServerLevel level, final BlockPos pos) {
        final int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (pos.getY() >= top) {
            return 1;
        }
        double through = 1;
        final BlockPos.MutableBlockPos above = pos.mutable();
        for (int i = 0; i < LOOKED_ABOVE && above.getY() < top; i++) {
            above.move(0, 1, 0);
            if (BlockDensity.of(level, above, level.getBlockState(above)) >= SOLID) {
                through *= PER_BLOCK_ABOVE;
            }
        }
        return through;
    }
}
