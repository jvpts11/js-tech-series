/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The clock the Core's models are animated by: the world's own, so an animation stands still while the game is paused
 * and goes on with the world; the wall clock where there is no world, as on the title screen. And the count of frames,
 * by which a thing drawn more than once in a frame is animated once.
 */
public final class KitClock {

    private static final double TICKS_PER_SECOND = 20.0;

    private static long frames;

    private KitClock() {
    }

    /** Counts the frames, once, from the client setup. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, event -> frames++);
    }

    /** The time now, in seconds, {@code partialTick} of a tick after the world's last. */
    public static double seconds(final float partialTick) {
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return Util.getMillis() / 1000.0;
        }
        return (game.level.getGameTime() + partialTick) / TICKS_PER_SECOND;
    }

    /** The time now, in seconds, with the frame's own part of a tick. */
    public static double now() {
        return seconds(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    /** The frame being drawn, a number that is the same through one frame and different in the next. */
    public static long frame() {
        return frames;
    }
}
