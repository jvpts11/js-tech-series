/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

/**
 * How an item looks in a slot and in the hand. The generator writes the item model from it.
 */
public sealed interface IItemLook
        permits IItemLook.Standard,
        IItemLook.Parent {

    /** A block's item that shows its block's model, the one named after it. */
    IItemLook OF_BLOCK = Standard.OF_BLOCK;

    /** A flat sprite: the texture {@code item/<id>}. */
    IItemLook FLAT = Standard.FLAT;

    /**
     * Drawn by the renderer of the block's entity rather than by a model, held and placed in the slot the way a block
     * is: for a body too large or too alive for a baked model (a cabinet, an animated machine).
     */
    IItemLook DRAWN_BY_ENTITY = Standard.DRAWN_BY_ENTITY;

    /** A model written by hand in the mod's resources, used as it is: nothing is generated. */
    IItemLook HANDMADE = Standard.HANDMADE;

    /** The model given, as it is: {@code "block/ethernet_cable_core"}. */
    static IItemLook parent(final String model) {
        return new Parent(model);
    }

    /** The looks that need nothing but the item's own id. */
    enum Standard implements IItemLook {
        OF_BLOCK,
        FLAT,
        DRAWN_BY_ENTITY,
        HANDMADE
    }

    /** An item whose model is another model, unchanged. */
    record Parent(String model) implements IItemLook {
    }
}
