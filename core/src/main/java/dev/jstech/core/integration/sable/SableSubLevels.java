/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.sable;

import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.IForeignKind;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.region.Box;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Sable's sub-levels as the kit sees them, read through Sable's companion library: each one an obstacle that moves,
 * its blocks in its plot, a chunk grid of the level, placed in the world as its pose places them, a pose being the
 * same turn about a pivot as a frame of the kit's; the region of its plots, whose blocks the kit takes into none of its
 * structures; and a place of a plot, where its sub-level has it in the world. A sub-level drawn larger or smaller than
 * its blocks, by a scale of its pose, is none the kit can meet, and is left alone.
 */
final class SableSubLevels implements IForeignKind {

    private static final double SAME_SCALE = 1.0e-6;
    private static final double HALF = 0.5;

    @Override
    public String name() {
        return SableIntegration.NAME;
    }

    @Override
    public void near(final Level level, final AABB box, final Consumer<ForeignMover> into) {
        for (final SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(box))) {
            final Pose3dc pose = sub.logicalPose();
            if (!unscaled(pose)) {
                continue;
            }
            final BoundingBox3d plot = sub.boundingBox().transformInverse(pose, new BoundingBox3d());
            // How fast its pivot goes, and how fast it turns from how fast three places a block from it go besides.
            final Vector3dc pivot = pose.rotationPoint();
            final Vector3d speed = SableCompanion.INSTANCE.getVelocity(level, sub, pivot, new Vector3d());
            final Vector3d spin = new Vector3d();
            final Vector3d axis = new Vector3d();
            final Vector3d beside = new Vector3d();
            for (int i = 0; i < 3; i++) {
                axis.set(i == 0 ? 1 : 0, i == 1 ? 1 : 0, i == 2 ? 1 : 0);
                SableCompanion.INSTANCE.getVelocity(level, sub, new Vector3d(pivot).add(axis), beside).sub(speed);
                spin.add(pose.transformNormal(axis, new Vector3d()).cross(beside));
            }
            spin.mul(HALF);
            into.accept(new ForeignMover(SableIntegration.NAME, frameOf(pose), new Box((int) Math.floor(plot.minX()),
                    (int) Math.floor(plot.minY()), (int) Math.floor(plot.minZ()), (int) Math.floor(plot.maxX()),
                    (int) Math.floor(plot.maxY()), (int) Math.floor(plot.maxZ())), level, level,
                    new Vec3(speed.x, speed.y, speed.z), new Vec3(spin.x, spin.y, spin.z)));
        }
    }

    @Override
    public boolean holds(final Level level, final BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    @Override
    public Vec3 inWorld(final Level level, final Vec3 place) {
        if (!SableCompanion.INSTANCE.isInPlotGrid(level, place)
                || SableCompanion.INSTANCE.getContaining(level, place) == null) {
            return place;
        }
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, place);
    }

    /* Whether the pose draws its blocks at their own size, as the kit's frames do. */
    private static boolean unscaled(final Pose3dc pose) {
        final Vector3dc scale = pose.scale();
        return Math.abs(scale.x() - 1) < SAME_SCALE && Math.abs(scale.y() - 1) < SAME_SCALE
                && Math.abs(scale.z() - 1) < SAME_SCALE;
    }

    /* The frame of the kit's that places a plot's blocks where the pose does: turned about the pose's own pivot. */
    private static StructureFrame frameOf(final Pose3dc pose) {
        final Vector3dc at = pose.position();
        final Vector3dc pivot = pose.rotationPoint();
        final Quaterniondc turn = pose.orientation();
        return new StructureFrame(new StructurePose(at.x(), at.y(), at.z(), turn.x(), turn.y(), turn.z(), turn.w()),
                pivot.x(), pivot.y(), pivot.z());
    }
}
