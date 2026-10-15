/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/** What a box is to the overlap check run on the laid-out screen. */
public enum Role {

    /** A box that only holds others: it takes no part in the check, its children do. */
    NONE,
    /** Something solid (a button, a slot, a gauge): two of these may never overlap. */
    SOLID,
    /** A line of text: it may stand on something solid, but never run outside the screen. */
    TEXT
}
