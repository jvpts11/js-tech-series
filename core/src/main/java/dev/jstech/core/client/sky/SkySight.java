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
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What the day of a sky drawn by the Core looks like at a moment: how strong its first sun's light is at its distance,
 * the colour its suns tint the day, and how dark an eclipse makes it, from where the suns and the bodies stand. Only
 * what the player sees: the game's own light levels, and what spawns and grows by them, stay as they are.
 */
public final class SkySight {

    /* How far the day takes the colour of its suns: a G star's light is near white, a red dwarf's plainly red. */
    private static final float TINT = 0.6F;
    private static final float CHANNEL = 255F;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int BYTE = 0xFF;
    private static final int LIGHT_LEVELS = 15;
    /* How the game builds the sky's light in its light map: a level's brightness times the darkening kept almost
     * whole, its red and green the darkening drawn a third of the way to white, the whole drawn a little to grey. */
    private static final float SKY_KEEP = 0.95F;
    private static final float SKY_FLOOR = 0.05F;
    private static final float SKY_WHITE = 0.35F;
    private static final float BLEND_KEEP = 0.96F;

    private SkySight() {
    }

    /** How much of its day's light the sky shows now where the camera is, eclipses taken in, 0 to 1. */
    public static double eclipseLight(final ClientLevel level, final SkySpec sky, final float partialTick) {
        return eclipseLight(level, sky, SkyPlace.of(level, sky).celestial());
    }

    /** The same with the first sun {@code celestial} turns round the sky. */
    public static double eclipseLight(final ClientLevel level, final SkySpec sky, final double celestial) {
        if (sky.suns().isEmpty() || sky.bodies().isEmpty()) {
            return 1;
        }
        final double days = level.getDayTime() / SkyMath.DAY;
        double hidden = 0;
        for (final SkySpec.Sun sun : sky.suns()) {
            final double[] towards = SkyMath.direction(celestial + sun.offset(), 0);
            final double radius = SkyMath.sunRadius(sun.apparentSize());
            for (final SkySpec.Body body : sky.bodies()) {
                if (body.point()) {
                    // A point of light hides nothing of a sun.
                    continue;
                }
                final double[] at = SkyMath.direction(celestial + SkyMath.drift(body.offset(), body.orbitDays(),
                        days), body.tilt());
                hidden = Math.max(hidden, SkyMath.coverage(radius, SkyMath.bodyRadius(body.size()),
                        SkyMath.separation(towards, at)));
            }
        }
        return SkyMath.eclipseLight(hidden);
    }

    /** How strong the first sun's light is at its distance, beside the overworld's. */
    public static double sunlight(final SkySpec sky) {
        return sky.suns().isEmpty() ? SkyMath.DIMMEST : SkyMath.sunlight(sky.suns().getFirst().distance());
    }

    /** The colour the suns tint the day, as red, green and blue from 0 to 1: their colours weighed by their light. */
    public static Vector3f tint(final SkySpec sky) {
        final List<SkySpec.Sun> suns = sky.suns();
        if (suns.isEmpty()) {
            return new Vector3f(1, 1, 1);
        }
        double red = 0;
        double green = 0;
        double blue = 0;
        double weight = 0;
        for (final SkySpec.Sun sun : suns) {
            final double light = sun.apparentSize() * sun.apparentSize() * SkyMath.sunlight(sun.distance());
            final int colour = sun.kind().colour();
            red += (colour >> RED_SHIFT & BYTE) * light;
            green += (colour >> GREEN_SHIFT & BYTE) * light;
            blue += (colour & BYTE) * light;
            weight += light;
        }
        final float most = (float) Math.max(red, Math.max(green, blue));
        if (weight <= 0 || most <= 0) {
            return new Vector3f(1, 1, 1);
        }
        return new Vector3f(toward(red / most), toward(green / most), toward(blue / most));
    }

    /** {@code colour} ({@code 0xRRGGBB}) as the sky shows it in {@code daylight}. */
    public static Vec3 shown(final int colour, final float daylight) {
        return new Vec3((colour >> RED_SHIFT & BYTE) / CHANNEL * daylight,
                (colour >> GREEN_SHIFT & BYTE) / CHANNEL * daylight, (colour & BYTE) / CHANNEL * daylight);
    }

    /**
     * Darkens and tints the part of a light-map colour the sky gives: {@code skyLevel} of the sky's light and
     * {@code blockLevel} of the blocks', in a day {@code daylight} bright, by {@code light} and {@code tint}.
     */
    public static void adjust(final Vector3f colours, final int blockLevel, final int skyLevel, final float daylight,
                              final float light, final Vector3f tint) {
        final float share = skyLevel / (float) LIGHT_LEVELS * (1F - blockLevel / (float) LIGHT_LEVELS) * daylight;
        if (share <= 0) {
            return;
        }
        colours.set(colours.x * (1F - share + share * light * tint.x),
                colours.y * (1F - share + share * light * tint.y),
                colours.z * (1F - share + share * light * tint.z));
        colours.set(Math.min(1F, colours.x), Math.min(1F, colours.y), Math.min(1F, colours.z));
    }

    /**
     * Lights the sky's part of a light-map colour by the day's light at the place, {@code to}, where the game lit it
     * by its clock's, {@code from}: the part the game adds for a level whose own brightness is {@code brightness},
     * taken out as the game made it and put back as the place's.
     */
    public static void relight(final Vector3f colours, final float brightness, final float from, final float to) {
        final float before = brightness * (from * SKY_KEEP + SKY_FLOOR);
        final float after = brightness * (to * SKY_KEEP + SKY_FLOOR);
        // The game takes the sky's colour from the darkening, bluer at night, and blends the whole a little to grey.
        final float red = (sky(to) * after - sky(from) * before) * BLEND_KEEP;
        final float blue = (after - before) * BLEND_KEEP;
        colours.set(Math.max(0F, colours.x + red), Math.max(0F, colours.y + red), Math.max(0F, colours.z + blue));
    }

    /* The red and green of the game's sky light at a darkening, its blue being whole. */
    private static float sky(final float darkening) {
        return darkening + (1F - darkening) * SKY_WHITE;
    }

    /* A share of a sun's colour, taken {@link #TINT} of the way from white. */
    private static float toward(final double share) {
        return (float) (1 - TINT * (1 - share));
    }
}
