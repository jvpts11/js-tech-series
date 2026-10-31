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
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.space.JsSpace;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * A ship's boost, held at its helm: its key, B until the player changes it, and, on the server, when each ship's pilot
 * last held it. Told nothing for a second, it is let go, as the helm's keys are.
 */
public final class ShipBoost {

    /** The key that boosts the ship its player pilots. */
    public static final KeyAction KEY = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_boost"), ShipFlightTexts.BOOST_KEY)
            .key(GLFW.GLFW_KEY_B));
    /* How long a hold lasts untold, in ticks. */
    private static final int LASTS = 20;
    /* When each ship's pilot last held its boost, by the game's tick. */
    private static final Map<MovingStructure, Long> HELD = new WeakHashMap<>();

    private ShipBoost() {
    }

    /** Declares the key, once, from the mod's constructor: loading the class declares it. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** {@code player}'s game says whether they hold the boost: heard only from a pilot at a ship's helm. */
    public static void told(final ServerPlayer player, final boolean held) {
        if (!(player.getVehicle() instanceof StructureSeatEntity seat) || !seat.helm()
                || !(seat.structure() instanceof MovingStructure ship)) {
            return;
        }
        if (held) {
            HELD.put(ship, player.level().getGameTime());
        } else {
            HELD.remove(ship);
        }
    }

    /** Whether {@code ship}'s pilot holds its boost at the game's tick {@code now}. */
    public static boolean held(final MovingStructure ship, final long now) {
        final Long at = HELD.get(ship);
        return at != null && now - at <= LASTS;
    }

    /** Has {@code ship}'s boost held at {@code now}, as a pilot holding its key would: for an autopilot or a test. */
    public static void hold(final MovingStructure ship, final long now) {
        HELD.put(ship, now);
    }
}
