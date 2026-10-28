/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Draws the planets and moons of a sky. Each is a sphere seen from the ground: lit from where its first sun stands, so
 * its phases come of the geometry (a thin crescent beside the sun, full opposite it), its surface map wrapped round it
 * and turning, its dark side the colour of the sky behind, hiding the stars and, passing over a sun, eclipsing it.
 * Rings are a flat band round it, leaning as they say, their far half behind it.
 */
final class SkyBodies {

    private static final float DISTANCE = 100F;
    private static final float MOON_HALF = 20F;
    private static final int RINGS = 10;
    private static final int SEGMENTS = 32;
    private static final int RING_SEGMENTS = 64;
    private static final float AMBIENT = 0.03F;
    /* How bright a point shines beside the stars: twice, as a planet outshines them. */
    private static final float POINT_SHINE = 2.0F;
    /* How quickly a face turned towards the sun goes solid: a soft terminator, then the whole surface. */
    private static final double TERMINATOR = 3.0;
    /* The ring's bands, from its inner edge out: how much of its colour each holds. */
    private static final float[] BANDS = {0.55F, 1.0F, 0.35F, 0.8F};
    private static final Set<ResourceLocation> WRAPPED = new HashSet<>();

    private SkyBodies() {
    }

    /**
     * Draws every body of {@code sky}, the sky's own colour behind them being {@code skyColour}, the points among them
     * as bright as the stars are, {@code night}.
     */
    static void render(final ClientLevel level, final SkySpec sky, final PoseStack pose, final float celestial,
                       final Vec3 skyColour, final float clear, final float night) {
        if (sky.bodies().isEmpty()) {
            return;
        }
        final double days = level.getDayTime() / SkyMath.DAY;
        final double[] sun = SkyMath.direction(celestial + (sky.suns().isEmpty() ? 0 : sky.suns().getFirst()
                .offset()), 0);
        for (final SkySpec.Body body : sky.bodies()) {
            final double turns = celestial + SkyMath.drift(body.offset(), body.orbitDays(), days);
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees((float) body.tilt()));
            pose.mulPose(Axis.XP.rotationDegrees((float) (turns * 360.0)));
            // A lump's disc is shorter than it is wide; it is lit as the sphere it is drawn over.
            pose.scale(1.0F, 1.0F, (float) body.squash());
            final Matrix4f matrix = pose.last().pose();
            final float half = (float) (MOON_HALF * body.size());
            if (body.point()) {
                point(matrix, body, half, night);
            } else {
                body.rings().ifPresent(rings -> rings(matrix, rings, half, false, clear));
                base(matrix, half, skyColour);
                lit(matrix, body, half, sun, turns, SkyMath.turned(body.turnDays(), days), clear);
                body.rings().ifPresent(rings -> rings(matrix, rings, half, true, clear));
            }
            pose.popPose();
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /* A body too far to show a disc: a square of its colour, added to the sky as a star is, as bright as they are. */
    private static void point(final Matrix4f matrix, final SkySpec.Body body, final float half, final float night) {
        if (night <= 0) {
            return;
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final float[] colour = SkyRenderer.rgb(body.colour());
        final float shine = Math.min(1F, night * POINT_SHINE);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, -half, DISTANCE, -half).setColor(colour[0], colour[1], colour[2], shine);
        builder.addVertex(matrix, half, DISTANCE, -half).setColor(colour[0], colour[1], colour[2], shine);
        builder.addVertex(matrix, half, DISTANCE, half).setColor(colour[0], colour[1], colour[2], shine);
        builder.addVertex(matrix, -half, DISTANCE, half).setColor(colour[0], colour[1], colour[2], shine);
        RenderSystem.disableCull();
        SkyRenderer.drawNow(builder);
        RenderSystem.enableCull();
    }

    /* The body's whole disc in the colour of the sky behind: its dark side, hiding what lies beyond. */
    private static void base(final Matrix4f matrix, final float half, final Vec3 skyColour) {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
                DefaultVertexFormat.POSITION_COLOR);
        final float red = (float) skyColour.x;
        final float green = (float) skyColour.y;
        final float blue = (float) skyColour.z;
        builder.addVertex(matrix, 0F, DISTANCE, 0F).setColor(red, green, blue, 1F);
        for (int i = 0; i <= SEGMENTS; i++) {
            final float angle = i * Mth.TWO_PI / SEGMENTS;
            builder.addVertex(matrix, Mth.cos(angle) * half, DISTANCE, Mth.sin(angle) * half)
                    .setColor(red, green, blue, 1F);
        }
        SkyRenderer.drawNow(builder);
    }

