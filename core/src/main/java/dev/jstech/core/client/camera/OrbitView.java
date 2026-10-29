/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * A camera a screen holds while it is open, turning round a point, as an editor's view turns round what is built in
 * it: the game's camera moves to a stand-in circling the point at a distance, turned as the player drags, brought
 * nearer and further by the wheel, the point raised and lowered along its upright, and the whole kept inside a box, as
 * a building's walls, so it never looks through them. While it holds the view, the player's own body, hands and HUD are
 * not drawn. Each frame it keeps how the world was drawn, so the screen finds where a point of the world falls on it,
 * and which way into the world a point of the screen looks, to pick what is under the mouse.
 */
public final class OrbitView {

    private final AABB bounds;
    private Vec3 target;
    private float yaw;
    private float pitch;
    private double distance;
    private Matrix4f view = new Matrix4f();
    private Matrix4f projection = new Matrix4f();
    private Vec3 eye = Vec3.ZERO;
    private boolean seen;
    private @Nullable Entity stand;

    /** The nearest and the furthest the camera comes to its point, in blocks. */
    public static final double NEAREST = 2.0;
    public static final double FURTHEST = 96.0;
    /* How far the camera keeps inside its box, so the near side of the view does not cut into a wall. */
    private static final double INSET = 0.3;
    private static final float STEEPEST = 89.0F;

    private static @Nullable OrbitView holding;

    private OrbitView(final Vec3 target, final AABB bounds, final float yaw, final float pitch,
                      final double distance) {
        this.bounds = bounds;
        this.target = target;
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
    }

