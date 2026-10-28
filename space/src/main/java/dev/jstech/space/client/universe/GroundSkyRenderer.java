/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import dev.jstech.core.client.sky.SkyPlace;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.GroundSky;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Draws the Overworld in the Moon's sky, standing still over the Moon's near side: high over the places near the middle
 * of the Moon's map, lower the farther one goes, on the horizon a quarter of the way round and hidden on the far side
 * ({@link GroundSky}). It is the same sphere as in space, its continents, poles and clouds turned as they are there,
 * lit by the Sun where J's Core's sky puts it at the place, so its phases come of the Moon's hour; its texels are about
 * as wide as those of the game's own moon.
 */
final class GroundSkyRenderer {

    /* The Overworld in the Moon's sky keeps texels about as wide as the game's moon's: two pixels and more. */
    private static final float SKY_TEXEL_PIXELS = 2F;
    private static final double TURN_DEGREES = 360;

    private GroundSkyRenderer() {
    }

    static void onStage(final RenderLevelStageEvent event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || level == null || !SphereDrawing.ready()
                || !onTheMoon(level)) {
            return;
        }
        final Vec3 eye = event.getCamera().getPosition();
        final WorldBorder border = level.getWorldBorder();
        final double size = Math.max(1, border.getSize());
        final double longitude = SkyMath.longitude(eye.x, border.getCenterX(), size) * TURN_DEGREES;
        final double latitude = SkyMath.latitude(eye.z, border.getCenterZ(), size);
        final double tick = SphereDrawing.time(level, event);
        final double[] towards = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, longitude, latitude, tick);
        final double radius = GroundSky.radius(SolarBody.MOON, SolarBody.OVERWORLD);
        if (towards[1] < -Math.sin(radius)) {
            // Wholly below the horizon: the far side of the Moon never sees it.
            return;
        }
        final double[] north = GroundSky.seen(SolarBody.MOON, longitude, latitude, tick,
                SystemLayout.north(SolarBody.OVERWORLD));
        final double[] middle = GroundSky.seen(SolarBody.MOON, longitude, latitude, tick,
                SystemLayout.middle(SolarBody.OVERWORLD, tick));
        final SkySpec sky = Skies.of(level);
        final Vector3f sun = SkyPlace.of(level, sky).sun(sky.suns().isEmpty() ? 0 : sky.suns().getFirst().offset());
        // Its farthest point within reach of the eye, so the game draws it, however far it is.
        final double distance = SphereDrawing.DRAWN_WITHIN / (1 + Math.sin(radius));
        SphereDrawing.begin();
        SphereDrawing.draw(SolarBody.OVERWORLD, tick, new Vector3d(towards[0], towards[1], towards[2]).mul(distance),
                distance * Math.sin(radius), north, middle, new double[] {sun.x, sun.y, sun.z}, SKY_TEXEL_PIXELS,
                1F, SphereDrawing.Veil.NONE, event.getModelViewMatrix(), event.getProjectionMatrix());
        SphereDrawing.end();
    }

    private static boolean onTheMoon(final ClientLevel level) {
        return SpaceWorlds.get(level, SolarSystemWorlds.MOON).map(SpaceWorld::dimension)
                .filter(level.dimension()::equals).isPresent();
    }
}
