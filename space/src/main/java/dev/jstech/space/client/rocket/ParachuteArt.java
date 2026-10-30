/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

/**
 * A capsule's three main parachutes, open over it where there is air, as the approved look draws them: three domes of
 * orange and white gores tilted a little apart, their shroud lines running down to one point over the capsule and a
 * single riser from there to its top. Drawn in the capsule's own frame, its foot's middle at the origin.
 */
@PaletteHolder
public final class ParachuteArt {

    /** The parachutes' colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "parachutes", new Colours(
            0xFFD8641E, 0xFFE4E2DA, 0xFFD8D0B8));
    private static final int CANOPIES = 3;
    private static final int GORES = 16;
    private static final int RINGS = 6;
    /* For a capsule three blocks wide: how far over its top the lines meet, how far over them the domes stand. */
    private static final double MEET = 7;
    private static final double DOMES = 6;
    /* How far apart the domes stand, how wide each is, how far down its hem reaches and how far it leans out. */
    private static final double APART = 2.4;
    private static final double RADIUS = 2.8;
    private static final double HEM = Math.toRadians(72);
    private static final double LEAN = Math.toRadians(16);
    private static final double WIDTH = 3;
    /* How much darker each ring down the dome is, its inside with the sky through it, and how dark the night is. */
    private static final float RING_SHADE = 0.07F;
    private static final float INSIDE = 0.92F;
    private static final float NIGHT = 0.35F;
    private static final int LINES = 8;

    private ParachuteArt() {
    }

    /**
     * Draws the parachutes over a capsule whose top stands {@code top} blocks over the foot and which is {@code width}
     * blocks wide, as bright as the sky is, {@code daylight} from 0 at night to 1 at noon: high in the open air, the
     * light round the capsule's own blocks says nothing of theirs.
     */
    public static void draw(final PoseStack poses, final MultiBufferSource buffers, final double top,
                            final double width, final float daylight) {
        final Colours c = PALETTE.get();
        final double scale = width / WIDTH;
        final double meet = top + MEET * scale;
        final float lit = NIGHT + (1 - NIGHT) * Math.clamp(daylight, 0F, 1F);
        final PoseStack.Pose pose = poses.last();
        final VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        final double[][] hems = new double[CANOPIES][];
        for (int canopy = 0; canopy < CANOPIES; canopy++) {
            final double around = Math.PI / 2 + canopy * 2 * Math.PI / CANOPIES;
            final double[] centre = {Math.cos(around) * APART * scale, meet + DOMES * scale,
                Math.sin(around) * APART * scale};
            hems[canopy] = dome(quads, pose, centre, around, RADIUS * scale, c, lit);
        }
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (final double[] hem : hems) {
            for (int line = 0; line < LINES; line++) {
                line(lines, pose, hem[line * 3], hem[line * 3 + 1], hem[line * 3 + 2], 0, meet, 0, c.lines());
            }
        }
        line(lines, pose, 0, meet, 0, 0, top, 0, c.lines());
    }

    /*
     * A dome over {@code centre}, leaning out toward {@code around}, its gores in turn orange and white, each ring down
     * a little darker outside; the points of its hem the lines hang from, as x, y, z in a row.
     */
    private static double[] dome(final VertexConsumer quads, final PoseStack.Pose pose, final double[] centre,
                                 final double around, final double radius, final Colours c, final float lit) {
        final double[][][] at = new double[RINGS + 1][GORES + 1][];
        for (int ring = 0; ring <= RINGS; ring++) {
            final double down = HEM * ring / RINGS;
            for (int gore = 0; gore <= GORES; gore++) {
                final double turn = 2 * Math.PI * gore / GORES;
                at[ring][gore] = leaned(Math.sin(down) * Math.cos(turn) * radius, Math.cos(down) * radius - radius,
                        Math.sin(down) * Math.sin(turn) * radius, around, centre);
            }
        }
        for (int ring = 0; ring < RINGS; ring++) {
            final float shade = lit * (1 - RING_SHADE * ring);
            for (int gore = 0; gore < GORES; gore++) {
                final int cloth = gore % 2 == 0 ? c.orange() : c.white();
                final int outside = shaded(cloth, shade);
                // The same cloth seen from under the dome, the sky shining through it.
                final int inside = shaded(cloth, lit * INSIDE);
                quad(quads, pose, at[ring][gore], at[ring + 1][gore], at[ring + 1][gore + 1], at[ring][gore + 1],
                        outside);
                quad(quads, pose, at[ring][gore + 1], at[ring + 1][gore + 1], at[ring + 1][gore], at[ring][gore],
                        inside);
            }
        }
        final double[] hem = new double[LINES * 3];
        for (int line = 0; line < LINES; line++) {
            final double[] point = at[RINGS][line * GORES / LINES];
            hem[line * 3] = point[0];
            hem[line * 3 + 1] = point[1];
            hem[line * 3 + 2] = point[2];
        }
        return hem;
    }

    /* A point of a dome round its top, leaned out toward {@code around} and set over {@code centre}. */
    private static double[] leaned(final double x, final double y, final double z, final double around,
                                   final double[] centre) {
        // Leaned about the level line square to the way out: the way out tips down, up tips out.
        final double outX = Math.cos(around);
        final double outZ = Math.sin(around);
        final double along = x * outX + z * outZ;
        final double across = -x * outZ + z * outX;
        final double tippedAlong = along * Math.cos(LEAN) - y * Math.sin(LEAN);
        final double tippedY = along * Math.sin(LEAN) + y * Math.cos(LEAN);
        return new double[] {centre[0] + tippedAlong * outX - across * outZ, centre[1] + tippedY,
            centre[2] + tippedAlong * outZ + across * outX};
    }

    private static void quad(final VertexConsumer quads, final PoseStack.Pose pose, final double[] a,
                             final double[] b, final double[] d, final double[] e, final int colour) {
        for (final double[] point : new double[][] {a, b, d, e}) {
            quads.addVertex(pose, (float) point[0], (float) point[1], (float) point[2]).setColor(colour);
        }
    }

    private static void line(final VertexConsumer lines, final PoseStack.Pose pose, final double x0, final double y0,
                             final double z0, final double x1, final double y1, final double z1, final int colour) {
        final float dx = (float) (x1 - x0);
        final float dy = (float) (y1 - y0);
        final float dz = (float) (z1 - z0);
        final float length = Math.max(1.0e-6F, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        lines.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(colour).setNormal(pose, dx / length,
                dy / length, dz / length);
        lines.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(colour).setNormal(pose, dx / length,
                dy / length, dz / length);
    }

    /* {@code argb} darkened to {@code shade}, opaque. */
    private static int shaded(final int argb, final float shade) {
        return FastColor.ARGB32.color(0xFF, (int) (FastColor.ARGB32.red(argb) * shade),
                (int) (FastColor.ARGB32.green(argb) * shade), (int) (FastColor.ARGB32.blue(argb) * shade));
    }

    /**
     * The parachutes' colours.
     *
     * @param orange the orange gores
     * @param white  the white gores
     * @param lines  the shroud lines and the riser
     */
    public record Colours(int orange, int white, int lines) {
    }
}
