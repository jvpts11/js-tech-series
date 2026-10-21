/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Looking about freely, as a head turns in a cockpit while the vehicle keeps its course: while the free look key is
 * held, the mouse turns the view and never the player, so whatever the player's facing steers keeps its course; let
 * go, the view comes back ahead over a moment.
 */
public final class FreeLook {

    /* The game turns the view by the mouse's move times this, times eight times the cube of its sensitivity. */
    private static final double DEGREES_PER_MOVE = 0.15;
    private static final double SENSITIVITY_SCALE = 0.6;
    private static final double SENSITIVITY_FLOOR = 0.2;
    private static final double CUBED_SCALE = 8.0;
    /* The sensitivity that turns the player by nothing: the cube of its speed is then 0. */
    private static final double STILL = -SENSITIVITY_FLOOR / SENSITIVITY_SCALE;
    /* How far the head turns either way, and how much of the way back it comes each tick once the key is let go. */
    private static final float MOST_YAW = 150.0F;
    private static final float MOST_PITCH = 90.0F;
    private static final float BACK = 0.5F;
    private static final float GONE = 0.05F;

    private static float yaw;
    private static float pitch;
    private static double lastX = Double.NaN;
    private static double lastY = Double.NaN;

    private FreeLook() {
    }

    /** Whether the player is looking about freely now. */
    public static boolean active() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.screen == null
                && KeyActionsClient.mapping(CoreKeys.FREE_LOOK).isDown();
    }

    /** How far the view is turned from the player's facing, in degrees: across, then down. */
    public static float[] turned() {
        return new float[] {yaw, pitch};
    }

    /** Turns the head by {@code dx} and {@code dy} of the mouse's move, as the game turns a player by it. */
    public static void turn(final double dx, final double dy) {
        final Minecraft minecraft = Minecraft.getInstance();
        final double base = minecraft.options.sensitivity().get() * SENSITIVITY_SCALE + SENSITIVITY_FLOOR;
        final double speed = base * base * base * CUBED_SCALE * DEGREES_PER_MOVE;
        final double down = minecraft.options.invertYMouse().get() ? -dy : dy;
        yaw = Mth.clamp((float) (yaw + dx * speed), -MOST_YAW, MOST_YAW);
        pitch = Mth.clamp((float) (pitch + down * speed), -MOST_PITCH, MOST_PITCH);
    }

    /* While looking freely, the mouse turns the player by nothing. */
    static void onTurn(final CalculatePlayerTurnEvent event) {
        if (active()) {
            event.setMouseSensitivity(STILL);
            event.setCinematicCameraEnabled(false);
        }
    }

    /* The mouse's move since the frame before turns the head, while looking freely. */
    static void onFrame(final RenderFrameEvent.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final double x = minecraft.mouseHandler.xpos();
        final double y = minecraft.mouseHandler.ypos();
        if (active() && minecraft.mouseHandler.isMouseGrabbed() && !Double.isNaN(lastX)) {
            turn(x - lastX, y - lastY);
        }
        lastX = x;
        lastY = y;
    }

    /* Let go, the head comes back ahead. */
    static void onTick(final ClientTickEvent.Post event) {
        if (!active()) {
            yaw = Math.abs(yaw) < GONE ? 0.0F : yaw * BACK;
            pitch = Math.abs(pitch) < GONE ? 0.0F : pitch * BACK;
        }
    }

    /* The view turned from the player's facing by the head's turn. */
    static void onAngles(final ViewportEvent.ComputeCameraAngles event) {
        if (yaw != 0.0F || pitch != 0.0F) {
            event.setYaw(event.getYaw() + yaw);
            event.setPitch(Mth.clamp(event.getPitch() + pitch, -MOST_PITCH, MOST_PITCH));
        }
    }
}
