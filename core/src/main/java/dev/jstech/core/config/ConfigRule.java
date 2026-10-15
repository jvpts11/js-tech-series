/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A rule between a file's settings, such as "the smallest at most the largest": checked as the file is read, its
 * breach written to the log, and checked as a player changes the settings, its breach said on the screen, with Save
 * held back until it is put right.
 *
 * @param message what is said when it is broken, translated
 * @param holds   whether it holds for the values in hand
 */
public record ConfigRule(TextKey message, Predicate<IConfigValues> holds) {

    public ConfigRule {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(holds, "holds");
    }
}
