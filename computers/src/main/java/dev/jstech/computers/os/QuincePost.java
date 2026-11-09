/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.Objects;

/**
 * What the self-test of one of Quince's machines puts on the glass (computers/docs/design/quince.md), beside what every
 * self-test does: how the machine starts, whether it stands at the start-up choices a sneaking power-on opened, and
 * whether its parts make no computer, which is the Sad Quince's.
 *
 * @param start   how it starts; {@link QuinceStart#NONE} for any machine that is not Quince's
 * @param choices whether it stands at its start-up choices
 * @param fault   whether it stands at a hardware fault, the self-test's complaint naming it
 */
public record QuincePost(QuinceStart start, boolean choices, boolean fault) {

    /** What any machine that is not Quince's shows: nothing of Quince's. */
    public static final QuincePost NONE = new QuincePost(QuinceStart.NONE, false, false);

    public QuincePost {
        Objects.requireNonNull(start, "start");
    }

    /** Whether the machine is one of Quince's, so its firmware draws its self-test. */
    public boolean quince() {
        return start != QuinceStart.NONE;
    }
}
