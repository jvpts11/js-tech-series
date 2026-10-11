/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

/**
 * Where a point of a scene stands on its frame, as a view sees it: the same sums the scene is drawn with, so a balloon
 * drawn on the frame lands on the point it is pinned to, and the part it names is outlined where it is drawn.
 *
 * <p>A scene is drawn without perspective, as a manual's drawings are. It is turned round its middle by the view's yaw
 * (a right-handed turn about the upward axis), then tilted by its pitch (a right-handed turn about the axis across),
 * and scaled so the whole scene, turned any way at all, fits the frame at a zoom of 1. Across the frame runs with the
 * scene's east, down the frame against its up, and a point's depth grows towards the reader.
 *
 * <p>Pure: a scene and a view in, pixels out.
 */
public final class SceneProjection {

    private final double middleX;
    private final double middleY;
    private final double middleZ;
    private final double sinYaw;
    private final double cosYaw;
    private final double sinPitch;
    private final double cosPitch;
    private final double scale;
    private final double frameMiddleX;
    private final double frameMiddleY;

    /** The projection of that scene, seen that way, on a frame of that size in pixels. */
    public SceneProjection(final GuideBlock.Scene scene, final GuideBlock.SceneView view, final int width,
                           final int height) {
        this.middleX = scene.width() / 2.0;
        this.middleY = scene.tall() / 2.0;
        this.middleZ = scene.depth() / 2.0;
        this.sinYaw = Math.sin(Math.toRadians(view.yaw()));
        this.cosYaw = Math.cos(Math.toRadians(view.yaw()));
        this.sinPitch = Math.sin(Math.toRadians(view.pitch()));
        this.cosPitch = Math.cos(Math.toRadians(view.pitch()));
        // The scene's longest line, corner to far corner, fits the frame's shorter side whichever way it is turned.
        final double across = Math.sqrt(scene.width() * (double) scene.width() + scene.tall() * (double) scene.tall()
                + scene.depth() * (double) scene.depth());
        this.scale = Math.min(width, height) / across * view.zoom();
        this.frameMiddleX = width / 2.0;
        this.frameMiddleY = height / 2.0;
    }

    /** How many pixels of the frame one block spans. */
    public double scale() {
        return this.scale;
    }

    /** The middle of the scene, which it is turned round, in blocks from the grid's corner: across, up and back. */
    public double[] middle() {
        return new double[] {this.middleX, this.middleY, this.middleZ};
    }

    /** Where a point of the scene, in blocks from the grid's corner, stands on the frame, and how near it is. */
    public Point project(final double x, final double y, final double z) {
        final double dx = x - this.middleX;
        final double dy = y - this.middleY;
        final double dz = z - this.middleZ;
        // Turned round the upward axis, then tilted about the axis across.
        final double turnedX = dx * this.cosYaw + dz * this.sinYaw;
        final double turnedZ = -dx * this.sinYaw + dz * this.cosYaw;
        final double tiltedY = dy * this.cosPitch - turnedZ * this.sinPitch;
        final double tiltedZ = dy * this.sinPitch + turnedZ * this.cosPitch;
        return new Point(this.frameMiddleX + turnedX * this.scale, this.frameMiddleY - tiltedY * this.scale,
                tiltedZ * this.scale);
    }

    /** The middle of a part, where its balloon is pinned. */
    public Point middleOf(final GuideBlock.ScenePart part) {
        final double pixel = 1.0 / GuideBlock.ScenePart.PIXELS;
        return this.project(part.x() + (part.fromX() + part.toX()) / 2.0 * pixel,
                part.y() + (part.fromY() + part.toY()) / 2.0 * pixel,
                part.z() + (part.fromZ() + part.toZ()) / 2.0 * pixel);
    }

    /** The smallest box of the frame a part is drawn inside: its eight corners, seen this way. */
    public Bounds bounds(final GuideBlock.ScenePart part) {
        final double pixel = 1.0 / GuideBlock.ScenePart.PIXELS;
        double left = Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            final Point point = this.project(
                    part.x() + ((corner & 1) == 0 ? part.fromX() : part.toX()) * pixel,
                    part.y() + ((corner & 2) == 0 ? part.fromY() : part.toY()) * pixel,
                    part.z() + ((corner & 4) == 0 ? part.fromZ() : part.toZ()) * pixel);
            left = Math.min(left, point.x());
            top = Math.min(top, point.y());
            right = Math.max(right, point.x());
            bottom = Math.max(bottom, point.y());
        }
        return new Bounds(left, top, right, bottom);
    }

    /**
     * A point on the frame.
     *
     * @param x     across, in pixels from the frame's left edge
     * @param y     down, from its top edge
     * @param depth how near the reader, in pixels; more is nearer
     */
    public record Point(double x, double y, double depth) {
    }

    /** A box of the frame, in pixels from its top left corner. */
    public record Bounds(double left, double top, double right, double bottom) {
    }
}
