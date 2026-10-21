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
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.light.Beams;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The shaft of a spotlight's or a beam's light through the air, as dust and haze show it: its cone drawn from the
 * lamp out along its rays, each ending where it meets something, bright at the lamp and fading out along its reach,
 * light added to what lies behind it. Worked out as the server lays its light ({@link Beams}), once a second each, and
 * each tick for a lamp aboard a moving structure that goes, its shaft drawn from where its structure is drawn.
 */
final class BeamVisuals {

    private static final Map<BlockPos, Cached> RAYS = new ConcurrentHashMap<>();
    private static final long KEEP_TICKS = 20;
    /* How strong the shaft is at the lamp at its brightest, and at its far end. */
    private static final double AT_LAMP = 0.18;
    private static final double FAR_SHARE = 0.25;
    private static final int CHANNEL = 255;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int WARM_WHITE = 0xFFF2DC;

    private BeamVisuals() {
    }

    /**
     * Draws the shafts of the lamps the player's game has, the pose stack standing at the camera, {@code partialTick}
     * through the tick. A lamp aboard a moving structure shines from where its structure is drawn.
     */
    static void draw(final ClientLevel level, final Camera camera, final PoseStack poses,
                     final MultiBufferSource buffers, final float partialTick) {
        final double bloom = LightingSettings.bloom();
        if (bloom <= 0) {
            return;
        }
        final Vec3 eye = camera.getPosition();
        final double reach = LightingSettings.reach();
        final Matrix4f pose = poses.last().pose();
        VertexConsumer consumer = null;
        for (final LampBlockEntity lamp : LampBlockEntity.seen()) {
            final LightKind kind = lamp.kind();
            if (lamp.getLevel() != level || !lamp.lit() || lamp.shining() <= 0
                    || kind.shape() == LightKind.Shape.POINT || MovingStructures.inWorld(level,
                    Vec3.atCenterOf(lamp.getBlockPos())).distanceToSqr(eye) > reach * reach) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.getBuffer(LightRenderTypes.GLOW);
            }
            shaft(consumer, pose, eye, drawn(level, lamp, rays(level, lamp, kind), partialTick), kind,
                    lamp.shining(), bloom);
        }
    }

    /** Forgets every lamp's rays, as the world closes. */
    static void forget() {
        RAYS.clear();
    }

    private static List<Beams.Ray> rays(final ClientLevel level, final LampBlockEntity lamp, final LightKind kind) {
        final long now = level.getGameTime();
        final Cached cached = RAYS.get(lamp.getBlockPos());
        // A lamp aboard a structure going sweeps the world: its rays are worked out again each tick.
        final long keep = Beams.goingAboard(level, lamp.getBlockPos()) ? 1 : KEEP_TICKS;
        if (cached != null && now >= cached.at() && now - cached.at() < keep) {
            return cached.rays();
        }
        final List<Beams.Ray> rays = Beams.lampRays(level, lamp.getBlockPos(), lamp.facing(), kind);
        RAYS.put(lamp.getBlockPos(), new Cached(now, rays));
        return rays;
    }

    /*
     * The rays of a lamp aboard a moving structure where the structure is drawn this frame: worked out where it stood
     * this tick, they go with it to where it is drawn between its ticks. Any other lamp's rays as they are.
     */
    private static List<Beams.Ray> drawn(final ClientLevel level, final LampBlockEntity lamp,
                                         final List<Beams.Ray> rays, final float partialTick) {
        if (!YardLayout.isYardBlock(lamp.getBlockPos().getX())) {
            return rays;
        }
        final IStructureBody body = MovingStructures.at(level, lamp.getBlockPos());
        if (!(body instanceof ClientStructure structure)) {
            return rays;
        }
        final StructureFrame now = structure.frame();
        final StructureFrame drawn = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                structure.pivotY(), structure.pivotZ());
        final List<Beams.Ray> out = new ArrayList<>(rays.size());
        for (final Beams.Ray ray : rays) {
            out.add(new Beams.Ray(moved(now, drawn, ray.start()), moved(now, drawn, ray.end()), ray.hit(),
                    ray.length()));
        }
        return out;
    }

    /* Where the world's place {@code at}, of a structure standing at {@code from}, is as it stands at {@code to}. */
    private static Vec3 moved(final StructureFrame from, final StructureFrame to, final Vec3 at) {
        final double x = from.yardX(at.x, at.y, at.z);
        final double y = from.yardY(at.x, at.y, at.z);
        final double z = from.yardZ(at.x, at.y, at.z);
        return new Vec3(to.worldX(x, y, z), to.worldY(x, y, z), to.worldZ(x, y, z));
    }

    /* The cone's side, a fan between each ray of its outer ring and the next, from the lamp out. */
    private static void shaft(final VertexConsumer consumer, final Matrix4f pose, final Vec3 eye,
                              final List<Beams.Ray> rays, final LightKind kind, final int shining,
                              final double bloom) {
        // The rays are the middle one and then rings of six more each than the last; the outer ring is the last.
        int rings = 0;
        int counted = 1;
        while (counted + (rings + 1) * 6 <= rays.size()) {
            rings++;
            counted += rings * 6;
        }
        final int outer = rings * 6;
        if (outer == 0) {
            return;
        }
        final int first = rays.size() - outer;
        final int colour = kind.colour() == LightMath.WHITE ? WARM_WHITE : kind.colour();
        final int red = colour >> RED_SHIFT & CHANNEL;
        final int green = colour >> GREEN_SHIFT & CHANNEL;
        final int blue = colour & CHANNEL;
        final double strength = Math.min(1.0, AT_LAMP * bloom * shining / LightMath.BRIGHTEST);
        final int near = (int) Math.round(strength * CHANNEL);
        for (int i = 0; i < outer; i++) {
            final Beams.Ray a = rays.get(first + i);
            final Beams.Ray b = rays.get(first + (i + 1) % outer);
            final int farA = far(strength, a.length(), kind.reach());
            final int farB = far(strength, b.length(), kind.reach());
            vertex(consumer, pose, a.start(), eye, red, green, blue, near);
            vertex(consumer, pose, a.end(), eye, red, green, blue, farA);
            vertex(consumer, pose, b.end(), eye, red, green, blue, farB);
            vertex(consumer, pose, b.start(), eye, red, green, blue, near);
        }
    }

    /* How strong the shaft is where a ray ends: fading along the reach, less still where it goes all the way. */
    private static int far(final double strength, final double length, final int reach) {
        final double left = reach <= 0 ? 0 : Math.max(0, 1.0 - length / reach);
        return (int) Math.round(strength * FAR_SHARE * left * CHANNEL);
    }

    private static void vertex(final VertexConsumer consumer, final Matrix4f pose, final Vec3 at, final Vec3 eye,
                               final int red, final int green, final int blue, final int alpha) {
        consumer.addVertex(pose, (float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z))
                .setColor(red, green, blue, alpha);
    }

    private record Cached(long at, List<Beams.Ray> rays) {

        Cached {
            rays = List.copyOf(rays);
        }
    }
}
