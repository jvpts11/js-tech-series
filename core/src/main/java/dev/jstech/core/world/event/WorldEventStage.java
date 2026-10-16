/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * A world event moving on, posted on the game's bus: warned of, begun, ended. A mod does what its event does from
 * here: rains meteors while a shower lasts, shakes the ground in an earthquake.
 *
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener(WorldEventStage.Started.class, stage -> {
 *     if (stage.event().type().equals(SOLAR_STORM)) {
 *         Grids.surge(stage.level());
 *     }
 * });
 * }</pre>
 */
public abstract class WorldEventStage extends Event {

    private final ServerLevel level;
    private final WorldEvent event;
    private final WorldEventType type;

    protected WorldEventStage(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
        this.level = level;
        this.event = event;
        this.type = type;
    }

    public ServerLevel level() {
        return this.level;
    }

    /** The happening, its times and its place. */
    public WorldEvent event() {
        return this.event;
    }

    /** Its kind. */
    public WorldEventType type() {
        return this.type;
    }

    /**
     * Whether it may change the world (craters, radiation, cracks), as the server's settings say: a mod still plays
     * its effects and warns, but leaves the world as it is when this is false.
     */
    public boolean consequences() {
        return !WorldEventBalance.harmless(this.event.type());
    }

    /** Those carrying its instrument have been warned that it is coming. */
    public static final class Warned extends WorldEventStage {

        public Warned(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
            super(level, event, type);
        }
    }

    /** It has begun. */
    public static final class Started extends WorldEventStage {

        public Started(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
            super(level, event, type);
        }
    }

    /** It is over, its time run out or stopped. */
    public static final class Ended extends WorldEventStage {

        public Ended(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
            super(level, event, type);
        }
    }
}
