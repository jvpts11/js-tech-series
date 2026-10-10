/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.JsComputers;

/**
 * The shape of the ids hardware is known by, kept in one place so the kinds of hardware that carry an id cannot drift
 * apart on what a valid one is. Plain text, so hardware stays testable without Minecraft loaded.
 */
final class HardwareIds {

    private HardwareIds() {
    }

    /**
     * Checks that {@code id} reads {@code namespace:path}, with something on both sides of the colon.
     *
     * @param id   the id to check
     * @param what what the id names, for the message ("a socket", "an ISA")
     * @throws IllegalArgumentException when the id does not have that shape
     */
    static void requireNamespaced(final String id, final String what) {
        final int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException(what + " id reads namespace:path; got '" + id + "'");
        }
    }

    /**
     * The id of one of this mod's own hardware under {@code path}. The mod id is a constant the compiler writes in
     * here as the text itself, so naming it does not drag the mod class, and Minecraft with it, into hardware that is
     * tested without the game.
     */
    static String own(final String path) {
        return JsComputers.MODID + ":" + path;
    }
}