    /*
     * The lit side, laid over the dark: a grid over the disc, each point a point of the sphere facing the ground, as
     * solid as its face turns to the sun, its surface map read where the turning sphere puts it; so in daylight its
     * colours show against the sky rather than washing out, and its dark side is the sky's.
     */
    private static void lit(final Matrix4f matrix, final SkySpec.Body body, final float half, final double[] sun,
                            final double turns, final double turned, final float clear) {
        RenderSystem.defaultBlendFunc();
        final double angle = turns * SkyMath.TURN;
        final double tilt = Math.toRadians(body.tilt());
        // The body's own axes in the sky: across its disc, up its disc, and from it to the ground.
        final double[] across = {Math.cos(tilt), Math.sin(tilt), 0};
        final double[] up = {Math.sin(angle) * Math.sin(tilt), -Math.sin(angle) * Math.cos(tilt), Math.cos(angle)};
        final double[] out = SkyMath.direction(turns, body.tilt());
        final float[] tint = SkyRenderer.rgb(body.colour());
        final boolean mapped = body.texture().isPresent();
        if (mapped) {
            final ResourceLocation texture = body.texture().get();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture);
            wrapAround(texture);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                mapped ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
        for (int ring = 0; ring < RINGS; ring++) {
            for (int segment = 0; segment < SEGMENTS; segment++) {
                final double[][] corners = {
                    point(ring, segment), point(ring + 1, segment), point(ring + 1, segment + 1),
                    point(ring, segment), point(ring + 1, segment + 1), point(ring, segment + 1)};
                for (int k = 0; k < corners.length; k += 3) {
                    triangle(builder, matrix, mapped, half, corners[k], corners[k + 1], corners[k + 2], across, up,
                            out, sun, turned, tint, clear);
                }
            }
        }
        SkyRenderer.drawNow(builder);
    }

    /* A point of the disc on a polar grid: {x, z}, each -1 to 1. */
    private static double[] point(final int ring, final int segment) {
        final double radius = (double) ring / RINGS;
        final double angle = segment * SkyMath.TURN / SEGMENTS;
        return new double[] {radius * Math.cos(angle), radius * Math.sin(angle)};
    }

    private static void triangle(final BufferBuilder builder, final Matrix4f matrix, final boolean mapped,
                                 final float half, final double[] a, final double[] b, final double[] c,
                                 final double[] across, final double[] up, final double[] out, final double[] sun,
                                 final double turned, final float[] tint, final float clear) {
        final double ua = longitude(a, turned);
        double ub = longitude(b, turned);
        double uc = longitude(c, turned);
        // Keep a triangle on one side of the map's seam: the texture wraps round.
        ub += Math.rint(ua - ub);
        uc += Math.rint(ua - uc);
        corner(builder, matrix, mapped, half, a, ua, across, up, out, sun, tint, clear);
        corner(builder, matrix, mapped, half, b, ub, across, up, out, sun, tint, clear);
        corner(builder, matrix, mapped, half, c, uc, across, up, out, sun, tint, clear);
    }

