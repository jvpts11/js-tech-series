/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

import dev.jstech.core.JsCore;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionSpec;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The one clock every motion on the screen is read against: the client's ticks, and how far the frame is into the
 * next one.
 *
 * <p>A motion holds no timer of its own; it is read against this each frame, so everything that moves moves on the
 * same clock, smoothly between ticks, and stops with the game when the game stops. With "reduce motion" on, every
 * motion started is already over: whatever moves is drawn where it ends, at once, which is also what the client tests
 * draw against.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class MotionClock {

    /** How long a tick lasts, in milliseconds. */
    private static final double TICK_MS = 50.0;

    private static long ticks;
    private static volatile boolean reduced;

    private MotionClock() {
    }

    /** The time now, in milliseconds on this clock, at the frame being drawn. */
    public static double now() {
        return (ticks + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) * TICK_MS;
    }

    /** A motion of that kind starting now; one already over when nothing moves or motion is reduced. */
    public static Motion start(final MotionSpec spec) {
        return reduced || !spec.moves() ? Motion.FINISHED : new Motion(spec, now());
    }

    /**
     * The clock in ticks, smooth between them, for what turns round and round while a machine waits (a running bar,
     * a ring of dots): held at 0 while motion is reduced, so each is drawn still in its first position.
     */
    public static double loopTicks() {
        return reduced ? 0.0 : now() / TICK_MS;
    }

    /** Whether every motion is drawn where it ends, at once. */
    public static boolean reduced() {
        return reduced;
    }

    /** Draws every motion from now on where it ends, at once, or lets them move again. */
    public static void setReduced(final boolean value) {
        reduced = value;
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        ticks++;
    }
}