    /**
     * Takes the view, turning round {@code target} inside {@code bounds}, from {@code yaw} and {@code pitch} in
     * degrees, as the game turns the player, at {@code distance}; the view another screen held is let go.
     */
    public static OrbitView hold(final Vec3 target, final AABB bounds, final float yaw, final float pitch,
                                 final double distance) {
        final OrbitView was = holding;
        if (was != null) {
            was.release();
        }
        final OrbitView view = new OrbitView(bounds.contains(target) ? target : clamp(bounds, target), bounds, yaw,
                Math.clamp(pitch, -STEEPEST, STEEPEST), Math.clamp(distance, NEAREST, FURTHEST));
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            view.stand = new Marker(EntityType.MARKER, level);
            view.place();
            Minecraft.getInstance().setCameraEntity(view.stand);
        }
        holding = view;
        return view;
    }

    /** The view a screen holds now, if one does. */
    public static Optional<OrbitView> held() {
        return Optional.ofNullable(holding);
    }

    /** Gives the view back to the player's own eyes; nothing for a view already let go. */
    public void release() {
        if (holding != this) {
            return;
        }
        holding = null;
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
        this.stand = null;
    }

    /** Turns the camera round its point by {@code yaw} and {@code pitch} degrees, never past straight up or down. */
    public void turn(final float yaw, final float pitch) {
        this.yaw += yaw;
        this.pitch = Math.clamp(this.pitch + pitch, -STEEPEST, STEEPEST);
    }

    /** Brings the camera nearer its point by {@code factor}, under 1, or further, over 1. */
    public void zoom(final double factor) {
        this.distance = Math.clamp(this.distance * factor, NEAREST, FURTHEST);
    }

    /** Raises the point turned round by {@code by} blocks, or lowers it, within the box. */
    public void lift(final double by) {
        this.target = clamp(this.bounds, this.target.add(0, by, 0));
    }

    /** Turns the camera round {@code point} from now on, kept inside the box. */
    public void aim(final Vec3 point) {
        this.target = clamp(this.bounds, point);
    }

    /** The point turned round. */
    public Vec3 target() {
        return this.target;
    }

    /** How far the camera stands from its point, in blocks, before the box brings it nearer. */
    public double distance() {
        return this.distance;
    }

    /** The camera's turn round its point, in degrees, as the game turns the player. */
    public float yaw() {
        return this.yaw;
    }

    /** How steeply the camera looks down at its point, in degrees, as the game tilts the player. */
    public float pitch() {
        return this.pitch;
    }

    /** Where the camera's eye is in the world, as the last frame drew it. */
    public Vec3 eye() {
        return this.eye;
    }

    /**
     * Where {@code point} of the world falls on the screen, in the screen's scaled pixels, as the last frame drew the
     * world; none before a frame was drawn, or for a point behind the camera.
     */
    public Optional<Vec2> project(final Vec3 point) {
        if (!this.seen) {
            return Optional.empty();
        }
        final Vector4f at = new Vector4f((float) (point.x - this.eye.x), (float) (point.y - this.eye.y),
                (float) (point.z - this.eye.z), 1.0F);
        this.view.transform(at);
        this.projection.transform(at);
        if (at.w <= 0) {
            return Optional.empty();
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final float x = (at.x / at.w + 1) / 2 * minecraft.getWindow().getGuiScaledWidth();
        final float y = (1 - at.y / at.w) / 2 * minecraft.getWindow().getGuiScaledHeight();
        return Optional.of(new Vec2(x, y));
    }

    /**
     * The way into the world the point {@code x, y} of the screen, in its scaled pixels, looks, as the last frame drew
     * it: from the camera's eye, a block long; none before a frame was drawn.
     */
    public Optional<Ray> ray(final double x, final double y) {
        if (!this.seen) {
            return Optional.empty();
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final float nx = (float) (2 * x / minecraft.getWindow().getGuiScaledWidth() - 1);
        final float ny = (float) (1 - 2 * y / minecraft.getWindow().getGuiScaledHeight());
        final Matrix4f undo = new Matrix4f(this.projection).mul(this.view).invert();
        final Vector4f near = undo.transform(new Vector4f(nx, ny, -1, 1));
        final Vector4f far = undo.transform(new Vector4f(nx, ny, 1, 1));
        near.div(near.w);
        far.div(far.w);
        final Vec3 from = this.eye.add(near.x, near.y, near.z);
        final Vec3 way = new Vec3(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
        return Optional.of(new Ray(from, way));
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, event -> {
            final OrbitView view = holding;
            if (view != null) {
                view.place();
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            final OrbitView view = holding;
            if (view != null && event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                view.view = new Matrix4f(event.getModelViewMatrix());
                view.projection = new Matrix4f(event.getProjectionMatrix());
                view.eye = event.getCamera().getPosition();
                view.seen = true;
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, event -> {
            if (holding != null) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, event -> {
            if (holding != null) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderPlayerEvent.Pre.class, event -> {
            if (holding != null && event.getEntity() == Minecraft.getInstance().player) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            final OrbitView view = holding;
            if (view != null) {
                view.release();
            }
        });
    }

    /* Puts the stand-in where the camera is: round its point, as far as the box lets it be. */
    private void place() {
        final Entity at = this.stand;
        if (at == null) {
            return;
        }
        final double yawRadians = Math.toRadians(this.yaw);
        final double pitchRadians = Math.toRadians(this.pitch);
        // The way the camera looks, as the game's look for a turn and a tilt; the camera stands back along it.
        final Vec3 look = new Vec3(-Math.sin(yawRadians) * Math.cos(pitchRadians), -Math.sin(pitchRadians),
                Math.cos(yawRadians) * Math.cos(pitchRadians));
        final double back = Math.min(this.distance, exit(this.bounds.deflate(INSET), this.target, look.reverse()));
        final Vec3 stand = this.target.subtract(look.scale(back));
        at.setPos(stand);
        at.xo = stand.x;
        at.yo = stand.y;
        at.zo = stand.z;
        at.xOld = stand.x;
        at.yOld = stand.y;
        at.zOld = stand.z;
        at.setYRot(this.yaw);
        at.setXRot(this.pitch);
        at.yRotO = this.yaw;
        at.xRotO = this.pitch;
    }

    /* How far from {@code from}, inside {@code box}, a line going {@code way} leaves it. */
    private static double exit(final AABB box, final Vec3 from, final Vec3 way) {
        double out = Double.MAX_VALUE;
        out = Math.min(out, along(from.x, way.x, box.minX, box.maxX));
        out = Math.min(out, along(from.y, way.y, box.minY, box.maxY));
        out = Math.min(out, along(from.z, way.z, box.minZ, box.maxZ));
        return Math.max(0, out);
    }

    private static double along(final double from, final double way, final double low, final double high) {
        if (way > 0) {
            return (high - from) / way;
        }
        return way < 0 ? (low - from) / way : Double.MAX_VALUE;
    }

    private static Vec3 clamp(final AABB box, final Vec3 point) {
        return new Vec3(Math.clamp(point.x, box.minX, box.maxX), Math.clamp(point.y, box.minY, box.maxY),
                Math.clamp(point.z, box.minZ, box.maxZ));
    }

    /**
     * A line into the world from the screen.
     *
     * @param from where it starts, on the camera's near side
     * @param way  the way it goes, a block long
     */
    public record Ray(Vec3 from, Vec3 way) {

        /** The point {@code distance} blocks along it. */
        public Vec3 at(final double distance) {
            return this.from.add(this.way.scale(distance));
        }
    }
}
