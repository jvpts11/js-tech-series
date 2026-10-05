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

    /** The step's place along its axis, counted from 0. */
    int level();

    /** The name the step is saved and written by in data files, lower case: {@code legacy}, {@code t3}. */
    String serializedName();

    /** The step's name as a player reads it. */
    Text text();
}
