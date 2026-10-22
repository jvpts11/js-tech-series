/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.CameraMotions;
import dev.jstech.core.camera.CameraShake;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.effect.EffectSettings;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3d;

/**
 * The view moved on its own, on the player's game, for any mod: shaken by what shakes it (a blast, a rocket lifting,
 * a machine's rumble), each shake wandering smoothly and dying away, several at once adding up; and turned by what
 * leans it (a cockpit banking, a recoil, a ship's deck rolling), each owner's turn eased towards where it wants it and
 * back as it lets go, as fast as it can without swinging past. The player's shake setting scales every shake, and
 * asking for less movement stills them all.
 *
 * <pre>{@code
 * CameraMotion.shake(CameraShake.of(0.6, 1.5).from(blast, 24));   // felt up to 24 blocks off
 * CameraMotion.lean(this, 0, 0, bank, 0.4);                         // the view banks with the aircraft
 * CameraMotion.release(this);                                       // and comes back level
 * }</pre>
 */
public final class CameraMotion {

    private static final List<Running> SHAKES = new ArrayList<>();
    private static final Map<Object, Lean> LEANS = new IdentityHashMap<>();
    private static final double MILLIS = 1000.0;
    /* A lean let go is dropped once it is this near level, in degrees. */
    private static final double LEVEL = 1.0e-3;
    /* The longest step a frame takes of the easing, in seconds, so a stall does not throw the view. */
    private static final double LONGEST_STEP = 0.1;
    private static final long YAW = 1L;
    private static final long PITCH = 2L;
    private static final long ROLL = 3L;

    private static double lastFrame = Double.NaN;
    private static Turn last = Turn.NONE;

    private CameraMotion() {
    }

    /** Shakes the view by {@code shake}, from now. */
    public static void shake(final CameraShake shake) {
        SHAKES.add(new Running(shake, MotionClock.now() / MILLIS, SHAKES.size() * 7L + 11L));
    }

    /**
     * Has {@code owner} hold the view turned by {@code yaw}, {@code pitch} and {@code roll} degrees, eased there over
     * about {@code seconds}; asked again, it eases to the new turn from where it is.
     */
    public static void lean(final Object owner, final float yaw, final float pitch, final float roll,
                            final double seconds) {
        LEANS.computeIfAbsent(owner, key -> new Lean()).aim(yaw, pitch, roll, seconds, false);
    }

    /** Has {@code owner} let the view go: its turn eases back to none, and then it is gone. */
    public static void release(final Object owner) {
        final Lean lean = LEANS.get(owner);
        if (lean != null) {
            lean.aim(0, 0, 0, lean.seconds, true);
        }
    }

    /** How the view was turned in the last frame by every shake and lean together, in degrees. */
    public static Turn turn() {
        return last;
    }

    /** Whether anything shakes or leans the view now. */
    public static boolean active() {
        return !SHAKES.isEmpty() || !LEANS.isEmpty();
    }

    /** Lets every shake and lean go at once, as the player leaves the world. */
    static void clear() {
        SHAKES.clear();
        LEANS.clear();
        lastFrame = Double.NaN;
        last = Turn.NONE;
    }

    /* Turns the view by the shakes and leans of this frame. */
    static void onAngles(final ViewportEvent.ComputeCameraAngles event) {
        final double now = MotionClock.now() / MILLIS;
        final double step = Double.isNaN(lastFrame) ? 0 : Math.clamp(now - lastFrame, 0, LONGEST_STEP);
        lastFrame = now;
        double yaw = 0;
        double pitch = 0;
        double roll = 0;
        for (final Iterator<Map.Entry<Object, Lean>> it = LEANS.entrySet().iterator(); it.hasNext();) {
            final Lean lean = it.next().getValue();
            lean.step(step);
            if (lean.letGo && lean.level()) {
                it.remove();
                continue;
            }
            yaw += lean.yaw[0];
            pitch += lean.pitch[0];
            roll += lean.roll[0];
        }
        final double shaken = MotionClock.reduced() ? 0 : EffectSettings.shake();
        final Vec3 eye = event.getCamera().getPosition();
        for (final Iterator<Running> it = SHAKES.iterator(); it.hasNext();) {
            final Running running = it.next();
            final double elapsed = now - running.began;
            if (elapsed >= running.shake.seconds()) {
                it.remove();
                continue;
            }
            final Vector3d from = running.shake.from();
            final double away = from == null ? 0 : from.distance(eye.x, eye.y, eye.z);
            final double degrees = CameraMotions.shakeDegrees(running.shake.felt(away) * running.shake.left(elapsed)
                    * shaken);
            if (degrees <= 0) {
                continue;
            }
            final double time = elapsed * running.shake.roughness();
            yaw += degrees * CameraMotions.noise(time, running.seed * YAW);
            pitch += degrees * CameraMotions.noise(time, running.seed * PITCH);
            roll += degrees * 0.5 * CameraMotions.noise(time, running.seed * ROLL);
        }
        last = new Turn((float) yaw, (float) pitch, (float) roll);
        if (yaw != 0 || pitch != 0 || roll != 0) {
            event.setYaw(event.getYaw() + (float) yaw);
            event.setPitch(event.getPitch() + (float) pitch);
            event.setRoll(event.getRoll() + (float) roll);
        }
    }

    /**
     * How the view is turned, in degrees.
     *
     * @param yaw   to the right
     * @param pitch down
     * @param roll  the right side down
     */
    public record Turn(float yaw, float pitch, float roll) {

        /** Not turned at all. */
        public static final Turn NONE = new Turn(0, 0, 0);
    }

    /* A shake under way: what it is, when it began, in seconds, and the seed of its noise. */
    private record Running(CameraShake shake, double began, long seed) {
    }

    /* An owner's turn of the view: where it is and how fast it goes on each axis, where it is wanted, how quickly. */
    private static final class Lean {

        private final double[] yaw = new double[2];
        private final double[] pitch = new double[2];
        private final double[] roll = new double[2];
        private double toYaw;
        private double toPitch;
        private double toRoll;
        private double seconds = 0.3;
        private boolean letGo;

        void aim(final double yawTo, final double pitchTo, final double rollTo, final double over,
                 final boolean going) {
            this.toYaw = yawTo;
            this.toPitch = pitchTo;
            this.toRoll = rollTo;
            this.seconds = Math.max(0.0, over);
            this.letGo = going;
        }

        void step(final double step) {
            settle(this.yaw, this.toYaw, step);
            settle(this.pitch, this.toPitch, step);
            settle(this.roll, this.toRoll, step);
        }

        boolean level() {
            return Math.abs(this.yaw[0]) < LEVEL && Math.abs(this.pitch[0]) < LEVEL && Math.abs(this.roll[0]) < LEVEL;
        }

        private void settle(final double[] axis, final double target, final double step) {
            if (this.seconds <= 0) {
                axis[0] = target;
                axis[1] = 0;
                return;
            }
            final double[] next = CameraMotions.settle(axis[0], axis[1], target, this.seconds, step);
            axis[0] = next[0];
            axis[1] = next[1];
        }
    }
}
