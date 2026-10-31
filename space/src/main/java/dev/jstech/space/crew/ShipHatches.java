/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.region.Box;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A starship's airlock door, passed through as a hatch is: used from outside it takes a player in, standing on the
 * airlock's floor just inside it; used from inside, out, standing just outside it at its foot, where the land under it
 * catches them, or space. Who may pass is the crew's to say ({@link ShipCrews}).
 */
public final class ShipHatches {

    /* How far under the place just inside a door its floor may be looked for, in blocks. */
    private static final int ROOM_BELOW = 3;
    private static final double HALF = 0.5;
    /*
     * How high over a block's foot a floor may be, in blocks, to be stood on; how far over it the room a player
     * standing needs is tried from, a hair, so the floor itself is not in the way.
     */
    private static final double LOW_FLOOR = 0.5;
    private static final double OFF_THE_FLOOR = 0.01;

    private ShipHatches() {
    }

    /**
     * Takes {@code player} through the airlock door of {@code ship} at {@code door}, in its yard: in from outside, out
     * from inside; whether they went.
     */
    public static boolean through(final ServerPlayer player, final MovingStructure ship, final BlockPos door) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return false;
        }
        final Optional<Direction> out = outward(ship, door);
        if (out.isEmpty()) {
            return false;
        }
        final BlockPos foot = footOf(level, door);
        final Vec3 at = MovingStructures.toYard(ship, player.position());
        final double side = (at.x - foot.getX() - HALF) * out.get().getStepX()
                + (at.z - foot.getZ() - HALF) * out.get().getStepZ();
        final StructureFrame frame = ship.frame();
        if (side > 0) {
            final BlockPos inside = foot.relative(out.get().getOpposite());
            final Optional<Vec3> spot = standing(level, player, inside);
            if (spot.isEmpty()) {
                return false;
            }
            go(player, level, frame, spot.get());
            StructureFooting.stood(player, ship, BlockPos.containing(spot.get().x, spot.get().y - 1, spot.get().z));
            return true;
        }
        final BlockPos outside = foot.relative(out.get());
        go(player, level, frame, new Vec3(outside.getX() + HALF, foot.getY() + OFF_THE_FLOOR, outside.getZ() + HALF));
        return true;
    }

    /**
     * The way out of {@code ship}'s hull at {@code door}, in its yard: away from the ship's middle line, across its
     * nose, as a door in its side opens; for a door on that line, at its nose or its tail, along the nose. None for a
     * structure that is no ship.
     */
    public static Optional<Direction> outward(final MovingStructure ship, final BlockPos door) {
        if (!(ship.motion() instanceof ShipFlight flight)) {
            return Optional.empty();
        }
        final Box bounds = ship.bounds();
        final double dx = door.getX() + HALF - (bounds.minX() + bounds.maxX() + 1) / 2.0;
        final double dz = door.getZ() + HALF - (bounds.minZ() + bounds.maxZ() + 1) / 2.0;
        final int acrossX = -flight.noseZ();
        final int acrossZ = flight.noseX();
        final double across = dx * acrossX + dz * acrossZ;
        if (Math.abs(across) >= HALF) {
            final int sign = across > 0 ? 1 : -1;
            return Optional.ofNullable(Direction.fromDelta(acrossX * sign, 0, acrossZ * sign));
        }
        final int sign = dx * flight.noseX() + dz * flight.noseZ() > 0 ? 1 : -1;
        return Optional.ofNullable(Direction.fromDelta(flight.noseX() * sign, 0, flight.noseZ() * sign));
    }

    /* The lowest block of the door at {@code door}, a door two blocks high clicked at its top. */
    private static BlockPos footOf(final ServerLevel level, final BlockPos door) {
        BlockPos foot = door;
        while (FormedPartBlock.originalAt(level, foot.below(), level.getBlockState(foot.below()))
                .is(ShipKitBlocks.AIRLOCK_DOOR.get())) {
            foot = foot.below();
        }
        return foot;
    }

    /*
     * Where {@code player} stands in the block {@code cell} of a ship's yard: the first floor down from it with room
     * over it for them standing, by the shapes of the ship's blocks; none with none near.
     */
    private static Optional<Vec3> standing(final ServerLevel level, final ServerPlayer player, final BlockPos cell) {
        final double x = cell.getX() + HALF;
        final double z = cell.getZ() + HALF;
        final EntityDimensions size = player.getDimensions(Pose.STANDING);
        final double half = size.width() / 2;
        for (int y = cell.getY() + 1; y > cell.getY() - ROOM_BELOW; y--) {
            final AABB under = new AABB(x - half, y - 1, z - half, x + half, y + LOW_FLOOR, z + half);
            double floor = Double.NEGATIVE_INFINITY;
            for (final VoxelShape shape : level.getBlockCollisions(player, under)) {
                for (final AABB part : shape.toAabbs()) {
                    floor = part.intersects(under) ? Math.max(floor, part.maxY) : floor;
                }
            }
            if (floor == Double.NEGATIVE_INFINITY || floor > y + LOW_FLOOR) {
                continue;
            }
            final AABB room = new AABB(x - half, floor + OFF_THE_FLOOR, z - half, x + half, floor + size.height(),
                    z + half);
            boolean clear = true;
            for (final VoxelShape shape : level.getBlockCollisions(player, room)) {
                clear &= shape.isEmpty();
            }
            if (clear) {
                return Optional.of(new Vec3(x, floor, z));
            }
        }
        return Optional.empty();
    }

    /* Puts {@code player} at {@code yard} of a ship standing as {@code frame}, in the world. */
    private static void go(final ServerPlayer player, final ServerLevel level, final StructureFrame frame,
                           final Vec3 yard) {
        player.teleportTo(level, frame.worldX(yard.x, yard.y, yard.z), frame.worldY(yard.x, yard.y, yard.z),
                frame.worldZ(yard.x, yard.y, yard.z), player.getYRot(), player.getXRot());
    }

}
