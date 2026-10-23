/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

/**
 * The three levels of processing an ore, each optional and each better than the one before: the furnace smelts an "X
 * ore" and the simple minerals into an ingot, as the game does; the Macerator grinds any mineral into two dusts of what
 * it is mined for and a little of its byproducts; the family's own route, later, gives more and all of them. What
 * comes of a mineral's byproducts at each level is {@link #byproduct}: a share of the mineral's own, counted, never by
 * chance. The numbers are estimates, to be tuned in playtesting (industrial/docs/design/metallurgy.md).
 *
 * <p>Pure: no game types.
 */
public enum OreLevel {
    /** The furnace, at the start: one ingot, no byproducts. */
    FURNACE(1, 0.0),
    /** The Macerator: two dusts, and a quarter of the byproducts. */
    MACERATOR(2, 0.25),
    /** The family's own route: more, and every byproduct. */
    FAMILY_ROUTE(0, 1.0);

    private final int mainOut;
    private final double byproducts;

    OreLevel(final int mainOut, final double byproducts) {
        this.mainOut = mainOut;
        this.byproducts = byproducts;
    }

    /** How many of what the mineral is mined for come of one: an ingot, two dusts; none said yet for the third. */
    public int mainOut() {
        return this.mainOut;
    }

    /** The share of its byproducts the level gets out of a mineral, from none to all. */
    public double byproducts() {
        return this.byproducts;
    }

    /**
     * How much of a byproduct comes with each of a mineral at this level, as a share of an item: the byproduct's
     * share of the mineral over the main element's, for each item of the main element that comes out, times what the
     * level gets out. Chalcopyrite, as much sulfur as copper by mass, gives half a sulfur dust with each two copper
     * dusts the Macerator grinds.
     *
     * @param mainShare the main element's share of the mineral's mass
     * @param share     the byproduct's share
     * @param mainOut   how many of the main element come out
     */
    public double byproduct(final double mainShare, final double share, final int mainOut) {
        if (!(mainShare > 0.0) || !(share > 0.0)) {
            throw new IllegalArgumentException("a mineral's shares are above none");
        }
        return mainOut * share / mainShare * this.byproducts;
    }
}
