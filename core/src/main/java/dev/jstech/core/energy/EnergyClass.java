/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * A class of line of an energy, as a voltage class is: lines of different classes never join, every port is of one
 * class, and a port that meets a live line of another class fares as its energy's rule says. The classes of an energy
 * are in order, from the lowest to the highest, and a class is so many steps above another by that order.
 *
 * @param path   the class's name in ids, as its grid is named: {@code low}, {@code medium}
 * @param name   what a player calls it
 * @param rating what it is rated at, as a tooltip shows it beside its name ("400 V"); none for a class with no rating
 */
public record EnergyClass(String path, TextKey name, @Nullable TextKey rating) {

    public EnergyClass {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(name, "name");
        if (path.isEmpty() || !path.chars().allMatch(c -> c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
            throw new IllegalArgumentException("a class's path is lower case letters, digits and _, not '" + path + "'");
        }
    }

    /** A class named {@code name}, with no rating. */
    public static EnergyClass of(final String path, final TextKey name) {
        return new EnergyClass(path, name, null);
    }

    /** A class named {@code name}, rated at {@code rating}. */
    public static EnergyClass rated(final String path, final TextKey name, final TextKey rating) {
        return new EnergyClass(path, name, Objects.requireNonNull(rating, "rating"));
    }

    /** What it is rated at, if it says. */
    public Optional<TextKey> ratingKey() {
        return Optional.ofNullable(this.rating);
    }
}
