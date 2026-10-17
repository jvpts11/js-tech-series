/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * What happens to a sealed room, posted on the game's event bus on the server: a room formed, its shape changed, it
 * filled with air that can be breathed, it began to leak, or it was taken away.
 *
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener((RoomEvent.Leaking event) -> alarm(event.level(), event.room()));
 * }</pre>
 */
public abstract class RoomEvent extends Event {

    private final ServerLevel level;
    private final Room room;

    protected RoomEvent(final ServerLevel level, final Room room) {
        this.level = level;
        this.room = room;
    }

    /** The dimension the room is in. */
    public ServerLevel level() {
        return this.level;
    }

    /** The room. */
    public Room room() {
        return this.room;
    }

    /** A room formed: closed blocks of air found where none was. */
    public static final class Formed extends RoomEvent {

        public Formed(final ServerLevel level, final Room room) {
            super(level, room);
        }
    }

    /** A room's shape changed: it grew, shrank, joined another, or its walls changed. */
    public static final class Changed extends RoomEvent {

        public Changed(final ServerLevel level, final Room room) {
            super(level, room);
        }
    }

    /** A room filled with air that can be breathed, from air that could not. */
    public static final class Filled extends RoomEvent {

        public Filled(final ServerLevel level, final Room room) {
            super(level, room);
        }
    }

    /** A room began to leak: a crack, a door opened to the outside, or a wall broken open. */
    public static final class Leaking extends RoomEvent {

        public Leaking(final ServerLevel level, final Room room) {
            super(level, room);
        }
    }

    /** A room was taken away: filled in, or joined into another. */
    public static final class Removed extends RoomEvent {

        public Removed(final ServerLevel level, final Room room) {
            super(level, room);
        }
    }
}
