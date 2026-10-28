/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Where a sky is seen from: how far round the sky the sun stands there and how far north the place is. On a round
 * world ({@link SkySpec#round}) the hour follows the longitude, later the farther east, and the sky's pole stands as
 * high over the north as the place's latitude, so the sun climbs less the nearer the poles; anywhere else the hour is
 * the clock's and the sun passes overhead, as the game's does. How light the day is, how bright the stars shine and
 * where a sun lies all follow from the sun's height at the place.
 *
 * @param celestial how far round the sky the first sun stands, in turns: 0 at noon
 * @param latitude  how far north the place is, in degrees; 0 where the world is not round
 */
public record SkyPlace(double celestial, double latitude) {

    /** Where the camera sees {@code level}'s sky from now. */
    public static SkyPlace of(final ClientLevel level, final SkySpec sky) {
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return at(level, sky, eye.x, eye.z);
    }

    /** Where {@code level}'s sky is seen from at ({@code x}, {@code z}). */
    public static SkyPlace at(final ClientLevel level, final SkySpec sky, final double x, final double z) {
        if (!sky.round()) {
            return new SkyPlace(level.getTimeOfDay(0F), 0);
        }
        final WorldBorder border = level.getWorldBorder();
        final double size = Math.max(1, border.getSize());
        final double longitude = SkyMath.longitude(x, border.getCenterX(), size);
        return new SkyPlace(SkyMath.celestial(SkyMath.localTime(level.getDayTime(), longitude)),
                SkyMath.latitude(z, border.getCenterZ(), size));
    }

    /** How high the first sun stands, as the sine of its height: 1 overhead, below 0 under the ground. */
    public double sunHeight() {
        return SkyMath.sunHeight(this.celestial, this.latitude);
    }

    /** Where a sun {@code offset} turns round the sky from the first lies, in the world's directions. */
    public Vector3f sun(final double offset) {
        final double[] towards = SkyMath.sky(this.celestial + offset, this.latitude);
        return new Vector3f((float) towards[0], (float) towards[1], (float) towards[2]);
    }

    /** How light the day is, rain and thunder taken in, as the game darkens its light: 0.2 at night to 1. */
    public float dayLight(final ClientLevel level, final float partialTick) {
        return (float) SkyMath.dayLight(sunHeight(), level.getRainLevel(partialTick),
                level.getThunderLevel(partialTick));
    }

    /** How bright the stars shine: 0.5 at midnight, none by day. */
    public float starlight() {
        return (float) SkyMath.starlight(sunHeight());
    }

    /** How light the sky's colour is, 0 at night to 1 by day, as the game works out the colour of its sky. */
    public float skyLight() {
        return (float) Math.max(0, Math.min(1, sunHeight() * 2 + 0.5));
    }
}
