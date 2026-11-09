/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One of Quince's computers as its case makes it (computers/docs/design/quince.md): which era it is of, the one logic
 * board it takes, what kind of machine it is, where its power button is and how much heat its case carries away.
 *
 * <p>An all-in-one carries its screen in its own block and needs no monitor; the Studio is a small box with its ports
 * in front; a tower opens on its left side as a PC does. The all-in-ones and the Studio have their power button behind,
 * as the machines they are drawn after did, and the towers on their front, where their model draws it. None of them
 * has a bay for a drive, so its slots and trays are drawn and drives go in through the drives that stand beside it.
 *
 * @param model    the name its model and texture go by, such as {@code quince_russet_128k}
 * @param era      its era
 * @param board    the form of the one logic board it takes
 * @param kind     what kind of machine it is
 * @param power    its power button: on its front for a tower, on its back for the others, in sixty-fourths of that
 *                 face from its top left as it is seen
 * @param glass    an all-in-one's screen on its front; null for the others, which show theirs on a monitor
 * @param coolWatt the heat its case carries away, in watts; it has no fan to add
 */
public record QuinceCase(String model, Era era, FormFactor board, Kind kind, FrontArea power,
                         @Nullable CaseGlass glass, int coolWatt) {

    /** What kind of machine it is. */
    public enum Kind {
        /** The screen in its own block, the cables only from behind. */
        ALL_IN_ONE,
        /** A tower that opens on its left side. */
        TOWER,
        /** A small box, its cables only from behind. */
        STUDIO
    }

    public QuinceCase {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(power, "power");
        if ((kind == Kind.ALL_IN_ONE) != (glass != null)) {
            throw new IllegalArgumentException("an all-in-one, and only an all-in-one, carries its own glass");
        }
        if (coolWatt < 0) {
            throw new IllegalArgumentException("a case carries no less than no heat away");
        }
    }

    /** Whether its power button is behind it rather than on its front. */
    public boolean powerBehind() {
        return kind != Kind.TOWER;
    }

    /** Whether its cables come in only from behind. */
    public boolean cablesBehind() {
        return kind != Kind.TOWER;
    }

    /** The front a click on it is read against: the power button of a tower, and nothing else. */
    public CaseFrontLayout front() {
        return new CaseFrontLayout(powerBehind() ? FrontArea.NOWHERE : power, List.of(), List.of());
    }

    /** What its case cools. */
    public CaseCooling cooling() {
        return new CaseCooling(coolWatt, 0, 0, 0);
    }
}
