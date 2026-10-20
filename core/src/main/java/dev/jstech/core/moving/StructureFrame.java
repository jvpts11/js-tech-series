/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * How a place among a structure's blocks, kept in the yard, stands in the world, and back: the structure's pivot, a
 * place in the yard, is where its pose stands, and every other block is turned round it by the pose's rotation. The
 * rotation is worked out once into a matrix, so turning a place costs a few multiplications; methods give one axis of
 * the answer at a time, so the busy paths make no garbage. Pure: no part of the game.
 *
 * <pre>{@code
 * StructureFrame frame = new StructureFrame(pose, pivotX, pivotY, pivotZ);
 * double worldY = frame.worldY(yardX, yardY, yardZ);
 * }</pre>
 */
public final class StructureFrame {

    private final StructurePose pose;
    private final double pivotX;
    private final double pivotY;
    private final double pivotZ;
    /* The rotation, row by row: world = pose + R (yard - pivot). */
    private final double r00;
    private final double r01;
    private final double r02;
    private final double r10;
    private final double r11;
    private final double r12;
    private final double r20;
    private final double r21;
    private final double r22;

    /* How close to 1 a matrix entry has to be for the rotation to count as one that maps axes onto axes. */
    private static final double ALIGNED = 1.0e-9;

    /**
     * The frame of a structure whose pivot, at {@code (pivotX, pivotY, pivotZ)} in the yard, stands at {@code pose}.
     */
    public StructureFrame(final StructurePose pose, final double pivotX, final double pivotY, final double pivotZ) {
        this.pose = pose;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        final double x = pose.qx();
        final double y = pose.qy();
        final double z = pose.qz();
        final double w = pose.qw();
        this.r00 = 1 - 2 * (y * y + z * z);
        this.r01 = 2 * (x * y - w * z);
        this.r02 = 2 * (x * z + w * y);
        this.r10 = 2 * (x * y + w * z);
        this.r11 = 1 - 2 * (x * x + z * z);
        this.r12 = 2 * (y * z - w * x);
        this.r20 = 2 * (x * z - w * y);
        this.r21 = 2 * (y * z + w * x);
        this.r22 = 1 - 2 * (x * x + y * y);
    }

    public StructurePose pose() {
        return pose;
    }

    public double pivotX() {
        return pivotX;
    }

    public double pivotY() {
        return pivotY;
    }

    public double pivotZ() {
        return pivotZ;
    }

    /** The world's east of the yard's place {@code (x, y, z)}. */
    public double worldX(final double x, final double y, final double z) {
        return pose.x() + r00 * (x - pivotX) + r01 * (y - pivotY) + r02 * (z - pivotZ);
    }

    /** The world's up of the yard's place {@code (x, y, z)}. */
    public double worldY(final double x, final double y, final double z) {
        return pose.y() + r10 * (x - pivotX) + r11 * (y - pivotY) + r12 * (z - pivotZ);
    }

    /** The world's south of the yard's place {@code (x, y, z)}. */
    public double worldZ(final double x, final double y, final double z) {
        return pose.z() + r20 * (x - pivotX) + r21 * (y - pivotY) + r22 * (z - pivotZ);
    }

    /** The yard's east of the world's place {@code (x, y, z)}. */
    public double yardX(final double x, final double y, final double z) {
        return pivotX + r00 * (x - pose.x()) + r10 * (y - pose.y()) + r20 * (z - pose.z());
    }

    /** The yard's up of the world's place {@code (x, y, z)}. */
    public double yardY(final double x, final double y, final double z) {
        return pivotY + r01 * (x - pose.x()) + r11 * (y - pose.y()) + r21 * (z - pose.z());
    }

    /** The yard's south of the world's place {@code (x, y, z)}. */
    public double yardZ(final double x, final double y, final double z) {
        return pivotZ + r02 * (x - pose.x()) + r12 * (y - pose.y()) + r22 * (z - pose.z());
    }

    /** The world's east part of the yard's direction {@code (x, y, z)}: a direction turns, it does not move. */
    public double turnX(final double x, final double y, final double z) {
        return r00 * x + r01 * y + r02 * z;
    }

    public double turnY(final double x, final double y, final double z) {
        return r10 * x + r11 * y + r12 * z;
    }

    public double turnZ(final double x, final double y, final double z) {
        return r20 * x + r21 * y + r22 * z;
    }

    /** The yard's east part of the world's direction {@code (x, y, z)}. */
    public double unturnX(final double x, final double y, final double z) {
        return r00 * x + r10 * y + r20 * z;
    }

    public double unturnY(final double x, final double y, final double z) {
        return r01 * x + r11 * y + r21 * z;
    }

    public double unturnZ(final double x, final double y, final double z) {
        return r02 * x + r12 * y + r22 * z;
    }

