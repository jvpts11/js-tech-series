/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

/**
 * The player's inventory in a menu: the 27 slots of the main grid, then the 9 of the hotbar, added one after the
 * other, so they also make one group.
 *
 * @param main   the main grid
 * @param hotbar the hotbar
 */
public record PlayerSlots(SlotGroup main, SlotGroup hotbar) {

    /** The whole inventory, the main grid and the hotbar together. */
    public SlotGroup all() {
        return new SlotGroup(main.start(), hotbar.end(), true);
    }
}
