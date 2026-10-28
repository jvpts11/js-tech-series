/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkySpec;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * How a kind of place drawn as {@link Skies#EFFECTS} looks: the overworld's ground and light, with the sky, the clouds,
 * the sunsets and the day's light of its dimension's {@link SkySpec}.
 */
public final class SkyEffects extends DimensionSpecialEffects {

    private static final float BRIGHT = 0.94F;
    private static final float BLUE_BRIGHT = 0.91F;

    public SkyEffects() {
        super(OverworldEffects.CLOUD_LEVEL, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(final Vec3 fogColour, final float brightness) {
        return fogColour.multiply(brightness * BRIGHT + (1 - BRIGHT), brightness * BRIGHT + (1 - BRIGHT),
                brightness * BLUE_BRIGHT + (1 - BLUE_BRIGHT));
    }

    @Override
    public boolean isFoggyAt(final int x, final int y) {
        return false;
    }

    /* The game's sunrise, in the colour of the sky's sunsets when it has its own. */
    @Override
    public float @Nullable [] getSunriseColor(final float timeOfDay, final float partialTicks) {
        final float[] game = super.getSunriseColor(timeOfDay, partialTicks);
        final SkySpec sky = sky();
        if (game == null || sky == null || sky.sunsetColour().isEmpty()) {
            return game;
        }
        final float[] colour = SkyRenderer.rgb(sky.sunsetColour().get());
        return new float[] {colour[0], colour[1], colour[2], game[3]};
    }

    @Override
    public float getCloudHeight() {
        final SkySpec sky = sky();
        return sky == null ? super.getCloudHeight() : sky.clouds().map(clouds -> (float) clouds.height())
                .orElseGet(super::getCloudHeight);
    }

    @Override
    public boolean renderSky(final ClientLevel level, final int ticks, final float partialTick,
                             final Matrix4f modelView, final Camera camera, final Matrix4f projection,
                             final boolean foggy, final Runnable setupFog) {
        return SkyRenderer.render(level, Skies.of(level), ticks, partialTick, modelView, camera, projection, foggy,
                setupFog);
    }

    @Override
    public boolean renderClouds(final ClientLevel level, final int ticks, final float partialTick,
                                final PoseStack pose, final double camX, final double camY, final double camZ,
                                final Matrix4f modelView, final Matrix4f projection) {
        final SkySpec sky = Skies.of(level);
        return sky.clouds().isPresent() && SkyClouds.render(level, sky.clouds().get(), ticks, partialTick, camX, camY,
                camZ, modelView, projection);
    }

    /*
     * The day's light from the sky: on a round world as light as the hour and the latitude make it where the player
     * is; dimmer with its sun's distance and in an eclipse; tinted by its suns.
     */
    @Override
    public void adjustLightmapColors(final ClientLevel level, final float partialTicks, final float skyDarken,
                                     final float blockLightRedFlicker, final float skyLight, final int pixelX,
                                     final int pixelY, final Vector3f colors) {
        final SkySpec sky = Skies.of(level);
        final SkyPlace place = SkyPlace.of(level, sky);
        float darkening = skyDarken;
        if (sky.round()) {
            darkening = place.dayLight(level, partialTicks);
            SkySight.relight(colors, LightTexture.getBrightness(level.dimensionType(), pixelY), skyDarken,
                    darkening);
        }
        final float light = (float) (Math.min(1.0, SkySight.sunlight(sky))
                * SkySight.eclipseLight(level, sky, place.celestial()));
        SkySight.adjust(colors, pixelX, pixelY, Math.max(0F, Math.min(1F, darkening)), light, SkySight.tint(sky));
    }

    /* The sky of the level the player stands in, if they stand in one. */
    private static @Nullable SkySpec sky() {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : Skies.of(level);
    }
}
