/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.Locale;

/**
 * How urgently a program is run. A low one sits out every other round of its machine's tick, so the others get on
 * faster, and an idle one runs only when every other program waits. On the mod's systems medium and high are run
 * alike; on a kernel a player made, a high one gets two slices a round (computers/docs/design/player-systems.md). A
 * save writes it by its name.
 */
public enum ProgramPriority implements IStableName {
    /** Passed over every other round. */
    LOW("low"),
    /** What a program runs at when nobody said otherwise, the same as anything at the prompt. */
    MEDIUM("medium"),
    /** Asked for as urgent: run like medium, or twice as often on a kernel a player made. */
    HIGH("high"),
    /** Run only when every other program waits. */
    IDLE("idle");

    /* What a player's system calls medium. */
    private static final String NORMAL = "normal";

    private static final StableNames<ProgramPriority> NAMES = StableNames.of(ProgramPriority.class);

    private final String serializedName;

    ProgramPriority(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /**
     * The priority a program or a player named, in any case, normal being medium; medium for nothing, or for a name no
     * priority has.
     */
    public static ProgramPriority named(final String name) {
        if (name == null) {
            return MEDIUM;
        }
        final String word = name.trim().toLowerCase(Locale.ROOT);
        return NORMAL.equals(word) ? MEDIUM : NAMES.byName(word, MEDIUM);
    }
}
