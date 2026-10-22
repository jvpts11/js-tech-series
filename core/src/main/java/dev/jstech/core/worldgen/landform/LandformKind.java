/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import dev.jstech.core.id.IStableName;

/**
 * The kinds of landform, and how far past its radius each reaches: a crater's ejecta and rays lie beyond its rim.
 * What each lays is said by its {@link LandformConfig}: its {@code surface}, the {@code fill} it is built of, its
 * {@code special} block and its {@code scatter}.
 */
public enum LandformKind implements IStableName {
    /** A bowl with a raised rim, ejecta and rays round it, a central peak in a large one; its floor the surface. */
    CRATER("crater", 2.0),
    /** Ridges of the fill blown across the land, the windward side gentle and the lee steep. */
    DUNES("dunes", 1.0),
    /** A long winding cut with steep walls; its floor the surface, the special lying in its bottom if it says one. */
    CANYON("canyon", 1.0),
    /** A narrow channel winding tightly, a smooth trough as a lava channel whose roof fell; its floor the surface. */
    RILLE("rille", 1.0),
    /** A tunnel under the ground along a line, its floor lined with the special, skylights open to the sky here. */
    LAVA_TUBE("lava_tube", 1.0),
    /** A sheet of the special (ice) over the land, its top the surface (snow). */
    ICE_SHEET("ice_sheet", 1.0),
    /**
     * A tongue of the special (ice) lying down the land along a line, thick near its head and thinning to its snout,
     * its top the surface (snow), cracked by crevasses down its lower half, and the scatter (gravel) heaped as moraines
     * along its edges and before its snout.
     */
    GLACIER("glacier", 1.0),
    /** A mound of the fill (sinter) round a vent going down to the special (water) over the scatter (magma). */
    GEYSER("geyser", 1.0),
    /**
     * A broad dome of the fill (ice), its slopes the surface (snow), the special (water, or any liquid of the cold) in
     * its summit, and what it threw out, the scatter (snow), lying in a blanket round its foot, thinning outwards.
     */
    CRYOVOLCANO("cryovolcano", 1.5),
    /** A round shaft down into the ground, the special (water) in its bottom if it says one. */
    SINKHOLE("sinkhole", 1.0),
    /** Land made level, its top the special (salt), cracked in a net of the scatter. */
    SALT_FLAT("salt_flat", 1.0),
    /** A flat-topped hill with cliffs, its body in bands of the band blocks, its top the surface. */
    MESA("mesa", 1.0),
    /** A broad, gentle cone of the fill, its slopes the surface, the special (lava) in its summit crater. */
    SHIELD_VOLCANO("shield_volcano", 1.0),
    /** A steep cone of the fill, its slopes the surface, the special (lava) in its summit crater. */
    STRATOVOLCANO("stratovolcano", 1.0),
    /** A bowl filled with the special, any liquid, frozen to the scatter where it is cold, dry where it boils away. */
    LAKE("lake", 1.0),
    /** A shallow winding channel filled with the special, any liquid, freezing or boiling away as a lake does. */
    RIVER("river", 1.0);

    private final String serializedName;
    private final double reach;

    LandformKind(final String serializedName, final double reach) {
        this.serializedName = serializedName;
        this.reach = reach;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** How far past its radius it reaches, as a share of it. */
    public double reach() {
        return this.reach;
    }

    /** Whether it lays a liquid, which freezes or boils away by the place's temperature. */
    public boolean liquid() {
        return this == LAKE || this == RIVER;
    }
}
