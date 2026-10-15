/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

/**
 * The control a settings screen gives a setting where its kind of value alone does not say: a number with a range is a
 * slider anyway, a word from a list a drop-down list; a colour, and a list or a single id of the game's items, blocks
 * or fluids, are said with this.
 */
public enum ConfigControl {

    /** What the kind of value says. */
    AUTO,
    /** A colour, picked on a colour picker; the setting holds {@code #RRGGBB}. */
    COLOUR,
    /** A list of item ids, each picked from the game's items. */
    ITEMS,
    /** A list of block ids. */
    BLOCKS,
    /** A list of fluid ids. */
    FLUIDS,
    /** One item id. */
    ITEM,
    /** One block id. */
    BLOCK,
    /** One fluid id. */
    FLUID
}