    /** One entry of the rotation's matrix, row {@code row} and column {@code column}, each from 0 to 2. */
    public double rotation(final int row, final int column) {
        return switch (row * 3 + column) {
            case 0 -> r00;
            case 1 -> r01;
            case 2 -> r02;
            case 3 -> r10;
            case 4 -> r11;
            case 5 -> r12;
            case 6 -> r20;
            case 7 -> r21;
            case 8 -> r22;
            default -> throw new IllegalArgumentException("a rotation has rows and columns 0 to 2: " + row + ", "
                    + column);
        };
    }

    /**
     * The box in the world that holds the yard's box from {@code (minX, minY, minZ)} to {@code (maxX, maxY, maxZ)}
     * however the structure is turned, as {@code {minX, minY, minZ, maxX, maxY, maxZ}}.
     */
    public double[] worldBox(final double minX, final double minY, final double minZ, final double maxX,
                             final double maxY, final double maxZ) {
        final double cx = (minX + maxX) / 2;
        final double cy = (minY + maxY) / 2;
        final double cz = (minZ + maxZ) / 2;
        final double hx = (maxX - minX) / 2;
        final double hy = (maxY - minY) / 2;
        final double hz = (maxZ - minZ) / 2;
        final double wx = worldX(cx, cy, cz);
        final double wy = worldY(cx, cy, cz);
        final double wz = worldZ(cx, cy, cz);
        final double ex = Math.abs(r00) * hx + Math.abs(r01) * hy + Math.abs(r02) * hz;
        final double ey = Math.abs(r10) * hx + Math.abs(r11) * hy + Math.abs(r12) * hz;
        final double ez = Math.abs(r20) * hx + Math.abs(r21) * hy + Math.abs(r22) * hz;
        return new double[] {wx - ex, wy - ey, wz - ez, wx + ex, wy + ey, wz + ez};
    }

    /**
     * The box in the yard that holds the world's box from {@code (minX, minY, minZ)} to {@code (maxX, maxY, maxZ)},
     * as {@code {minX, minY, minZ, maxX, maxY, maxZ}}: the blocks worth looking at for what stands in that box.
     */
    public double[] yardBox(final double minX, final double minY, final double minZ, final double maxX,
                            final double maxY, final double maxZ) {
        final double cx = (minX + maxX) / 2;
        final double cy = (minY + maxY) / 2;
        final double cz = (minZ + maxZ) / 2;
        final double hx = (maxX - minX) / 2;
        final double hy = (maxY - minY) / 2;
        final double hz = (maxZ - minZ) / 2;
        final double yx = yardX(cx, cy, cz);
        final double yy = yardY(cx, cy, cz);
        final double yz = yardZ(cx, cy, cz);
        final double ex = Math.abs(r00) * hx + Math.abs(r10) * hy + Math.abs(r20) * hz;
        final double ey = Math.abs(r01) * hx + Math.abs(r11) * hy + Math.abs(r21) * hz;
        final double ez = Math.abs(r02) * hx + Math.abs(r12) * hy + Math.abs(r22) * hz;
        return new double[] {yx - ex, yy - ey, yz - ez, yx + ex, yy + ey, yz + ez};
    }

    /**
     * Where a structure held by this one stands in the world, its pose {@code local} given in this one's yard: its
     * place a place of this yard, its rotation from this yard's axes. A turret on a ship stands so.
     */
    public StructurePose placed(final StructurePose local) {
        final StructurePose turned = local.then(pose);
        return turned.movedTo(worldX(local.x(), local.y(), local.z()), worldY(local.x(), local.y(), local.z()),
                worldZ(local.x(), local.y(), local.z()));
    }

    /** The pose in this structure's yard of a structure standing at {@code world}: what {@link #placed} undoes. */
    public StructurePose local(final StructurePose world) {
        final StructurePose undo = new StructurePose(0, 0, 0, -pose.qx(), -pose.qy(), -pose.qz(), pose.qw());
        final StructurePose turned = world.then(undo);
        return turned.movedTo(yardX(world.x(), world.y(), world.z()), yardY(world.x(), world.y(), world.z()),
                yardZ(world.x(), world.y(), world.z()));
    }

    /**
     * Whether the rotation maps each axis onto an axis, a quarter turn or none on each: then a block of the yard is a
     * box of the world too, exactly, and the structure can be set down on the world's grid.
     */
    public boolean aligned() {
        return alignedRow(r00, r01, r02) && alignedRow(r10, r11, r12) && alignedRow(r20, r21, r22);
    }

    private static boolean alignedRow(final double a, final double b, final double c) {
        final double big = Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c)));
        return Math.abs(big - 1) < ALIGNED && Math.abs(a) + Math.abs(b) + Math.abs(c) - big < ALIGNED * 3;
    }
}
