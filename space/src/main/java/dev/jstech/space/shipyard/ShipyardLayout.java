/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * A design laid on a Small Shipyard's bay, block by block: its spine's sections, flipped as it says, and its hung
 * parts, from their kits; whether it fits the bay, every block over the pit, under the crane and clear of the dock;
 * and how many blocks it takes, the whole ship's and each part's.
 */
public final class ShipyardLayout {

    private ShipyardLayout() {
    }

    /** The blocks of the ship of {@code design}, its parts and their kits read from {@code parts} and {@code kits}. */
    public static ShipLayout.Laid laid(final Map<ResourceLocation, ShipPart> parts,
                                       final Map<ResourceLocation, ShipKit> kits, final ShipDesign design) {
        final List<ShipLayout.Section> spine = new ArrayList<>();
        for (int k = 0; k < design.spine().size(); k++) {
            final ResourceLocation id = design.spine().get(k);
            final ShipPart part = parts.get(id);
            final ShipKit kit = kits.get(id);
            spine.add(new ShipLayout.Section(part == null ? 0 : (int) part.length(),
                    kit == null ? List.of() : kit.pieces(), design.flips(k)));
        }
        final List<List<ShipKit.Piece>> hung = new ArrayList<>();
        for (final ResourceLocation id : design.hung()) {
            final ShipKit kit = kits.get(id);
            hung.add(kit == null ? List.of() : kit.pieces());
        }
        return ShipLayout.lay(spine, hung);
    }

    /** Whether the ship laid as {@code laid} fits the bay: none of its parts clashing, each block where room is. */
    public static boolean fits(final ShipLayout.Laid laid) {
        if (!laid.clashes().isEmpty()) {
            return false;
        }
        for (final ShipLayout.Cell cell : laid.cells().keySet()) {
            if (!ShipyardStructure.holds(cell.x(), cell.y(), cell.z())) {
                return false;
            }
        }
        return true;
    }

    /** How many blocks the kit {@code kit} lays: each of its pieces', twice for a piece set on both sides. */
    public static int blocks(final ShipKit kit) {
        int count = 0;
        for (final ShipKit.Piece piece : kit.pieces()) {
            int one = 0;
            for (final List<String> layer : piece.layers()) {
                for (final String row : layer) {
                    for (int x = 0; x < row.length(); x++) {
                        if (row.charAt(x) != ' ') {
                            one++;
                        }
                    }
                }
            }
            count += piece.mirrored() ? one * 2 : one;
        }
        return count;
    }
}
