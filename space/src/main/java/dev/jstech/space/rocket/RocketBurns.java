/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * What a rocket's burns take from its tanks and its boosters, written into its record: the climb, once it is over, and
 * a burn of so much delta-v in a vacuum, as its transfer burn. A stage's tanks are emptied in the order its stages
 * burn, the last one burning taking its share of what it burnt. The boosters' burn takes from the stage's tanks only
 * what its core burns beside them, and burns its boosters down.
 */
public final class RocketBurns {

    private RocketBurns() {
    }

    /**
     * The climb of {@code body} is over, its burn {@code lit} burning with {@code mass} kilograms left of the whole
     * rocket: the stages it burnt out are empty, and the one burning keeps what it has not burnt.
     */
    public static void climbed(final ServerLevel level, final MovingStructure body, final int lit, final double mass) {
        final List<StageMath.Burn> burns = RocketAscent.burns(level, body);
        for (int i = 0; i < burns.size() && i <= lit; i++) {
            final StageMath.Burn burn = burns.get(i);
            final double held = burn.startMass() - burn.endMass();
            final double used = i < lit ? held : Math.clamp(burn.startMass() - mass, 0, held);
            take(level, body, burn, held <= 0 ? 0 : used / held);
        }
    }

    /**
     * Burns {@code deltaV} metres a second in a vacuum from the stages of {@code body} not let go, those after the
     * first {@code separated}; whether its stages had that much in them.
     */
    public static boolean burn(final ServerLevel level, final MovingStructure body, final int separated,
                               final double deltaV) {
        double left = deltaV;
        for (final StageMath.Burn burn : RocketAscent.burns(level, body)) {
            if (burn.stage() <= separated || left <= 0 || burn.vacuumDeltaV() <= 0) {
                continue;
            }
            if (left >= burn.vacuumDeltaV()) {
                take(level, body, burn, 1);
                left -= burn.vacuumDeltaV();
                continue;
            }
            // The rocket's equation: the mass left after burning {@code left} of what the burn could give.
            final double exhaust = burn.vacuumDeltaV() / Math.log(burn.startMass() / burn.endMass());
            final double used = burn.startMass() * (1 - Math.exp(-left / exhaust));
            take(level, body, burn, used / (burn.startMass() - burn.endMass()));
            left = 0;
        }
        return left <= 0;
    }

    /*
     * Takes what {@code share} of {@code burn} burns from {@code body}'s rocket: that much of the share of its stage's
     * tanks the burn burns, and, in the boosters' burn, that much of what its boosters still hold.
     */
    private static void take(final ServerLevel level, final MovingStructure body, final StageMath.Burn burn,
                             final double share) {
        final Optional<Rockets.Rocket> rocket = Rockets.settle(level.getServer(), body.id());
        if (rocket.isEmpty() || share <= 0) {
            return;
        }
        final double burnt = Math.clamp(share, 0, 1);
        final double core = burnt * Math.clamp(burn.coreShare(), 0, 1);
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final Map<ResourceLocation, Propellant> propellants = Propellants.all(level);
        for (final int section : RocketStages.stage(parts, rocket.get().stack(), burn.stage())) {
            final TankLoad load = rocket.get().tanks().get(section);
            /*
             * The burn is reckoned in what the engine can burn, so that is what goes: each material by its share, and
             * what is left over of one, which was never burnt, stays in the tank.
             */
            final Propellant propellant = load == null ? null : propellants.get(load.propellant());
            if (load != null && propellant != null && core > 0) {
                Rockets.load(level.getServer(), body.id(), section, load.burned(load.propellant(), propellant, core));
            }
            if (burn.boosters() && !rocket.get().stack().sections().get(section).boosters().isEmpty()) {
                Rockets.boosters(level.getServer(), body.id(), section,
                        rocket.get().boosters(section) * (1 - burnt));
            }
        }
    }
}
