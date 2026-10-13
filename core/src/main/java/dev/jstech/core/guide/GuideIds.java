/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

/** The parts of a guide's ids, which are text in the {@code namespace:path} shape so the layout needs no game. */
public final class GuideIds {

    private GuideIds() {
    }

    /** The namespace of an id, or {@code minecraft} for an id that names none, as the game reads it. */
    public static String namespace(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }

    /** The path of an id. */
    public static String path(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    /** The id of that path in that namespace. */
    public static String of(final String namespace, final String path) {
        return namespace + ":" + path;
    }
}
