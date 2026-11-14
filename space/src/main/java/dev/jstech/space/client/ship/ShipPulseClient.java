/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.space.network.ShipPulsePayload;
import dev.jstech.space.network.SpacePayloads;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * On a player's game, how far the pulse jump of the ship they are aboard has gone, as the server tells it each tick:
 * drawn at the server's tick the ship itself is drawn at ({@link ClientStructure#drawnTick}), between the two told
 * round it, so the worlds and the Sun, drawn that much further on, move round the ship smoothly.
 *
 * <p>As the jump ends the ship is set where it took it, and the player's game draws them going there a moment after,
 * a seat a tick behind its ship: from then on what is left of the jump is as far as they have still to go there, so
 * the space is drawn from where the jump ended all the while they go, never jumping.
 */
public final class ShipPulseClient {

    /* What has been told of the jump of the ship aboard, the oldest first, and of which ship. */
    private static final Deque<ShipPulsePayload> TOLD = new ArrayDeque<>();
    /* The most told kept, the most ticks the jump is carried on past the newest at its last speed, and the most it
     * is waited, its jump over, for the player to be drawn where it ended. */
    private static final int KEPT = 40;
    private static final int GOES_ON = 10;
    private static final int ARRIVES_WITHIN = 40;
    private static int entity = -1;
    /* Its jump over: where the eye stood as what was left of it began to be drawn, or none yet. */
    private static @Nullable Vec3 leftFrom;

    private ShipPulseClient() {
    }

    /** Hands the payloads what this answers, once, from the mod's player-side entry point. */
    public static void listen() {
        SpacePayloads.onShipPulse(ShipPulseClient::told);
    }

    /**
     * How far the jump of the ship the player is aboard has carried it, east and south in blocks, as a frame
     * {@code partialTick} of the way through this tick draws the ship: nothing out of a jump.
     */
    public static double[] offset(final float partialTick) {
        final ClientStructure ship = aboard();
        if (ship == null || ship.entity().getId() != entity || TOLD.isEmpty()) {
            return new double[] {0, 0};
        }
        final double at = ship.drawnTick(partialTick);
        if (Double.isNaN(at)) {
            return new double[] {0, 0};
        }
        final ShipPulsePayload newest = TOLD.peekLast();
        if (!newest.jumping() && at >= newest.tick() - 1) {
            return left(newest, at);
        }
        ShipPulsePayload before = null;
        ShipPulsePayload after = null;
        for (final ShipPulsePayload one : TOLD) {
            if (one.tick() <= at) {
                before = one;
            } else {
                after = one;
                break;
            }
        }
        if (before == null) {
            // Before the first word, the jump was beginning where the ship stands, a tick before it.
            final ShipPulsePayload first = TOLD.peekFirst();
            final double t = Math.clamp(at - (first.tick() - 1), 0, 1);
            return new double[] {first.dx() * t, first.dz() * t};
        }
        if (after != null) {
            final double t = (at - before.tick()) / (after.tick() - before.tick());
            return new double[] {before.dx() + (after.dx() - before.dx()) * t,
                    before.dz() + (after.dz() - before.dz()) * t};
        }
        return carriedOn(before, at);
    }

    /** Whether the ship the player is aboard is in a pulse jump, as last told. */
    public static boolean jumping() {
        final ClientStructure ship = aboard();
        return ship != null && ship.entity().getId() == entity && !TOLD.isEmpty() && TOLD.peekLast().jumping();
    }

    /** Which way the ship the player is aboard jumps, east and south, as its last two words tell; none out of one. */
    public static double[] way() {
        if (!jumping() || TOLD.size() < 2) {
            return new double[] {0, 0};
        }
        final ShipPulsePayload newest = TOLD.peekLast();
        final ShipPulsePayload earlier = earlierThan(newest);
        return earlier == null ? new double[] {0, 0}
                : new double[] {newest.dx() - earlier.dx(), newest.dz() - earlier.dz()};
    }

    /*
     * Its jump over, {@code ended} saying how far it went: what is left of it is as far as the eye has still to go
     * along it to where it ended, nothing once there, or once it has waited long enough.
     */
    private static double[] left(final ShipPulsePayload ended, final double at) {
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (leftFrom == null) {
            leftFrom = eye;
        }
        final double length = ended.dx() * ended.dx() + ended.dz() * ended.dz();
        final double gone = length <= 0 ? 1 : ((eye.x - leftFrom.x) * ended.dx() + (eye.z - leftFrom.z)
                * ended.dz()) / length;
        if (gone >= 1 || at - ended.tick() > ARRIVES_WITHIN) {
            return new double[] {0, 0};
        }
        final double left = 1 - Math.max(0, gone);
        return new double[] {ended.dx() * left, ended.dz() * left};
    }

    /* The ship the player is aboard, sitting or standing on it, as their game has it; or none. */
    private static @Nullable ClientStructure aboard() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        final IStructureBody body = player.getVehicle() instanceof StructureSeatEntity seat ? seat.structure()
                : StructureFooting.standingOn(player);
        return body instanceof ClientStructure ship ? ship : null;
    }

    /* Past the newest told, the jump carried on at its last speed a few ticks, as the ship's poses are. */
    private static double[] carriedOn(final ShipPulsePayload newest, final double at) {
        final ShipPulsePayload earlier = earlierThan(newest);
        if (earlier == null || earlier.tick() >= newest.tick()) {
            return new double[] {newest.dx(), newest.dz()};
        }
        final double ticks = newest.tick() - earlier.tick();
        final double past = Math.min(GOES_ON, at - newest.tick());
        return new double[] {newest.dx() + (newest.dx() - earlier.dx()) / ticks * past,
                newest.dz() + (newest.dz() - earlier.dz()) / ticks * past};
    }

    /* The word told just before {@code newest}, or none. */
    private static @Nullable ShipPulsePayload earlierThan(final ShipPulsePayload newest) {
        ShipPulsePayload earlier = null;
        for (final ShipPulsePayload one : TOLD) {
            if (one != newest) {
                earlier = one;
            }
        }
        return earlier;
    }

    /* The server told how far the jump of the ship aboard has gone. */
    private static void told(final ShipPulsePayload payload) {
        if (payload.entity() != entity) {
            TOLD.clear();
            entity = payload.entity();
        }
        if (!TOLD.isEmpty() && payload.tick() <= TOLD.peekLast().tick()) {
            return;
        }
        if (!TOLD.isEmpty() && !TOLD.peekLast().jumping()) {
            // A jump begun again after one ended: the last one is gone.
            TOLD.clear();
        }
        leftFrom = null;
        TOLD.addLast(payload);
        while (TOLD.size() > KEPT) {
            TOLD.removeFirst();
        }
        if (ClientMovingStructures.byEntity(payload.entity()) == null) {
            TOLD.clear();
            entity = -1;
        }
    }
}
