/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * A ship's boost, held at its helm: its key, left Ctrl until the player changes it, and, on the server, when each
 * ship's pilot last held it ({@link HeldAtTheHelm}). The game's sprint key starts on the same key, but a pilot at a
 * helm has nothing to sprint with.
 */
public final class ShipBoost {

    /** The key that boosts the ship its player pilots. */
    public static final KeyAction KEY = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_boost"), ShipFlightTexts.BOOST_KEY)
            .key(GLFW.GLFW_KEY_LEFT_CONTROL).atTheHelm());
    /* When each ship's pilot last held its boost. */
    private static final HeldAtTheHelm HELD = new HeldAtTheHelm();

    private ShipBoost() {
    }

    /** Declares the key, once, from the mod's constructor: loading the class declares it. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** {@code player}'s game says whether they hold the boost: heard only from a pilot at a ship's helm. */
    public static void told(final ServerPlayer player, final boolean held) {
        HELD.told(player, held);
    }

    /** Whether {@code ship}'s pilot holds its boost at the game's tick {@code now}. */
    public static boolean held(final MovingStructure ship, final long now) {
        return HELD.held(ship, now);
    }

    /** Has {@code ship}'s boost held at {@code now}, as a pilot holding its key would: for an autopilot or a test. */
    public static void hold(final MovingStructure ship, final long now) {
        HELD.hold(ship, now);
    }
}
