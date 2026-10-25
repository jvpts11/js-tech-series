/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.text.Text;

/**
 * One step along a {@link ProgressionAxis}: an era of hardware, a tier of industry, an age a mod counts its own way.
 * The steps of an axis have levels counted from 0 without gaps, and a step is reached once every step below it is.
 */
public interface IAxisStep {

    /** The colour a step is shown in when it says none of its own: the grey of a tooltip's second line. */
    int PLAIN_COLOUR = 0xAAAAAA;

    /** The step's place along its axis, counted from 0. */
    int level();

    /** The name the step is saved and written by in data files, lower case: {@code jscore:legacy}, {@code t3}. */
    String serializedName();

    /** The step's name as a player reads it. */
    Text text();

    /** Whether a save or a data file that names {@code name} means this step: its own saved name unless told. */
    default boolean answersTo(final String name) {
        return serializedName().equals(name);
    }

    /** The step's name with what it is a step of, as a tooltip says it ("Legacy era"); its bare name unless told. */
    default Text named() {
        return text();
    }

    /** The colour the step is shown in, as an RGB int, so a step reads at a glance before its word does. */
    default int colour() {
        return PLAIN_COLOUR;
    }
}
