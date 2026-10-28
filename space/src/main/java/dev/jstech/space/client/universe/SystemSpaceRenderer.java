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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
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

    /* The game's sun is a square half its picture wide in the middle of it. */
    private static final double SUN_PICTURE = 2;
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceKey<Level> SOLAR_SYSTEM_SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
    /* In space a texel is never drawn smaller than a pixel. */
    private static final float TEXEL_PIXELS = 1F;

    @Nullable
    private static VertexBuffer sunSquare;

    private SystemSpaceRenderer() {
    }

    /** Hands the buses what draws the worlds, in space and in the skies, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        SphereDrawing.listen(modBus);
        SpacePayloads.onMapSeed(WorldSpheres::receive);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, SystemSpaceRenderer::onStage);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, GroundSkyRenderer::onStage);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> WorldSpheres.clear());
    }

    private static void onStage(final RenderLevelStageEvent event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || level == null || !SphereDrawing.ready()
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
        SphereDrawing.begin();
        for (final Drawn each : drawn) {
            final SolarBody body = each.body();
            if (body == null) {
                drawSun(sun, eye, view, projection);
            } else {
                drawWorld(body, tick, eye, view, projection);
            }
        }
        SphereDrawing.end();
    }

    /* A world where it stands, brought within reach of the eye. */
    private static void drawWorld(final SolarBody body, final long tick, final Vec3 eye, final Matrix4f view,
                                  final Matrix4f projection) {
        final double[] centre = SystemLayout.centre(body);
        final Vector3d at = new Vector3d(centre[0] - eye.x, centre[1] - eye.y, centre[2] - eye.z);
        final double radius = SystemLayout.radius(body);
        final double shrink = SphereDrawing.DRAWN_WITHIN / (at.length() + radius);
        SphereDrawing.draw(body, tick, at.mul(shrink), radius * shrink, SystemLayout.north(body),
                SystemLayout.middle(body, tick), SystemLayout.sunward(body), TEXEL_PIXELS, view, projection);
    }

    /* The Sun, the game's square, as big as a sphere of the Sun's radius looks from here, added to what is behind. */
    private static void drawSun(final double[] sun, final Vec3 eye, final Matrix4f view, final Matrix4f projection) {
        final Vector3d at = new Vector3d(sun[0] - eye.x, sun[1] - eye.y, sun[2] - eye.z);
        final double away = at.length();
        if (away <= SystemLayout.SUN_RADIUS) {
            return;
        }
        final double shrink = SphereDrawing.DRAWN_WITHIN / (away + SystemLayout.SUN_RADIUS * SUN_PICTURE);
        RenderSystem.setShaderTexture(0, SUN);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        final MeshData square = SphereDrawing.square(at.mul(shrink), SystemLayout.SUN_RADIUS * SUN_PICTURE * shrink,
                DefaultVertexFormat.POSITION_TEX, true);
        if (sunSquare == null) {
            sunSquare = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        final ShaderInstance textured = GameRenderer.getPositionTexShader();
        if (textured != null) {
            SphereDrawing.draw(sunSquare, square, view, projection, textured);
        } else {
            square.close();
        }
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
}
