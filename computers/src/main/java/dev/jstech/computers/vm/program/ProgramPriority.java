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
 * faster; medium and high are run alike. A save writes it by its name.
 */
public enum ProgramPriority implements IStableName {
    /** Passed over every other round. */
    LOW("low"),
    /** What a program runs at when nobody said otherwise, the same as anything at the prompt. */
    MEDIUM("medium"),
    /** Asked for as urgent, and run like medium. */
    HIGH("high");

    private static final StableNames<ProgramPriority> NAMES = StableNames.of(ProgramPriority.class);

    private final String serializedName;

    ProgramPriority(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** The priority a program or a player named, in any case; medium for nothing, or for a name no priority has. */
    public static ProgramPriority named(final String name) {
        return name == null ? MEDIUM : NAMES.byName(name.trim().toLowerCase(Locale.ROOT), MEDIUM);
    }
}
