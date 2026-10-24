/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import java.util.List;

/**
 * The names the series' mods share for modules, so one mod's module fits another's host with neither knowing the
 * other: the kinds of socket of the series' tools and armour, the piece of armour a host is, and whether it is sealed.
 * J's Industrial's suits are hosts of them; J's Space's space module needs {@link #SEALED}, so it fits only the closed
 * exo-suits, never an open frame.
 *
 * <pre>{@code
 * Modules.declareHost(EXO_CHEST.getId(), ModuleHostDef.worn(List.of(new ModuleHostDef.Socket(SeriesModules.ARMOUR, 4)))
 *         .with(SeriesModules.CHEST, SeriesModules.SEALED));
 * Modules.declare(SPACE_MODULE.getId(),
 *         new ModuleDef(SeriesModules.ARMOUR, 50, effects).needing(SeriesModules.SEALED));
 * }</pre>
 */
public final class SeriesModules {

    /** The socket of a piece of armour or suit. */
    public static final String ARMOUR = "armour";
    /** The socket of a tool. */
    public static final String TOOL = "tool";
    /** The pieces of armour, as traits of their hosts. */
    public static final String HEAD = "head";
    public static final String CHEST = "chest";
    public static final String LEGS = "legs";
    public static final String FEET = "feet";
    /** A host that closes its wearer in: a sealed suit, which a module of the vacuum or the deep may need. */
    public static final String SEALED = "sealed";

    private SeriesModules() {
    }

    /** The pieces of armour, head to feet. */
    public static List<String> pieces() {
        return List.of(HEAD, CHEST, LEGS, FEET);
    }
}
