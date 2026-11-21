/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonObject;
import dev.jstech.core.agent.AgentReadings;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureSeatEntity;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What the player rides, as an agent reads it in {@code player_state}: the entity, the seat of a moving structure
 * and whether it is a helm, the structure, how the vehicle stands (its heading, pitch and roll, in the game's
 * degrees: the heading as a yaw, the pitch down from level, the roll its right side down), how fast it goes and
 * which way, in blocks a second, and where the player looks in the vehicle's own frame, ahead being nought; and,
 * standing on a moving structure's deck, the same of the structure.
 */
public final class PlayRiding {

    /** Its fields' names. */
    public static final String RIDING = "riding";
    public static final String ABOARD = "aboard";
    public static final String STRUCTURE = "structure";
    public static final String HEADING = "heading";
    public static final String PITCH = "pitch";
    public static final String ROLL = "roll";
    public static final String SPEED = "speed";
    public static final String VELOCITY = "velocity";
    public static final String LOOK = "look";
    public static final String YAW = "yaw";
    private static final double TICKS_PER_SECOND = 20;
    private static final double TENTHS = 10;

    private PlayRiding() {
    }

    /**
     * What {@code player} rides, read on the game's own thread; none when they ride nothing. Public for the tests
     * that check what an agent is told.
     */
    public static Optional<JsonObject> riding(final LocalPlayer player) {
        final Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return Optional.empty();
        }
        final JsonObject out = new JsonObject();
        out.addProperty("entity", BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString());
        out.addProperty("id", vehicle.getId());
        IStructureBody body = null;
        if (vehicle instanceof StructureSeatEntity seat) {
            final JsonObject sat = new JsonObject();
            // The seat's block, a place of the structure's yard.
            sat.add(Play.PLACE, Play.place(seat.seat()));
            sat.addProperty("helm", seat.helm());
            out.add("seat", sat);
            body = seat.structure();
        }
        if (body != null) {
            structure(out, body, player);
        } else {
            entity(out, vehicle, player);
            PlayReadings.add(out, AgentReadings.ofEntity(vehicle));
        }
        return Optional.of(out);
    }

    /** The moving structure {@code player} stands on, riding nothing; none for none. */
    public static Optional<JsonObject> aboard(final LocalPlayer player) {
        final IStructureBody body = player.getVehicle() == null ? StructureFooting.standingOn(player) : null;
        if (body == null) {
            return Optional.empty();
        }
        final JsonObject out = new JsonObject();
        structure(out, body, player);
        return Optional.of(out);
    }

    /* How the structure {@code body} stands and moves, and where {@code player} looks in its frame. */
    private static void structure(final JsonObject out, final IStructureBody body, final LocalPlayer player) {
        final StructureFrame now = body.frame();
        final StructureFrame was = body.previousFrame();
        out.addProperty(STRUCTURE, body.id().value().toString());
        // Its own forward is its yard's south, its up the yard's up, its right the side a pilot's right hand is on.
        final Vec3 forward = new Vec3(now.turnX(0, 0, 1), now.turnY(0, 0, 1), now.turnZ(0, 0, 1));
        final Vec3 up = new Vec3(now.turnX(0, 1, 0), now.turnY(0, 1, 0), now.turnZ(0, 1, 0));
        final Vec3 right = forward.cross(up);
        out.addProperty(HEADING, tenths(now.pose().yawDegrees()));
        out.addProperty(PITCH, tenths(-Math.toDegrees(Math.asin(Mth.clamp(forward.y, -1, 1)))));
        out.addProperty(ROLL, tenths(Math.toDegrees(Math.atan2(-right.y, up.y))));
        moving(out, new Vec3(now.pose().x() - was.pose().x(), now.pose().y() - was.pose().y(),
                now.pose().z() - was.pose().z()));
        final Vec3 view = player.getViewVector(1.0F);
        look(out, new Vec3(now.unturnX(view.x, view.y, view.z), now.unturnY(view.x, view.y, view.z),
                now.unturnZ(view.x, view.y, view.z)));
        // What the mods flying it tell of it, as a ship's throttle, its autopilot and the world below.
        PlayReadings.add(out, AgentReadings.ofStructure(body));
    }

    /* How an entity ridden stands and moves, and where {@code player} looks from its heading. */
    private static void entity(final JsonObject out, final Entity vehicle, final LocalPlayer player) {
        out.addProperty(HEADING, tenths(vehicle.getYRot()));
        out.addProperty(PITCH, tenths(vehicle.getXRot()));
        out.addProperty(ROLL, 0);
        moving(out, new Vec3(vehicle.getX() - vehicle.xo, vehicle.getY() - vehicle.yo, vehicle.getZ() - vehicle.zo));
        final JsonObject look = new JsonObject();
        look.addProperty(YAW, tenths(Mth.wrapDegrees(player.getYRot() - vehicle.getYRot())));
        look.addProperty(PITCH, tenths(player.getXRot() - vehicle.getXRot()));
        out.add(LOOK, look);
    }

    /* Its speed and its way, from how far it went in a tick. */
    private static void moving(final JsonObject out, final Vec3 perTick) {
        final Vec3 perSecond = perTick.scale(TICKS_PER_SECOND);
        out.addProperty(SPEED, tenths(perSecond.length()));
        final JsonObject velocity = new JsonObject();
        velocity.addProperty("x", tenths(perSecond.x));
        velocity.addProperty("y", tenths(perSecond.y));
        velocity.addProperty("z", tenths(perSecond.z));
        out.add(VELOCITY, velocity);
    }

    /* Where a look along {@code way}, in the vehicle's frame, points from its ahead, as the game's yaw and pitch. */
    private static void look(final JsonObject out, final Vec3 way) {
        final JsonObject look = new JsonObject();
        look.addProperty(YAW, tenths(Math.toDegrees(Math.atan2(-way.x, way.z))));
        look.addProperty(PITCH, tenths(-Math.toDegrees(Math.asin(Mth.clamp(way.y, -1, 1)))));
        out.add(LOOK, look);
    }

    private static double tenths(final double value) {
        return Math.round(value * TENTHS) / TENTHS;
    }
}
