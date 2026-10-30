/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.shipyard.ShipyardPlans;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The holes in a drawn ship, on the player's game: the bones of its parts' models hidden for the blocks missing from
 * its formed parts, worked out again only when its holes change, for whatever draws it, its dock or its console.
 */
public final class ShipHoles {

    /* What each owner's ship's holes last were, and the bones they hide. */
    private static final Map<BlockEntity, Kept> KEPT = new WeakHashMap<>();

    private ShipHoles() {
    }

    /**
     * The bones the places {@code holes} of the plan of {@code design}, its frame turned as {@code turn} says, hide in
     * the ship {@code owner} draws: by the part's number, then its piece's model's name.
     */
    public static Map<Integer, Map<String, Set<String>>> of(final BlockEntity owner, final ShipDesign design,
                                                            final Orientation turn, final List<Integer> holes) {
        if (holes.isEmpty() || owner.getLevel() == null) {
            return Map.of();
        }
        final Kept kept = KEPT.get(owner);
        if (kept != null && kept.design().equals(design) && kept.turn().equals(turn) && kept.holes().equals(holes)) {
            return kept.bones();
        }
        final Map<Integer, Map<String, Set<String>>> bones = ShipyardPlans.holes(ShipyardPlans.of(owner.getLevel(),
                design, turn), holes);
        KEPT.put(owner, new Kept(design, turn, List.copyOf(holes), bones));
        return bones;
    }

    /* An owner's ship's holes as last worked out. */
    private record Kept(ShipDesign design, Orientation turn, List<Integer> holes,
                        Map<Integer, Map<String, Set<String>>> bones) {
    }
}
