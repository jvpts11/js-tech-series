/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * A dimension is about to harm a player: its air is about to choke them, its heat to burn them, its cold to freeze
 * them. Posted on the game's event bus once a second for each hazard the player stands in; cancelling it spares them,
 * which is what a space suit, a heat shield or a warm coat of another mod does.
 */
public final class DimensionHazardEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final Hazard hazard;
    private final DimensionRules rules;

    public DimensionHazardEvent(final Player player, final Hazard hazard, final DimensionRules rules) {
        this.player = player;
        this.hazard = hazard;
        this.rules = rules;
    }

    /** What a dimension does to a player not made for it. */
    public enum Hazard {
        /** The air cannot be breathed. */
        AIR,
        /** It is hot enough to burn. */
        HEAT,
        /** It is cold enough to freeze. */
        COLD
    }

    public Player player() {
        return player;
    }

    public Hazard hazard() {
        return hazard;
    }

    /** The rules of the dimension the player stands in. */
    public DimensionRules rules() {
        return rules;
    }
}
