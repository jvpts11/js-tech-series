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
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Draws the Solar System's space: the Overworld, the Moon and Mars as spheres in their places ({@link SystemLayout}),
 * each turning, lit by the Sun from where it stands, over the stars of J's Core's sky; and the Sun, the game's own
 * square, where it stands. Everything is drawn after the sky and under the blocks, the farthest first.
 *
 * <p>The game sees only a few hundred blocks, and the worlds are thousands away, so each is drawn shrunk towards the
 * eye until it fits: a world shrunk with its distance looks the same, and nothing here is tested against depth.
 */
public final class SystemSpaceRenderer {

    /* How far from the eye the farthest point of each body is drawn: within the nearest the game ever sees. */
    private static final double DRAWN_WITHIN = 90;
    /* How much wider than its disc and its air the square a world is drawn on is. */
    private static final double MARGIN = 1.02;
    /* The game's sun is a square half its picture wide in the middle of it. */
    private static final double SUN_PICTURE = 2;
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceKey<Level> SOLAR_SYSTEM_SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
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

    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static VertexBuffer squares;
    @Nullable
    private static VertexBuffer sunSquare;

    private SystemSpaceRenderer() {
    }

    /** Hands the buses what this answers, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "world_sphere"),
                        DefaultVertexFormat.POSITION), loaded -> shader = loaded);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        SpacePayloads.onMapSeed(WorldSpheres::receive);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, SystemSpaceRenderer::onStage);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> WorldSpheres.clear());
    }

    private static void onStage(final RenderLevelStageEvent event) {
        final ClientLevel level = Minecraft.getInstance().level;
        final ShaderInstance spheres = shader;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || level == null || spheres == null
                || !level.dimension().equals(SOLAR_SYSTEM_SPACE)) {
            return;
        }
        final Vec3 eye = event.getCamera().getPosition();
        final long tick = level.getGameTime();
        final double[] sun = SystemLayout.sun();
        final List<Drawn> drawn = new ArrayList<>();
        drawn.add(new Drawn(null, distance(sun, eye) - SystemLayout.SUN_RADIUS));
        for (final SolarBody body : SolarBody.values()) {
            drawn.add(new Drawn(body, distance(SystemLayout.centre(body), eye) - SystemLayout.radius(body)));
        }
        drawn.sort(Comparator.comparingDouble(Drawn::nearest).reversed());
        final Matrix4f view = event.getModelViewMatrix();
        final Matrix4f projection = event.getProjectionMatrix();
        final int[] samplers = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1),
            RenderSystem.getShaderTexture(2)};
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        for (final Drawn each : drawn) {
            final SolarBody body = each.body();
            if (body == null) {
                drawSun(sun, eye, view, projection);
            } else {
                drawWorld(spheres, body, tick, eye, view, projection);
            }
        }
        for (int unit = 0; unit < samplers.length; unit++) {
            RenderSystem.setShaderTexture(unit, samplers[unit]);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /* A world, on the square that covers it, coloured and lit by the shader from its maps. */
    private static void drawWorld(final ShaderInstance spheres, final SolarBody body, final long tick,
                                  final Vec3 eye, final Matrix4f view, final Matrix4f projection) {
        final WorldSpheres.Maps maps = WorldSpheres.of(body);
        if (maps == null) {
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
        final double[] centre = SystemLayout.centre(body);
        final Vector3d at = new Vector3d(centre[0] - eye.x, centre[1] - eye.y, centre[2] - eye.z);
        final double radius = SystemLayout.radius(body);
        final double reach = radius * (1 + look.airThickness() * 1.6) * MARGIN;
        final double away = at.length();
        if (away <= reach) {
            return;
        }
        final double shrink = DRAWN_WITHIN / (away + radius);
        final double[] sunward = SystemLayout.sunward(body);
        final double[] north = SystemLayout.north(body);
        final double[] middle = SystemLayout.middle(body, tick);
        RenderSystem.setShaderTexture(0, maps.colour().getId());
        RenderSystem.setShaderTexture(1, maps.height().getId());
        RenderSystem.setShaderTexture(2, cover != null ? cover.getId() : maps.colour().getId());
        spheres.safeGetUniform("Centre").set((float) (at.x * shrink), (float) (at.y * shrink),
                (float) (at.z * shrink));
        spheres.safeGetUniform("Radius").set((float) (radius * shrink));
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
        // The disc comes solid and the glow of the air already weighed by its own strength.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        final MeshData square = square(at.mul(shrink), reach * shrink, DefaultVertexFormat.POSITION, false);
        if (squares == null) {
            squares = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        draw(squares, square, view, projection, spheres);
    }

    /* The Sun, the game's square, as big as a sphere of the Sun's radius looks from here, added to what is behind. */
    private static void drawSun(final double[] sun, final Vec3 eye, final Matrix4f view, final Matrix4f projection) {
        final Vector3d at = new Vector3d(sun[0] - eye.x, sun[1] - eye.y, sun[2] - eye.z);
        final double away = at.length();
        if (away <= SystemLayout.SUN_RADIUS) {
            return;
        }
        final double shrink = DRAWN_WITHIN / (away + SystemLayout.SUN_RADIUS * SUN_PICTURE);
        RenderSystem.setShaderTexture(0, SUN);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        final MeshData square = square(at.mul(shrink), SystemLayout.SUN_RADIUS * SUN_PICTURE * shrink,
                DefaultVertexFormat.POSITION_TEX, true);
        if (sunSquare == null) {
            sunSquare = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        final ShaderInstance textured = GameRenderer.getPositionTexShader();
        if (textured != null) {
            draw(sunSquare, square, view, projection, textured);
        } else {
            square.close();
        }
    }

    /*
     * A square facing the eye round {@code at} that covers a sphere of {@code radius} there: as wide as the cone from
     * the eye that touches the sphere is where it passes the sphere's middle. With {@code textured}, the square is
     * also a picture, its corners the picture's.
     */
    private static MeshData square(final Vector3d at, final double radius, final VertexFormat format,
                                   final boolean textured) {
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

    private static void draw(final VertexBuffer buffer, final MeshData square, final Matrix4f view,
                             final Matrix4f projection, final ShaderInstance with) {
        buffer.bind();
        buffer.upload(square);
        buffer.drawWithShader(view, projection, with);
        VertexBuffer.unbind();
    }

    private static double distance(final double[] point, final Vec3 eye) {
        final double dx = point[0] - eye.x;
        final double dy = point[1] - eye.y;
        final double dz = point[2] - eye.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /* A body to draw, the Sun when none, and how far its nearest point is from the eye. */
    private record Drawn(@Nullable SolarBody body, double nearest) {
    }

    /* How a world is lit; no dusk and no air where it has no air. */
    private record Look(double relief, double shadow, float[] night, float @Nullable [] dusk, float @Nullable [] air,
                        double airThickness) {
    }
}
