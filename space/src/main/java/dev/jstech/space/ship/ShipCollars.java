/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.region.Box;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Where a ship's docking collar opens: the middle of the top of its docking module's collar, the mouth another ship's
 * collar meets, in its yard; read once from its blocks, and again when they change. A ship with no docking module has
 * none.
 */
public final class ShipCollars {

    private static final double HALF = 0.5;
    /* Each ship's mouth as last read, with the box and count of blocks it was read from. */
    private static final Map<MovingStructure, Read> READ = new WeakHashMap<>();

    private ShipCollars() {
    }

    /** The mouth of {@code ship}'s collar in the world, where the ship stands now; none with no collar. */
    public static Optional<Vec3> mouth(final ServerLevel level, final MovingStructure ship) {
        final Optional<Vec3> yard = inYard(level, ship);
        if (yard.isEmpty()) {
            return Optional.empty();
        }
        final StructureFrame frame = ship.frame();
        final Vec3 at = yard.get();
        return Optional.of(new Vec3(frame.worldX(at.x, at.y, at.z), frame.worldY(at.x, at.y, at.z),
                frame.worldZ(at.x, at.y, at.z)));
    }

    /** The mouth of {@code ship}'s collar in its yard; none with no collar. */
    public static Optional<Vec3> inYard(final ServerLevel level, final MovingStructure ship) {
        final Read known = READ.get(ship);
        if (known != null && known.bounds().equals(ship.bounds()) && known.blocks() == ship.blocks()) {
            return known.mouth();
        }
        final Read read = new Read(ship.bounds(), ship.blocks(), read(level, ship.bounds()));
        READ.put(ship, read);
        return read.mouth();
    }

    private static Optional<Vec3> read(final ServerLevel level, final Box bounds) {
        double x = 0;
        double z = 0;
        int top = Integer.MIN_VALUE;
        int found = 0;
        for (final BlockPos at : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(),
                bounds.maxY(), bounds.maxZ())) {
            if (FormedPartBlock.originalAt(level, at, level.getBlockState(at)).is(
                    ShipKitBlocks.DOCKING_COLLAR.get())) {
                x += at.getX() + HALF;
                z += at.getZ() + HALF;
                top = Math.max(top, at.getY() + 1);
                found++;
            }
        }
        return found == 0 ? Optional.empty() : Optional.of(new Vec3(x / found, top, z / found));
    }

    /* A ship's mouth as read from its blocks in {@code bounds}, {@code blocks} of them. */
    private record Read(Box bounds, int blocks, Optional<Vec3> mouth) {
    }
}
