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
    /** A tunnel under the ground along a line, walled all round in the special, skylights open to the sky here. */
    LAVA_TUBE("lava_tube", 1.05),
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
    RIVER("river", 1.0),
    /**
     * An apron of the fill sloping down from its middle, stepped into level pools, each held at its lower edge by a rim
     * of the scatter, the pools' floors the surface: the sinter terraces of a hot spring, dry or not.
     */
    TERRACES("terraces", 1.4),
    /** A narrow column of the fill, its top the surface, hollow at its top: a spring's chimney, a fumarole's. */
    CHIMNEY("chimney", 1.0),
    /**
     * Troughs cut into the ground from a pit at its middle, branching outwards as legs, their floors the surface, and
     * a fan of the scatter laid over the ground downwind of the pit, along its turn: the spiders gas carves under
     * Mars' seasonal ice, and the dark fans its jets leave.
     */
    SPIDER("spider", 1.6),
    /** Straight ridges of the fill crossing it at a few angles, sloping either side, topped by the surface. */
    RIDGES("ridges", 1.0),
    /**
     * A broad mound in bands of the band blocks, each band set back from the one under it, so its slopes climb in
     * steps; its top the surface: a mound of layered sediments.
     */
    LAYERED_MOUND("layered_mound", 1.0),
    /**
     * Caves under the ground: a chamber deep under its middle, opened to the sky by a crack over it, and winding
     * tunnels running out from it, coming out where they meet a steep slope or a wall, one rising to open on the ground
     * as a mouth, wherever the ground stands, a slope, a plain, a crater's wall; hollow with cave air, its rock left as
     * it was.
     */
    CAVE("cave", 1.15),
    /**
     * Grottos in whatever stands over the ground round its middle, a mesa, a butte, a mound's flank: tunnels running
     * out from its middle a block over its ground, some climbing as they go, hollowed only where they run through
     * rock, so each opens in a face, at its foot or up it; hollow with cave air, roofed.
     */
    GROTTO("grotto", 1.15),
    /**
     * A round shaft from the ground down into a chamber under it, as wide as its radius and as deep as its height
     * says, the light falling in and the rubble of its roof heaped on the chamber's floor under it, of the scatter.
     */
    PIT("pit", 2.6);

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

    /**
     * Whether it is laid on the ground as it stands when it is laid, the other landforms laid before it among it,
     * rather than the ground as the noise made it: a grotto, which opens in what other landforms raised, and so is
     * laid in a later step of the generation than they are.
     */
    public boolean onTheGroundAsItStands() {
        return this == GROTTO;
    }
}
