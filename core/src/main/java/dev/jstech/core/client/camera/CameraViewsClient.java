/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.CameraClientHooks;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraView;
import dev.jstech.core.camera.CameraViewEndedPayload;
import dev.jstech.core.camera.CameraViewEvent;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Views from elsewhere on a player's game, as the server starts them: the game's camera moves to a stand-in placed
 * along a scene's path each frame, to the entity watched, or to a stand-in at the point looked from, while the
 * player's body stands where it is, still. Sneaking skips a scene or leaves a view; a scene ends at its last keyframe,
 * a watch when its entity is gone; the server is told how each ended.
 */
public final class CameraViewsClient implements CameraClientHooks.IHooks {

    private static final CameraViewsClient INSTANCE = new CameraViewsClient();
    private static @Nullable CameraView showing;
    private static @Nullable CameraPath path;
    private static @Nullable Entity stand;
    private static long startTick;
    private static boolean sneaking;
    private static double fov;
    private static float roll;

    private CameraViewsClient() {
    }

    /** The view the player sees now, or none. */
    public static Optional<CameraView> showing() {
        return Optional.ofNullable(showing);
    }

    @Override
    public void view(final Optional<CameraView> view) {
        if (showing != null) {
            restore();
        }
        view.ifPresent(CameraViewsClient::start);
    }

    /** The answers this game gives the server's views. */
    static CameraClientHooks.IHooks hooks() {
        return INSTANCE;
    }

    static void onTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final CameraView view = showing;
        if (view == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        final boolean sneak = minecraft.options.keyShift.isDown();
        if (sneak && !sneaking) {
            end(CameraViewEvent.Ending.SKIPPED);
            return;
        }
        sneaking = sneak;
        if (view instanceof CameraView.Scene && path != null
                && minecraft.level.getGameTime() - startTick >= path.length()) {
            end(CameraViewEvent.Ending.FINISHED);
        } else if (view instanceof CameraView.Watch watch) {
            final Entity watched = minecraft.level.getEntity(watch.entity());
            if (watched == null || watched.isRemoved()) {
                end(CameraViewEvent.Ending.LOST);
            }
        }
    }

    /* Each frame, the stand-in of a scene goes where its path has the camera now, and the point's turns as asked. */
    static void onFrame(final RenderFrameEvent.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Entity at = stand;
        if (at == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (showing instanceof CameraView.Scene scene && path != null) {
            final double ticks = minecraft.level.getGameTime() - startTick
                    + event.getPartialTick().getGameTimeDeltaPartialTick(true);
            final CameraPath.Sample sample = path.at(ticks);
            final double radians = Math.toRadians(scene.yaw());
            final double leftX = Math.cos(radians);
            final double leftZ = Math.sin(radians);
            final double aheadX = -Math.sin(radians);
            final double aheadZ = Math.cos(radians);
            place(at, scene.origin().add(leftX * sample.x() + aheadX * sample.z(), sample.y(),
                    leftZ * sample.x() + aheadZ * sample.z()), scene.yaw() + sample.yaw(), sample.pitch());
            fov = sample.fov();
            roll = sample.roll();
        } else if (showing instanceof CameraView.Point point && point.turns()) {
            place(at, at.position(), minecraft.player.getYRot(), minecraft.player.getXRot());
        }
    }

    /* The roll of a scene's keyframe. */
    static void onAngles(final ViewportEvent.ComputeCameraAngles event) {
        if (showing instanceof CameraView.Scene && stand != null && event.getCamera().getEntity() == stand) {
            event.setRoll(event.getRoll() + roll);
        }
    }

    /* The field of view of a scene's keyframe, when it names one. */
    static void onFov(final ViewportEvent.ComputeFov event) {
        if (showing instanceof CameraView.Scene && fov > 0 && event.usedConfiguredFov()) {
            event.setFOV(fov);
        }
    }

    /* The body stays still while the player sees from elsewhere. */
    static void onInput(final MovementInputUpdateEvent event) {
        if (showing != null) {
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }

    /* A scene that hides the HUD hides all of it. */
    static void onLayer(final RenderGuiLayerEvent.Pre event) {
        if (showing instanceof CameraView.Scene && path != null && path.hideHud()) {
            event.setCanceled(true);
        }
    }

    /* Seen from elsewhere, the player's own hands are not before the camera. */
    static void onHand(final RenderHandEvent event) {
        if (showing != null) {
            event.setCanceled(true);
        }
    }

    /* Nor does the camera read the player's own name over their head. */
    static void onNameTag(final RenderNameTagEvent event) {
        if (showing != null && event.getEntity() == Minecraft.getInstance().player) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static void start(final CameraView view) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        showing = view;
        sneaking = minecraft.options.keyShift.isDown();
        startTick = level.getGameTime();
        fov = 0;
        roll = 0;
        switch (view) {
            case CameraView.Scene scene -> {
                final Optional<CameraPath> found = CameraPathsClient.get(scene.path());
                if (found.isEmpty()) {
                    end(CameraViewEvent.Ending.LOST);
                    return;
                }
                path = found.get();
                stand = standIn(level, scene.origin(), scene.yaw(), 0);
            }
            case CameraView.Watch watch -> {
                final Entity watched = level.getEntity(watch.entity());
                if (watched == null) {
                    end(CameraViewEvent.Ending.LOST);
                    return;
                }
                minecraft.setCameraEntity(watched);
                return;
            }
            case CameraView.Point point -> {
                if (point.turns()) {
                    player.setYRot(point.yaw());
                    player.setXRot(point.pitch());
                }
                stand = standIn(level, point.place(), point.yaw(), point.pitch());
            }
        }
        minecraft.setCameraEntity(stand);
    }

    /* Ends the view on this game and tells the server how. */
    private static void end(final CameraViewEvent.Ending ending) {
        restore();
        PacketDistributor.sendToServer(CameraViewEndedPayload.of(ending));
    }

    /* Back to the player's own eyes. */
    private static void restore() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
        showing = null;
        path = null;
        stand = null;
    }

    /* A stand-in for the camera, kept out of the world: nothing sees or touches it. */
    private static Entity standIn(final ClientLevel level, final Vec3 at, final float yaw, final float pitch) {
        final Marker marker = new Marker(EntityType.MARKER, level);
        place(marker, at, yaw, pitch);
        return marker;
    }

    private static void place(final Entity entity, final Vec3 at, final float yaw, final float pitch) {
        entity.setPos(at);
        entity.xo = at.x;
        entity.yo = at.y;
        entity.zo = at.z;
        entity.xOld = at.x;
        entity.yOld = at.y;
        entity.zOld = at.z;
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
    }
}
