/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * How a form of a metal is made: what from, by hand and by which machines, each with its yield; whether a die shapes
 * it; whether the work leaves scraps of the metal ({@link FormRoutes#SCRAP_SHARE}).
 *
 * <p>Pure: no game types. Forms and machines are named by their ids' paths, {@code "plate"}, {@code "compressor"}.
 *
 * @param form        the form it makes
 * @param from        the form it is made from
 * @param hand        how a player makes it without a machine, if at all
 * @param handYield   what that yields, if a player can
 * @param machines    the machines that make it, the first first
 * @param leavesScrap whether the work leaves scraps: rolling and drawing do
 */
public record FormRoute(String form, String from, ByHand hand, Optional<Yield> handYield, List<MachineWay> machines,
                        boolean leavesScrap) {

    public FormRoute {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(hand, "hand");
        machines = List.copyOf(machines);
        if ((hand == ByHand.NONE) != handYield.isEmpty()) {
            throw new IllegalArgumentException("the form " + form + " is made by hand with a yield, or not at all");
        }
    }

    /** The way of {@code machine}, or none when it does not make this form. */
    public Optional<MachineWay> by(final String machine) {
        for (final MachineWay way : this.machines) {
            if (way.machine().equals(machine)) {
                return Optional.of(way);
            }
        }
        return Optional.empty();
    }

    /**
     * How a player makes a form without a machine.
     */
    public enum ByHand {
        /** Not at all. */
        NONE,
        /** On an anvil, with the Forge Hammer: plates. */
        FORGE_HAMMER,
        /** Pulled through the Draw Plate: wire. */
        DRAW_PLATE,
        /** Ground in the Mortar: dust. */
        MORTAR,
        /** At a crafting table, with nothing but the hands. */
        CRAFTING_TABLE
    }

    /**
     * One machine that makes the form.
     *
     * @param machine the machine, by its id's path
     * @param tier    its industrial tier
     * @param yield   what it yields
     * @param die     whether it takes the form's die, which shapes the part
     */
    public record MachineWay(String machine, int tier, Yield yield, boolean die) {

        public MachineWay {
            Objects.requireNonNull(machine, "machine");
            Objects.requireNonNull(yield, "yield");
        }
    }
}
