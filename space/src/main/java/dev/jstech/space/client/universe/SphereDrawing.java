/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.space.JsSpace;
import dev.jstech.space.universe.SolarBody;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Draws a world's sphere wherever it is seen from, in the system's space or in another world's sky: a square facing
 * the eye that covers it and its air, on which the world-sphere shader finds the sphere along each pixel's ray, reads
 * its maps and lights it as the approved views light each world. Everything here is called on the render thread,
 * between {@link #begin} and {@link #end}.
 */
final class SphereDrawing {

    /* How far from the eye the farthest point of each world is drawn: within the nearest the game ever sees. */
    static final double DRAWN_WITHIN = 90;
    /* How much wider than its disc and its air the square a world is drawn on is. */
    private static final double MARGIN = 1.02;
    /* How far the Moon's and Mars' heights run from their lowest to their highest, in the units their relief is
     * weighed in: their height maps stretch that from black to white. */
    private static final double MOON_HEIGHTS = 5.614;
    private static final double MARS_HEIGHTS = 4.080;
    /* How each world is lit, as the approved views light it: its relief, the tone its shadows fall to, its night,
     * its dusk and its air, if any. The relief is per step from black to white of its height map. */
    private static final Look OVERWORLD = new Look(0.010, 0.45, new float[] {0.010F, 0.015F, 0.035F},
            new float[] {0.90F, 0.46F, 0.24F}, new float[] {0.42F, 0.62F, 1.0F}, 0.016);
    private static final Look MOON = new Look(0.007 * MOON_HEIGHTS, 0.14, new float[] {0.012F, 0.012F, 0.016F},
            null, null, 0);
    private static final Look MARS = new Look(0.015 * MARS_HEIGHTS, 0.30, new float[] {0.020F, 0.010F, 0.008F},
            new float[] {0.55F, 0.62F, 0.85F}, new float[] {0.80F, 0.58F, 0.46F}, 0.010);
    private static final int SAMPLERS = 3;
    /* The textures the game had bound where the worlds' maps go, given back once they are drawn. */
    private static final int[] SAVED = new int[SAMPLERS];

    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static VertexBuffer squares;

    private SphereDrawing() {
    }

    /** Hands the mod's bus the world-sphere shader to build, once, from the mod's client. */
    static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "world_sphere"),
                        DefaultVertexFormat.POSITION), loaded -> shader = loaded);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** Whether worlds can be drawn: the shader is built. */
    static boolean ready() {
        return shader != null;
    }

    /** Readies the card to draw worlds over the sky and under the blocks, keeping what the game had bound. */
    static void begin() {
        for (int unit = 0; unit < SAMPLERS; unit++) {
            SAVED[unit] = RenderSystem.getShaderTexture(unit);
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
    }

    /** Gives the card back as the game had it. */
    static void end() {
        for (int unit = 0; unit < SAMPLERS; unit++) {
            RenderSystem.setShaderTexture(unit, SAVED[unit]);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /**
     * Draws {@code body} at {@code tick}, its middle {@code at} from the eye and as wide as {@code radius}, both
     * already brought within {@link #DRAWN_WITHIN}; its pole and its map's middle pointing {@code north} and
     * {@code middle}, lit from {@code sunward}, its texels at least {@code texelPixels} pixels wide. Not drawn until
     * its maps are on the card.
     */
    static void draw(final SolarBody body, final long tick, final Vector3d at, final double radius,
                     final double[] north, final double[] middle, final double[] sunward, final float texelPixels,
                     final Matrix4f view, final Matrix4f projection) {
        final ShaderInstance spheres = shader;
        final WorldSpheres.Maps maps = WorldSpheres.of(body);
        if (spheres == null || maps == null) {
            return;
        }
        final SphereTexture cover = body == SolarBody.OVERWORLD ? WorldSpheres.clouds(tick) : null;
        if (body == SolarBody.OVERWORLD && cover == null) {
            // Not without its clouds, which are drawn a moment after its ground.
            return;
        }
        final Look look = switch (body) {
            case OVERWORLD -> OVERWORLD;
            case MOON -> MOON;
            case MARS -> MARS;
        };
        final double reach = radius * (1 + look.airThickness() * 1.6) * MARGIN;
        if (at.length() <= reach) {
            return;
        }
        RenderSystem.setShaderTexture(0, maps.colour().getId());
        RenderSystem.setShaderTexture(1, maps.height().getId());
        RenderSystem.setShaderTexture(2, cover != null ? cover.getId() : maps.colour().getId());
        spheres.safeGetUniform("Centre").set((float) at.x, (float) at.y, (float) at.z);
        spheres.safeGetUniform("Radius").set((float) radius);
        spheres.safeGetUniform("North").set((float) north[0], (float) north[1], (float) north[2]);
        spheres.safeGetUniform("Middle").set((float) middle[0], (float) middle[1], (float) middle[2]);
        spheres.safeGetUniform("SunDir").set((float) sunward[0], (float) sunward[1], (float) sunward[2]);
        spheres.safeGetUniform("Relief").set((float) look.relief());
        spheres.safeGetUniform("Shadow").set((float) look.shadow());
        spheres.safeGetUniform("Night").set(look.night()[0], look.night()[1], look.night()[2]);
        final float[] dusk = look.dusk() != null ? look.dusk() : new float[3];
        spheres.safeGetUniform("Dusk").set(dusk[0], dusk[1], dusk[2]);
        spheres.safeGetUniform("HasDusk").set(look.dusk() != null ? 1 : 0);
        final float[] air = look.air() != null ? look.air() : new float[3];
        spheres.safeGetUniform("Air").set(air[0], air[1], air[2]);
        spheres.safeGetUniform("AirThickness").set((float) look.airThickness());
        spheres.safeGetUniform("HasClouds").set(cover != null ? 1 : 0);
        spheres.safeGetUniform("TexelPixels").set(texelPixels);
        // The disc comes solid and the glow of the air already weighed by its own strength.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (squares == null) {
            squares = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        draw(squares, square(at, reach, DefaultVertexFormat.POSITION, false), view, projection, spheres);
    }

    /*
     * A square facing the eye round {@code at} that covers a sphere of {@code radius} there: as wide as the cone from
     * the eye that touches the sphere is where it passes the sphere's middle. With {@code textured}, the square is
     * a picture {@code radius} wide each way from its middle, its corners the picture's.
     */
    static MeshData square(final Vector3d at, final double radius, final VertexFormat format, final boolean textured) {
        final double away = at.length();
        final double half = textured ? radius : radius * away / Math.sqrt(away * away - radius * radius);
        final Vector3d forward = new Vector3d(at).normalize();
        final Vector3d helper = Math.abs(forward.y) > 0.9 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        final Vector3d right = new Vector3d(forward).cross(helper).normalize().mul(half);
        final Vector3d up = new Vector3d(right).cross(forward).normalize().mul(half);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        final double[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (final double[] corner : corners) {
            final float x = (float) (at.x + right.x * corner[0] + up.x * corner[1]);
            final float y = (float) (at.y + right.y * corner[0] + up.y * corner[1]);
            final float z = (float) (at.z + right.z * corner[0] + up.z * corner[1]);
            if (textured) {
                builder.addVertex(x, y, z).setUv((float) (corner[0] + 1) / 2, (float) (1 - corner[1]) / 2);
            } else {
                builder.addVertex(x, y, z);
            }
        }
        return builder.buildOrThrow();
    }

    /** Puts {@code square} in {@code buffer} and draws it with {@code with}. */
    static void draw(final VertexBuffer buffer, final MeshData square, final Matrix4f view, final Matrix4f projection,
                     final ShaderInstance with) {
        buffer.bind();
        buffer.upload(square);
        buffer.drawWithShader(view, projection, with);
        VertexBuffer.unbind();
    }

    /* How a world is lit; no dusk and no air where it has no air. */
    private record Look(double relief, double shadow, float[] night, float @Nullable [] dusk, float @Nullable [] air,
                        double airThickness) {
    }
}
