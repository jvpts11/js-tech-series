/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureSeatEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * A key a ship's pilot holds at its helm, as the server hears it: when each ship's pilot last held it, heard only
 * from a pilot sitting at a ship's helm. Told nothing for a second, it is let go, as the helm's own keys are, so a
 * pilot whose game went quiet does not hold it on.
 */
final class HeldAtTheHelm {

    private final Map<MovingStructure, Long> held = new WeakHashMap<>();
    /* How long a hold lasts untold, in ticks. */
    private static final int LASTS = 20;

    /** {@code player}'s game says whether they hold the key: heard only from a pilot at a ship's helm. */
    void told(final ServerPlayer player, final boolean holds) {
        if (!(player.getVehicle() instanceof StructureSeatEntity seat) || !seat.helm()
                || !(seat.structure() instanceof MovingStructure ship)) {
            return;
        }
        if (holds) {
            held.put(ship, player.level().getGameTime());
        } else {
            held.remove(ship);
        }
    }

    /** Whether {@code ship}'s pilot holds the key at the game's tick {@code now}. */
    boolean held(final MovingStructure ship, final long now) {
        final Long at = held.get(ship);
        return at != null && now - at <= LASTS;
    }

    /** Has {@code ship}'s key held at {@code now}, as a pilot holding it would. */
    void hold(final MovingStructure ship, final long now) {
        held.put(ship, now);
    }
}
