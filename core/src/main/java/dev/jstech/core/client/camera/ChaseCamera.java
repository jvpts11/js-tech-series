/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.CameraSettings;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.vehicle.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The camera of a player riding something, on their game: riding a vehicle, the player turns with it, as a boat
 * turns its rider, so the camera behind follows its turn and the view ahead keeps facing where it faced on it; and the
 * view rolls with the vehicle's lean, or with the moving structure whose seat the player sits in, in the first person
 * and behind alike. A moving structure turns its riders already. The player's setting turns it all off.
 */
final class ChaseCamera {

    private static float lastYaw;
    private static int lastVehicle = -1;

    private ChaseCamera() {
    }

    /* Turns the rider of a vehicle by the vehicle's turn since the tick before. */
    static void onTick(final ClientTickEvent.Post event) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CameraSettings.chase() || !(player.getRootVehicle() instanceof VehicleEntity vehicle)) {
            lastVehicle = -1;
            return;
        }
        final float yaw = vehicle.getYRot();
        if (lastVehicle == vehicle.getId()) {
            final float turn = Mth.wrapDegrees(yaw - lastYaw);
            if (turn != 0.0F) {
                player.setYRot(player.getYRot() + turn);
                player.yRotO += turn;
                player.setYHeadRot(player.getYHeadRot() + turn);
            }
        }
        lastYaw = yaw;
        lastVehicle = vehicle.getId();
    }

    /* Rolls the view with what the player rides. */
    static void onAngles(final ViewportEvent.ComputeCameraAngles event) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CameraSettings.chase() || event.getCamera().getEntity() != player) {
            return;
        }
        final Entity ridden = player.getRootVehicle();
        final float roll;
        if (ridden instanceof VehicleEntity vehicle) {
            /* A vehicle leaning left tilts its rider's head left: the camera rolls the other way of its own sign. */
            roll = -vehicle.leanRoll();
        } else if (player.getVehicle() instanceof StructureSeatEntity seat
                && seat.structure() instanceof ClientStructure structure) {
            roll = structureRoll(structure.drawnAt((float) event.getPartialTick()), event.getYaw());
        } else {
            return;
        }
        event.setRoll(event.getRoll() + roll);
    }

    /*
     * How far the structure's up leans to the right of a view facing {@code yaw}, in degrees: the roll a camera
     * aboard it has.
     */
    private static float structureRoll(final StructurePose pose, final float yaw) {
        final Vector3d up = new Quaterniond(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                .transform(new Vector3d(0.0, 1.0, 0.0));
        final double radians = Math.toRadians(yaw);
        final double rightX = -Math.cos(radians);
        final double rightZ = -Math.sin(radians);
        return (float) Math.toDegrees(Math.atan2(up.x * rightX + up.z * rightZ, up.y));
    }
}
