/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import java.util.Optional;

/**
 * How a family of minerals is processed, by its chemistry: whether a furnace smelts the raw mineral at the start, as it
 * does a native metal, an oxide or a carbonate, or whether it must first be broken down, as a sulfide is roasted and an
 * arsenide gives off its arsenic. A family another mod adds and this does not know is not smelted raw.
 *
 * <p>Pure: no game types. Each family is named by its id's path, {@code "sulfide"}.
 */
public enum FamilyRoute {
    /** Roasted, which gives sulfur dioxide and from it sulfuric acid. */
    SULFIDE("sulfide", false),
    /** Reduced, in a blast furnace or an electric one. */
    OXIDE("oxide", true),
    /** Calcined, which gives carbon dioxide. */
    CARBONATE("carbonate", true),
    /** Harder to break down. */
    SILICATE("silicate", false),
    /** Gives arsenic, which is toxic. */
    ARSENIDE("arsenide", false),
    /** A native metal or a natural alloy, which needs no breaking down. */
    NATIVE("native", true),
    /** Each of the rest by its own chemistry. */
    HALIDE("halide", false),
    SULFATE("sulfate", false),
    PHOSPHATE("phosphate", false),
    ARSENATE("arsenate", false),
    VANADATE("vanadate", false);

    private final String path;
    private final boolean smeltsRaw;

    FamilyRoute(final String path, final boolean smeltsRaw) {
        this.path = path;
        this.smeltsRaw = smeltsRaw;
    }

    /** The family whose id's path is {@code path}, or none for a family this does not know. */
    public static Optional<FamilyRoute> of(final String path) {
        for (final FamilyRoute route : values()) {
            if (route.path.equals(path)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    /** Whether a raw mineral of {@code familyPath} goes in a furnace at the start; none of an unknown family does. */
    public static boolean smeltsRaw(final String familyPath) {
        return of(familyPath).map(FamilyRoute::smeltsRaw).orElse(false);
    }

    /** Its id's path: {@code "sulfide"}. */
    public String path() {
        return this.path;
    }

    /** Whether a furnace smelts a raw mineral of the family at the start, the first level. */
    public boolean smeltsRaw() {
        return this.smeltsRaw;
    }
}
