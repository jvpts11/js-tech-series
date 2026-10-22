/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.CameraMotions;
import dev.jstech.core.camera.ThirdPersonView;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * The cameras mods put in the third person, on the player's game: each says, every frame, where it stands, or leaves
 * the view to the next, the highest priority first, and in the end to the game. A camera stands back as far as it
 * says, over a shoulder, round a place of its own; a wall between it and where it would stand brings it nearer, as the
 * game's own is brought nearer, and a change from one view to another, or back to the game's, is eased. Asking for less
 * movement makes every change at once.
 *
 * <pre>{@code
 * ThirdPersonCameras.register(id("tank"), 10, (viewer, front, partialTick) ->
 *         viewer.getVehicle() instanceof Tank tank
 *                 ? ThirdPersonView.behind(9).shoulder(1.2, 0.6).around(tank.turretAt(partialTick)) : null);
 * }</pre>
 */
public final class ThirdPersonCameras {

    private static final Object LOCK = new Object();
    private static final double MILLIS = 1000.0;
    /* A frame this long after the last one of the third person starts the view where it is wanted, not eased. */
    private static final double FRESH = 0.25;
    /* The longest step a frame takes of the easing, in seconds, so a stall does not throw the view. */
    private static final double LONGEST_STEP = 0.1;
    /* How near what it is eased to the view is taken as there, in blocks. */
    private static final double THERE = 1.0e-3;
    /*
     * How far short of a wall the camera stops when brought nearer, in blocks: the game then checks its way back from
     * a tenth of a block round it, which must stay out of the wall.
     */
    private static final double CLEAR = 0.2;
    private static final double SMALLEST_SCALE = 1.0e-3;
    private static final int DISTANCE = 0;
    private static final int RIGHT = 1;
    private static final int UP = 2;
    private static final int PIVOT_X = 3;
    private static final int PIVOT_Y = 4;
    private static final int PIVOT_Z = 5;
    private static final int AXES = 6;
    /* What is eased, on each axis its value and speed: how far back, to the right, up, and the pivot from the eye. */
    private static final double[][] EASED = new double[AXES][2];

    private static volatile List<Registered> cameras = List.of();
    private static double lastFrame = Double.NaN;
    private static double seconds = ThirdPersonView.EASED;
    private static boolean resting = true;

    private ThirdPersonCameras() {
    }

    /**
     * Registers {@code camera} as {@code id}, asked before every camera of a lower {@code priority} and after those
     * registered before it of the same.
     *
     * @throws DeclarationError when a camera of that id is registered already
     */
    public static void register(final ResourceLocation id, final int priority, final IThirdPersonCamera camera) {
        synchronized (LOCK) {
            final List<Registered> next = new ArrayList<>(cameras);
            if (next.stream().anyMatch(registered -> registered.id.equals(id))) {
                throw DeclarationError.of(id.getNamespace(), "the third person camera " + id, "is registered twice",
                        "register it once, from the mod's constructor on the player's game");
            }
            next.add(new Registered(id, priority, camera, next.size()));
            next.sort(Comparator.comparingInt(Registered::priority).reversed().thenComparingInt(Registered::order));
            cameras = List.copyOf(next);
        }
    }

    /** The view the cameras give for {@code viewer} now, the first one's that gives one, or none for the game's. */
    @Nullable
    public static ThirdPersonView chosen(final Entity viewer, final boolean front, final float partialTick) {
        for (final Registered registered : cameras) {
            final ThirdPersonView view = registered.camera.view(viewer, front, partialTick);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    /** Forgets the view eased, as the player leaves the world. */
    static void clear() {
        lastFrame = Double.NaN;
        resting = true;
        seconds = ThirdPersonView.EASED;
    }

    /* Stands the player's camera where the cameras say, just before the game brings it nearer for its walls. */
    static void onDistance(final CalculateDetachedCameraDistanceEvent event) {
        final Camera camera = event.getCamera();
        final Entity viewer = camera.getEntity();
        if (camera != Minecraft.getInstance().gameRenderer.getMainCamera() || viewer == null) {
            return;
        }
        final ThirdPersonView view = chosen(viewer, event.isCameraFlipped(), camera.getPartialTickTime());
        final double scale = Math.max(SMALLEST_SCALE, event.getEntityScalingFactor());
        final double now = MotionClock.now() / MILLIS;
        final double gap = Double.isNaN(lastFrame) ? Double.POSITIVE_INFINITY : now - lastFrame;
        lastFrame = now;
        final double[] wanted = new double[AXES];
        wanted[DISTANCE] = event.getDistance() * scale;
        if (resting) {
            if (view == null) {
                return;
            }
            // A view taken from the game's own is eased from where the game's stands.
            ease(wanted, true, 0);
        }
        final Vec3 eye = camera.getPosition();
        if (view != null) {
            wanted[DISTANCE] = view.distance();
            wanted[RIGHT] = view.right();
            wanted[UP] = view.up();
            final Vector3d pivot = view.pivot();
            if (pivot != null) {
                wanted[PIVOT_X] = pivot.x - eye.x;
                wanted[PIVOT_Y] = pivot.y - eye.y;
                wanted[PIVOT_Z] = pivot.z - eye.z;
            }
            seconds = view.seconds();
        }
        ease(wanted, gap > FRESH || MotionClock.reduced() || seconds <= 0, Math.clamp(gap, 0, LONGEST_STEP));
        resting = view == null && there(wanted);
        if (resting) {
            return;
        }
        final Vec3 pivot = eye.add(EASED[PIVOT_X][0], EASED[PIVOT_Y][0], EASED[PIVOT_Z][0]);
        final Vector3f left = camera.getLeftVector();
        final Vector3f up = camera.getUpVector();
        final double right = EASED[RIGHT][0];
        final double raised = EASED[UP][0];
        final Vec3 side = new Vec3(up.x * raised - left.x * right, up.y * raised - left.y * right,
                up.z * raised - left.z * right);
        camera.setPosition(nearer(viewer, pivot, pivot.add(side)));
        event.setDistance((float) (EASED[DISTANCE][0] / scale));
    }

    /* Eases every axis a step towards {@code wanted}, or puts it there at once. */
    private static void ease(final double[] wanted, final boolean atOnce, final double step) {
        for (int axis = 0; axis < AXES; axis++) {
            final double[] eased = EASED[axis];
            if (atOnce) {
                eased[0] = wanted[axis];
                eased[1] = 0;
            } else {
                final double[] next = CameraMotions.settle(eased[0], eased[1], wanted[axis], seconds, step);
                eased[0] = next[0];
                eased[1] = next[1];
            }
        }
    }

    /* Whether every axis eased is where it is wanted. */
    private static boolean there(final double[] wanted) {
        for (int axis = 0; axis < AXES; axis++) {
            if (Math.abs(EASED[axis][0] - wanted[axis]) > THERE) {
                return false;
            }
        }
        return true;
    }

    /* Where between {@code from} and {@code to} the camera can stand, short of the first wall on the way. */
    private static Vec3 nearer(final Entity viewer, final Vec3 from, final Vec3 to) {
        final Vec3 way = to.subtract(from);
        final double length = way.length();
        if (length < THERE) {
            return to;
        }
        final HitResult hit = viewer.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, viewer));
        if (hit.getType() == HitResult.Type.MISS) {
            return to;
        }
        final double reach = Math.max(0, hit.getLocation().distanceTo(from) - CLEAR);
        return from.add(way.scale(reach / length));
    }

    /* A camera registered: its id, its priority, it, and how many were registered before it. */
    private record Registered(ResourceLocation id, int priority, IThirdPersonCamera camera, int order) {
    }
}
