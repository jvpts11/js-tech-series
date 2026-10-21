/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.event.ICoreEvent;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * A view a player saw from elsewhere ended, on the server: posted on the Core's events, so the mod that started a
 * scene carries on when it is over or skipped (the rocket lifts off, the door opens).
 *
 * @param player who saw it
 * @param view   the view
 * @param ending how it ended
 */
public record CameraViewEvent(ServerPlayer player, CameraView view, Ending ending) implements ICoreEvent {

    @Override
    public String eventId() {
        return "camera.view_ended";
    }

    /** How a view ended. */
    public enum Ending {
        /** It played to its end: a scene's last keyframe. */
        FINISHED(0),
        /** The player sneaked to skip or leave it. */
        SKIPPED(1),
        /** What it showed is gone from the player's game: the entity it watched, or the path a scene named. */
        LOST(2),
        /** The server ended it, or started another. */
        STOPPED(3);

        private final int code;

        Ending(final int code) {
            this.code = code;
        }

        /** How it is sent. */
        public int code() {
            return this.code;
        }

        /** The ending sent as {@code code}, or none. */
        public static Optional<Ending> byCode(final int code) {
            for (final Ending ending : values()) {
                if (ending.code == code) {
                    return Optional.of(ending);
                }
            }
            return Optional.empty();
        }
    }
}
