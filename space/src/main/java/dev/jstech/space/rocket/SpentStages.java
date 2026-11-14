/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The stages a rocket lets go in a system's space, with no world under them to fall to: each comes loose with the puff
 * of its ring and is gone from the game once its body is made. A piece still waiting for its plot of the yard when the
 * world is saved stays part of its rocket, as any piece let go does.
 */
public final class SpentStages {

    /* The pieces let go in space, gone once their body is made. */
    private static final Set<UUID> GOING = ConcurrentHashMap.newKeySet();

    private SpentStages() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Parted.class, SpentStages::parted);
    }

    /** The piece {@code piece}, a stage let go in space, is gone once its body is made. */
    static void going(final StructureUuid piece) {
        GOING.add(piece.value());
    }

    private static void parted(final MovingStructureEvent.Parted event) {
        for (final MovingStructure piece : event.pieces()) {
            if (GOING.remove(piece.id().value())) {
                MovingStructures.takeAway(piece);
            }
        }
    }
}
