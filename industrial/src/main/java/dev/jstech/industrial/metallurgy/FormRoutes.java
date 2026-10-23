/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import java.util.List;
import java.util.Optional;

/**
 * Which machine makes which form of a metal, from what and with what yield, and what a player makes by hand at the
 * start: the plates of the Compressor and the Forge Hammer, the wire of the Wire Drawer and the Draw Plate, the dust
 * of the Macerator and the Mortar, the sheets and foil of the Rolling Mill. By hand always yields less
 * ({@link Yield#byHand()}). Rolling and drawing leave scraps of the metal they work, counted, never by chance: one in
 * so many times ({@link #SCRAP_SHARE}). A machine's recipes come from its line here once the machine is in the game.
 * The numbers are estimates, to be tuned in playtesting (industrial/docs/design/metallurgy.md).
 *
 * <p>Pure: no game types.
 */
public final class FormRoutes {

    /** The share of a scrap each rolling or drawing leaves: one scrap every eight. */
    public static final double SCRAP_SHARE = 0.125;
    /** How many nuggets an ingot is, and ingots a block: nine, as in the game. */
    public static final int NINE = 9;

    private static final List<FormRoute> ALL = List.of(
            machine("plate", "ingot", FormRoute.ByHand.FORGE_HAMMER, false,
                    way("compressor", 1, 1, 1), way("hydraulic_press", 2, 1, 1)),
            machine("sheet", "plate", FormRoute.ByHand.NONE, true, way("rolling_mill", 2, 1, 2)),
            machine("foil", "sheet", FormRoute.ByHand.NONE, true, way("rolling_mill", 2, 1, 4)),
            machine("wire", "ingot", FormRoute.ByHand.DRAW_PLATE, true,
                    way("wire_drawer", 1, 1, 2), way("industrial_wire_drawer", 3, 1, 2)),
            machine("coil", "wire", FormRoute.ByHand.CRAFTING_TABLE, false, way("coil_winder", 1, 4, 1)),
            machine("spring", "wire", FormRoute.ByHand.NONE, false, way("coil_winder", 1, 1, 1)),
            machine("rod", "ingot", FormRoute.ByHand.NONE, false, way("lathe", 1, 1, 1), way("extruder", 2, 1, 2)),
            machine("dust", "ingot", FormRoute.ByHand.MORTAR, false, way("macerator", 1, 1, 1)),
            table("nugget", "ingot", new Yield(1, NINE)),
            table("block", "ingot", new Yield(NINE, 1)));

    private FormRoutes() {
    }

    /** Every route, in the order of the forms. */
    public static List<FormRoute> all() {
        return ALL;
    }

    /** The route of {@code form}, by its id's path, or none. */
    public static Optional<FormRoute> of(final String form) {
        for (final FormRoute route : ALL) {
            if (route.form().equals(form)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    /* A form made by machines, and by hand as the first of them yields, less. */
    private static FormRoute machine(final String form, final String from, final FormRoute.ByHand hand,
                                     final boolean leavesScrap, final FormRoute.MachineWay... ways) {
        final Optional<Yield> byHand = hand == FormRoute.ByHand.NONE ? Optional.empty()
                : Optional.of(ways[0].yield().byHand());
        return new FormRoute(form, from, hand, byHand, List.of(ways), leavesScrap);
    }

    /* A form made at a crafting table only, as the game makes nuggets and blocks. */
    private static FormRoute table(final String form, final String from, final Yield yield) {
        return new FormRoute(form, from, FormRoute.ByHand.CRAFTING_TABLE, Optional.of(yield), List.of(), false);
    }

    private static FormRoute.MachineWay way(final String machine, final int tier, final int in, final int out) {
        return new FormRoute.MachineWay(machine, tier, new Yield(in, out), false);
    }
}
