/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import java.util.Map;

/**
 * One step a settings file takes from one version of its layout to the next: a value renamed, moved, split or
 * written in another unit. A file several versions behind takes every step from its version on, in order, before
 * its values are read, so what a person set survives the layout changing under it.
 */
@FunctionalInterface
public interface IConfigUpgrade {

    /** Rewrites the file's values, read in the version before this step, into the version after it. */
    void upgrade(Map<String, Object> values);

    /** The value at {@code from}, if there is one, moved to {@code to}; both are dotted paths. */
    static IConfigUpgrade rename(final String from, final String to) {
        return values -> {
            final Object value = ConfigTree.remove(values, ConfigTree.path(from));
            if (value != null) {
                ConfigTree.put(values, ConfigTree.path(to), value);
            }
        };
    }

    /** The value at a dotted path taken out, for a setting that is no more. */
    static IConfigUpgrade remove(final String path) {
        return values -> ConfigTree.remove(values, ConfigTree.path(path));
    }
}
