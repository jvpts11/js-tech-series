/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipPlace;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Whether a starship of its parts is whole and flies: a cockpit at the nose and at least one section behind it, every
 * section of the spine where it may go and of the same class, so the spine's joints all fit; every part hung on it in
 * its place (wings at the second section behind the cockpit, nacelles on wings that carry them, the tail engine at
 * the spine's end but never behind a ramp opening there); no more modules than the hull has room for; and the flight
 * check: a cockpit, an airlock, a reactor, a main engine and the gear.
 *
 * <p>Pure: no game types.
 */
public final class ShipChecks {

    private ShipChecks() {
    }

    /** What keeps the ship of {@code spine}, from its nose to its tail, and of the parts {@code hung} on it, down. */
    public static Set<Problem> problems(final List<Piece> spine, final List<Piece> hung) {
        final Set<Problem> out = EnumSet.noneOf(Problem.class);
        if (spine.isEmpty() || spine.get(0).kind() != ShipPartKind.COCKPIT || spine.get(0).place() != ShipPlace.NOSE) {
            out.add(Problem.NO_COCKPIT);
        }
        if (spine.size() < 2) {
            out.add(Problem.BROKEN_SPINE);
        }
        final ShipClass shipClass = spine.isEmpty() ? null : spine.get(0).shipClass();
        int room = 0;
        for (int i = 1; i < spine.size(); i++) {
            final Piece section = spine.get(i);
            room += section.room();
            final boolean last = i == spine.size() - 1;
            if (section.place() != ShipPlace.SPINE && !(section.place() == ShipPlace.TAIL && last)) {
                out.add(Problem.MISPLACED);
            }
        }
        final boolean rampAtTail = spine.size() > 1 && spine.get(spine.size() - 1).place() == ShipPlace.TAIL;
        final boolean carriedWings = hung.stream().anyMatch(part -> part.place() == ShipPlace.WING_ROOT
                && part.carries());
        for (final Piece part : hung) {
            switch (part.place()) {
                case NOSE, SPINE, TAIL -> out.add(Problem.MISPLACED);
                case ON_WINGS -> {
                    if (!carriedWings) {
                        out.add(Problem.MISPLACED);
                    }
                }
                case SPINE_END -> {
                    if (rampAtTail && part.kind() == ShipPartKind.MAIN_ENGINE) {
                        out.add(Problem.MISPLACED);
                    }
                }
                default -> {
                    // Round the ship, under it, along its sides or at its wings' root: always somewhere to go.
                }
            }
        }
        if (shipClass != null && (spine.stream().anyMatch(piece -> piece.shipClass() != shipClass)
                || hung.stream().anyMatch(piece -> piece.shipClass() != shipClass))) {
            out.add(Problem.MIXED_CLASSES);
        }
        if (shipClass != null && room > shipClass.room()) {
            out.add(Problem.TOO_MUCH);
        }
        if (spine.stream().noneMatch(Piece::airlock)) {
            out.add(Problem.NO_AIRLOCK);
        }
        if (spine.stream().noneMatch(piece -> piece.kind() == ShipPartKind.REACTOR)) {
            out.add(Problem.NO_REACTOR);
        }
        if (hung.stream().noneMatch(piece -> piece.kind() == ShipPartKind.MAIN_ENGINE)) {
            out.add(Problem.NO_MAIN_ENGINE);
        }
        if (hung.stream().noneMatch(piece -> piece.kind() == ShipPartKind.GEAR)) {
            out.add(Problem.NO_GEAR);
        }
        return out;
    }

    /** What keeps a ship down. */
    public enum Problem {
        NO_COCKPIT,
        BROKEN_SPINE,
        MISPLACED,
        MIXED_CLASSES,
        TOO_MUCH,
        NO_AIRLOCK,
        NO_REACTOR,
        NO_MAIN_ENGINE,
        NO_GEAR,
        UNKNOWN_PART
    }

    /**
     * A part as the check sees it.
     *
     * @param kind      what it is
     * @param place     where it goes
     * @param shipClass the class of ship it is for
     * @param room      how much of the hull's room it takes
     * @param airlock   whether it holds an airlock
     * @param carries   for wings, whether they carry nacelles
     */
    public record Piece(ShipPartKind kind, ShipPlace place, ShipClass shipClass, int room, boolean airlock,
                        boolean carries) {

        public Piece {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(place, "place");
            Objects.requireNonNull(shipClass, "shipClass");
        }
    }
}
