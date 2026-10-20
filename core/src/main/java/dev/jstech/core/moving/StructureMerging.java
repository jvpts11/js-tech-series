/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * Joining two structures for good, as two docked ships made one: the visitor's blocks are moved whole into the host's
 * plot, where they stand on it now, turned with it, and the visitor is no more. Only two structures square to each
 * other join so, the visitor turned from the host by quarter turns about the host's up, since a block only turns so;
 * and only where none of the visitor's blocks would fall on one of the host's. What stood on the visitor, the joints
 * tied to it and what it held go to the host; their momentum is shared.
 */
public final class StructureMerging {

    /* How close a structure's up must be to the other's, and its turn to a quarter turn, to be square to it. */
    private static final double SQUARE = 1.0e-3;
    private static final double HALF = 0.5;

    private StructureMerging() {
    }

    /**
     * Joins {@code visitor} into {@code host} for good, where it stands on it now. Refused, saying why, when they are
     * not square to each other, when a block would fall on another, or when the host's plot has no room.
     */
    public static StructureResult join(final MovingStructure host, final MovingStructure visitor) {
        if (!(host.level() instanceof ServerLevel level) || host == visitor || host.level() != visitor.level()) {
            return StructureResult.refused(StructureTexts.MOUNT_LOOP.text());
        }
        if (!host.ready() || !visitor.ready()) {
            return StructureResult.refused(StructureTexts.NOT_READY.text());
        }
        final StructureFrame hostFrame = host.frame();
        final StructureFrame visitorFrame = visitor.frame();
        final StructurePose between = hostFrame.local(visitorFrame.pose());
        final StructureFrame square = new StructureFrame(between, 0, 0, 0);
        if (!square.aligned() || Math.abs(square.turnY(0, 1, 0) - 1) > SQUARE) {
            return StructureResult.refused(StructureTexts.NOT_ALIGNED.text());
        }
        final int quarters = Math.floorMod((int) Math.round(-between.yawDegrees() / 90.0), 4);
        final Rotation rotation = switch (quarters) {
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
        final Map<BlockPos, BlockPos> moves = new LinkedHashMap<>();
        final Box bounds = visitor.bounds();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    at.set(x, y, z);
                    if (level.getBlockState(at).isAir()) {
                        continue;
                    }
                    final BlockPos to = hostPlace(hostFrame, visitorFrame, x + HALF, y + HALF, z + HALF);
                    if (!YardLayout.inPlot(host.plot(), to.getX(), to.getZ()) || level.isOutsideBuildHeight(to)) {
                        return StructureResult.refused(StructureTexts.TOO_WIDE.text());
                    }
                    final BlockState there = level.getBlockState(to);
                    if (!there.isAir() && !there.canBeReplaced()) {
                        return StructureResult.refused(StructureTexts.NO_ROOM.with(to.toShortString()));
                    }
                    moves.put(at.immutable(), to);
                }
            }
        }
        if (moves.isEmpty()) {
            return StructureResult.refused(StructureTexts.NOTHING.text());
        }
        share(host, visitor);
        if (visitor.mount() != null) {
            StructureMounts.unmount(visitor, false);
        }
        StructureYard.hold(level, Box.between(moves.values().stream().mapToInt(BlockPos::getX).min().orElse(0),
                moves.values().stream().mapToInt(BlockPos::getY).min().orElse(0),
                moves.values().stream().mapToInt(BlockPos::getZ).min().orElse(0),
                moves.values().stream().mapToInt(BlockPos::getX).max().orElse(0),
                moves.values().stream().mapToInt(BlockPos::getY).max().orElse(0),
                moves.values().stream().mapToInt(BlockPos::getZ).max().orElse(0)));
        visitor.parting(true);
        try {
            BlockMover.move(level, level, moves, rotation, false, false);
        } finally {
            visitor.parting(false);
        }
        StructureFooting.moved(visitor, host, moves);
        moveJoints(level, host, visitor, hostFrame, visitorFrame);
        moveHeld(host, visitor, moves);
        return StructureResult.done(host, moves.size());
    }

    /* The host's yard block holding the visitor's yard place {@code (x, y, z)}, as both stand now. */
    private static BlockPos hostPlace(final StructureFrame host, final StructureFrame visitor, final double x,
                                      final double y, final double z) {
        final double wx = visitor.worldX(x, y, z);
        final double wy = visitor.worldY(x, y, z);
        final double wz = visitor.worldZ(x, y, z);
        return BlockPos.containing(host.yardX(wx, wy, wz), host.yardY(wx, wy, wz), host.yardZ(wx, wy, wz));
    }

    /* Their momentum shared: the host, moving by its physics, goes on as the two went together. */
    private static void share(final MovingStructure host, final MovingStructure visitor) {
        final StructurePhysics carrier = StructureMounts.rootOf(host).physics();
        final StructurePhysics coming = visitor.physics();
        if (carrier == null || !carrier.active() || coming == null || !coming.active()) {
            return;
        }
        final double total = carrier.mass() + coming.mass();
        if (total <= 0) {
            return;
        }
        final Vector3d momentum = new Vector3d(carrier.body().velocity()).mul(carrier.mass())
                .fma(coming.mass(), coming.body().velocity());
        carrier.body().velocity().set(momentum.div(total));
        carrier.wake();
    }

    /* The joints tied to the visitor are tied to the host, where their ends stood on it. */
    private static void moveJoints(final ServerLevel level, final MovingStructure host, final MovingStructure visitor,
                                   final StructureFrame hostFrame, final StructureFrame visitorFrame) {
        if (StructureJoints.of(level, visitor.id()).isEmpty()) {
            return;
        }
        StructureJoints.change(level, joint -> {
            if (!joint.touches(visitor.id())) {
                return joint;
            }
            return joint.between(moved(joint.a(), host, visitor, hostFrame, visitorFrame),
                    moved(joint.b(), host, visitor, hostFrame, visitorFrame));
        });
    }

    private static JointEnd moved(final JointEnd end, final MovingStructure host, final MovingStructure visitor,
                                  final StructureFrame hostFrame, final StructureFrame visitorFrame) {
        if (!end.on(visitor.id())) {
            return end;
        }
        final Vec3 place = end.place();
        final double wx = visitorFrame.worldX(place.x, place.y, place.z);
        final double wy = visitorFrame.worldY(place.x, place.y, place.z);
        final double wz = visitorFrame.worldZ(place.x, place.y, place.z);
        return end.onto(Optional.of(host.id()), new Vec3(hostFrame.yardX(wx, wy, wz), hostFrame.yardY(wx, wy, wz),
                hostFrame.yardZ(wx, wy, wz)), carried(end.axis(), hostFrame, visitorFrame),
                carried(end.normal(), hostFrame, visitorFrame));
    }

    /* A way of the visitor's yard as a way of the host's. */
    private static Vec3 carried(final Vec3 way, final StructureFrame host, final StructureFrame visitor) {
        final double x = visitor.turnX(way.x, way.y, way.z);
        final double y = visitor.turnY(way.x, way.y, way.z);
        final double z = visitor.turnZ(way.x, way.y, way.z);
        return new Vec3(host.unturnX(x, y, z), host.unturnY(x, y, z), host.unturnZ(x, y, z));
    }

    /* What the visitor held is held by the host, where it stands on it now, its anchor where that block went. */
    private static void moveHeld(final MovingStructure host, final MovingStructure visitor,
                                 final Map<BlockPos, BlockPos> moves) {
        for (final MovingStructure child : StructureMounts.children(visitor)) {
            final StructureMount mount = child.mount();
            if (mount == null) {
                continue;
            }
            StructureMounts.unfile(child);
            child.mounted(new StructureMount(host.id(), host.frame().local(child.pose()),
                    mount.anchor().map(moves::get)));
            StructureMounts.file(child);
        }
    }
}
