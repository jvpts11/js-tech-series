/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.reservoir;

import dev.jstech.core.worldgen.vein.VeinField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lays one reservoir over a box of columns and nothing outside it: its lens of soaked rock where the rock it replaces
 * lies, and over the lens a dome of cap rock as thick as its kind says. It lays no liquid and replaces no air, liquid
 * or other block, so nothing floods and caves stay as they were. The world's generation lays reservoirs a chunk at a
 * time ({@link ReservoirFeature}); anything else can lay one where it is wanted.
 */
public final class ReservoirPainter {

    private static final int UPDATE = 2;

    private ReservoirPainter() {
    }

    /** Lays {@code reservoir} over the columns from {@code x0, z0} to {@code x1, z1}; how many blocks it soaked. */
    public static int paint(final WorldGenLevel level, final Reservoirs.Reservoir reservoir, final int x0,
                            final int z0, final int x1, final int z1) {
        final VeinField.Vein body = reservoir.body();
        final ReservoirType type = reservoir.kind().type();
        final BlockState soaked = reservoir.soaked().defaultBlockState();
        final BlockState cap = reservoir.cap().defaultBlockState();
        final int fromX = Math.max(x0, (int) Math.floor(body.x() - body.radius()));
        final int toX = Math.min(x1, (int) Math.ceil(body.x() + body.radius()));
        final int fromZ = Math.max(z0, (int) Math.floor(body.z() - body.radius()));
        final int toZ = Math.min(z1, (int) Math.ceil(body.z() + body.radius()));
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int laid = 0;
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                final double dx = (x - body.x()) / body.radius();
                final double dz = (z - body.z()) / body.radius();
                final double across = dx * dx + dz * dz;
                if (across >= 1) {
                    continue;
                }
                // The lens's top and bottom over this column, and its cap above the top.
                final double half = body.thickness() * Math.sqrt(1 - across);
                final int bottom = (int) Math.ceil(body.y() - half);
                final int top = (int) Math.floor(body.y() + half);
                for (int y = bottom; y <= top + type.capping(); y++) {
                    if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    final boolean lens = y <= top && body.share(x, y, z) > 0;
                    if (!lens && y <= top) {
                        continue;
                    }
                    if (level.getBlockState(at.set(x, y, z)).is(type.replaces())) {
                        level.setBlock(at, lens ? soaked : cap, UPDATE);
                        laid += lens ? 1 : 0;
                    }
                }
            }
        }
        return laid;
    }
}
