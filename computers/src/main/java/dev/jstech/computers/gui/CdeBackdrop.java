/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

/**
 * The patterns a CDE workspace's backdrop can wear. A pattern is drawn in the palette's second backdrop colour
 * over its first, never in colours of its own, so no backdrop can clash with the frames standing on it.
 *
 * <p>Pure, with no Minecraft types: which texture draws a pattern is the drawing's business, and this only says
 * what the pattern is called and which mask it is laid with.
 */
public enum CdeBackdrop {

    HATCH("hatch", "Hatch", 0),
    PINSTRIPE("pinstripe", "Pinstripe", 1),
    TILES("tiles", "Tiles", 2),
    WEAVE("weave", "Weave", 3),
    DOTS("dots", "Dots", 4),
    /** The first colour alone, for a workspace that wants nothing on it. */
    PLAIN("plain", "Plain", 5);

    /** How it is written down when a style is kept, which does not change if its label is ever reworded. */
    private final String key;

    /** What the Style Manager lists it as. */
    private final String label;

    /** Its place in the Style Manager's list, said outright rather than read off the order. */
    private final int place;

    CdeBackdrop(final String key, final String label, final int place) {
        this.key = key;
        this.label = label;
        this.place = place;
    }

    /** The pattern written down under that key, or null for none. */
    public static CdeBackdrop keyed(final String key) {
        for (final CdeBackdrop each : values()) {
            if (each.key.equals(key)) {
                return each;
            }
        }
        return null;
    }

    public String key() {
        return this.key;
    }

    public String label() {
        return this.label;
    }

    public int place() {
        return this.place;
    }

    /** The mask the pattern is laid with, by the last part of its texture's name, or empty for the plain one. */
    public String mask() {
        return this == PLAIN ? "" : this.key;
    }
}
