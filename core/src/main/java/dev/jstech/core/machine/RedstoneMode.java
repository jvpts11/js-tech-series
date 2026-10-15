/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * How a machine answers redstone: it runs whatever the signal, only with a signal, only without one, or one recipe
 * for each pulse it is given.
 */
@TextHolder
public enum RedstoneMode implements IStableName {

    IGNORE("ignore", TextKey.of("jscore.machine.redstone.ignore", "Ignore")),
    HIGH("high", TextKey.of("jscore.machine.redstone.high", "With a signal")),
    LOW("low", TextKey.of("jscore.machine.redstone.low", "Without a signal")),
    PULSE("pulse", TextKey.of("jscore.machine.redstone.pulse", "One recipe a pulse"));

    private final String serializedName;
    private final TextKey name;

    RedstoneMode(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }

    /** The modes in the order a selector goes round them. */
    public static List<RedstoneMode> round() {
        return List.of(IGNORE, HIGH, LOW, PULSE);
    }
}
