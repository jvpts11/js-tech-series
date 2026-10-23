/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.api;

import dev.jstech.core.config.ConfigKey;

/**
 * The switch every consequence of J's Industrial has. Overvoltage burns machines, pipes corrode, gases leak and boilers
 * blow: each is real, and each is a setting of its own, on unless a server turns it off. Off, a consequence gives way
 * to what harms nothing (a machine on a line too high just does not start, a blocked boiler just stops), never to a
 * part of the mod being gone: no setting turns a part of it off as a "module".
 *
 * <p>A mod declares the switch once, as a constant in its settings file, and asks it where the consequence happens:
 *
 * <pre>{@code
 * public static final ConfigKey<Boolean> BOILERS_EXPLODE = Consequences.of("fluids.boilers_explode",
 *         "Boilers explode", "A boiler held past its pressure bursts, as a real one does.",
 *         "a blocked boiler just stops.");
 * }</pre>
 */
public final class Consequences {

    private Consequences() {
    }

    /**
     * The switch of a consequence at {@code path} in its mod's settings file, called {@code name} where a settings
     * screen shows it: on unless a server turns it off, its comment saying {@code what} the consequence is and what
     * happens instead {@code whenOff}.
     */
    public static ConfigKey<Boolean> of(final String path, final String name, final String what,
                                        final String whenOff) {
        return ConfigKey.flag(path, true).comment(what, "Off, " + whenOff).named(name);
    }
}
