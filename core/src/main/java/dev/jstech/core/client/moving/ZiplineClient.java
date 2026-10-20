/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.StructureZiplinePayload;
import dev.jstech.core.moving.ZiplineEntity;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ziplines on a player's game: using a rope looked at within reach, nearer than any block, takes hold of it there, the
 * server told; and a hold on a rope hangs from it as the rope is drawn, so the rider never parts from the rope.
 */
public final class ZiplineClient {

    /* How far a player reaches for a rope, and how near their look must come to it, in blocks. */
    private static final double REACH = 4.5;
    private static final double GRAB = 0.4;

    private ZiplineClient() {
    }

    /** Hands the kit and the game's bus what ziplines answer on the player's game, once, as it starts. */
    public static void listen() {
        ZiplineEntity.ClientHooks.set(ZiplineClient::hang);
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, ZiplineClient::onUse);
    }

    /* A hold on a rope hangs under the rope where it is drawn now. */
    private static void hang(final ZiplineEntity zipline) {
        final ClientLevel level = Minecraft.getInstance().level;
        final UUID id = zipline.joint();
        if (level == null || id == null) {
            return;
        }
        for (final StructureJoint joint : StructureJoints.seen()) {
            if (joint.id().equals(id)) {
                final Vec3 a = JointRenderer.drawnEnd(level, joint.a(), 1F);
                final Vec3 b = JointRenderer.drawnEnd(level, joint.b(), 1F);
                if (a != null && b != null) {
                    zipline.hangUnder(ZiplineEntity.along(a, b, zipline.place()), false);
                }
                return;
            }
        }
    }

    /* The player uses a rope they look at, nearer than what else they aim at: they take hold of it there. */
    private static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft game = Minecraft.getInstance();
        final LocalPlayer player = game.player;
        final ClientLevel level = game.level;
        if (!event.isUseItem() || player == null || level == null || player.isPassenger()) {
            return;
        }
        final Vec3 eye = player.getEyePosition();
        final Vec3 reach = eye.add(player.getLookAngle().scale(REACH));
        final double aimed = game.hitResult == null || game.hitResult.getType() == HitResult.Type.MISS
                ? REACH : game.hitResult.getLocation().distanceTo(eye);
        StructureJoint best = null;
        double bestAlong = 0;
        double bestDistance = Math.min(aimed, REACH);
        for (final StructureJoint joint : StructureJoints.seen()) {
            if (!(joint.kind() instanceof JointKind.Rope rope)) {
                continue;
            }
            final Vec3 a = JointRenderer.drawnEnd(level, joint.a(), 1F);
            final Vec3 b = JointRenderer.drawnEnd(level, joint.b(), 1F);
            if (a == null || b == null) {
                continue;
            }
            final List<Vec3> points = JointRenderer.ropePoints(a, b, rope.length());
            for (int k = 1; k < points.size(); k++) {
                final double[] near = nearest(eye, reach, points.get(k - 1), points.get(k));
                final double fromEye = near[0] * REACH;
                if (near[2] < GRAB && fromEye < bestDistance) {
                    best = joint;
                    bestDistance = fromEye;
                    bestAlong = (k - 1 + near[1]) / (points.size() - 1);
                }
            }
        }
        if (best == null) {
            return;
        }
        PacketDistributor.sendToServer(new StructureZiplinePayload(best.id(), (float) bestAlong));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    /*
     * Where the look from {@code p} to {@code q} comes nearest the piece of rope from {@code r} to {@code s}: as
     * {@code {share along the look, share along the piece, distance}}.
     */
    private static double[] nearest(final Vec3 p, final Vec3 q, final Vec3 r, final Vec3 s) {
        final Vec3 d1 = q.subtract(p);
        final Vec3 d2 = s.subtract(r);
        final Vec3 w = p.subtract(r);
        final double a = d1.dot(d1);
        final double b = d1.dot(d2);
        final double c = d2.dot(d2);
        final double d = d1.dot(w);
        final double e = d2.dot(w);
        final double denominator = a * c - b * b;
        double sc = denominator < 1.0e-9 ? 0 : (b * e - c * d) / denominator;
        sc = Math.max(0, Math.min(1, sc));
        double tc = c < 1.0e-9 ? 0 : (b * sc + e) / c;
        tc = Math.max(0, Math.min(1, tc));
        sc = a < 1.0e-9 ? 0 : Math.max(0, Math.min(1, (b * tc - d) / a));
        final Vec3 onLook = p.add(d1.scale(sc));
        final Vec3 onRope = r.add(d2.scale(tc));
        return new double[] {sc, tc, onLook.distanceTo(onRope)};
    }
}
