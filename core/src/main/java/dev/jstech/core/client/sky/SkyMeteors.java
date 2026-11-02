/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.sky.SkySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Meteors across a sky's night: streaks of light burning through its air, high over the horizon, which brighten as
 * they go, leave a short trail and are gone in less than a second. They fall at random, as often as the sky says, and
 * a mod can make one fall where and when it wants ({@link #fall}), for a story's shooting star or a test. Being in the
 * air, they stand still with the horizon, not with the turning stars.
 */
public final class SkyMeteors {

    /* How long one burns, in ticks, at least and at most more; how long its trail is, as a share of its path. */
    private static final float LEAST_LIFE = 10F;
    private static final float MORE_LIFE = 14F;
    private static final float TRAIL = 0.35F;
    /* Where one starts, in degrees over the horizon, and how far it runs, at least and at most more. */
    private static final double LOWEST = 25.0;
    private static final double HIGHER = 45.0;
    private static final double LEAST_RUN = 8.0;
    private static final double MORE_RUN = 7.0;
    /* How many texel-squares it is drawn in along its path, and how large each is. */
    private static final int STEPS = 48;
    private static final float SIZE = 0.2F;
    private static final float DISTANCE = 100F;
    private static final float TICKS_A_MINUTE = 1_200F;

    private static final List<Meteor> FALLING = new ArrayList<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static float last = Float.NaN;

    private SkyMeteors() {
    }

    /**
     * Makes a meteor fall from the direction {@code from} to {@code to}, each its yaw round the horizon and its pitch
     * over it in degrees, burning for {@code ticks} game ticks from now.
     */
    public static void fall(final double fromYaw, final double fromPitch, final double toYaw, final double toPitch,
                            final float ticks) {
        FALLING.add(new Meteor(direction(fromYaw, fromPitch), direction(toYaw, toPitch), ticks));
    }

    /**
     * Draws the meteors falling at {@code time}, in ticks, over the horizon {@code pose} stands at, as bright as the
     * starlight {@code night}; and starts new ones at random as {@code meteors} says, if the sky has any.
     */
    static void render(final SkySpec sky, final PoseStack pose, final float time, final float night) {
        final float since = Float.isNaN(last) ? 0F : Math.max(0F, Math.min(TICKS_A_MINUTE, time - last));
        last = time;
        sky.meteors().ifPresent(meteors -> {
            if (night > 0 && RANDOM.nextDouble() < meteors.perMinute() * since / TICKS_A_MINUTE) {
                FALLING.add(random());
            }
        });
        if (FALLING.isEmpty()) {
            return;
        }
        final float[] colour = SkyRenderer.rgb(sky.meteors().map(SkySpec.Meteors::colour)
                .orElse(SkySpec.Meteors.WHITE));
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final Matrix4f matrix = pose.last().pose();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        final List<Meteor> burnt = new ArrayList<>();
        for (final Meteor meteor : FALLING) {
            if (Float.isNaN(meteor.born)) {
                meteor.born = time;
            }
            final float age = (time - meteor.born) / meteor.life;
            if (age >= 1F || age < 0F) {
                burnt.add(meteor);
                continue;
            }
            streak(builder, matrix, meteor, age, colour, Math.max(night, 0F));
        }
        FALLING.removeAll(burnt);
        RenderSystem.disableCull();
        SkyRenderer.drawNow(builder);
        RenderSystem.enableCull();
    }

    /*
     * One meteor at {@code age}, 0 to 1 of its life: its head that far along its path, its trail behind it fading,
     * brighter as it goes, the whole fading as it burns out.
     */
    private static void streak(final BufferBuilder builder, final Matrix4f matrix, final Meteor meteor, final float age,
                               final float[] colour, final float night) {
        final float tail = Math.max(0F, age - TRAIL);
        final float out = age > 1F - TRAIL ? (1F - age) / TRAIL : 1F;
        for (int k = 0; k <= STEPS; k++) {
            final float t = tail + (age - tail) * k / STEPS;
            final float behind = (age - t) / TRAIL;
            final float glow = (float) (Math.pow(Math.sin(Math.PI * t), 0.7) * (0.35 + 0.65 * t)) * (1F - behind)
                    * out * Math.min(1F, night * 2F);
            if (glow <= 0) {
                continue;
            }
            final Vector3f at = new Vector3f(meteor.from).lerp(meteor.to, t).normalize(DISTANCE);
            final Quaternionf turn = new Quaternionf().rotateTo(new Vector3f(0F, 0F, -1F), at);
            for (final float[] corner : new float[][] {{SIZE, -SIZE}, {SIZE, SIZE}, {-SIZE, SIZE}, {-SIZE, -SIZE}}) {
                final Vector3f v = new Vector3f(at).add(new Vector3f(corner[0], corner[1], 0F).rotate(turn));
                builder.addVertex(matrix, v.x, v.y, v.z).setColor(colour[0] * glow, colour[1] * glow,
                        colour[2] * glow, 1F);
            }
        }
    }

    /* A meteor of the sky's own: somewhere high over the horizon, running a little way one way or another. */
    private static Meteor random() {
        final double yaw = RANDOM.nextDouble() * 360;
        final double pitch = LOWEST + RANDOM.nextDouble() * HIGHER;
        final double heading = RANDOM.nextDouble() * 2 * Math.PI;
        final double run = LEAST_RUN + RANDOM.nextDouble() * MORE_RUN;
        final double toPitch = Math.max(2, pitch + Math.sin(heading) * run);
        final double toYaw = yaw + Math.cos(heading) * run / Math.max(0.2, Math.cos(Math.toRadians(pitch)));
        return new Meteor(direction(yaw, pitch), direction(toYaw, toPitch),
                (float) (LEAST_LIFE + RANDOM.nextDouble() * MORE_LIFE));
    }

    /* The direction {@code yaw} round the horizon, as the game turns a player, and {@code pitch} over it. */
    private static Vector3f direction(final double yaw, final double pitch) {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Vector3f((float) (-Math.sin(y) * Math.cos(p)), (float) Math.sin(p),
                (float) (Math.cos(y) * Math.cos(p)));
    }

    /* One meteor: where it starts and ends, how long it burns, in ticks, and when it was first drawn (none yet). */
    private static final class Meteor {

        private final Vector3f from;
        private final Vector3f to;
        private final float life;
        private float born = Float.NaN;

        private Meteor(final Vector3f from, final Vector3f to, final float life) {
            this.from = from;
            this.to = to;
            this.life = life;
        }
    }
}
