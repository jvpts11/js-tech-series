/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

/**
 * One of a machine's slots where its Main tab's face shows it, in the screen's pixels from its top left, the item
 * inside the slot's frame: the menu places the slot there, and the face draws its frame around it.
 *
 * @param index  the slot of the machine's inventory
 * @param x      across, the item's left
 * @param y      down, the item's top
 * @param output whether only the machine puts into it
 */
public record FaceSlot(int index, int x, int y, boolean output) {
}
