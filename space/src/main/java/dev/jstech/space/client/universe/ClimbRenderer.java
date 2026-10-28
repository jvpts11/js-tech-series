/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.shaders.FogShape;
import dev.jstech.core.client.sky.GroundCovers;
import dev.jstech.core.client.sky.SkyLayers;
import dev.jstech.core.sky.Skies;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.ClimbView;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws the way up from a world to its space ({@link ClimbView}), behind the world's own blocks: the sky darkening to
 * space over the bent horizon, the stars of space coming out, the system's space with the world's painted sphere, and
 * over it the far ground out to the horizon with the game's clouds, its haze the game's fog, so the loaded blocks melt
 * into it without a seam; and from halfway up, the far map's own picture of the loaded blocks in their place, the game
 * drawing none of them ({@link GroundCovers}).
 */
final class ClimbRenderer {

    /* The sky overhead on the way up turns navy and then black; the haze goes from the game's own to a deep blue. */
    private static final float[] NAVY = {0.03F, 0.05F, 0.14F};
    private static final float[] HAZE_HIGH = {0.23F, 0.37F, 0.66F};
    /* The veil the real ground melts under, and how much it hides at its thickest. */
    private static final float[] VEIL = {0.60F, 0.72F, 0.92F};
    private static final float VEIL_STRENGTH = 0.85F;
    /* How far off the haze is whole: a reach of blocks, longer the higher the eye. */
    private static final double HAZE_REACH = 900;
    private static final double HAZE_PER_BLOCK = 8;
    /* The game's starlight at midnight, as the space's own sky shows its stars. */
    private static final float MIDNIGHT = 0.5F;
    /* From how far up the loaded blocks start being read into their map, before they give way to it. */
    private static final double START_READING = 0.3;

    /* The climb as this frame draws it, worked out as its fog's colour is; none when no one is climbing. */
    @Nullable
    private static Frame frame;

    private ClimbRenderer() {
    }

    /** Hands the game's bus what draws the way up, once, from the mod's client. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, ClimbRenderer::onFogColour);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, ClimbRenderer::onFog);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ClimbRenderer::onStage);
        // From halfway up the game draws none of the loaded blocks, which would stand at the climber's own scale under
        // an eye far higher: their own map stands in for them once read, the far map until then.
        GroundCovers.add(() -> {
            final Frame drawn = frame;
            return drawn != null && drawn.climb().blocksGone();
        });
        // On the way up the loaded ground is drawn however far below it lies, until the far map's picture takes over.
        GroundCovers.reachDown(() -> frame != null);
    }

    /* Works out this frame's climb, and turns the game's fog into the climb's haze. */
    private static void onFogColour(final ViewportEvent.ComputeFogColor event) {
        final ClientLevel level = Minecraft.getInstance().level;
        final Camera camera = event.getCamera();
        final SolarBody body = level != null ? bodyHere(level) : null;
        final Vec3 eye = camera.getPosition();
        if (level == null || body == null || eye.y <= level.getMaxBuildHeight()) {
            frame = null;
            if (level == null || eye.y < level.getMaxBuildHeight() - 1) {
                FarGround.forgetLoaded();
            }
            return;
        }
        final float partial = (float) event.getPartialTick();
        final double width = Math.max(1, level.getWorldBorder().getSize());
        final ClimbView.Climb climb = ClimbView.at(body, SphereMap.longitude(eye.x, width),
                SphereMap.latitude(eye.z, width), eye.y, level.getMaxBuildHeight(), FarGround.sea(level), width);
        final float light = level.getSkyDarken(partial);
        final float rising = (float) Math.min(1, climb.share() * 2);
        final float[] haze = new float[3];
        final float[] fog = {event.getRed(), event.getGreen(), event.getBlue()};
        for (int i = 0; i < haze.length; i++) {
            final float high = HAZE_HIGH[i] * light;
            haze[i] = (float) ((fog[i] + (high - fog[i]) * rising) * (1 - climb.horizon()));
        }
        final Vec3 sky = level.getSkyColor(eye, partial);
        frame = new Frame(level, body, eye, climb, partial, light, haze,
                (float) (HAZE_REACH + climb.eyeHeight() * HAZE_PER_BLOCK),
                new float[] {(float) sky.x, (float) sky.y, (float) sky.z});
        event.setRed(haze[0]);
        event.setGreen(haze[1]);
        event.setBlue(haze[2]);
    }

