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

    private SkySight() {
    }

    /** How much of its day's light the sky shows now, eclipses taken in, 0 to 1. */
    public static double eclipseLight(final ClientLevel level, final SkySpec sky, final float partialTick) {
        if (sky.suns().isEmpty() || sky.bodies().isEmpty()) {
            return 1;
        }
        final double celestial = level.getTimeOfDay(partialTick);
        final double days = level.getDayTime() / SkyMath.DAY;
        double hidden = 0;
        for (final SkySpec.Sun sun : sky.suns()) {
            final double[] towards = SkyMath.direction(celestial + sun.offset(), 0);
            final double radius = SkyMath.sunRadius(sun.apparentSize());
            for (final SkySpec.Body body : sky.bodies()) {
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

    /**
     * How light the sky is at {@code celestial} (the sun's turn round the sky), 0 at night to 1 at noon, as the game
     * works out the colour of its sky.
     */
    public static float daylight(final float celestial) {
        final float cos = (float) Math.cos(celestial * SkyMath.TURN) * 2.0F + 0.5F;
        return Math.max(0F, Math.min(1F, cos));
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

    /* A share of a sun's colour, taken {@link #TINT} of the way from white. */
    private static float toward(final double share) {
        return (float) (1 - TINT * (1 - share));
    }
}
