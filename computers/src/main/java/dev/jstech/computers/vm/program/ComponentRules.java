/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.function.Function;

/**
 * How the runtime looks up the kinds of generic component the mods added.
 *
 * <p>The runtime knows nothing of mods: the game installs the lookup once as it loads, and until it does, or in a
 * runtime run with no game around it, no kind is known. A component of a kind nobody knows keeps its value and hears
 * nothing from a screen, since nobody can say what it would take.
 */
public final class ComponentRules {

    private static volatile Function<String, IComponentRule> lookup = id -> null;

    private ComponentRules() {
    }

    /** Installs how a kind is looked up by its name. */
    public static void install(final Function<String, IComponentRule> found) {
        lookup = found == null ? id -> null : found;
    }

    /** The kind of that name, or null when none is known. */
    public static IComponentRule find(final String id) {
        return id == null ? null : lookup.apply(id);
    }
}