    /* The loaded blocks are hazed as the far ground is, so they melt into it. */
    private static void onFog(final ViewportEvent.RenderFog event) {
        final Frame drawn = frame;
        if (drawn == null || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        event.setNearPlaneDistance(0F);
        event.setFarPlaneDistance(drawn.hazeEnd());
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    private static void onStage(final RenderLevelStageEvent event) {
        final Frame drawn = frame;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || drawn == null || !SphereDrawing.ready()
                || Minecraft.getInstance().level != drawn.level()) {
            return;
        }
        final ClimbView.Climb climb = drawn.climb();
        final ClientLevel level = drawn.level();
        if (climb.share() >= START_READING && Minecraft.getInstance().player != null) {
            FarGround.gather(level, Minecraft.getInstance().player.blockPosition());
        }
        final Matrix4f view = event.getModelViewMatrix();
        final Matrix4f projection = event.getProjectionMatrix();
        final double time = SphereDrawing.time(level, event);
        final double dip = Math.toDegrees(Math.acos(climb.bend() / (climb.bend() + climb.eyeHeight())));
        final float zenith = (float) climb.zenith();
        final float[] overhead = new float[3];
        for (int i = 0; i < overhead.length; i++) {
            overhead[i] = zenith < 0.5F ? drawn.sky()[i] + (NAVY[i] - drawn.sky()[i]) * zenith * 2
                    : NAVY[i] * (2 - zenith * 2);
        }
        SphereDrawing.begin();
        ClimbSky.draw(climb.up(), dip, drawn.haze(), overhead, (float) climb.skyCover(), view, projection);
        if (climb.stars() > 0) {
            SkyLayers.stars(Skies.of(level, SystemSpaces.TEMPLATE.id()), view, projection, 0, 0,
                    MIDNIGHT * (float) climb.stars());
        }
        final float sun = (float) climb.sun();
        // The same veil over the real ground and over the painted sphere it melts into.
        final float[] veil = {VEIL[0] * drawn.light(), VEIL[1] * drawn.light(), VEIL[2] * drawn.light()};
        SystemSpaceRenderer.drawScene(new Vec3(climb.space()[0], climb.space()[1], climb.space()[2]), time,
                drawn.body(), (float) climb.sphere(), new SphereDrawing.Veil((float) climb.veil() * VEIL_STRENGTH,
                        veil), sun, sun, view, projection);
        if (FarGround.has(level) && climb.ground() > 0) {
            final double[] sunward = SystemLayout.sunward(drawn.body());
            FarGround.draw(level, drawn.eye(), climb, new FarGround.Look(drawn.partialTick(), drawn.light(),
                    drawn.haze(), drawn.hazeEnd(), veil, VEIL_STRENGTH,
                    new float[] {(float) sunward[0], (float) sunward[1], (float) sunward[2]}), view, projection);
        }
        SphereDrawing.end();
    }

    /* The world here, if it is drawn in the Solar System's space. */
    private static @Nullable SolarBody bodyHere(final ClientLevel level) {
        for (final SolarBody body : SolarBody.values()) {
            final boolean here = SpaceWorlds.get(level, SolarSystemWorlds.worldOf(body))
                    .filter(world -> world.dimension().equals(level.dimension())).isPresent();
            if (here) {
                return body;
            }
        }
        return null;
    }

    /**
     * One frame of the climb.
     *
     * @param level        the world climbed out of
     * @param body         its sphere in space
     * @param eye          the climber's eye
     * @param climb        what is seen from it
     * @param partialTick  the share of the next tick gone
     * @param light        the day's light on the blocks
     * @param haze         the haze's colour, the game's fog's
     * @param hazeEnd      how far off the haze is whole, in blocks
     * @param sky          the game's own sky overhead
     */
    private record Frame(ClientLevel level, SolarBody body, Vec3 eye, ClimbView.Climb climb, float partialTick,
                         float light, float[] haze, float hazeEnd, float[] sky) {

        Frame {
            haze = haze.clone();
            sky = sky.clone();
        }
    }
}
