/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The glow of bright things, drawn without a shader: a soft halo about each lamp, each block that gives light of a
 * colour or bright light, and each glowing part of a model that casts light, in its colour, as strong as it shines.
 * Light added to what lies behind it, hidden behind walls, the nearest few drawn; as strong as the player's setting.
 */
final class Halos {

    /* The most halos a frame draws, the nearest; how far towards the eye one stands; and its size and strength. */
    private static final int MOST = 192;
    private static final double TOWARDS_EYE = 0.55;
    private static final float LEAST_SIZE = 0.6F;
    private static final float MORE_SIZE = 1.4F;
    private static final double STRENGTH = 0.45;
    /* The rings and segments a halo is drawn in, and how quickly its bell falls. */
    private static final int RINGS = 5;
    private static final int SEGMENTS = 16;
    private static final double SPREAD = 4.0;
    /* The colour of the glow of light that has none: a lamp's warm white. */
    private static final int WARM_WHITE = 0xFFF2DC;
    private static final int CHANNEL = 255;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private Halos() {
    }

    /** Draws the halos of the frame into {@code buffers}, the pose stack standing at the camera. */
    static void draw(final ClientLevel level, final Camera camera, final PoseStack poses,
                     final MultiBufferSource buffers) {
        final double bloom = LightingSettings.bloom();
        if (bloom <= 0) {
            return;
        }
        final Vec3 eye = camera.getPosition();
        final double reach = LightingSettings.reach();
        final List<Glow> glows = new ArrayList<>();
        for (final ColouredLights.Source source : ColouredLights.glowing()) {
            glows.add(new Glow(new Vec3(source.x() + 0.5, source.y() + 0.5, source.z() + 0.5), source.level(),
                    source.colour()));
        }
        for (final LampBlockEntity lamp : LampBlockEntity.seen()) {
            if (lamp.getLevel() == level && lamp.lit() && lamp.shining() > 0) {
                glows.add(new Glow(Vec3.atCenterOf(lamp.getBlockPos()), lamp.shining(), lamp.kind().colour()));
            }
        }
        for (final KitGlows.Glowing part : KitGlows.last()) {
            if (part.level() == level) {
                glows.add(new Glow(part.at(), part.light(), part.colour()));
            }
        }
        glows.removeIf(glow -> glow.at().distanceToSqr(eye) > reach * reach);
        glows.sort(Comparator.comparingDouble(glow -> glow.at().distanceToSqr(eye)));
        final Vector3f left = camera.getLeftVector();
        final Vector3f up = camera.getUpVector();
        final VertexConsumer consumer = buffers.getBuffer(LightRenderTypes.GLOW);
        final Matrix4f pose = poses.last().pose();
        for (int i = 0; i < Math.min(MOST, glows.size()); i++) {
            final Glow glow = glows.get(i);
            final Vec3 toward = eye.subtract(glow.at()).normalize().scale(TOWARDS_EYE);
            final Vec3 at = glow.at().add(toward).subtract(eye);
            final float share = (float) glow.level() / LightMath.BRIGHTEST;
            final float size = LEAST_SIZE + MORE_SIZE * share;
            halo(consumer, pose, at, left, up, size, glow.colour() == LightMath.WHITE ? WARM_WHITE : glow.colour(),
                    Math.min(1.0, STRENGTH * bloom * share));
        }
    }

    /*
     * A disc facing the eye about {@code at}, in rings from its middle out, each ring as strong as a soft bell is
     * there, light scattering in the eye, and nothing at its rim.
     */
    private static void halo(final VertexConsumer consumer, final Matrix4f pose, final Vec3 at, final Vector3f left,
                             final Vector3f up, final float size, final int colour, final double strength) {
        final int red = colour >> RED_SHIFT & CHANNEL;
        final int green = colour >> GREEN_SHIFT & CHANNEL;
        final int blue = colour & CHANNEL;
        for (int ring = 0; ring < RINGS; ring++) {
            final float inner = (float) ring / RINGS;
            final float outer = (float) (ring + 1) / RINGS;
            final int innerAlpha = (int) Math.round(strength * bell(inner) * CHANNEL);
            final int outerAlpha = (int) Math.round(strength * bell(outer) * CHANNEL);
            for (int step = 0; step < SEGMENTS; step++) {
                final double a = 2 * Math.PI * step / SEGMENTS;
                final double b = 2 * Math.PI * (step + 1) / SEGMENTS;
                corner(consumer, pose, at, left, up, size * inner, a, red, green, blue, innerAlpha);
                corner(consumer, pose, at, left, up, size * outer, a, red, green, blue, outerAlpha);
                corner(consumer, pose, at, left, up, size * outer, b, red, green, blue, outerAlpha);
                corner(consumer, pose, at, left, up, size * inner, b, red, green, blue, innerAlpha);
            }
        }
    }

    /* How strong a glow is at {@code distance} from its middle, its rim at 1: a bell brought to nothing there. */
    private static double bell(final double distance) {
        return Math.exp(-distance * distance * SPREAD) * Math.max(0.0, 1.0 - distance);
    }

    private static void corner(final VertexConsumer consumer, final Matrix4f pose, final Vec3 at, final Vector3f left,
                               final Vector3f up, final float radius, final double angle, final int red,
                               final int green, final int blue, final int alpha) {
        final float across = (float) Math.cos(angle) * radius;
        final float upward = (float) Math.sin(angle) * radius;
        consumer.addVertex(pose, (float) at.x + left.x() * across + up.x() * upward,
                (float) at.y + left.y() * across + up.y() * upward,
                (float) at.z + left.z() * across + up.z() * upward).setColor(red, green, blue, alpha);
    }

    /* A thing that glows: where, as bright as, and in what colour. */
    private record Glow(Vec3 at, int level, int colour) {
    }
}
