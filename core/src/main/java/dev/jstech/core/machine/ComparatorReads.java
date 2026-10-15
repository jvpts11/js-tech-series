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
 * What a comparator reads from a machine, as the player picks it: the progress of the recipe under way, how full its
 * inputs are, how full its outputs are, or its energy.
 */
@TextHolder
public enum ComparatorReads implements IStableName {

    PROGRESS("progress", TextKey.of("jscore.machine.comparator.progress", "Progress")),
    INPUT("input", TextKey.of("jscore.machine.comparator.input", "Input")),
    OUTPUT("output", TextKey.of("jscore.machine.comparator.output", "Output")),
    ENERGY("energy", TextKey.of("jscore.machine.comparator.energy", "Energy"));

    private final String serializedName;
    private final TextKey name;

    ComparatorReads(final String serializedName, final TextKey name) {
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

    /** The choices in the order a selector goes round them, which is also the order of a machine's readings. */
    public static List<ComparatorReads> round() {
        return List.of(PROGRESS, INPUT, OUTPUT, ENERGY);
    }
}
