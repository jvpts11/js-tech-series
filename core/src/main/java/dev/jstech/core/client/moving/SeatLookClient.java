/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.SeatLook;
import dev.jstech.core.moving.StructureSeatEntity;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * The player's own look on a seat of a moving structure, on their game: the mouse turns it in the seat's frame
 * ({@link SeatLook}) while the structure leans or pitches, and as the game turns it otherwise. Walking on the floor of
 * a structure in its frame ({@link DeckWalk}), their look is kept and turned in its frame the same way.
 */
@ApiStatus.Internal
public final class SeatLookClient {

    private SeatLookClient() {
    }

    /** Whether {@code player} sits on a seat of a moving structure, which carries their look as it turns. */
    public static boolean seated(final LocalPlayer player) {
        return player.getVehicle() instanceof StructureSeatEntity seat && seat.structure() != null;
    }

    /**
     * Whether {@code player}'s look is kept in a moving structure's frame: sitting on one of its seats, or walking on
     * its floor in its frame ({@link DeckWalk}).
     */
    public static boolean inFrame(final LocalPlayer player) {
        return seated(player) || DeckWalk.walkingIn(player) != null;
    }

    /**
     * Turns {@code player} by the mouse's {@code yaw} and {@code pitch} in the frame of the seat they sit on, when its
     * structure leans or pitches, or of the structure whose floor they walk on in its frame; whether it did, the
     * game's own turn to be done otherwise.
     */
    public static boolean turn(final LocalPlayer player, final double yaw, final double pitch) {
        final IStructureBody walked = DeckWalk.walkingIn(player);
        if (walked != null) {
            SeatLook.turn(player, walked.frame(), yaw, pitch);
            return true;
        }
        if (!(player.getVehicle() instanceof StructureSeatEntity seat)) {
            return false;
        }
        final IStructureBody body = seat.structure();
        if (body == null || !SeatLook.tilted(body.frame())) {
            return false;
        }
        SeatLook.turn(player, body.frame(), yaw, pitch);
        seat.onPassengerTurned(player);
        return true;
    }
}
