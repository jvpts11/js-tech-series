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
     * The block's own item, carrying what its block entity keeps as components (a tank's fluid, say), so placing the
     * item again gives the block back as it was. The block entity says what it keeps by writing it into the item.
     */
    SELF_WITH_CONTENTS,

    /**
     * Nothing from the table: the block has no item, or it hands its item over itself when it is broken (a
     * multiblock whose controller spills what it holds and pops its item while the rest of the structure is taken
     * down without drops).
     */
    NONE
}
