/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

/**
 * What a block's loot table gives when the block is broken.
 */
public enum Drops {

    /** The block's own item. */
    SELF,

    /**
     * Nothing from the table: the block has no item, or it hands its item over itself when it is broken (a
     * multiblock whose controller spills what it holds and pops its item while the rest of the structure is taken
     * down without drops).
     */
    NONE
}