    private static void corner(final BufferBuilder builder, final Matrix4f matrix, final boolean mapped,
                               final float half, final double[] at, final double u, final double[] across,
                               final double[] up, final double[] out, final double[] sun, final float[] tint,
                               final float clear) {
        final double x = at[0];
        final double z = at[1];
        final double depth = Math.sqrt(Math.max(0, 1 - x * x - z * z));
        // The sphere's face here, turned towards the ground, and how squarely it faces the sun.
        final double lit = Math.max(0, (x * across[0] + z * up[0] - depth * out[0]) * sun[0]
                + (x * across[1] + z * up[1] - depth * out[1]) * sun[1]
                + (x * across[2] + z * up[2] - depth * out[2]) * sun[2]);
        // Fully lit faces show the surface whole; the terminator softens into the dark side, the sky's colour.
        final float shade = (float) Math.min(1, AMBIENT + lit * TERMINATOR) * clear;
        final float v = (float) (0.5 - Math.asin(Math.max(-1, Math.min(1, z))) / Math.PI);
        final float light = (float) Math.min(1, AMBIENT + lit);
        if (mapped) {
            builder.addVertex(matrix, (float) x * half, DISTANCE, (float) z * half).setUv((float) u, v)
                    .setColor(tint[0] * light, tint[1] * light, tint[2] * light, shade);
        } else {
            builder.addVertex(matrix, (float) x * half, DISTANCE, (float) z * half)
                    .setColor(tint[0] * light, tint[1] * light, tint[2] * light, shade);
        }
    }

    /* Where on the surface map a point of the disc shows, across: the half of it facing the ground, turned. */
    private static double longitude(final double[] at, final double turned) {
        final double x = at[0];
        final double z = at[1];
        final double depth = Math.sqrt(Math.max(0, 1 - x * x - z * z));
        return 0.5 + Math.atan2(x, depth) / SkyMath.TURN + turned;
    }

    /* Half of the rings, the far half or the near, in their bands, laid over the sky in their own colour. */
    private static void rings(final Matrix4f matrix, final SkySpec.Rings rings, final float half, final boolean near,
                              final float clear) {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final float[] colour = SkyRenderer.rgb(rings.colour());
        final double lean = Math.toRadians(rings.tilt());
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int band = 0; band < BANDS.length; band++) {
            final double inner = rings.inner() + (rings.outer() - rings.inner()) * band / BANDS.length;
            final double outer = rings.inner() + (rings.outer() - rings.inner()) * (band + 1) / BANDS.length;
            final float alpha = (float) (rings.opacity() * BANDS[band]) * clear;
            for (int i = 0; i < RING_SEGMENTS; i++) {
                final double from = i * SkyMath.TURN / RING_SEGMENTS;
                final double to = (i + 1) * SkyMath.TURN / RING_SEGMENTS;
                // Towards the ground is the near half: the half whose depth is below the body's middle.
                if ((Math.sin((from + to) / 2) * Math.cos(lean) < 0) != near) {
                    continue;
                }
                ringVertex(builder, matrix, inner * half, from, lean, colour, alpha);
                ringVertex(builder, matrix, outer * half, from, lean, colour, alpha);
                ringVertex(builder, matrix, outer * half, to, lean, colour, alpha);
                ringVertex(builder, matrix, inner * half, to, lean, colour, alpha);
            }
        }
        SkyRenderer.drawNow(builder);
    }

    /*
     * A point of the rings' plane at {@code radius} and {@code angle} round: across the disc, and between the depth and
     * the disc's up as far as the rings lean towards whoever looks.
     */
    private static void ringVertex(final BufferBuilder builder, final Matrix4f matrix, final double radius,
                                   final double angle, final double lean, final float[] colour, final float alpha) {
        final double x = radius * Math.cos(angle);
        final double along = radius * Math.sin(angle);
        builder.addVertex(matrix, (float) x, (float) (DISTANCE + along * Math.cos(lean)),
                (float) (along * Math.sin(lean))).setColor(colour[0], colour[1], colour[2], alpha);
    }

    /* Has a surface map repeat across, so a map wrapped round a turning sphere has no seam. */
    private static void wrapAround(final ResourceLocation texture) {
        Minecraft.getInstance().getTextureManager().getTexture(texture).bind();
        if (WRAPPED.add(texture)) {
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        }
    }
}
